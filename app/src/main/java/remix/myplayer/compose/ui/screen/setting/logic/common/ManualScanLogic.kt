package remix.myplayer.compose.ui.screen.setting.logic.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.dialog.FolderDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.NormalPreference
import remix.myplayer.compose.viewmodel.SettingViewModel
import remix.myplayer.misc.MediaScanner
import java.io.File

@Composable
fun ManualScanLogic() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val vm = activityViewModel<SettingViewModel>()
  val dialogState = rememberDialogState()
  var initialPath by rememberSaveable {
    mutableStateOf(vm.settingPrefs.manualScanFolder)
  }

  NormalPreference(stringResource(R.string.manual_scan), stringResource(R.string.manual_scan_tip)) {
    initialPath = vm.settingPrefs.manualScanFolder
    dialogState.show()
  }

  FolderDialog(
    dialogState = dialogState,
    initialFolder = initialPath,
    onFolderSelection = {
      initialPath = it.absolutePath
    },
    onPositive = {
      dialogState.dismiss()
      vm.settingPrefs.manualScanFolder = it
      scope.launch {
        MediaScanner(context).scan(File(it))
      }
    }
  )
}