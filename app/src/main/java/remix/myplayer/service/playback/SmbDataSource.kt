package remix.myplayer.service.playback

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import remix.myplayer.data.remote.RemoteFileReader
import remix.myplayer.data.remote.smb.SmbFileSystem
import java.io.IOException

@UnstableApi
class SmbDataSource : BaseDataSource(true) {
  private var reader: RemoteFileReader? = null
  private var uri: Uri? = null
  private var position: Long = 0
  private var remaining: Long = 0
  private var started = false

  override fun open(dataSpec: DataSpec): Long {
    close()
    transferInitializing(dataSpec)
    val (system, path) = SmbFileSystem.fromUrl(dataSpec.uri.toString())
    val opened = system.open(path)
    try {
      if (dataSpec.position > opened.size) throw IOException("Start position beyond end of file")
      val available = opened.size - dataSpec.position
      remaining = if (dataSpec.length == -1L) available else minOf(available, dataSpec.length)
      position = dataSpec.position
      uri = dataSpec.uri
      reader = opened
      transferStarted(dataSpec)
      started = true
      return remaining
    } catch (e: Exception) {
      opened.close()
      throw e
    }
  }

  override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
    if (length == 0) return 0
    if (remaining == 0L) return -1
    val bytes = checkNotNull(reader).readAt(position, buffer, offset, minOf(length.toLong(), remaining).toInt())
    if (bytes > 0) {
      position += bytes
      remaining -= bytes
      bytesTransferred(bytes)
    }
    return bytes
  }

  override fun getUri(): Uri? = uri

  override fun close() {
    try { reader?.close() } finally {
      reader = null
      uri = null
      if (started) { started = false; transferEnded() }
    }
  }
}
