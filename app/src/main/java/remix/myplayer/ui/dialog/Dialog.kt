package remix.myplayer.ui.dialog

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Song
import remix.myplayer.ui.theme.LocalTheme
import timber.log.Timber

@Composable
internal fun BaseDialog(
  show: Boolean,
  onDismissRequest: (() -> Unit)?,
  onDismiss: (() -> Unit)? = null,
  cancelOutside: Boolean = true,
  usePlatformDefaultWidth: Boolean = true,
  content: @Composable () -> Unit,
) {
  if (!show) {
    return
  }
  val currentOnDismiss = rememberUpdatedState(onDismiss)

  Dialog(
    onDismissRequest = {
      Timber.v("BaseDialog onDismissRequest")
      onDismissRequest?.invoke()
    }, properties = DialogProperties(
      dismissOnBackPress = cancelOutside,
      dismissOnClickOutside = cancelOutside,
      usePlatformDefaultWidth = usePlatformDefaultWidth
    )
  ) {
    BoxWithConstraints(contentAlignment = Alignment.Center) {
      Surface(
        modifier = Modifier
          .fillMaxWidth(if (usePlatformDefaultWidth) 1f else 0.8f)
          .heightIn(max = maxHeight * 0.8f),
        color = LocalTheme.current.dialogBackground,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 8.dp,
      ) {
        content()
      }
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      currentOnDismiss.value?.invoke()
    }
  }
}

@Stable
data class SongDetailState(val dialogState: DialogState, val song: Song = Song.EMPTY_SONG)

@Stable
data class ReNamePlayListState(
  val dialogState: DialogState,
  val playList: PlayList? = null,
)

@Stable
data class DeleteSongState(
  val dialogState: DialogState = DialogState(),
  val titleRes: Int = R.string.confirm_delete_from_library,
  val models: List<APlayerModel> = emptyList(),
  val deleteSource: Boolean = false,
  val parent: APlayerModel? = null
)

@Stable
data class ImportPlayListState(
  val rootDialogState: DialogState,
  val inputDialogState: DialogState,
  val inputText: String = "",
  val songIds: List<Long> = emptyList()
)
