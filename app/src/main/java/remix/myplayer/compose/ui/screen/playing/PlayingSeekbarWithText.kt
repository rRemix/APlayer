package remix.myplayer.compose.ui.screen.playing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.palette.graphics.Palette
import kotlinx.coroutines.delay
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.dialog.LineSlider
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.viewmodel.MusicState
import remix.myplayer.compose.viewmodel.MusicViewModel
import remix.myplayer.util.Util

@Composable
internal fun PlayingSeekbarWithText(musicState: MusicState, swatch: Palette.Swatch) {
  val musicVM = activityViewModel<MusicViewModel>()
  var dragging by remember {
    mutableStateOf(false)
  }

  val duration = musicState.song.duration
  var progress by remember {
    mutableFloatStateOf(musicVM.getProgress().toFloat() / duration)
  }

  val time by remember {
    derivedStateOf {
      val elapsed = (progress * duration).toLong()
      Time(Util.getTime(elapsed), Util.getTime(duration - elapsed))
    }
  }

  Row(
    horizontalArrangement = Arrangement.spacedBy(20.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(horizontal = 20.dp)
  ) {

    Text(
      text = time.elapsed,
      fontSize = 12.sp
    )
    LineSlider(
      value = progress,
      onValueChange = {
        dragging = true
        progress = it
      },
      onValueChangeFinished = {
        musicVM.setProgress((progress * duration).toInt())
        dragging = false
      },
      modifier = Modifier
        .height(12.dp)
        .weight(1f),
      trackHeight = 2.dp,
      trackBackgroundColor = trackBackgroundColor(),
      trackProgressColor = Color((swatch.rgb)),
      thumbColor = Color(swatch.rgb),
      thumbWidth = 2.dp,
      thumbHeight = 6.dp,
      thumbShape = RectangleShape
    )

    Text(
      text = time.remaining,
      fontSize = 12.sp
    )
  }

  LaunchedEffect(musicState.playing) {
    while (musicState.playing) {
      withFrameMillis {
        progress = musicVM.getProgress().toFloat() / duration
      }
      delay(500)
    }
  }
}

@Composable
fun trackBackgroundColor(): Color = Color(
  if (LocalTheme.current.isLight) {
    "#efeeed"
  } else {
    "#343438"
  }.toColorInt()
)

private data class Time(val elapsed: String, val remaining: String)