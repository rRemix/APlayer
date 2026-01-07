package remix.myplayer.misc

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.ui.dialog.dismissLoading
import remix.myplayer.ui.dialog.showLoading
import remix.myplayer.ui.dialog.updateLoadingText
import remix.myplayer.ui.nav.MessageNotifier
import timber.log.Timber
import java.io.File
import kotlin.coroutines.resume


/**
 * Created by Remix on 2017/11/20.
 */

class MediaScanner(private val context: Context) {

  suspend fun scan(folder: File) {
    try {
      showLoading(false, context.getString(R.string.please_wait))

      val toScanFiles = ArrayList<File>()
      withContext(Dispatchers.IO) {
        getScanFiles(folder, toScanFiles)
      }

      for (file in toScanFiles) {
        updateLoadingText(file.name)
        val uri = withContext(Dispatchers.IO) {
          scanSingleFile(context, file)
        }
        Timber.v("MediaScanner scan file: $file uri: $uri")
      }

      MessageNotifier.show(R.string.scanned_finish)
      context.contentResolver.notifyChange(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        null
      )
    } catch (e: Exception) {
      MessageNotifier.show(R.string.scan_failed, e.toString())
    } finally {
      dismissLoading()
    }
  }

  // scan single file
  suspend fun scanSingleFile(context: Context, file: File) =
    suspendCancellableCoroutine { continuation ->
      var connection: MediaScannerConnection? = null
      connection = MediaScannerConnection(
        context,
        object : MediaScannerConnection.MediaScannerConnectionClient {
          override fun onMediaScannerConnected() {
            connection?.scanFile(file.absolutePath, "audio/*")
          }

          override fun onScanCompleted(path: String, uri: Uri?) {
            connection?.disconnect()
            continuation.resume(uri)
          }
        })

      connection.connect()

      continuation.invokeOnCancellation {
        connection.disconnect()
      }
    }

  private fun getScanFiles(file: File, toScanFiles: ArrayList<File>) {
    if (file.isFile) {
      if (isAudioFile(file)) {
        toScanFiles.add(file)
      }
    } else if (file.isDirectory) {
      file.listFiles()?.forEach {
        getScanFiles(it, toScanFiles)
      }
    }
  }

  private fun isAudioFile(file: File): Boolean {
    val ext = getFileExtension(file.name)
    val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    return !mime.isNullOrEmpty() && mime.startsWith("audio") && !mime.contains("mpegurl")
  }

  private fun getFileExtension(fileName: String): String? {
    val i = fileName.lastIndexOf('.')
    return if (i > 0) {
      fileName.substring(i + 1)
    } else
      null
  }

  companion object {

    private const val TAG = "MediaScanner"
  }
}
