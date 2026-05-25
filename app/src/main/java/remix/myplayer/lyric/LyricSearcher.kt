package remix.myplayer.lyric

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.misc.LyricOrder
import remix.myplayer.data.prefs.LyricPrefs
import remix.myplayer.data.prefs.delegate
import remix.myplayer.lyric.provider.ILyricsProvider
import remix.myplayer.util.ext.checkWorkerThread
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@OptIn(ExperimentalSerializationApi::class)
@Singleton
class LyricSearcher @Inject constructor(
  @param:ApplicationContext
  private val context: Context,
  private val lyricPrefs: LyricPrefs,
  private val providers: Set<@JvmSuppressWildcards ILyricsProvider>
) {

  private fun getCacheFile(storageKey: String, persistent: Boolean): File {
    val dir = getCacheDir(persistent)
    dir.mkdirs()
    return File(dir, "$storageKey.json")
  }

  private fun getCacheDir(persistent: Boolean): File {
    val baseDir: File = context.run {
      if (persistent) getExternalFilesDir(null) ?: filesDir
      else externalCacheDir ?: cacheDir
    }
    return File(baseDir, CACHE_DIRECTORY_NAME)
  }

  private fun getCachedOrNull(song: Song): Pair<List<LyricLine>, Long>? {
    val key = getStorageKey(song)
    listOf(true, false).map { getCacheFile(key, it) }.forEach {
      try {
        var offset by lyricPrefs.sp.delegate("${LyricPrefs.KEY_OFFSET_PREFIX}${key}", 0L)
        return Pair(Json.decodeFromStream<List<LyricLine>>(it.inputStream()), offset)
      } catch (_: FileNotFoundException) {
      } catch (t: Throwable) {
        Timber.tag(TAG).i(t, "Failed to get lyrics from cache $it")
      }
    }
    return null
  }

  internal fun clearCache(song: Song) {
    val key = getStorageKey(song)
    listOf(true, false).map { getCacheFile(key, it) }.forEach {
      it.delete()
    }
  }

  internal fun clearAllCache(includePersistent: Boolean) {
    checkWorkerThread()
    getCacheDir(false).deleteRecursively()
    if (includePersistent) {
      getCacheDir(true).deleteRecursively()
    }
  }

  private fun saveLyrics(song: Song, lyrics: List<LyricLine>, persistent: Boolean) {
    if (song == Song.EMPTY_SONG) {
      Timber.tag(TAG).e("Trying to save lyrics for empty song")
      return
    }
    Timber.tag(TAG).v("Saving lyrics to cache, song: $song")
    val key = getStorageKey(song)
    lyricPrefs.remove("${LyricPrefs.KEY_OFFSET_PREFIX}${key}")
    if (!persistent) {
      getCacheFile(key, true).delete()
    }
    try {
      val cacheFile = getCacheFile(key, persistent)
      cacheFile.delete()
      cacheFile.createNewFile()
      Json.encodeToStream(lyrics, cacheFile.outputStream())
    } catch (t: Throwable) {
      Timber.tag(TAG).e(t, "Failed to save lyrics to cache")
    }
  }

  fun saveOffset(song: Song, offset: Long) {
    if (song == Song.EMPTY_SONG) {
      Timber.tag(TAG).e("Trying to save offset for empty song")
      return
    }
    Timber.tag(TAG).v("Saving offset: song=$song, offset=$offset")

    var delegate by lyricPrefs.sp.delegate(
      "${LyricPrefs.KEY_OFFSET_PREFIX}${getStorageKey(song)}",
      0L
    )
    delegate = offset
  }

  /**
   * 针对某一首歌曲，解析出搜索顺序
   * 一般情况是使用默认配置的顺序（通过 DefProvider）
   * 如果用户手动选择了，则只搜索用户选择的
   */
  private fun resolveProvider(song: Song): ILyricsProvider {
    val key = getStorageKey(song)
    val select by lyricPrefs.sp.delegate(
      "${LyricPrefs.KEY_SONG_PREFIX}${key}",
      LyricOrder.Def.toString()
    )

    return providers.first { it.id == select }
  }

  /**
   * @param specifyProvider 由用户指定的歌词源或 null（默认）
   */
  internal suspend fun getLyricsAndOffset(
    song: Song,
    specifyProvider: ILyricsProvider?
  ): Pair<List<LyricLine>, Long> {
    if (song == Song.EMPTY_SONG) {
      return Pair(listOf(), 0)
    }
    if (specifyProvider == null) {
      getCachedOrNull(song)?.let {
        Timber.tag(TAG).v("Got lyrics from cache, song: $song")
        return it
      }
    }
    Timber.tag(TAG).v("Searching lyrics for song: $song")

    (specifyProvider ?: resolveProvider(song)).let { p ->
      Timber.tag(TAG).v("Trying provider: ${p.id}")
      try {
        val ret = p.getLyrics(song)
        if (specifyProvider != null || ret.providerId != LyricOrder.Ignore.toString()) {
          // Fallback 到 ignored 可能是因为网络等问题，如果缓存将会导致以后需要手动点击才能获取到歌词
          saveLyrics(song, ret.data, specifyProvider != null)
        }
        return Pair(ret.data, 0)
      } catch (t: Throwable) {
        Timber.tag(TAG).v(t, "Failed to get lyrics from provider `${p.id}`")
      }
    }

    clearCache(song)
    Timber.tag(TAG).i("Failed to get lyrics from any provider, returning empty list")
    return Pair(listOf(), 0)
  }

  companion object {

    internal const val TAG = "LyricSearcher"

    private const val CACHE_DIRECTORY_NAME = "lyric"

    fun getStorageKey(song: Song): String {
      require(song != Song.EMPTY_SONG)
      val rawKey = Json.encodeToString(
        listOf(
          when (song) {
            is Song.Local -> "local"
            is Song.Remote -> "remote"
          },
          if (song is Song.Local) song.id.toString() else song.data,
          song.title,
          song.artist,
          song.album
        )
      )
      val msg = (if (song.album.isNotBlank()) "%1\$s-%2\$s-%3\$s" else "%1\$s-%3\$s").format(
        buildValidFilename(song.artist, 8),
        buildValidFilename(song.album, 8),
        buildValidFilename(song.title, 16)
      )
      val digest = MessageDigest.getInstance("SHA-1")
        .digest(rawKey.toByteArray())
        .fold("") { str, it -> str + "%02x".format(it) }
      return "$msg-$digest"
    }

    // See isValidFatFilenameChar in frameworks/base/core/java/android/os/FileUtils.java
    private fun isValidFilenameChar(c: Char): Boolean {
      return !(c.code in 0x00..0x1f || c.code == 0x7f || listOf(
        '"', '*', '/', ':', '<', '>', '?', '\\', '|'
      ).contains(c))
    }

    private fun buildValidFilename(name: String, maxLength: Int): String {
      val builder = StringBuilder(min(name.length, maxLength))
      for (c in name) {
        if (builder.length == maxLength) {
          builder.replace(maxLength - 1, maxLength, "~")
          break
        }
        builder.append(if (isValidFilenameChar(c) && !c.isWhitespace() && c != '-') c else '_')
      }
      return builder.toString()
    }
  }
}
