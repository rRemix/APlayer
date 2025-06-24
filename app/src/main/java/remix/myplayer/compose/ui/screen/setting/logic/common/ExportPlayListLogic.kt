package remix.myplayer.compose.ui.screen.setting.logic.common

import android.app.Activity
import android.content.Intent
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.NormalPreference
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.helper.M3UHelper.exportPlayListToFile

@Composable
fun ExportPlayListLogic() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val state = rememberDialogState(false)

  var allPlayListName by remember {
    mutableStateOf(emptyList<String>())
  }
  var select by remember {
    mutableStateOf("")
  }

  NormalPreference(
    stringResource(R.string.export_playlist),
    stringResource(R.string.export_play_list_tip)
  ) {
    scope.launch {
      allPlayListName = getAllPlayListName()
      if (allPlayListName.isEmpty()) {
        return@launch
      }

      state.show()
    }
  }

  val fileLauncher =
    rememberLauncherForActivityResult<Intent, ActivityResult>(
      contract = ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        exportPlayListToFile(context, select, uri)
      }
    }

  NormalDialog(
    dialogState = state,
    title = stringResource(R.string.choose_playlist_to_export),
    positive = null,
    items = allPlayListName,
    itemsCallback = { index, text ->
      select = text
      fileLauncher.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        type = MimeTypeMap.getSingleton().getMimeTypeFromExtension("m3u")
        addCategory(Intent.CATEGORY_OPENABLE)
        putExtra(Intent.EXTRA_TITLE, "$text.m3u")
      })
    }
  )

}

private suspend fun getAllPlayListName(): List<String> = withContext(Dispatchers.IO) {
  DatabaseRepository.getInstance().getAllPlaylist().blockingGet().map { it.name }
}