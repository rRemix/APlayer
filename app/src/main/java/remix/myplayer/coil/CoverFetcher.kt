package remix.myplayer.coil

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import remix.myplayer.data.model.audio.APlayerModel

/** Read cached artwork before resolving a local source or searching the network. */
class CoverFetcher(
  private val options: Options,
  private val imageLoader: ImageLoader
) : Fetcher {
  override suspend fun fetch(): FetchResult? {
    if (!options.diskCachePolicy.readEnabled) return null
    val key = options.diskCacheKey ?: return null
    val cache = imageLoader.diskCache ?: return null
    val snapshot = cache.openSnapshot(key) ?: return null
    return SourceFetchResult(
      source = ImageSource(
        file = snapshot.data,
        fileSystem = cache.fileSystem,
        diskCacheKey = key,
        closeable = snapshot
      ),
      mimeType = null,
      dataSource = DataSource.DISK
    )
  }

  class Factory : Fetcher.Factory<APlayerModel> {
    override fun create(data: APlayerModel, options: Options, imageLoader: ImageLoader): Fetcher =
      CoverFetcher(options, imageLoader)
  }
}
