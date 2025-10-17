package remix.myplayer.compose.ui.screen.setting.logic.common

import android.app.Activity
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.NormalPreference
import remix.myplayer.compose.viewmodel.libraryViewModel
import remix.myplayer.compose.viewmodel.settingViewModel
import remix.myplayer.db.room.model.PlayList

@Composable
fun ExportPlayListLogic() {
  val scope = rememberCoroutineScope()
  val libraryVM = libraryViewModel
  val settingVM = settingViewModel
  val state = rememberDialogState(false)

  var allPlayList by remember {
    mutableStateOf(emptyList<PlayList>())
  }
  var select by remember {
    mutableStateOf("")
  }

  NormalPreference(
    stringResource(R.string.export_playlist),
    stringResource(R.string.export_play_list_tip)
  ) {
    scope.launch {
      if (allPlayList.isEmpty()) {
        return@launch
      }

      state.show()
    }
  }

  val uriLauncher =
    rememberLauncherForActivityResult<Intent, ActivityResult>(
      contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        settingVM.exportPlayListToFile(allPlayList.firstOrNull { it.name == select }, uri)
      }
    }

  NormalDialog(
    dialogState = state,
    title = stringResource(R.string.choose_playlist_to_export),
    positive = null,
    items = allPlayList.map { it.name },
    itemsCallback = { index, text ->
      select = text
      uriLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        type = MimeTypeMap.getSingleton().getMimeTypeFromExtension("m3u")
        addCategory(Intent.CATEGORY_OPENABLE)
        putExtra(Intent.EXTRA_TITLE, "$text.m3u")
      })
    }
  )

  LaunchedEffect(Unit) {
    libraryVM.playLists.collect {
      allPlayList = it
    }
  }
}