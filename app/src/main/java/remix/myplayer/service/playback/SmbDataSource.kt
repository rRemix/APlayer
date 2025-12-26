package remix.myplayer.service.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
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
import java.util.HashSet
import java.util.concurrent.TimeUnit

class SmbDataSource : BaseDataSource(true) {

    private var client: SMBClient? = null
    private var connection: Connection? = null
    private var session: Session? = null
    private var diskShare: DiskShare? = null
    private var file: File? = null

    // 使用缓冲流来减少网络 IO 次数
    private var bufferedInputStream: InputStream? = null

    private var dataSpec: DataSpec? = null
    private var bytesRemaining: Long = 0
    private var opened: Boolean = false

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

            // 禁用签名(Signing)可以显著提高传输速度（局域网通常安全）。
            // 增大缓冲区和超时时间。
            val config = SmbConfig.builder()
                .withTimeout(120, TimeUnit.SECONDS)
                .withSoTimeout(180, TimeUnit.SECONDS)
                .withSigningRequired(false) // 关闭签名验证
                .withDfsEnabled(false)      // 如果不用 DFS，关闭可加快连接
                .build()

            client = SMBClient(config)
            connection = client?.connect(host)
            val authContext = AuthenticationContext(username, password.toCharArray(), domain)
            session = connection?.authenticate(authContext)

            val pathSegments = uri.pathSegments
            if (pathSegments.isEmpty()) throw IOException("Invalid path")

            val shareName = pathSegments[0]
            val filePath = pathSegments.drop(1).joinToString("\\")

            Timber.d("SMB Connecting (Buffered): Host=$host, Path=$filePath")

            diskShare = session?.connectShare(shareName) as? DiskShare
            if (diskShare == null) throw IOException("Connect share failed")

            if (!diskShare!!.fileExists(filePath)) {
                throw IOException("File not found: $filePath")
            }

            val accessMask = setOf(
                AccessMask.FILE_READ_DATA,
                AccessMask.FILE_READ_ATTRIBUTES,
                AccessMask.FILE_READ_EA
            )
            val shareMode = setOf(
                SMB2ShareAccess.FILE_SHARE_READ,
                SMB2ShareAccess.FILE_SHARE_WRITE,
                SMB2ShareAccess.FILE_SHARE_DELETE
            )

            val smbFile = diskShare!!.openFile(
                filePath,
                accessMask,
                null,
                shareMode,
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
            file = smbFile

            val fileSize = smbFile.fileInformation.standardInformation.endOfFile

            if (dataSpec.position > fileSize) {
                throw IOException("Position out of range")
            }

            // 创建缓冲输入流
            // 默认 smbj 的 read 是一次网络请求对应一次读取。
            // 这里我们用 64KB 的 Buffer，意味着每 64KB 数据才请求一次网络，
            // 哪怕 ExoPlayer 每次只读 1KB，也会非常快。
            val rawStream = smbFile.inputStream
            // skip 到指定位置 (断点续传或 Seek)
            if (dataSpec.position > 0) {
                rawStream.skip(dataSpec.position)
            }
            // 包装流
            bufferedInputStream = BufferedInputStream(rawStream, 64 * 1024)

            bytesRemaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
                fileSize - dataSpec.position
            } else {
                dataSpec.length
            }

            opened = true
            transferStarted(dataSpec)
            return bytesRemaining
        } catch (e: Exception) {
            Timber.e(e, "SMB Open Failed")
            cleanup()
            if (e is IOException) throw e else throw IOException(e)
        }
    }

    override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
        if (readLength == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        try {
            val bytesToRead = if (bytesRemaining == C.LENGTH_UNSET.toLong()) {
                readLength
            } else {
                minOf(bytesRemaining, readLength.toLong()).toInt()
            }

            // 使用 bufferedInputStream 读取
            val bytesRead = bufferedInputStream!!.read(buffer, offset, bytesToRead)

            if (bytesRead > 0) {
                if (bytesRemaining != C.LENGTH_UNSET.toLong()) {
                    bytesRemaining -= bytesRead
                }
                bytesTransferred(bytesRead)
                return bytesRead
            } else {
                return C.RESULT_END_OF_INPUT
            }
        } catch (e: Exception) {
            Timber.e(e, "SMB Read Failed")
            throw IOException(e)
        }
    }

    override fun getUri(): Uri? = dataSpec?.uri

    override fun close() {
        if (opened) {
            opened = false
            transferEnded()
        }
        cleanup()
    }

    private fun cleanup() {
        try { bufferedInputStream?.close() } catch (e: Exception) {}
        bufferedInputStream = null

        try { file?.close() } catch (e: Exception) {}
        file = null
        try { diskShare?.close() } catch (e: Exception) {}
        diskShare = null
        try { session?.close() } catch (e: Exception) {}
        session = null
        try { connection?.close() } catch (e: Exception) {}
        connection = null
        try { client?.close() } catch (e: Exception) {}
        client = null
    }
}