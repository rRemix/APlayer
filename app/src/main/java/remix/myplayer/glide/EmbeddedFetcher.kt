package remix.myplayer.glide

import android.media.MediaMetadataRetriever
import android.net.Uri
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

/**
 * created by Remix on 2021/4/27
 */
class EmbeddedFetcher(private val fileUri: Uri) : DataFetcher<InputStream> {
  private var stream: InputStream? = null

  override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
    val mediaDataRetriever = MediaMetadataRetriever()
    try {
      mediaDataRetriever.setDataSource(fileUri.path)
      val bytes = mediaDataRetriever.embeddedPicture
      if (bytes != null) {
        stream = ByteArrayInputStream(bytes)
        callback.onDataReady(stream)
      }
    } finally {
      mediaDataRetriever.release()
    }

  }

  override fun cleanup() {
    try {
      stream?.close()
    } catch (ignore: IOException) {
    }
  }

  override fun cancel() {

  }

  override fun getDataClass(): Class<InputStream> {
    return InputStream::class.java
  }

  override fun getDataSource(): DataSource {
    return DataSource.LOCAL
  }
}