package remix.myplayer.compose.ui.screen.playing

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.AudioTagEditor
import remix.myplayer.compose.activity.base.BaseActivity
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.dialog.DialogState
import remix.myplayer.compose.ui.dialog.InputDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.compose.viewmodel.SettingViewModel
import remix.myplayer.compose.viewmodel.TimerViewModel
import remix.myplayer.helper.EQHelper
import remix.myplayer.util.ToastUtil

@Composable
fun PlayingDropDownMenu(
  expanded: Boolean,
  song: Song,
  onDismissRequest: () -> Unit
) {
  val libraryVM = activityViewModel<LibraryViewModel>()
  val settingVM = activityViewModel<SettingViewModel>()
  val menuItems =
    listOf(
      R.string.song_edit,
      R.string.song_detail,
      R.string.collect,
      R.string.add_to_playlist,
      R.string.sleep_timer,
      R.string.eq,
      R.string.lyric,
      R.string.speed,
      R.string.delete,
    )
  val activity = LocalActivity.current as? BaseActivity

  val timerVM = activityViewModel<TimerViewModel>()

  val speedDialogState = rememberDialogState()
  SpeedDialog(speedDialogState)

  DropdownMenu(
    modifier = Modifier.wrapContentSize(Alignment.TopEnd),
    expanded = expanded,
    containerColor = LocalTheme.current.dialogBackground,
    onDismissRequest = onDismissRequest
  ) {
    menuItems.forEachIndexed { _, res ->
      DropdownMenuItem(
        text = { Text(stringResource(res), color = LocalTheme.current.textPrimary) },
        onClick = {
          when (res) {
            R.string.song_edit -> {
              if (activity == null) {
                return@DropdownMenuItem
              }
              if (song.isLocal()) {
                AudioTagEditor(activity, song).edit()
              }
            }

            R.string.song_detail -> {
              if (activity == null) {
                return@DropdownMenuItem
              }
              if (song.isLocal()) {
                AudioTagEditor(activity, song).detail()
              }
            }

            R.string.collect -> {
              if (activity == null) {
                return@DropdownMenuItem
              }
              val favorite =
                libraryVM.playLists.value.firstOrNull { it.isFavorite() } ?: return@DropdownMenuItem

              libraryVM.addSongsToPlayList(listOf(song.id), favorite.name)
            }

            R.string.add_to_playlist -> {
              settingVM.showImportPlayListDialog(listOf(song.id))
            }

            R.string.sleep_timer -> {
              timerVM.showTimerDialog()
            }

            R.string.eq -> {
              if (activity == null) {
                return@DropdownMenuItem
              }
              EQHelper.startEqualizer(activity)
            }

            R.string.lyric -> {
              // TODO
            }

            R.string.speed -> {
              speedDialogState.show()
            }

            R.string.delete -> {
              settingVM.showDeleteSongDialog(listOf(song))
            }
          }

          onDismissRequest()
        }
      )
    }
  }
}

@Composable
private fun SpeedDialog(state: DialogState) {
  val settingPrefs = activityViewModel<SettingViewModel>().settingPrefs
  val context = LocalContext.current

  var text by rememberSaveable {
    mutableStateOf(settingPrefs.speed)
  }

  InputDialog(
    dialogState = state,
    text = text,
    title = stringResource(R.string.speed),
    onValueChange = {
      text = it
    },
    onInput = {
      it.toFloatOrNull()?.let { speed ->
        if (speed > 2f || speed < 0.5f) {
          ToastUtil.show(context, R.string.speed_range_tip)
          return@let
        }
        settingPrefs.speed = it
      }
    })
}


