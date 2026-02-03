package remix.myplayer.service.playback

import android.media.MediaDataSource
import android.os.Build
import androidx.annotation.RequiresApi
import remix.myplayer.data.model.smb.SmbRandomAccessDelegate
import timber.log.Timber

@RequiresApi(Build.VERSION_CODES.M)
class SmbMediaDataSource(uriString: String) : MediaDataSource() {

  private var delegate: SmbRandomAccessDelegate? = null

  init {
    try {
      val clazz = Class.forName("remix.myplayer.smb.SmbRandomAccessDelegateImpl")
      delegate = clazz.getDeclaredConstructor().newInstance() as SmbRandomAccessDelegate
      delegate?.open(uriString)
    } catch (e: Exception) {
      Timber.e(e, "Failed to initialize SmbMediaDataSource delegate")
      delegate = null
    }
  }

  override fun readAt(position: Long, buffer: ByteArray?, offset: Int, size: Int): Int {
    if (buffer == null) return -1
    return delegate?.readAt(position, buffer, offset, size) ?: -1
  }

  override fun getSize(): Long {
    return delegate?.getSize() ?: -1
  }

  override fun close() {
    delegate?.close()
    delegate = null
  }
}
