package remix.myplayer.misc

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import io.reactivex.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.reactivestreams.Subscription
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.theme.Theme
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.MediaStoreUtil.getBaseSelectionArgs
import remix.myplayer.util.ToastUtil
import timber.log.Timber
import java.io.File


/**
 * Created by Remix on 2017/11/20.
 */

class MediaScanner(private val context: Context) {

  fun scanFilesSimply(folder: File) {
    val toScanFiles = ArrayList<File>()

    val dispose = Completable
        .fromAction {
          getScanFiles(folder, toScanFiles)
          if (toScanFiles.isNotEmpty()) {
            MediaScannerConnection.scanFile(
                context,
                toScanFiles.map { it.absolutePath }.toTypedArray(),
                toScanFiles.map { "audio/*" }.toTypedArray()
            ) { path, uri ->
//              Timber.tag(TAG).v("scanCompleted, path: $path uri: $uri")
            }
          }
        }
        .subscribeOn(Schedulers.io())
        .subscribe({
          Timber.tag(TAG).v("scanAll completed")
        }, {
          Timber.tag(TAG).w("scan err: $it")
        })

  }

  fun scanFiles(folder: File) {
    var subscription: Subscription? = null
    var connection: MediaScannerConnection? = null
    val toScanFiles = ArrayList<File>()

    val loadingDialog = Theme.getBaseDialog(context)
        .cancelable(false)
        .title(R.string.please_wait)
        .content(R.string.scaning)
        .progress(true, 0)
        .progressIndeterminateStyle(false)
        .dismissListener { connection?.disconnect() }
        .build()

    connection = MediaScannerConnection(context, object : MediaScannerConnection.MediaScannerConnectionClient {
      override fun onMediaScannerConnected() {
        Flowable.create(FlowableOnSubscribe<File> { emitter ->
          getScanFiles(folder, toScanFiles)
          for (file in toScanFiles) {
            emitter.onNext(file)
          }
          emitter.onComplete()
        }, BackpressureStrategy.BUFFER)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(object : FlowableSubscriber<File> {
              override fun onSubscribe(s: Subscription) {
                loadingDialog.show()
                subscription = s
                subscription?.request(1)
              }

              override fun onNext(file: File) {
                loadingDialog.setContent(file.absolutePath)
                connection?.scanFile(file.absolutePath, "audio/*")
              }

              override fun onError(throwable: Throwable) {
                loadingDialog.dismiss()
                ToastUtil.show(context, R.string.scan_failed, throwable.toString())
              }

              override fun onComplete() {
                loadingDialog.dismiss()
                ToastUtil.show(context, context.getString(R.string.scanned_finish))
                App.getContext().contentResolver.notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null)
              }
            })
      }


      override fun onScanCompleted(path: String, uri: Uri) {
        subscription?.request(1)
        Timber.tag(TAG).v("onScanCompleted, path: $path uri: $uri")
      }
    })
    connection.connect()
  }

  private fun getScanFiles(file: File, toScanFiles: ArrayList<File>) {
    val baseSelection = MediaStoreUtil.getBaseSelection()
    val selection = "$baseSelection and media_type = 2"
    val selectionArgs = getBaseSelectionArgs()

    context.contentResolver.query(MediaStore.Files.getContentUri("external"),
        arrayOf(MediaStore.Files.FileColumns.DATA),
        selection, selectionArgs, null)?.use {
      while (it.moveToNext()) {
        val path = it.getString(0)
        if (path.startsWith(file.absolutePath)) {
          toScanFiles.add(File(path))
        }
      }
    }
//    if (file.isFile && file.length() >= MediaStoreUtil.SCAN_SIZE) {
//      if (isAudioFile(file))
//        toScanFiles.add(file)
//    } else {
//      val files = file.listFiles() ?: return
//      for (temp in files) {
//        getScanFiles(temp, toScanFiles)
//      }
//    }
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
