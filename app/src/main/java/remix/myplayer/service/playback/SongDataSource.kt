package remix.myplayer.service.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import kotlinx.coroutines.runBlocking
import okio.ByteString.Companion.encodeUtf8
import remix.myplayer.data.adapter.RemotePaths
import remix.myplayer.data.model.audio.Song
import remix.myplayer.repo.MusicSourceRepository
import remix.myplayer.repo.PlayableSource

/** Resolve the current access address on Media3's loader thread, before consulting byte caches. */
@UnstableApi
internal class SongDataSource(
  private val song: Song,
  private val resolve: suspend (Song) -> PlayableSource,
  private val upstreamFactory: DataSource.Factory,
  private val cacheProvider: () -> Cache?
) : DataSource {
  private var delegate: DataSource? = null
  private var requestedUri: Uri? = null
  private val listeners = mutableListOf<TransferListener>()

  override fun addTransferListener(listener: TransferListener) {
    listeners += listener
    delegate?.addTransferListener(listener)
  }

  override fun open(dataSpec: DataSpec): Long {
    val source = runBlocking { resolve(song) }
    val locator = if (song.isRemote()) RemotePaths.withoutCredentials(source.locator) else source.locator
    val uri = Uri.parse(locator)
    // Authentication and its URI exist only inside the upstream adapter, outside CacheDataSource.
    val upstream = DataSource.Factory { AuthenticatedUpstream(upstreamFactory.createDataSource(), source) }
    val actual = cacheProvider()?.let { cache ->
      CacheDataSource.Factory().setCache(cache).setUpstreamDataSourceFactory(upstream)
        .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR).createDataSource()
    } ?: upstream.createDataSource()
    delegate = actual
    requestedUri = uri
    listeners.forEach(actual::addTransferListener)
    return actual.open(dataSpec.buildUpon().setUri(uri).setKey(cacheKey(locator)).build())
  }

  private fun cacheKey(locator: String): String {
    val identity = song.getKey()
    // Source identity isolates accounts sharing a URL; the current address isolates replaced roots.
    return "audio-v2:" + "${identity.length}:$identity$locator".encodeUtf8().sha256().hex()
  }

  override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
    checkNotNull(delegate).read(buffer, offset, length)
  override fun getUri(): Uri? = requestedUri
  override fun getResponseHeaders(): Map<String, List<String>> = delegate?.responseHeaders.orEmpty()
  override fun close() {
    try { delegate?.close() } finally { delegate = null; requestedUri = null }
  }

  /** CacheDataSource must never see resolved SMB userInfo or persist a redirected secret URI. */
  private class AuthenticatedUpstream(
    private val delegate: DataSource,
    private val source: PlayableSource
  ) : DataSource by delegate {
    private var requestedUri: Uri? = null
    override fun open(dataSpec: DataSpec): Long {
      requestedUri = dataSpec.uri
      return delegate.open(dataSpec.buildUpon().setUri(source.locator)
        .setHttpRequestHeaders(dataSpec.httpRequestHeaders + source.headers).build())
    }
    override fun getUri(): Uri? = requestedUri
    override fun close() {
      try { delegate.close() } finally { requestedUri = null }
    }
  }

  companion object {
    fun factory(context: Context, song: Song, sources: MusicSourceRepository): DataSource.Factory {
      val appContext = context.applicationContext
      val upstream = DataSource.Factory {
        if (song.sourceId.value.startsWith("smb:")) SmbDataSource()
        else DefaultDataSource.Factory(appContext).createDataSource()
      }
      return DataSource.Factory {
        SongDataSource(song, sources::resolve, upstream) {
          if (song.isRemote()) MediaCache.get(appContext) else null
        }
      }
    }
  }
}
