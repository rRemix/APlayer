package remix.myplayer.compose.ui.dialog

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.compose.viewmodel.mainViewModel
import remix.myplayer.misc.update.DownloadService
import remix.myplayer.misc.update.DownloadService.Companion.EXTRA_RESPONSE

@Composable
fun InAppUpdateDialog() {
  val mainVM = mainViewModel
  val state by mainVM.inAppUpdateState.collectAsStateWithLifecycle()

  val release = state.release ?: return
  val context = LocalContext.current

  val force = release.isForceUpdate()

  NormalDialog(
    dialogState = state.dialogState,
    cancelOutside = !force,
    title = stringResource(R.string.new_version_found),
    content = release.body ?: "",
    positive = stringResource(R.string.update),
    onPositive = {
      context.startService(
        Intent(context, DownloadService::class.java)
          .putExtra(EXTRA_RESPONSE, release)
      )
    },
    negative = if (!force) stringResource(R.string.ignore_check_update_forever) else null,
    onNegative = {
      if (!force) {
        mainVM.ignoreForever()
      }
    },
    neutral = if (!force) stringResource(R.string.ignore_this_version) else null,
    onNeutral = {
      if (!force) {
        mainVM.ignoreCurrentVersion(release)
      }
    }
  )
}