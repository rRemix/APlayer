package remix.myplayer.ui.screen.home

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.misc.manager.DynamicModuleStatus
import remix.myplayer.ui.dialog.DialogState
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.dismissLoading
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.dialog.showLoading
import remix.myplayer.ui.dialog.updateLoadingText
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.viewmodel.SmbViewModel
import timber.log.Timber

@Composable
internal fun SmbDropDownMenu(smbVM: SmbViewModel, onShowSmbAddDialog: () -> Unit) {
  val context = LocalContext.current

  val smbDialogState = rememberDialogState()
  val installStatus by smbVM.moduleInstallStatus.collectAsStateWithLifecycle()

  LaunchedEffect(installStatus) {
    val status = installStatus ?: return@LaunchedEffect
    Timber.v("installSmbModule, status: $status")
    when (status) {
      is DynamicModuleStatus.Downloading -> {
        showLoading(false, context.getString(R.string.downloading))
        updateLoadingText(
          context.getString(
            R.string.smb_downloading_with_progress,
            (status.progress * 100).toInt()
          )
        )
      }

      DynamicModuleStatus.Installing -> {
        showLoading(false)
        updateLoadingText(context.getString(R.string.smb_installing))
      }

      DynamicModuleStatus.Installed -> {
        dismissLoading()
        onShowSmbAddDialog()
        smbVM.clearInstallStatus()
      }

      DynamicModuleStatus.Canceled -> {
        dismissLoading()
        MessageNotifier.show(R.string.smb_download_canceled)
        smbVM.clearInstallStatus()
      }

      is DynamicModuleStatus.Error -> {
        dismissLoading()
        MessageNotifier.show(
          R.string.smb_download_failed,
          status.exception.localizedMessage
        )
        smbVM.clearInstallStatus()
      }

      is DynamicModuleStatus.Failed -> {
        dismissLoading()
        MessageNotifier.show(R.string.smb_download_failed, status.errorCode.toString())
        smbVM.clearInstallStatus()
      }

      DynamicModuleStatus.Pending -> {
        showLoading(false, context.getString(R.string.downloading))
      }

      DynamicModuleStatus.UnAvailable -> {
        dismissLoading()
        MessageNotifier.show(R.string.smb_module_not_supported)
        smbVM.clearInstallStatus()
      }

      DynamicModuleStatus.Unknown -> {
        dismissLoading()
        MessageNotifier.show(R.string.smb_unknown_error)
        smbVM.clearInstallStatus()
      }
    }
  }

  DropdownMenuItem(
    text = {
      Text(
        stringResource(R.string.smb),
        color = LocalTheme.current.textPrimary
      )
    },
    onClick = {
      if (smbVM.isSmbModuleInstalled) {
        onShowSmbAddDialog()
      } else {
        smbDialogState.show()
      }
    }
  )

  SmbDialog(smbDialogState) {
    smbVM.startSmbModuleInstallation()
  }
}

@Composable
private fun SmbDialog(dialogState: DialogState, onConfirm: () -> Unit) {
  NormalDialog(
    dialogState = dialogState,
    title = stringResource(R.string.smb_download_required_title),
    content = stringResource(R.string.smb_download_required_message),
    onPositive = onConfirm
  )
}