package remix.myplayer.service.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import timber.log.Timber
import java.io.EOFException
import java.io.IOException

class SmbDataSource : BaseDataSource(true) {

  private var client: SMBClient? = null
  private var connection: Connection? = null
  private var session: Session? = null
  private var diskShare: DiskShare? = null
  private var file: File? = null
  private var dataSpec: DataSpec? = null
  private var currentOffset: Long = 0

  override fun open(dataSpec: DataSpec): Long {
    this.dataSpec = dataSpec
    transferInitializing(dataSpec)
    try {
      val uri = dataSpec.uri
      val host = uri.host ?: throw IOException("Invalid host")
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

      client = SMBClient()
      connection = client?.connect(host)
      val authContext = AuthenticationContext(username, password.toCharArray(), domain)
      session = connection?.authenticate(authContext)
      
      val pathSegments = uri.pathSegments
      if (pathSegments.isEmpty()) throw IOException("Invalid path")
      val shareName = pathSegments[0]
      val filePath = pathSegments.drop(1).joinToString("\\")

      diskShare = session?.connectShare(shareName) as? DiskShare
      
      if (diskShare == null) throw IOException("Connect share failed")

      if (!diskShare!!.fileExists(filePath)) {
          throw IOException("File not found")
      }

      val smbFile = diskShare!!.openFile(
        filePath,
        setOf(com.hierynomus.mssmb2.SMB2ShareAccess.FILE_READ_DATA),
        null,
        setOf(com.hierynomus.mssmb2.SMB2ShareAccess.FILE_SHARE_READ),
        com.hierynomus.mssmb2.SMB2CreateDisposition.FILE_OPEN,
        null
      )
      file = smbFile

      val fileSize = smbFile.fileInformation.standardInformation.endOfFile
      
      if (dataSpec.position > fileSize) {
          throw IOException("Position out of range")
      }
      
      currentOffset = dataSpec.position
      
      bytesRemaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
        fileSize - dataSpec.position
      } else {
        dataSpec.length
      }
      
      isOpen = true
      transferStarted(dataSpec)
      return bytesRemaining
    } catch (e: Exception) {
      cleanup()
      throw IOException(e)
    }
  }

  override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
    if (readLength == 0) {
      return 0
    }
    if (bytesRemaining == 0L) {
      return C.RESULT_END_OF_INPUT
    }

    val bytesToRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
      readLength
    } else {
      minOf(bytesRemaining, readLength.toLong()).toInt()
    }

    try {
        val bytesRead = file!!.read(buffer, currentOffset, offset, bytesToRead)
        if (bytesRead > 0) {
            currentOffset += bytesRead
            if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
                bytesRemaining -= bytesRead
            }
            transferBytesTransferred(bytesRead)
            return bytesRead
        } else {
            return C.RESULT_END_OF_INPUT
        }
    } catch (e: Exception) {
        throw IOException(e)
    }
  }
    
  override fun getUri(): Uri? {
    return dataSpec?.uri
  }

  override fun close() {
    if (isOpen) {
      isOpen = false
      transferEnded()
    }
    cleanup()
  }
  
  private fun cleanup() {
      try {
          file?.close()
      } catch (e: Exception) {}
      file = null
      
      try {
          diskShare?.close()
      } catch (e: Exception) {}
      diskShare = null

      try {
          session?.close()
      } catch (e: Exception) {}
      session = null

      try {
          connection?.close()
      } catch (e: Exception) {}
      connection = null

      try {
          client?.close()
      } catch (e: Exception) {}
      client = null
  }
}
