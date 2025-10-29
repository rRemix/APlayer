package remix.myplayer.ui.screen.setting.logic.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.misc.MediaScanner
import remix.myplayer.ui.dialog.FolderDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.screen.setting.NormalPreference
import remix.myplayer.viewmodel.settingViewModel
import java.io.File

@Composable
fun ManualScanLogic() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()

  val dialogState = rememberDialogState()
  var currentPath by rememberSaveable {
    mutableStateOf(settingState.common.manualScanFolder)
  }

  NormalPreference(stringResource(R.string.manual_scan), stringResource(R.string.manual_scan_tip)) {
    dialogState.show()
  }

  FolderDialog(
    dialogState = dialogState,
    initialFolder = currentPath,
    onFolderSelection = {
      currentPath = it.absolutePath
    },
    onPositive = {
      dialogState.dismiss()
      settingVM.setManualScanFolder(it)
      scope.launch {
        MediaScanner(context).scan(File(it))
      }
    }
  )
}