package remix.myplayer.ui.screen.playing

import android.content.Context
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.misc.LyricOrder
import remix.myplayer.data.prefs.LyricPrefs
import remix.myplayer.data.prefs.delegate
import remix.myplayer.helper.EQHelper
import remix.myplayer.lyric.LyricManager.Companion.ACTION_LYRIC
import remix.myplayer.lyric.LyricManager.Companion.CHANGE_LYRIC
import remix.myplayer.lyric.LyricManager.Companion.EXTRA_LYRIC
import remix.myplayer.lyric.LyricManager.Companion.EXTRA_LYRIC_URI
import remix.myplayer.lyric.LyricManager.Companion.SHOW_OFFSET_PANEL
import remix.myplayer.lyric.LyricSearcher
import remix.myplayer.ui.activity.base.BaseActivity
import remix.myplayer.ui.dialog.DialogState
import remix.myplayer.ui.dialog.InputDialog
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.nav.RouteTagEdit
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.util.Util.sendLocalBroadcast
import remix.myplayer.util.ext.ShowLyricTipDialog
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.settingViewModel
import remix.myplayer.viewmodel.settings.SettingsState
import remix.myplayer.viewmodel.tagEditViewModel
import remix.myplayer.viewmodel.timerViewModel

@Composable
fun PlayingDropDownMenu(
  expanded: Boolean,
  song: Song,
  onDismissRequest: () -> Unit
) {
  if (!song.valid()) {
    return
  }
  val nav = LocalNavController.current
  val libraryVM = libraryViewModel
  val settingVM = settingViewModel
  val tagEditVM = tagEditViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()

  val menuItems =
    listOf(
//      R.string.song_edit,
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

  val timerVM = timerViewModel

  val speedDialogState = rememberDialogState()
  SpeedDialog(speedDialogState, settingState)

  val lyricDialogState = rememberDialogState()

  var checkLyricTip by rememberSaveable { mutableStateOf(false) }
  if (checkLyricTip) {
    settingVM.lyricPrefs.tipShown = true
    ShowLyricTipDialog {
      lyricDialogState.show()
    }
  }

  var order by settingVM.lyricPrefs.sp.delegate(
    "${LyricPrefs.KEY_SONG_PREFIX}${LyricSearcher.getStorageKey(song)}",
    LyricOrder.Def.toString()
  )

  val launcher =
    rememberLauncherForActivityResult(contract = object : ActivityResultContracts.GetContent() {
      override fun createIntent(context: Context, input: String): Intent {
        val intent = super.createIntent(context, input)
        return intent
      }
    }) {
      if (it != null) {
        sendLocalBroadcast(
          Intent(ACTION_LYRIC)
            .putExtra(EXTRA_LYRIC, CHANGE_LYRIC)
            .putExtra(EXTRA_LYRIC_URI, it)
        )
      } else {
        MessageNotifier.show(R.string.no_lrc)
      }
    }
  LyricSettingDialog(
    lyricDialogState,
    order,
    onSaveLyric = {
      if (it == LyricOrder.Manual) {
        try {
          // 这里用text/plain，避免某些机型找不到activity
          launcher.launch("text/plain")
        } catch (_: Exception) {
          MessageNotifier.show(R.string.activity_not_found_tip)
        }
      } else {
        order = it.toString()
        sendLocalBroadcast(Intent(ACTION_LYRIC).putExtra(EXTRA_LYRIC, CHANGE_LYRIC))
      }
    })

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
                tagEditVM.startTagEdit(song)
                nav.navigate(RouteTagEdit)
              }
            }

            R.string.song_detail -> {
              if (activity == null) {
                return@DropdownMenuItem
              }
              settingVM.showSongDetailDialog(song)
            }

            R.string.add_to_playlist -> {
              settingVM.showAddSongToPlayListDialog(listOf(song.id))
            }

            R.string.collect -> {
              if (activity == null) {
                return@DropdownMenuItem
              }
              val favorite =
                libraryVM.playLists.value.firstOrNull { it.isFavorite() } ?: return@DropdownMenuItem

              libraryVM.addSongsToPlayList(listOf(song.id), favorite.name)
            }

            R.string.sleep_timer -> {
              timerVM.showTimerDialog()
            }

            R.string.eq -> {
              if (activity == null) {
                return@DropdownMenuItem
              }
              EQHelper.startEqualizer(activity, nav)
            }

            R.string.lyric -> {
              if (settingVM.lyricPrefs.tipShown) {
                lyricDialogState.show()
              } else {
                checkLyricTip = true
              }
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
private fun LyricSettingDialog(
  state: DialogState,
  order: String,
  onSaveLyric: (LyricOrder) -> Unit
) {
  val alreadyIgnore = order == LyricOrder.Ignore.toString()
  val itemRes = listOf(
    R.string.embedded_lyric,
    R.string.local,
    R.string.kugou,
    R.string.netease,
    R.string.qq,
    R.string.select_lrc,
    if (!alreadyIgnore) R.string.ignore_lrc else R.string.cancel_ignore_lrc,
    R.string.lyric_adjust_font_size,
    R.string.change_offset
  )

  val fontScaleDialogState = rememberDialogState(false)
  LyricFontScaleDialog(fontScaleDialogState)

  NormalDialog(
    dialogState = state,
    itemRes = itemRes,
    positiveRes = null,
    negativeRes = null,
    itemsCallback = { index, _ ->

      when (index) {
        0 -> {
          onSaveLyric(LyricOrder.Embedded)
        }

        1 -> {
          onSaveLyric(LyricOrder.Local)
        }

        2 -> {
          onSaveLyric(LyricOrder.Kugou)
        }

        3 -> {
          onSaveLyric(LyricOrder.Netease)
        }

        4 -> {
          onSaveLyric(LyricOrder.Qq)
        }

        5 -> {
          onSaveLyric(LyricOrder.Manual)
        }

        6 -> {
          onSaveLyric(if (!alreadyIgnore) LyricOrder.Ignore else LyricOrder.Def)
        }

        // 调整歌词字体缩放
        7 -> {
          fontScaleDialogState.show()
        }

        // 调整歌词offset
        8 -> {
          sendLocalBroadcast(Intent(ACTION_LYRIC).putExtra(EXTRA_LYRIC, SHOW_OFFSET_PANEL))
        }
      }
    }
  )
}

@Composable
private fun LyricFontScaleDialog(state: DialogState) {
  val settingVM = settingViewModel

  NormalDialog(
    dialogState = state,
    items = listOf("0.5", "1.0", "1.5", "2.0"),
    positive = null,
    negative = null,
    itemsCallback = { index, str ->
      settingVM.setLyricFontScale(str.toFloat())
    }
  )
}

@Composable
private fun SpeedDialog(state: DialogState, settingState: SettingsState) {
  val settingVM = settingViewModel

  var text by rememberSaveable {
    mutableStateOf(settingState.play.speed)
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
          MessageNotifier.show(R.string.speed_range_tip)
          return@let
        }
        settingVM.setSpeed(it)
      }
    })
}
