package remix.myplayer.lyrics

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import remix.myplayer.App
import remix.myplayer.bean.mp3.Song
import remix.myplayer.lyrics.provider.EmbeddedProvider
import remix.myplayer.lyrics.provider.ILyricsProvider
import remix.myplayer.lyrics.provider.IgnoredProvider
import remix.myplayer.lyrics.provider.StubProvider
import remix.myplayer.util.SPUtil
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import kotlin.math.min

@OptIn(ExperimentalSerializationApi::class)
object LyricsSearcher {
  private const val TAG = "LyricsSearcher"

  private const val CACHE_DIRECTORY_NAME = "lyrics"

  // 同时作为默认顺序
  private val PROVIDERS = listOf(
    EmbeddedProvider,
    IgnoredProvider,
  )/*
   old:
    DEF(0, App.context.getString(R.string.default_lyric_priority)),
    IGNORE(1, App.context.getString(R.string.ignore_lrc)),
    EMBEDDED(2, App.context.getString(R.string.embedded_lyric)),
    LOCAL(3, App.context.getString(R.string.local)),
    KUGOU(4, App.context.getString(R.string.kugou)),
    NETEASE(5, App.context.getString(R.string.netease)),
    QQ(6, App.context.getString(R.string.qq)),
    MANUAL(7, App.context.getString(R.string.select_lrc));
   */

  private val ID_TO_PROVIDER: Map<String, ILyricsProvider> = run {
    val map = HashMap<String, ILyricsProvider>()
    PROVIDERS.forEach {
      map[it.id] = it
    }
    map
  }

  var order: List<ILyricsProvider>
    get() {
      val providers = ArrayList<ILyricsProvider>()
      try {
        Json.decodeFromString<List<String>>(
          SPUtil.getValue(
            App.context, SPUtil.LYRICS_KEY.NAME, SPUtil.LYRICS_KEY.ORDER, ""
          )
        ).forEach { id ->
          ID_TO_PROVIDER[id]?.let {
            if (!providers.contains(it)) {
              providers.add(it)
            }
          }
        }
      } catch (t: Throwable) {
        Timber.tag(TAG).i(t, "Failed to get search order from preference")
      }
      PROVIDERS.forEach {
        if (!providers.contains(it)) {
          providers.add(it)
        }
      }
      return providers
    }
    set(value) {
      SPUtil.putValue(
        App.context,
        SPUtil.LYRICS_KEY.NAME,
        SPUtil.LYRICS_KEY.ORDER,
        Json.encodeToString(value.map { it.id })
      )
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

  private fun getStorageKey(song: Song): String {
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

  private fun getCacheFile(storageKey: String, persistent: Boolean): File {
    val baseDir: File = App.context.run {
      if (persistent) getExternalFilesDir(null) ?: filesDir
      else externalCacheDir ?: cacheDir
    }
    val dir = File(baseDir, CACHE_DIRECTORY_NAME)
    dir.mkdirs()
    return File(dir, "$storageKey.json")
  }

  private fun getCachedOrNull(song: Song): Pair<List<LyricsLine>, Long>? {
    val key = getStorageKey(song)
    listOf(true, false).map { getCacheFile(key, it) }.forEach {
      try {
        return Pair(
          Json.decodeFromStream<List<LyricsLine>>(it.inputStream()), SPUtil.getValue(
            App.context, SPUtil.LYRICS_KEY.NAME, SPUtil.LYRICS_KEY.OFFSET_PREFIX + key, 0L
          )
        )
      } catch (_: FileNotFoundException) {
      } catch (t: Throwable) {
        Timber.tag(TAG).i(t, "Failed to get lyrics from cache $it")
      }
    }
    return null
  }

  private fun clearCache(song: Song) {
    val key = getStorageKey(song)
    listOf(true, false).map { getCacheFile(key, it) }.forEach {
      it.delete()
    }
  }

  private fun saveLyrics(song: Song, lyrics: List<LyricsLine>, persistent: Boolean) {
    if (song == Song.EMPTY_SONG) {
      Timber.tag(TAG).e("Trying to save lyrics for empty song")
      return
    }
    Timber.tag(TAG).v("Saving lyrics to cache, song: $song")
    val key = getStorageKey(song)
    SPUtil.deleteValue(
      App.context, SPUtil.LYRICS_KEY.NAME, SPUtil.LYRICS_KEY.OFFSET_PREFIX + key
    )
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
    val key = getStorageKey(song)
    SPUtil.putValue(
      App.context, SPUtil.LYRICS_KEY.NAME, SPUtil.LYRICS_KEY.OFFSET_PREFIX + key, offset
    )
  }

  /**
   * @param provider 由用户指定的歌词源（包括恢复默认）或 null
   */
  suspend fun getLyricsAndOffset(
    song: Song,
    provider: ILyricsProvider?
  ): Pair<List<LyricsLine>, Long> {
    if (song == Song.EMPTY_SONG) {
      return Pair(listOf(), 0)
    }
    if (provider == null) {
      getCachedOrNull(song)?.let {
        Timber.tag(TAG).v("Got lyrics from cache, song: $song")
        return it
      }
    }
    Timber.tag(TAG).v("Searching lyrics for song: $song")
    (if (provider != StubProvider && provider != null) listOf(provider) else order).forEach {
      Timber.tag(TAG).v("Trying provider: ${it.id}")
      try {
        return Pair(it.getLyrics(song).let { lyrics ->
          if (provider != null || it != IgnoredProvider) {
            // Fallback 到 ignored 可能是因为网络等问题，如果缓存将会导致以后需要手动点击才能获取到歌词
            saveLyrics(song, lyrics, provider != null && provider !is StubProvider)
          }
          lyrics
        }, 0)
      } catch (t: Throwable) {
        Timber.tag(TAG).i(t, "Failed to get lyrics from provider `${it.id}`")
      }
    }
    clearCache(song)
    Timber.tag(TAG).i("Failed to get lyrics from any provider, returning empty list")
    return Pair(listOf(), 0)
  }
}
