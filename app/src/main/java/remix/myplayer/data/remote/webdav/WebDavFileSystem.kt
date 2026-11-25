package remix.myplayer.data.remote.webdav

import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import com.thegrizzlylabs.sardineandroid.impl.SardineException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import remix.myplayer.data.adapter.RemotePaths
import remix.myplayer.data.adapter.WebDavDeleteInterceptor
import remix.myplayer.data.db.room.entity.WebDav
import remix.myplayer.data.remote.RemoteFile
import remix.myplayer.data.remote.RemoteFileException
import remix.myplayer.data.remote.RemoteFileSystem
import remix.myplayer.data.remote.remotePath
import java.util.concurrent.TimeUnit

/** Shared file operation contract; HTTP audio streaming stays on Media3's HTTP DataSource. */
class WebDavFileSystem(private val config: WebDav) : RemoteFileSystem {
  private val client by lazy {
    OkHttpSardine(OkHttpClient.Builder()
      .addInterceptor(WebDavDeleteInterceptor)
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .writeTimeout(20, TimeUnit.SECONDS)
      .build()).apply { setCredentials(config.account, config.pwd) }
  }

  override suspend fun checkConnection() = withContext(Dispatchers.IO) {
    client.list(config.getRoot()); Unit
  }

  override suspend fun list(path: String): List<RemoteFile> = withContext(Dispatchers.IO) {
    val dir = url(path)
    try {
      client.list(dir).map { resource ->
        RemoteFile(resource.name, remotePath(RemotePaths.relativePath(config.getRoot(), resource.path)),
          resource.isDirectory, resource.contentLength,
          resource.modified?.time ?: resource.creation?.time ?: 0L)
      }.filter { it.path != remotePath(path) && (it.isDirectory || it.isAudio) }
    } catch (e: SardineException) {
      if (e.statusCode == 404) throw RemoteFileException.NotFound(e)
      if (e.statusCode == 401 || e.statusCode == 403) throw RemoteFileException.PermissionDenied(e)
      throw e
    }
  }

  override suspend fun stat(path: String): RemoteFile = withContext(Dispatchers.IO) {
    val key = remotePath(path)
    val resource = try { client.list(url(key)).firstOrNull() } catch (e: SardineException) {
      if (e.statusCode == 404) throw RemoteFileException.NotFound(e)
      throw e
    } ?: throw RemoteFileException.NotFound()
    RemoteFile(resource.name, key, resource.isDirectory, resource.contentLength,
      resource.modified?.time ?: resource.creation?.time ?: 0L)
  }

  override suspend fun delete(path: String, isDirectory: Boolean) = withContext(Dispatchers.IO) {
    val key = remotePath(path)
    require(key.isNotEmpty()) { "Cannot delete WebDAV source root" }
    client.delete(url(key).let { if (isDirectory) "${it.trimEnd('/')}/" else it })
  }

  private fun url(path: String) = RemotePaths.resolve(config.getRoot(), remotePath(path))
}
