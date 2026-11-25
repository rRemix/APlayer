package remix.myplayer.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.data.adapter.RemotePaths
import remix.myplayer.data.adapter.song
import remix.myplayer.data.remote.RemoteBrowserSource
import remix.myplayer.data.remote.RemoteFile
import remix.myplayer.data.remote.RemoteFileSystem
import remix.myplayer.data.remote.RemoteFileSystemFactory
import remix.myplayer.repo.SmbRepository
import remix.myplayer.repo.SongMetadataRepository
import remix.myplayer.repo.WebDavRepository
import timber.log.Timber
import javax.inject.Inject

/** Scoped to a navigation entry; configuration dialogs stay in their original view models. */
@HiltViewModel
class RemoteBrowserViewModel @Inject constructor(
  private val fileSystems: RemoteFileSystemFactory,
  private val smbRepository: SmbRepository,
  private val webDavRepository: WebDavRepository,
  private val metadataRepository: SongMetadataRepository,
) : ViewModel() {

  private fun fileSystem(source: RemoteBrowserSource): RemoteFileSystem = when (source) {
    is RemoteBrowserSource.SmbSource -> fileSystems.create(source.config)
    is RemoteBrowserSource.WebDavSource -> fileSystems.create(source.config)
  }

  suspend fun list(source: RemoteBrowserSource, url: String): List<RemoteFile> {
    val files = fileSystem(source).list(RemotePaths.relativePath(source.root, url))
    try {
      metadataRepository.saveRemoteFiles(files.filter { it.isAudio }.map { file ->
        source.musicSource.song(file.path, file.name, file.size, file.lastModified)
      })
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      // A cache write failure must not make an otherwise successful directory listing fail.
      Timber.w(e, "Unable to cache remote file versions")
    }
    return files
  }

  suspend fun updateLastUrl(source: RemoteBrowserSource, url: String) {
    if (source.lastUrl == url) return
    when (source) {
      is RemoteBrowserSource.SmbSource -> withContext(Dispatchers.IO) {
        val config = source.config
        // Preserve the existing SMB repository's update behavior.
        config.lastUrl = url
        smbRepository.insertOrReplace(config)
      }
      is RemoteBrowserSource.WebDavSource -> webDavRepository.updateLastUrl(source.config, url)
    }
  }

  suspend fun delete(source: RemoteBrowserSource, file: RemoteFile) {
    when (source) {
      is RemoteBrowserSource.SmbSource -> {
        smbRepository.deleteResource(source.config, file.path, file.isDirectory) { key ->
          fileSystem(source).delete(key, file.isDirectory)
        }
      }
      is RemoteBrowserSource.WebDavSource -> {
        webDavRepository.deleteResource(source.config, file.path, file.isDirectory) { url ->
          fileSystem(source).delete(RemotePaths.relativePath(source.root, url), file.isDirectory)
        }
      }
    }
  }
}
