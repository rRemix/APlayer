package remix.myplayer.data.remote.smb

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.data.db.room.entity.Smb
import remix.myplayer.data.remote.RemoteFile
import remix.myplayer.data.remote.RemoteFileException
import remix.myplayer.data.remote.RemoteFileReader
import remix.myplayer.data.remote.RemoteReadableFileSystem
import remix.myplayer.data.remote.remotePath
import java.io.FileNotFoundException
import java.io.IOException

/** All methods operate on paths relative to a single SMB share. */
class SmbFileSystem private constructor(
  private val host: String,
  private val share: String,
  private val user: String,
  private val password: String,
  private val domain: String
) : RemoteReadableFileSystem {

  override suspend fun checkConnection() = withContext(Dispatchers.IO) {
    SmbNativeBridge.checkConnection(host, share, user, password, domain)
  }

  override suspend fun list(path: String): List<RemoteFile> = withContext(Dispatchers.IO) {
    try {
      SmbNativeBridge.list(host, share, user, password, domain, remotePath(path))
        .filter { it.isDirectory || it.isAudio }
    } catch (e: FileNotFoundException) { throw RemoteFileException.NotFound(e) }
  }

  override suspend fun stat(path: String): RemoteFile = withContext(Dispatchers.IO) {
    try { SmbNativeBridge.stat(host, share, user, password, domain, remotePath(path)) }
    catch (e: FileNotFoundException) { throw RemoteFileException.NotFound(e) }
  }

  override suspend fun delete(path: String, isDirectory: Boolean) = withContext(Dispatchers.IO) {
    val key = remotePath(path)
    require(key.isNotEmpty()) { "Cannot delete share root" }
    SmbNativeBridge.delete(host, share, user, password, domain, key, isDirectory)
  }

  override fun open(path: String): RemoteFileReader {
    val handle = SmbNativeBridge.open(host, share, user, password, domain, remotePath(path))
    return SmbFileReader(handle)
  }

  companion object {
    fun fromConfig(smb: Smb): SmbFileSystem {
      val uri = Uri.parse(if (smb.server.startsWith("smb://")) smb.server else "smb://${smb.server}")
      return make(uri, smb.share, smb.account, smb.pwd, smb.domain.orEmpty())
    }

    /** Playback URI produced by Smb.generateUri; decode user/password segments separately. */
    fun fromUrl(url: String): Pair<SmbFileSystem, String> {
      val uri = Uri.parse(url)
      require(uri.scheme == "smb" && !uri.host.isNullOrBlank()) { "Invalid SMB URL" }
      val segments = uri.pathSegments
      require(segments.isNotEmpty()) { "Missing SMB share" }
      val userInfo = uri.encodedUserInfo.orEmpty()
      val userPart = userInfo.substringBefore(':')
      val domain = if (';' in userPart) Uri.decode(userPart.substringBefore(';')) else ""
      val user = Uri.decode(userPart.substringAfter(';'))
      val password = if (':' in userInfo) Uri.decode(userInfo.substringAfter(':')) else ""
      val system = make(uri, segments.first(), user, password, domain)
      return system to remotePath(segments.drop(1).joinToString("/"))
    }

    private fun make(uri: Uri, share: String, user: String, password: String, domain: String): SmbFileSystem {
      require(uri.scheme == "smb" && !uri.host.isNullOrBlank()) { "Invalid SMB host" }
      val authority = uri.encodedAuthority.orEmpty().substringAfterLast('@')
      val explicitPort = if (authority.startsWith("[")) {
        authority.substringAfter(']', "").startsWith(":")
      } else {
        ':' in authority
      }
      val port = uri.port
      require((!explicitPort && port == -1) || port in 1..65535) { "Invalid SMB port" }
      require(share.isNotEmpty() && '/' !in share && '\\' !in share) { "Invalid SMB share" }

      // libsmb2 accepts `host:port` (or `[IPv6]:port`) in smb2_connect_share.
      val host = checkNotNull(uri.host)
      val hostForNative = if (':' in host && !host.startsWith("[")) "[$host]" else host
      val server = if (port == -1 || port == 445) hostForNative else "$hostForNative:$port"
      return SmbFileSystem(server, share, user, password, domain)
    }
  }
}

/** One isolated native session and file handle per playback or metadata reader. */
internal class SmbFileReader(private var handle: Long) : RemoteFileReader {
  override val size: Long = SmbNativeBridge.size(handle)
  override fun readAt(position: Long, buffer: ByteArray, offset: Int, length: Int): Int {
    require(position >= 0 && offset >= 0 && length >= 0 && offset <= buffer.size - length)
    val current = handle
    if (current == 0L) throw IOException("SMB reader is closed")
    if (length == 0) return 0
    return SmbNativeBridge.read(current, position, buffer, offset, length)
  }
  @Synchronized override fun close() {
    val current = handle
    handle = 0L
    if (current != 0L) SmbNativeBridge.close(current)
  }
}
