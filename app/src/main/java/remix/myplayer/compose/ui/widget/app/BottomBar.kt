package remix.myplayer.compose.ui.widget.app

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.clickableWithoutRipple
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.compose.ui.widget.common.TextSecondary
import remix.myplayer.compose.ui.widget.library.GlideCover
import remix.myplayer.compose.viewmodel.MusicViewModel
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicService.Companion.EXTRA_CONTROL
import remix.myplayer.ui.activity.PlayerActivity
import remix.myplayer.util.Util
import kotlin.math.absoluteValue

private const val triggerThreshold = 10

@Composable
fun BottomBar(modifier: Modifier = Modifier, vm: MusicViewModel = activityViewModel()) {
  val currentSong by vm.currentSong.collectAsStateWithLifecycle()
  if (currentSong.id < 0) {
    return
  }
  val playing by vm.playing.collectAsStateWithLifecycle(false)

  val context = LocalContext.current
  val interactionSource = remember { MutableInteractionSource() }

  var hasTriggerAct by remember { mutableStateOf(false) }
  var hasTriggerOp by remember { mutableStateOf(false) }
  Row(
    modifier = modifier
      .clickableWithoutRipple(interactionSource) {
        if (currentSong.id == 0L) {
          return@clickableWithoutRipple
        }
        val intent = Intent(context, PlayerActivity::class.java)
        val activity = context as? Activity
        if (activity != null && !activity.isDestroyed) {
          activity.startActivity(intent)
        }
      }
      .pointerInput(Unit) {
        // 向上滑动跳转PlayerActivity
        detectVerticalDragGestures(onDragStart = {
          hasTriggerAct = false
        }) { _, dragAmount ->
          if (dragAmount < -triggerThreshold && !hasTriggerAct) {
            hasTriggerAct = true
            if (currentSong.id == 0L) {
              return@detectVerticalDragGestures
            }
            val activity = context as? Activity
            activity?.takeIf { !it.isDestroyed }?.run {
              startActivity(Intent(context, PlayerActivity::class.java))
            }
          }
        }
      }
      .pointerInput(Unit) {
        // 左右滑动切换歌曲
        detectHorizontalDragGestures(onDragStart = {
          hasTriggerOp = false
        }) { _, dragAmount ->
          if (dragAmount.absoluteValue > triggerThreshold && !hasTriggerOp) {
            hasTriggerOp = true
            Util.sendLocalBroadcast(
              Intent(MusicService.ACTION_CMD)
                .putExtra(
                  EXTRA_CONTROL, if (dragAmount < 0) {
                    Command.NEXT
                  } else Command.PREV
                )
            )
          }
        }
      }
      .fillMaxWidth()
      .height(72.dp)
      .background(LocalTheme.current.dialogBackground),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    GlideCover(
      model = currentSong,
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
      TextPrimary(currentSong.title, fontSize = 16.sp)
      Spacer(modifier = Modifier.height(2.dp))
      TextSecondary(text = currentSong.artist, fontSize = 14.sp)
    }

    Row(
      modifier = Modifier.padding(end = 16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      val buttonColor = LocalTheme.current.bottomBarButton
      Icon(
        modifier = modifier
          .clickableWithoutRipple(interactionSource) {
            Util.sendLocalBroadcast(
              Intent(MusicService.ACTION_CMD)
                .putExtra(EXTRA_CONTROL, Command.TOGGLE)
            )
          }
          .padding(end = 16.dp),
        painter = painterResource(if (playing) R.drawable.bf_btn_stop else R.drawable.bf_btn_play),
        contentDescription = "PlayPause",
        tint = buttonColor
      )
      Icon(
        modifier = Modifier.clickableWithoutRipple(interactionSource) {
          Util.sendLocalBroadcast(
            Intent(MusicService.ACTION_CMD)
              .putExtra(EXTRA_CONTROL, Command.NEXT)
          )
        },
        painter = painterResource(R.drawable.bf_btn_next),
        contentDescription = "Next",
        tint = buttonColor
      )
    }
  }
}
