package remix.myplayer.service.playback

import android.media.MediaDataSource
import android.os.Build
import androidx.annotation.RequiresApi
import remix.myplayer.data.remote.RemoteFileReader
import remix.myplayer.data.remote.smb.SmbFileSystem

@RequiresApi(Build.VERSION_CODES.M)
class SmbMediaDataSource(uriString: String) : MediaDataSource() {
  private var reader: RemoteFileReader? = SmbFileSystem.fromUrl(uriString).let { (system, path) -> system.open(path) }

  override fun readAt(position: Long, buffer: ByteArray?, offset: Int, size: Int): Int =
    if (buffer == null) -1 else checkNotNull(reader).readAt(position, buffer, offset, size)

  override fun getSize(): Long = reader?.size ?: -1L

  override fun close() {
    try { reader?.close() } finally { reader = null }
  }
}
