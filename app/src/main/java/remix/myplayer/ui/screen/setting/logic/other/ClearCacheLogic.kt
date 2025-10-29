package remix.myplayer.ui.screen.setting.logic.other

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.screen.setting.Preference
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.util.Util
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.settingViewModel

@Composable
fun ClearCacheLogic() {
  val libraryVM = libraryViewModel
  val settingVM = settingViewModel
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var cacheSize by remember {
    mutableLongStateOf(0L)
  }
  val state = rememberDialogState()
  Preference(onClick = {
    state.show()
  }, title = stringResource(R.string.clear_cache)) {
    TextSecondary(
      text = stringResource(R.string.cache_size, cacheSize.toFloat() / 1024f / 1024f),
      fontSize = 14.sp
    )
  }

  NormalDialog(
    dialogState = state,
    titleRes = R.string.confirm_clear_cache,
    onPositive = {
      Glide.get(context).clearMemory()

      scope.launch {
        settingVM.clearCache(context) {
          libraryVM.fetchMedia(true)
          cacheSize = 0
        }
      }
    }
  )

  LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
      var size = Util.getFolderSize(context.cacheDir)
      context.externalCacheDir?.let { ext ->
        size += Util.getFolderSize(ext)
      }
      cacheSize = size
    }
  }
}