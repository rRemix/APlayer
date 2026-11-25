package remix.myplayer.repo

import android.net.Uri
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import okhttp3.Credentials
import remix.myplayer.data.adapter.RemotePaths
import remix.myplayer.data.adapter.musicSource
import remix.myplayer.data.db.room.dao.SmbDao
import remix.myplayer.data.db.room.dao.WebDavDao
import remix.myplayer.data.model.audio.MusicSource
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.audio.SourceId
import remix.myplayer.data.remote.RemoteFile
import remix.myplayer.data.remote.RemoteFileSystemFactory
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.net.toUri

/** Adapts existing source configuration tables without copying credentials into Songs or queues. */
@Singleton
class MusicSourceRepository @Inject constructor(
  private val webDavDao: WebDavDao,
  private val smbDao: SmbDao,
  private val fileSystems: RemoteFileSystemFactory
) {
  fun observeSources() = combine(webDavDao.selectAll(), smbDao.selectAll()) { dav, smb ->
    dav.map { it.musicSource() } + smb.map { it.musicSource() }
  }.distinctUntilChanged()

  suspend fun get(id: SourceId): MusicSource? = when {
    id.value.startsWith("webdav:") -> id.value.substringAfter(':').toIntOrNull()?.let { webDavDao.byId(it)?.musicSource() }
    id.value.startsWith("smb:") -> id.value.substringAfter(':').toIntOrNull()?.let { smbDao.byId(it)?.musicSource() }
    else -> null
  }

  /** Retrieves the latest attributes of a remote file using the current source configuration. */
  suspend fun stat(song: Song): RemoteFile {
    val id = song.sourceId.value.substringAfter(':').toIntOrNull()
      ?: throw IOException("Invalid music source")
    return when {
      song.sourceId.value.startsWith("webdav:") -> {
        val source = webDavDao.byId(id) ?: throw IOException("Music source no longer exists")
        fileSystems.create(source).stat(song.sourceKey)
      }
      song.sourceId.value.startsWith("smb:") -> {
        val source = smbDao.byId(id) ?: throw IOException("Music source no longer exists")
        fileSystems.create(source).stat(song.sourceKey)
      }
      else -> throw IOException("Unsupported music source")
    }
  }

  /** Called at the I/O boundary so a changed server address/password takes effect on the next open. */
  suspend fun resolve(song: Song): PlayableSource {
    if (song.isLocal()) return PlayableSource(song.contentUri.toString())
    val id = song.sourceId.value.substringAfter(':').toIntOrNull()
      ?: throw IOException("Invalid music source")
    return when {
      song.sourceId.value.startsWith("webdav:") -> {
        val source = webDavDao.byId(id) ?: throw IOException("Music source no longer exists")
        PlayableSource(
          RemotePaths.resolve(source.musicSource().root, song.sourceKey),
          mapOf("Authorization" to Credentials.basic(source.account, source.pwd))
        )
      }
      song.sourceId.value.startsWith("smb:") -> {
        val source = smbDao.byId(id) ?: throw IOException("Music source no longer exists")
        val clean = RemotePaths.resolve(source.musicSource().root, song.sourceKey).toUri()
        val domain = source.domain?.takeIf { it.isNotEmpty() }?.let { "${Uri.encode(it)};" }.orEmpty()
        val userInfo = "$domain${Uri.encode(source.account)}:${Uri.encode(source.pwd)}"
        PlayableSource(clean.buildUpon().encodedAuthority("$userInfo@${clean.encodedAuthority}").build().toString())
      }
      else -> throw IOException("Unsupported music source")
    }
  }
}

/** Short-lived input to one media read. Never persist this value or use it as a cache key. */
class PlayableSource(val locator: String, val headers: Map<String, String> = emptyMap())
