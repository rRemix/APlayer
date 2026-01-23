package remix.myplayer.ui.screen.setting.logic.common

import android.app.Activity
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.screen.setting.NormalPreference
import remix.myplayer.viewmodel.libraryViewModel

@Composable
fun ExportPlayListLogic() {
  val libraryVM = libraryViewModel
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
    if (allPlayList.isEmpty()) {
      return@NormalPreference
    }

    state.show()
  }

  val uriLauncher =
    rememberLauncherForActivityResult(
      contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        libraryVM.exportPlayListToFile(
          allPlayList.firstOrNull { it.name == select },
          uri
        )
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
