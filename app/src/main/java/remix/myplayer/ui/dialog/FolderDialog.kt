package remix.myplayer.ui.dialog

import android.os.Environment
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import java.io.File

@Composable
fun FolderDialog(
  dialogState: DialogState,
  initialFolder: String,
  onFolderSelection: (File) -> Unit,
  onPositive: (String) -> Unit,
) {
  val folderState = FolderState(File(initialFolder))
  val contents = folderState.contents
  val canGoUp = folderState.canGoUp
  NormalDialog(
    dialogState = dialogState,
    autoDismiss = false,
    title = folderState.currentFolder.absolutePath,
    positive = stringResource(R.string.choose_folder),
    onPositive = { onPositive(folderState.currentFolder.absolutePath) },
    onNegative = { dialogState.dismiss() },
    items = contents.map { it.name }.toMutableList().apply {
      if (canGoUp) {
        add(0, "..")
      }
    },
    itemsCallback = { index, str ->
      val newFolder = if (str == "..") {
        folderState.parentFolder ?: return@NormalDialog
      } else {
        contents.getOrNull(if (canGoUp) index - 1 else index) ?: return@NormalDialog
      }
      onFolderSelection(newFolder)
    }
  )
}

internal data class FolderState(
  // TODO volume
//  var currentVolume: String = MediaStore.VOLUME_EXTERNAL_PRIMARY,
  var currentFolder: File = Environment.getExternalStorageDirectory(),
) {

  init {
    if (!currentFolder.exists() || !currentFolder.isDirectory) {
      currentFolder = Environment.getExternalStorageDirectory()
    }
  }

  val parentFolder: File? = currentFolder.parentFile

  val canGoUp: Boolean
    get() {
      if (parentFolder == null) {
        return false
      }
      if (currentFolder.absolutePath == Environment.getExternalStorageDirectory().absolutePath) {
        return false
      }
      return true
    }

  val contents
    get() = (currentFolder.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name }
      ?: emptyList<File>())
}
