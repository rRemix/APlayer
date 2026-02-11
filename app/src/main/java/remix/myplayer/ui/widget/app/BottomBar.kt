package remix.myplayer.ui.widget.app

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicService.Companion.EXTRA_COMMAND
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.ui.widget.library.GlideCover
import remix.myplayer.util.Util
import remix.myplayer.util.ext.clickableWithoutRipple
import remix.myplayer.viewmodel.PlaybackViewModel
import remix.myplayer.viewmodel.PlayingScreenValue
import remix.myplayer.viewmodel.mainViewModel
import remix.myplayer.viewmodel.playbackViewModel
import kotlin.math.absoluteValue

private const val triggerThreshold = 10

@Composable
fun BottomBar(modifier: Modifier = Modifier, vm: PlaybackViewModel = playbackViewModel) {
  val mainVM = mainViewModel
  val scope = rememberCoroutineScope()
  val playbackState by vm.playbackUiState.collectAsStateWithLifecycle()
  val interactionSource = remember { MutableInteractionSource() }

  var hasTriggerAct by remember { mutableStateOf(false) }
  var hasTriggerOp by remember { mutableStateOf(false) }

  val baseModifier = modifier
    .fillMaxWidth()
    .height(72.dp)
    .background(LocalTheme.current.dialogBackground)

  val isSongValid = playbackState.song.valid()
  val interactionModifiers = if (isSongValid) {
    Modifier
      // 点击跳转播放页
      .clickableWithoutRipple(interactionSource) {
        scope.launch {
          mainVM.playingScreenState.animateTo(PlayingScreenValue.Expanded)
        }
      }
      // 垂直滑动跳转播放页
      .pointerInput(Unit) {
        detectVerticalDragGestures(
          onDragStart = { hasTriggerAct = false }
        ) { _, dragAmount ->
          if (dragAmount < -triggerThreshold && !hasTriggerAct) {
            hasTriggerAct = true
            scope.launch {
              mainVM.playingScreenState.animateTo(PlayingScreenValue.Expanded)
            }
          }
        }
      }
      // 水平滑动切换歌曲
      .pointerInput(Unit) {
        detectHorizontalDragGestures(
          onDragStart = { hasTriggerOp = false }
        ) { _, dragAmount ->
          if (dragAmount.absoluteValue > triggerThreshold && !hasTriggerOp) {
            hasTriggerOp = true
            Util.sendLocalBroadcast(
              Intent(MusicService.ACTION_CMD)
                .putExtra(
                  EXTRA_COMMAND,
                  if (dragAmount < 0) Command.SKIP_TO_NEXT else Command.SKIP_TO_PREVIOUS
                )
            )
          }
        }
      }
  } else {
    // 歌曲无效时，不响应任何操作
    Modifier
  }

  Row(
    modifier = baseModifier
      .semantics{ contentDescription = "BottomBar" }
      .then(interactionModifiers),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    GlideCover(
      model = playbackState.song,
      modifier = Modifier
        .padding(start = 12.dp)
        .size(48.dp)
    )
    Column(
      verticalArrangement = Arrangement.Center,
      modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .padding(horizontal = 8.dp)
    ) {
      TextPrimary(playbackState.song.title, fontSize = 16.sp)
      Spacer(modifier = Modifier.height(2.dp))
      TextSecondary(text = playbackState.song.artist, fontSize = 14.sp)
    }

    Row(
      modifier = Modifier.padding(end = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      val buttonColor =
        Color((if (LocalTheme.current.isLight) "#323334" else "#FFFFFF").toColorInt())
      Icon(
        modifier = modifier
          .clickableWithoutRipple(interactionSource) {
            Util.sendLocalBroadcast(
              Intent(MusicService.ACTION_CMD)
                .putExtra(EXTRA_COMMAND, Command.PLAY_PAUSE)
            )
          }
          .padding(end = 16.dp),
        painter = painterResource(if (playbackState.isPlaying) R.drawable.bf_btn_stop else R.drawable.bf_btn_play),
        contentDescription = "PlayPause",
        tint = buttonColor
      )
      Icon(
        modifier = Modifier.clickableWithoutRipple(interactionSource) {
          Util.sendLocalBroadcast(
            Intent(MusicService.ACTION_CMD)
              .putExtra(EXTRA_COMMAND, Command.SKIP_TO_NEXT)
          )
        },
        painter = painterResource(R.drawable.bf_btn_next),
        contentDescription = "Next",
        tint = buttonColor
      )
    }
  }
}
