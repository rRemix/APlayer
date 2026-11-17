package remix.myplayer.coil

import android.media.MediaMetadataRetriever
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okio.Buffer
import java.io.File
import java.io.IOException
import timber.log.Timber

class EmbeddedFetcher(
  private val uri: Uri,
  private val options: Options,
  private val imageLoader: ImageLoader
) : Fetcher {
  override suspend fun fetch(): SourceFetchResult {
    currentCoroutineContext().ensureActive()
    val path = uri.toString().toUri().path
      ?: throw IOException("Missing embedded artwork path")
    val retriever = MediaMetadataRetriever()
    val embedded = try {
      retriever.setDataSource(path)
      retriever.embeddedPicture
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Timber.v(e)
      null
    } finally {
      retriever.release()
    }
    currentCoroutineContext().ensureActive()
    val bytes = embedded ?: findDirectoryCover(path).readBytes()
    currentCoroutineContext().ensureActive()
    return SourceFetchResult(
      // Coil does not automatically persist bytes returned by this local fetcher.
      source = writeToDiskCache(bytes) ?: ImageSource(Buffer().write(bytes), options.fileSystem),
      mimeType = null,
      dataSource = DataSource.DISK
    )
  }

  private fun findDirectoryCover(path: String): File {
    val directory = File(path).parentFile ?: throw IOException("Missing cover directory")
    for (name in FALLBACKS) {
      val file = File(directory, name)
      if (file.isFile) return file
    }
    throw IOException("No embedded or directory cover")
  }

  private fun writeToDiskCache(bytes: ByteArray): ImageSource? {
    if (!options.diskCachePolicy.writeEnabled) return null
    val key = options.diskCacheKey ?: return null
    val cache = imageLoader.diskCache ?: return null
    val editor = cache.openEditor(key) ?: return null
    return try {
      cache.fileSystem.write(editor.metadata) { }
      cache.fileSystem.write(editor.data) { write(bytes) }
      val snapshot = editor.commitAndOpenSnapshot() ?: return null
      ImageSource(
        file = snapshot.data,
        fileSystem = cache.fileSystem,
        diskCacheKey = key,
        closeable = snapshot
      )
    } catch (e: IOException) {
      runCatching { editor.abort() }
      Timber.v(e)
      null
    }
  }

  class Factory : Fetcher.Factory<Uri> {
    override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? =
      if (data.scheme == CoverUriResolver.SCHEME_EMBEDDED) EmbeddedFetcher(data, options, imageLoader)
      else null
  }

  companion object {
    private val FALLBACKS = listOf(
      "cover.jpg", "album.jpg", "folder.jpg", "cover.png", "album.png", "folder.png"
    )
  }
}
