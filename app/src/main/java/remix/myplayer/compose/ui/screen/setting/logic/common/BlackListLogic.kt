package remix.myplayer.compose.ui.screen.setting.logic.common

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.documentfile.provider.DocumentFile
import remix.myplayer.R
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.rememberMutableStateSetOf
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.NormalPreference
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.compose.viewmodel.SettingViewModel

@Composable
fun BlackListLogic() {
  val context = LocalContext.current
  val settingVM = activityViewModel<SettingViewModel>()
  val libraryVM = activityViewModel<LibraryViewModel>()

  val blackList = rememberMutableStateSetOf(*settingVM.settingPrefs.blacklist.toTypedArray())
  var pendingDelete by rememberSaveable {
    mutableStateOf("")
  }
  // dialog for blacklist
  val blacklistState = rememberDialogState()
  // dialog for clear all blacklist
  val clearAllState = rememberDialogState()
  // dialog for deleting a single blacklist entry
  val clearSingleState = rememberDialogState()

  NormalPreference(stringResource(R.string.blacklist), stringResource(R.string.blacklist_tip)) {
    blacklistState.show()
  }

  val launcher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        result.data?.data?.let { uri ->
          val folder = DocumentFile.fromTreeUri(context, uri)
          if (folder?.isDirectory == true) {
            val folderPath = parseDocument(folder)
            if (folderPath != null) {
              blackList.add("${Environment.getExternalStorageDirectory()}/$folderPath")
              settingVM.settingPrefs.blacklist = blackList
              libraryVM.fetchMedia()
            }
            blacklistState.show()
          }
        }
      }
    }

  NormalDialog(
    dialogState = blacklistState,
    title = stringResource(R.string.blacklist),
    items = blackList.toList(),
    itemsCallback = { _, str ->
      pendingDelete = str
      clearSingleState.show()
    },
    positive = stringResource(R.string.add),
    onPositive = {
      launcher.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        addCategory(Intent.CATEGORY_DEFAULT)
      })
    },
    negative = null,
    neutral = stringResource(R.string.clear),
    onNeutral = {
      clearAllState.show()
    }
  )

  NormalDialog(
    dialogState = clearSingleState,
    title = stringResource(R.string.remove_from_blacklist),
    content = stringResource(R.string.do_you_want_remove_from_blacklist, pendingDelete),
    onPositive = {
      blackList.remove(pendingDelete)
      settingVM.settingPrefs.blacklist = blackList
      libraryVM.fetchMedia()
    }
  )

  NormalDialog(
    dialogState = clearAllState,
    titleRes = R.string.clear_blacklist_title,
    contentRes = R.string.clear_blacklist_content,
    onPositive = {
      settingVM.settingPrefs.blacklist = emptySet<String>()
      libraryVM.fetchMedia(false)
    }
  )
}

private fun parseDocument(folder: DocumentFile): String? {
  val decodedUri = Uri.decode(folder.uri.toString())
  val folderPath = if (decodedUri.lastIndexOf("document/primary:") != -1) {
    decodedUri.split("document/primary:")[1]
  } else if (decodedUri.lastIndexOf("document/home:") != -1) {
    "Documents/" + decodedUri.split("document/home:")[1]
  } else {
    null
  }
  return folderPath
}