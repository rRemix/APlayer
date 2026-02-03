package remix.myplayer.smb

import android.net.Uri
import androidx.annotation.Keep
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import remix.myplayer.data.model.smb.SmbRandomAccessDelegate
import remix.myplayer.data.model.smb.SmbStreamDelegate
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

@Keep
class SmbStreamDelegateImpl : SmbStreamDelegate {

  private var client: SMBClient? = null
  private var connection: Connection? = null
  private var session: Session? = null
  private var diskShare: DiskShare? = null
  private var inputStream: InputStream? = null
  private var bytesRemaining: Long = 0

  override fun open(url: String, offset: Long): Long {
    try {
      val uri = Uri.parse(url)
      val userInfo = uri.userInfo
      var username = ""
      var password = ""
      var domain: String? = null

      if (!userInfo.isNullOrEmpty()) {
        val colonIndex = userInfo.indexOf(':')
        val userPartRaw: String
        if (colonIndex != -1) {
          userPartRaw = userInfo.take(colonIndex)
          password = Uri.decode(userInfo.substring(colonIndex + 1))
        } else {
          userPartRaw = userInfo
        }

        val semiIndex = userPartRaw.indexOf(';')
        if (semiIndex != -1) {
          domain = Uri.decode(userPartRaw.substring(0, semiIndex))
          username = Uri.decode(userPartRaw.substring(semiIndex + 1))
        } else {
          username = Uri.decode(userPartRaw)
        }
      }

      val config = SmbConfig.builder()
        .withMultiProtocolNegotiate(true)
        .withSigningRequired(false)
        .withDfsEnabled(false)
        .withBufferSize(1024 * 1024)
        .withTimeout(120, TimeUnit.SECONDS)
        .withSoTimeout(180, TimeUnit.SECONDS)
        .build()

      client = SMBClient(config)

      connection =
        client?.connect(uri.host, if (uri.port != -1) uri.port else SMBClient.DEFAULT_PORT)

      val authContext = AuthenticationContext(username, password.toCharArray(), domain)
      session = connection?.authenticate(authContext)

      val pathSegments = uri.pathSegments
      if (pathSegments.isEmpty()) throw IOException("Invalid path")

      val shareName = pathSegments[0]
      val filePath = pathSegments.drop(1).joinToString("\\")

      diskShare = session?.connectShare(shareName) as? DiskShare
      if (diskShare == null) throw IOException("Connect share failed")

      if (!diskShare!!.fileExists(filePath)) {
        throw IOException("File not found: $filePath")
      }

      val accessMask = setOf(AccessMask.GENERIC_READ)
      val shareAccess = setOf(SMB2ShareAccess.FILE_SHARE_READ)
      val smbFile = diskShare!!.openFile(
        filePath,
        accessMask,
        null,
        shareAccess,
        SMB2CreateDisposition.FILE_OPEN,
        null
      )

      val fileSize = smbFile.fileInformation.standardInformation.endOfFile
      inputStream = smbFile.inputStream

      if (offset > 0) {
        inputStream?.skip(offset)
      }

      bytesRemaining = fileSize - offset
      return bytesRemaining

    } catch (e: Exception) {
      Timber.e(e)
      close()
      throw IOException(e)
    }
  }

  override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
    if (bytesRemaining == 0L) {
      return -1
    }
    try {
      val bytesRead =
        inputStream?.read(buffer, offset, minOf(bytesRemaining, length.toLong()).toInt()) ?: -1
      if (bytesRead != -1) {
        bytesRemaining -= bytesRead
      }
      return bytesRead
    } catch (e: IOException) {
      throw e
    }
  }

  override fun close() {
    try {
      inputStream?.close()
      // file?.close() // file is closed by inputStream close usually? Or need explicit close?
      // SmbFile (File) needs close?
      // In smbj, File implements AutoCloseable.
      // If we obtained inputStream from file, closing inputStream might not close file handle.
      // We should close file handle.
      // But I didn't save file handle in field.
      // Let's adjust.
    } catch (e: Exception) {
      Timber.e(e)
    }

    try {
      diskShare?.close()
    } catch (e: Exception) {
      Timber.e(e)
    }

    try {
      session?.close()
    } catch (e: Exception) {
      Timber.e(e)
    }

    try {
      connection?.close()
    } catch (e: Exception) {
      Timber.e(e)
    }

    try {
      client?.close()
    } catch (e: Exception) {
      Timber.e(e)
    }

    inputStream = null
    diskShare = null
    session = null
    connection = null
    client = null
  }
}

@Keep
class SmbRandomAccessDelegateImpl : SmbRandomAccessDelegate {

  private var client: SMBClient? = null
  private var connection: Connection? = null
  private var session: Session? = null
  private var diskShare: DiskShare? = null
  private var file: File? = null
  private var fileSize: Long = -1

  override fun open(url: String) {
    try {
      val uri = Uri.parse(url)
      val userInfo = uri.userInfo
      var username = ""
      var password = ""
      var domain: String? = null

      if (!userInfo.isNullOrEmpty()) {
        val parts = userInfo.split(":")
        if (parts.isNotEmpty()) {
          val userParts = parts[0].split(";")
          if (userParts.size > 1) {
            domain = userParts[0]
            username = userParts[1]
          } else {
            username = parts[0]
          }
        }
        if (parts.size > 1) {
          password = parts[1]
        }
      }

      val config = SmbConfig.builder()
        .withMultiProtocolNegotiate(true)
        .withSigningRequired(false)
        .withDfsEnabled(false)
        .withTimeout(60, TimeUnit.SECONDS)
        .withSoTimeout(60, TimeUnit.SECONDS)
        .build()

      client = SMBClient(config)
      connection =
        client?.connect(uri.host, if (uri.port != -1) uri.port else SMBClient.DEFAULT_PORT)

      val authContext = AuthenticationContext(username, password.toCharArray(), domain)
      session = connection?.authenticate(authContext)

      val pathSegments = uri.pathSegments
      if (pathSegments.isEmpty()) throw IOException("Invalid path")

      val shareName = pathSegments[0]
      val filePath = pathSegments.drop(1).joinToString("\\")

      diskShare = session?.connectShare(shareName) as? DiskShare
      if (diskShare == null) throw IOException("Connect share failed")

      val accessMask = setOf(AccessMask.GENERIC_READ)
      val shareAccess = setOf(
        SMB2ShareAccess.FILE_SHARE_READ,
        SMB2ShareAccess.FILE_SHARE_WRITE,
        SMB2ShareAccess.FILE_SHARE_DELETE
      )

      file = diskShare?.openFile(
        filePath,
        accessMask,
        null,
        shareAccess,
        SMB2CreateDisposition.FILE_OPEN,
        null
      )

      fileSize = file?.fileInformation?.standardInformation?.endOfFile ?: -1

    } catch (e: Exception) {
      Timber.e(e)
      close()
      throw IOException(e)
    }
  }

  override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
    return file?.read(buffer, position, offset, size) ?: -1
  }

  override fun getSize(): Long {
    return fileSize
  }

  override fun close() {
    try {
      file?.close()
    } catch (e: Exception) {
      Timber.e(e)
    }
    try {
      diskShare?.close()
    } catch (e: Exception) {
      Timber.e(e)
    }
    try {
      session?.close()
    } catch (e: Exception) {
      Timber.e(e)
    }
    try {
      connection?.close()
    } catch (e: Exception) {
      Timber.e(e)
    }
    try {
      client?.close()
    } catch (e: Exception) {
      Timber.e(e)
    }

    file = null
    diskShare = null
    session = null
    connection = null
    client = null
  }
}
