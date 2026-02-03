package remix.myplayer.service.playback

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import remix.myplayer.data.model.smb.SmbStreamDelegate
import timber.log.Timber
import java.io.IOException

@UnstableApi
class SmbDataSource : BaseDataSource(true) {

  private var delegate: SmbStreamDelegate? = null
  private var uri: Uri? = null

  override fun open(dataSpec: DataSpec): Long {
    transferInitializing(dataSpec)
    uri = dataSpec.uri
    try {
      val clazz = Class.forName("remix.myplayer.smb.SmbStreamDelegateImpl")
      delegate = clazz.getDeclaredConstructor().newInstance() as SmbStreamDelegate
      val length = delegate!!.open(dataSpec.uri.toString(), dataSpec.position)
      transferStarted(dataSpec)
      return length
    } catch (e: Exception) {
      Timber.e(e)
      throw IOException(e)
    }
  }

  override fun read(buffer: ByteArray, offset: Int, readLength: Int): Int {
    return delegate?.read(buffer, offset, readLength) ?: -1
  }

  override fun getUri(): Uri? {
    return uri
  }

  override fun close() {
    delegate?.close()
    delegate = null
    transferEnded()
  }

  companion object {

    private const val TAG = "SmbDataSource"
  }
}
