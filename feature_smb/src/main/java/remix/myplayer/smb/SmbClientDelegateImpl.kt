package remix.myplayer.smb

import androidx.annotation.Keep
import com.hierynomus.mserref.NtStatus
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.data.db.room.entity.Smb
import remix.myplayer.data.model.smb.SmbClientDelegate
import remix.myplayer.data.model.smb.SmbException
import remix.myplayer.data.model.smb.SmbFile

@Keep
class SmbClientDelegateImpl : SmbClientDelegate {

  override suspend fun listFiles(smb: Smb, url: String): List<SmbFile> =
    withContext(Dispatchers.IO) {
      try {
        SMBClient().use { client ->
          val (host, port) = Smb.parseServerAddress(smb.server)
          val connection = if (port != null) client.connect(host, port) else client.connect(host)
          connection.use {
            val authContext = AuthenticationContext(smb.account, smb.pwd.toCharArray(), smb.domain)
            val session = connection.authenticate(authContext)
            session.use {
              val diskShare = session.connectShare(smb.share) as DiskShare
              diskShare.use { share ->
                val relativePath = smb.getRelativePath(url).replace('/', '\\')

                val fileInfos = share.list(relativePath)
                fileInfos.map {
                  val fileName = it.fileName
                  val fileRelativePath =
                    if (relativePath.isEmpty()) fileName else "$relativePath\\$fileName"
                  SmbFile(
                    name = fileName,
                    isDirectory = (it.fileAttributes and 16L) != 0L,
                    path = fileRelativePath.replace('\\', '/'),
                    size = it.endOfFile,
                    lastModified = it.changeTime.toEpochMillis()
                  )
                }.filter { it.name != "." && it.name != ".." }.filter {
                  it.isDirectory || it.isAudio
                }
              }
            }
          }
        }
      } catch (e: SMBApiException) {
        throw SmbException(
          e.message, e, e.status == NtStatus.STATUS_OBJECT_NAME_NOT_FOUND ||
              e.status == NtStatus.STATUS_OBJECT_PATH_NOT_FOUND
        )
      } catch (e: Exception) {
        throw SmbException(e.message, e)
      }
    }

  override suspend fun checkConnection(smb: Smb) {
    withContext(Dispatchers.IO) {
      SMBClient().use { client ->
        val (host, port) = Smb.parseServerAddress(smb.server)
        val connection = if (port != null) client.connect(host, port) else client.connect(host)
        connection.use {
          val authContext = AuthenticationContext(smb.account, smb.pwd.toCharArray(), smb.domain)
          val session = connection.authenticate(authContext)
          session.use {
            val diskShare = session.connectShare(smb.share) as DiskShare
            diskShare.use {
              // Just to check if connection works
            }
          }
        }
      }
    }
  }
}
