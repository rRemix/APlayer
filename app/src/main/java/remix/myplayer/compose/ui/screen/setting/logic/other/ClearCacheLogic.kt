package remix.myplayer.compose.ui.screen.setting.logic.other

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
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.Preference
import remix.myplayer.compose.ui.widget.common.TextSecondary
import remix.myplayer.compose.viewmodel.libraryViewModel
import remix.myplayer.misc.cache.DiskCache
import remix.myplayer.util.Util

@Composable
fun ClearCacheLogic() {
  val vm = libraryViewModel
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
        withContext(Dispatchers.IO) {
          Util.deleteFilesByDirectory(context.cacheDir)
          DiskCache.init(context, "lyric")
          Glide.get(context).clearDiskCache()
        }
        vm.fetchMedia(true)
        cacheSize = 0
      }
    }
  )

  LaunchedEffect(Unit) {
    withContext(Dispatchers.IO) {
      cacheSize = Util.getFolderSize(context.cacheDir)
    }
  }
}