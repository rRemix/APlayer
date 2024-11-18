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
import java.security.MessageDigest

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
        Timber.tag(TAG).w(t, "Failed to get search order from preference")
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

  private fun getHashKey(song: Song): String {
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
    // 要作为文件名，安全起见保证输出长度不超过 127 字节，SHA-384 输出 96 字节
    val digest = MessageDigest.getInstance("SHA-384").digest(rawKey.toByteArray())
    return digest.fold("") { str, it -> str + "%02x".format(it) }
  }

  private fun getCacheFile(hashKey: String, persistent: Boolean): File {
    val baseDir: File = App.context.run {
      if (persistent) getExternalFilesDir(null) ?: filesDir
      else externalCacheDir ?: cacheDir
    }
    val dir = File(baseDir, CACHE_DIRECTORY_NAME)
    dir.mkdirs()
    return File(dir, hashKey)
  }

  private fun getCachedOrNull(song: Song): Pair<List<LyricsLine>, Int>? {
    val hashKey = getHashKey(song)
    listOf(true, false).map { getCacheFile(hashKey, it) }.forEach {
      try {
        return Pair(
          Json.decodeFromStream<List<LyricsLine>>(it.inputStream()), SPUtil.getValue(
            App.context, SPUtil.LYRICS_KEY.NAME, SPUtil.LYRICS_KEY.OFFSET_PREFIX + hashKey, 0
          )
        )
      } catch (t: Throwable) {
        Timber.tag(TAG).i(t, "Failed to get lyrics from cache $it")
      }
    }
    return null
  }

  private fun clearCache(song: Song) {
    val hashKey = getHashKey(song)
    listOf(true, false).map { getCacheFile(hashKey, it) }.forEach {
      it.delete()
    }
  }

  private fun saveLyrics(song: Song, lyrics: List<LyricsLine>, persistent: Boolean) {
    if (song == Song.EMPTY_SONG) {
      Timber.tag(TAG).e("Trying to save lyrics for empty song")
      return
    }
    Timber.tag(TAG).v("Saving lyrics to cache, song: $song")
    val hashKey = getHashKey(song)
    SPUtil.deleteValue(
      App.context, SPUtil.LYRICS_KEY.NAME, SPUtil.LYRICS_KEY.OFFSET_PREFIX + hashKey
    )
    if (!persistent) {
      getCacheFile(hashKey, true).delete()
    }
    try {
      val cacheFile = getCacheFile(hashKey, persistent)
      cacheFile.delete()
      cacheFile.createNewFile()
      Json.encodeToStream(lyrics, cacheFile.outputStream())
    } catch (t: Throwable) {
      Timber.tag(TAG).e(t, "Failed to save lyrics to cache")
    }
  }

  fun saveOffset(song: Song, offset: Int) {
    if (song == Song.EMPTY_SONG) {
      Timber.tag(TAG).e("Trying to save offset for empty song")
      return
    }
    Timber.tag(TAG).v("Saving offset, song: $song")
    val hashKey = getHashKey(song)
    SPUtil.putValue(
      App.context, SPUtil.LYRICS_KEY.NAME, SPUtil.LYRICS_KEY.OFFSET_PREFIX + hashKey, offset
    )
  }

  /**
   * @param provider 由用户指定的歌词源
   */
  fun getLyricsAndOffset(
    song: Song, provider: ILyricsProvider? = null
  ): Pair<List<LyricsLine>, Int> {
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
    listOfNotNull(provider).ifEmpty { order }.forEach {
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
