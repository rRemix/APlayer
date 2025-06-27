package remix.myplayer.compose.ui.screen.setting.logic.common

import android.app.Activity
import android.content.Intent
import android.content.Intent.EXTRA_ALLOW_MULTIPLE
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateSet
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.compose.rememberMutableStateSetOf
import remix.myplayer.compose.ui.dialog.DialogState
import remix.myplayer.compose.ui.dialog.InputDialog
import remix.myplayer.compose.ui.dialog.ItemsCallbackMultiChoice
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.screen.setting.NormalPreference
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.helper.M3UHelper.importLocalPlayList
import remix.myplayer.helper.M3UHelper.importM3UFile
import remix.myplayer.util.ToastUtil

@Composable
fun ImportPlayListLogic() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val importPlaylistState = rememberDialogState(false)
  NormalPreference(
    stringResource(R.string.playlist_import),
    stringResource(R.string.playlist_import_tip)
  ) {
    importPlaylistState.show()
  }

  // dialog for choosing way to import playlist
  val addToPlayListState = rememberDialogState(false)
  // dialog for creating and import playlist
  val createPlayListState = rememberDialogState(false)
  // dialog for new playlistName
  val inputState = rememberDialogState(false)

  var allPlaylists by rememberSaveable {
    mutableStateOf(emptyList<String>())
  }
  var uri by rememberSaveable {
    mutableStateOf(Uri.EMPTY)
  }
  var inputText by rememberSaveable {
    mutableStateOf("")
  }

  ImportFromStorage(
    addToPlayListState,
    allPlaylists,
    uri,
    createPlayListState,
    inputState,
    inputText
  ) {
    inputText = it
  }

  // dialog for choosing mediaStore playlist to import
  val choosePlaylistState = rememberDialogState(false)
  var mediaStorePlayLists by rememberSaveable {
    mutableStateOf(emptyMap<String, List<Long>>())
  }
  val selectedIndicates = rememberMutableStateSetOf(*emptyArray<Int>())
  ImportFromMediaStore(choosePlaylistState, mediaStorePlayLists, selectedIndicates)

  val chooseM3ULauncher =
    rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
      if (result.resultCode == Activity.RESULT_OK) {
        uri = result.data?.data ?: return@rememberLauncherForActivityResult
        inputText = DocumentFile.fromSingleUri(context, uri)?.name?.removeSuffix(".m3u") ?: ""
        scope.launch {
          allPlaylists = withContext(Dispatchers.IO) {
            DatabaseRepository
              .getInstance()
              .getAllPlaylist()
              .blockingGet()
          }.map { it.name }
          if (allPlaylists.isEmpty()) {
            return@launch
          }
          addToPlayListState.show()
        }
      }
    }
  NormalDialog(
    dialogState = importPlaylistState,
    titleRes = R.string.choose_import_way,
    positiveRes = null,
    negativeRes = null,
    itemRes = listOf(R.string.import_from_external_storage, R.string.import_from_others),
    itemsCallback = { index, _ ->
      when (index) {
        0 -> {
          chooseM3ULauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply {
            putExtra(EXTRA_ALLOW_MULTIPLE, true)
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension("m3u")
            addCategory(Intent.CATEGORY_OPENABLE)
          })
        }

        1 -> {
          scope.launch {
            mediaStorePlayLists = withContext(Dispatchers.IO) {
              DatabaseRepository.getInstance().playlistFromMediaStore
            }
            selectedIndicates.addAll(List(mediaStorePlayLists.keys.size) { index ->
              index
            })
            if (mediaStorePlayLists.isEmpty()) {
              ToastUtil.show(
                context,
                R.string.import_fail,
                context.getString(R.string.no_playlist_can_import)
              )
              return@launch
            }

            choosePlaylistState.show()
          }
        }
      }
    }
  )
}

@Composable
private fun ImportFromStorage(
  addToPlayListState: DialogState,
  allPlaylists: List<String>,
  uri: Uri,
  createPlayListState: DialogState,
  inputState: DialogState,
  inputText: String,
  onValueChange: (String) -> Unit
) {
  val context = LocalContext.current

  NormalDialog(
    dialogState = addToPlayListState,
    title = stringResource(R.string.add_to_playlist),
    items = allPlaylists,
    neutral = stringResource(R.string.create_playlist),
    onNeutral = {
      inputState.show()
    },
    itemsCallback = { index, text ->
      importM3UFile(context, uri, text, false)
    })

  NormalDialog(
    dialogState = createPlayListState,
    titleRes = R.string.new_playlist,
    positiveRes = R.string.create,
    negativeRes = R.string.cancel
  )

  InputDialog(
    dialogState = inputState,
    text = inputText,
    title = stringResource(R.string.new_playlist),
    positive = stringResource(R.string.create),
    negative = stringResource(R.string.cancel),
    content = stringResource(R.string.input_playlist_name),
    onDismissRequest = {
      onValueChange("")
    },
    onValueChange = {
      onValueChange(it)
    }
  ) { input ->
    if (allPlaylists.contains(input)) {
      ToastUtil.show(context, R.string.playlist_already_exist)
    } else if (input.isNotBlank()) {
      importM3UFile(context, uri, input, true)
    }
  }
}

@Composable
private fun ImportFromMediaStore(
  choosePlaylistState: DialogState,
  mediaStorePlayLists: Map<String, List<Long>>,
  selectedIndicates: MutableSet<Int>
) {
  val context = LocalContext.current

  NormalDialog(
    dialogState = choosePlaylistState,
    title = stringResource(R.string.choose_import_playlist),
    onPositive = {
      val select: Array<CharSequence> = mediaStorePlayLists.keys.filterIndexed { index, _ ->
        selectedIndicates.contains(index)
      }.toTypedArray()
      importLocalPlayList(context, mediaStorePlayLists, select)
    },
    items = mediaStorePlayLists.keys.toList(),
    itemsCallbackMultiChoice = ItemsCallbackMultiChoice(selectedIndicates) { index, checked ->
      if (checked) {
        selectedIndicates.add(index)
      } else {
        selectedIndicates.remove(index)
      }
    }
  )
}