package remix.myplayer.compose.lyric

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import remix.myplayer.bean.misc.LyricOrder
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.lyric.provider.ILyricsProvider
import remix.myplayer.compose.lyric.provider.IgnoredProvider
import remix.myplayer.compose.lyric.provider.StubProvider
import remix.myplayer.compose.prefs.LyricPrefs
import remix.myplayer.compose.prefs.delegate
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

// TODO 配置offset
@OptIn(ExperimentalSerializationApi::class)
@Singleton
class LyricSearcher @Inject constructor(
  @ApplicationContext
  private val context: Context,
  val lyricPrefs: LyricPrefs,
  private val providers: Set<@JvmSuppressWildcards ILyricsProvider>
) {

  // 同时作为默认顺序
//  private val PROVIDERS = listOf(
//    EmbeddedProvider,
//    IgnoredProvider,
//  )
  /*
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
    providers.forEach {
      map[it.id] = it
    }
    map
  }

  var generalProviders: List<ILyricsProvider>
    get() {
      val providers = ArrayList<ILyricsProvider>()
      try {
        lyricPrefs.generalLyricOrderList.map { it.name }.forEach { id ->
          ID_TO_PROVIDER[id]?.let {
            if (!providers.contains(it)) {
              providers.add(it)
            }
          }
        }
      } catch (t: Throwable) {
        Timber.tag(TAG).i(t, "Failed to get search order from preference")
      }
      return providers
    }
    set(value) {
      lyricPrefs.generalLyricOrder = Json.encodeToString(value.map { it.id })
    }

  private fun getCacheFile(storageKey: String, persistent: Boolean): File {
    val baseDir: File = context.run {
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
        var offset by lyricPrefs.sp.delegate("${LyricPrefs.KEY_OFFSET_PREFIX}${key}", 0L)
        return Pair(Json.decodeFromStream<List<LyricsLine>>(it.inputStream()), offset)
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

  private fun saveLyrics(song: Song, lyrics: List<LyricsLine>, persistent: Boolean) {
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

  private fun findProvider(lyricOrder: LyricOrder): ILyricsProvider {
    val provider = ID_TO_PROVIDER[lyricOrder.toString()]
    requireNotNull(provider)
    return provider
  }

  /**
   * 针对某一首歌曲，解析出搜索顺序
   * 一般情况是默认(内嵌-本地-酷狗-网易-QQ-忽略)
   * 如果用户手动选择了，则只搜索用户选择的
   */
  private fun resolveProviders(song: Song): List<ILyricsProvider> {
    val key = getStorageKey(song)
    var select by lyricPrefs.sp.delegate(
      "${LyricPrefs.KEY_SONG_PREFIX}${key}",
      LyricOrder.Def.toString()
    )
    var order = LyricOrder.valueOf(select)

    return if (order == LyricOrder.Def) {
      generalProviders
    } else {
      listOf(findProvider(order))
    }
  }

  /**
   * @param provider 由用户指定的歌词源（包括恢复默认）或 null
   */
  internal suspend fun getLyricsAndOffset(
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
    (if (provider != StubProvider && provider != null) listOf(provider) else resolveProviders(song)).forEach {
      Timber.tag(TAG).v("Trying provider: ${it.id}")
      try {
        return Pair(it.getLyrics(song).let { lyrics ->
          if (provider != null || it !is IgnoredProvider) {
            // Fallback 到 ignored 可能是因为网络等问题，如果缓存将会导致以后需要手动点击才能获取到歌词
            saveLyrics(song, lyrics, provider != null && provider !is StubProvider)
          }
          lyrics
        }, 0)
      } catch (t: Throwable) {
        Timber.tag(TAG).v(t, "Failed to get lyrics from provider `${it.id}`")
      }
    }
    clearCache(song)
    Timber.tag(TAG).i("Failed to get lyrics from any provider, returning empty list")
    return Pair(listOf(), 0)
  }

  companion object {

    private const val TAG = "LyricsSearcher"

    private const val CACHE_DIRECTORY_NAME = "lyrics"

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
