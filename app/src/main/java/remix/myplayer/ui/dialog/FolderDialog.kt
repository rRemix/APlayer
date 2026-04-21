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
        add(0, PARENT_FOLDER_ITEM)
      }
    },
    itemsCallback = { index, str ->
      val newFolder = if (str == PARENT_FOLDER_ITEM) {
        folderState.parentFolder ?: return@NormalDialog
      } else {
        val childFolder = contents.getOrNull(if (canGoUp) index - 1 else index)
          ?: return@NormalDialog
        folderState.resolveChildFolder(childFolder)
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

  private val primaryExternalStorage: File = Environment.getExternalStorageDirectory()
  private val primaryExternalStorageParent: File? = primaryExternalStorage.parentFile
  private val storageRoot: File =
    primaryExternalStorageParent?.parentFile ?: primaryExternalStorage

  init {
    if (currentFolder.path.isBlank() || !currentFolder.exists() || !currentFolder.isDirectory) {
      currentFolder = primaryExternalStorage
    } else if (currentFolder.absolutePath == primaryExternalStorageParent?.absolutePath) {
      currentFolder = primaryExternalStorage
    }
  }

  val parentFolder: File?
    get() {
      val parent = currentFolder.parentFile ?: return null
      return if (parent.absolutePath == primaryExternalStorageParent?.absolutePath) {
        storageRoot
      } else {
        parent
      }
    }

  fun resolveChildFolder(childFolder: File): File {
    return if (childFolder.absolutePath == primaryExternalStorageParent?.absolutePath) {
      primaryExternalStorage
    } else {
      childFolder
    }
  }

  val canGoUp: Boolean
    get() {
      if (parentFolder == null) {
        return false
      }
      if (currentFolder.absolutePath == storageRoot.absolutePath) {
        return false
      }
      return true
    }

  val contents
    get() = (currentFolder.listFiles()?.filter { it.isDirectory }?.sortedBy { it.name }
      ?: emptyList<File>())
}

private const val PARENT_FOLDER_ITEM = "..."
