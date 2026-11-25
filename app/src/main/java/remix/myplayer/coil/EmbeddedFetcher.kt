package remix.myplayer.coil

import android.media.MediaMetadataRetriever
import android.net.Uri as AndroidUri
import android.os.Build
import androidx.core.net.toUri
import coil3.ImageLoader
import coil3.Uri
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okio.Buffer
import remix.myplayer.data.adapter.readSongMetadata
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.audio.SongMetadata
import remix.myplayer.data.model.audio.SourceId
import remix.myplayer.repo.SongMetadataRepository
import remix.myplayer.service.playback.SmbMediaDataSource
import java.io.File
import java.io.IOException
import timber.log.Timber

class EmbeddedFetcher(
  private val uri: Uri,
  private val options: Options,
  private val imageLoader: ImageLoader,
  private val metadataRepository: SongMetadataRepository
) : Fetcher {
  override suspend fun fetch(): SourceFetchResult {
    currentCoroutineContext().ensureActive()
    val bytes = withContext(Dispatchers.IO) { readBytes(uri.toString().toUri()) }
    currentCoroutineContext().ensureActive()
    return SourceFetchResult(
      // Coil does not automatically persist bytes returned by this local fetcher.
      source = writeToDiskCache(bytes) ?: ImageSource(Buffer().write(bytes), options.fileSystem),
      mimeType = null,
      dataSource = DataSource.DISK
    )
  }

  /** [source] is either a local file path or a remote request carrying locator and credentials. */
  private suspend fun readBytes(source: AndroidUri): ByteArray {
    val locator = source.getQueryParameter(CoverUriResolver.PARAM_LOCATOR)
    val song = if (locator == null) null else remoteSong(source)
    var tags: SongMetadata? = null
    val retriever = MediaMetadataRetriever()
    var smb: SmbMediaDataSource? = null
    val embedded = try {
      if (locator != null) {
        if (locator.startsWith("smb:") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
          val dataSource = SmbMediaDataSource(locator)
          smb = dataSource
          retriever.setDataSource(dataSource)
        } else {
          val authorization = source.getQueryParameter(CoverUriResolver.PARAM_AUTHORIZATION)
          retriever.setDataSource(
            locator,
            authorization?.takeIf { it.isNotEmpty() }
              ?.let { mapOf(AUTHORIZATION to it) }.orEmpty()
          )
        }
      } else {
        retriever.setDataSource(source.path ?: throw IOException("Missing embedded artwork path"))
      }
      // Extract tags while this same retriever is open; failure must not break artwork.
      if (song != null) {
        tags = runCatching { retriever.readSongMetadata(song) }
          .onFailure { Timber.v(it) }.getOrNull()
      }
      retriever.embeddedPicture
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Timber.v(e)
      null
    } finally {
      runCatching { retriever.release() }
      runCatching { smb?.close() }
    }
    if (song != null && tags != null) {
      try {
        metadataRepository.cacheIfAbsent(song, tags)
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Timber.v(e)
      }
    }
    return embedded ?: when {
      locator != null -> throw IOException("No embedded artwork on remote file")
      else -> findDirectoryCover(requireNotNull(source.path)).readBytes()
    }
  }

  private fun remoteSong(uri: AndroidUri): Song? {
    val sourceId = uri.getQueryParameter(CoverUriResolver.PARAM_SOURCE_ID)
      ?.let(::SourceId)?.takeIf { it.isRemote } ?: return null
    val sourceKey = uri.getQueryParameter(CoverUriResolver.PARAM_SOURCE_KEY)
      ?.takeIf { it.isNotEmpty() } ?: return null
    val name = sourceKey.substringAfterLast('/')
    return Song(
      sourceId = sourceId, sourceKey = sourceKey, data = "", displayName = name,
      title = name.substringBeforeLast('.', name),
      size = uri.getQueryParameter(CoverUriResolver.PARAM_SIZE)?.toLongOrNull() ?: 0,
      dateModified = uri.getQueryParameter(CoverUriResolver.PARAM_MODIFIED)?.toLongOrNull() ?: 0
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

  class Factory(private val metadataRepository: SongMetadataRepository) : Fetcher.Factory<Uri> {
    override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? =
      if (data.scheme == CoverUriResolver.SCHEME_EMBEDDED) EmbeddedFetcher(data, options, imageLoader, metadataRepository)
      else null
  }

  companion object {
    private const val AUTHORIZATION = "Authorization"

    private val FALLBACKS = listOf(
      "cover.jpg", "album.jpg", "folder.jpg", "cover.png", "album.png", "folder.png"
    )
  }
}
