package remix.myplayer.ui.misc

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import remix.myplayer.R
import remix.myplayer.theme.Theme
import remix.myplayer.ui.activity.base.BaseActivity
import remix.myplayer.ui.dialog.FileChooserDialog
import remix.myplayer.util.PermissionUtil
import remix.myplayer.util.SPUtil
import java.io.File

class FileChooser(val activity: BaseActivity,
                  val tag: String?,
                  private val extensionsFilter: Array<String>,
                  private val preferenceName: String?,
                  private val preferenceKey: String?,
                  val callback: FileCallback?
) : FileChooserDialog.FileCallback {

  interface FileCallback {
    fun onFileSelection(chooser: FileChooser, file: File)
    fun onFileChooserDismissed(chooser: FileChooser)
  }

  private data class Choice(
      val volume: String?,
      val directory: String
  )

  private var selectedVolume: String? = null

  fun show() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
        !PermissionUtil.hasManageExternalStorage()) {
      Theme
          .getBaseDialog(activity)
          .content(R.string.manual_scan_permission_tip)
          .positiveText(R.string.confirm)
          .negativeText(R.string.cancel)
          .onPositive { _, _ -> PermissionUtil.requestManageExternalStorage(activity) }
          .show()
      return
    }

    val builder = FileChooserDialog
        .Builder(activity)
        .tag(tag)
        .extensionsFilter(*extensionsFilter)
        .callback(this)

    var lastChoice: Choice? = null
    preferenceName?.let { preferenceName ->
      preferenceKey?.let { preferenceKey ->
        try {
          lastChoice = Gson().fromJson(
              SPUtil.getValue(activity, preferenceName, preferenceKey, ""),
              Choice::class.java
          )
        } catch (throwable: Throwable) {
        }
      }
    }

    val volumes: ArrayList<String> = ArrayList()
    val storageManager = activity.getSystemService(Context.STORAGE_SERVICE) as StorageManager?
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      storageManager?.storageVolumes?.forEach { storageVolume ->
        storageVolume?.directory?.let { directory ->
          volumes.add(directory.absolutePath)
        }
      }
    }

    if (volumes.size > 1) {
      Theme
          .getBaseDialog(activity)
          .items(volumes)
          .itemsCallback { _, _, _, text ->
            selectedVolume = text.toString()
            builder.initialPath(
                if (text == lastChoice?.volume) {
                  lastChoice!!.directory
                } else {
                  text.toString()
                }
            )
            builder.show()
          }
          .show()
    } else {
      var initialPath: String? = null
      lastChoice?.let {
        if (it.volume == null) {
          initialPath = it.directory
        }
      }
      if (initialPath != null) {
        builder.initialPath(initialPath)
      }
      builder.show()
    }
  }

  override fun onFileSelection(dialog: FileChooserDialog, file: File) {
    preferenceName?.let { preferenceName ->
      preferenceKey?.let { preferenceKey ->
        SPUtil.deleteValue(activity, preferenceName, preferenceKey)
        if (file.isFile && file.startsWith(selectedVolume ?: file.absolutePath)) {
          SPUtil.putValue(
              activity,
              preferenceName,
              preferenceKey,
              GsonBuilder()
                  .serializeNulls()
                  .create()
                  .toJson(Choice(selectedVolume, file.parent ?: return))
          )
        }
      }
    }

    callback?.onFileSelection(this, file)
  }

  override fun onFileChooserDismissed(dialog: FileChooserDialog) {
    callback?.onFileChooserDismissed(this)
  }
}