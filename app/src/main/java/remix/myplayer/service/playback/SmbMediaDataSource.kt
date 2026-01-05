package remix.myplayer.service.playback

import android.media.MediaDataSource
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
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
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

@RequiresApi(Build.VERSION_CODES.M)
class SmbMediaDataSource(private val uriString: String) : MediaDataSource() {

  private var client: SMBClient? = null
  private var connection: Connection? = null
  private var session: Session? = null
  private var diskShare: DiskShare? = null
  private var file: File? = null
  private var fileSize: Long = -1

  private var lastReadEndPosition: Long = 0
  private var inputStream: InputStream? = null

  init {
    try {
      openFile()
    } catch (e: Exception) {
      Timber.e(e, "SmbMediaDataSource init failed")
      close()
    }
  }

  @Throws(IOException::class)
  private fun openFile() {
    val uri = Uri.parse(uriString)
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
      .withSigningRequired(false) // 禁用签名
      .withDfsEnabled(false)
      .withTimeout(60, TimeUnit.SECONDS)
      .withSoTimeout(60, TimeUnit.SECONDS)
      .build()

    client = SMBClient(config)

    connection = client?.connect(uri.host, if (uri.port != -1) uri.port else SMBClient.DEFAULT_PORT)
    val authContext = AuthenticationContext(username, password.toCharArray(), domain)
    session = connection?.authenticate(authContext)

    val pathSegments = uri.pathSegments
    if (pathSegments.isEmpty()) throw IOException("Invalid path")

    val shareName = pathSegments[0]
    val filePath = pathSegments.drop(1).joinToString("\\")

    diskShare = session?.connectShare(shareName) as? DiskShare
    if (diskShare == null) throw IOException("Connect share failed")

    val accessMask: MutableSet<AccessMask> = HashSet()
    accessMask.add(AccessMask.GENERIC_READ)

    val shareMode: MutableSet<SMB2ShareAccess> = HashSet()
    shareMode.add(SMB2ShareAccess.FILE_SHARE_READ)
    shareMode.add(SMB2ShareAccess.FILE_SHARE_WRITE)
    shareMode.add(SMB2ShareAccess.FILE_SHARE_DELETE)

    file = diskShare?.openFile(
      filePath,
      accessMask,
      null,
      shareMode,
      SMB2CreateDisposition.FILE_OPEN,
      null
    )

    fileSize = file?.fileInformation?.standardInformation?.endOfFile ?: -1

    val rawStream = file?.inputStream
    if (rawStream != null) {
      inputStream = BufferedInputStream(rawStream, 64 * 1024)
    }
  }

  override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
    if (position >= fileSize) return -1

    var bytesToRead = size
    if (position + size > fileSize) {
      bytesToRead = (fileSize - position).toInt()
    }

    synchronized(this) {
      try {
        // 如果需要回退，或者流没打开，必须重新打开
        if (position < lastReadEndPosition || inputStream == null) {
          try {
            inputStream?.close()
          } catch (e: Exception) {
          }
          // 重新获取流并包装
          val rawStream = file?.inputStream ?: return -1
          inputStream = BufferedInputStream(rawStream, 64 * 1024)
          lastReadEndPosition = 0
        }

        val skipAmount = position - lastReadEndPosition
        if (skipAmount > 0) {
          var remainingSkip = skipAmount
          while (remainingSkip > 0) {
            val skipped = inputStream?.skip(remainingSkip) ?: 0
            if (skipped <= 0) {
              // 某些情况下 skip 可能返回 0，尝试读取丢弃
              if (inputStream?.read() == -1) break
              remainingSkip--
            } else {
              remainingSkip -= skipped
            }
          }
        }

        val bytesRead = inputStream?.read(buffer, offset, bytesToRead) ?: -1
        if (bytesRead > 0) {
          lastReadEndPosition = position + bytesRead
        }
        return bytesRead
      } catch (e: Exception) {
        Timber.e(e, "SmbMediaDataSource readAt failed")
        return -1
      }
    }
  }

  override fun getSize(): Long = fileSize

  override fun close() {
    synchronized(this) {
      try {
        inputStream?.close()
      } catch (e: Exception) {
      }
      inputStream = null
      try {
        file?.close()
      } catch (e: Exception) {
      }
      file = null
      try {
        diskShare?.close()
      } catch (e: Exception) {
      }
      diskShare = null
      try {
        session?.close()
      } catch (e: Exception) {
      }
      session = null
      try {
        connection?.close()
      } catch (e: Exception) {
      }
      connection = null
      try {
        client?.close()
      } catch (e: Exception) {
      }
      client = null
    }
  }
}