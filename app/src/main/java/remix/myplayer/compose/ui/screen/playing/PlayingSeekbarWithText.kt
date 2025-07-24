package remix.myplayer.compose.ui.screen.playing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import kotlinx.coroutines.isActive
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.common.LineSlider
import remix.myplayer.compose.ui.widget.common.defaultLineSliderProperties
import remix.myplayer.compose.viewmodel.MusicState
import remix.myplayer.compose.viewmodel.musicViewModel
import remix.myplayer.util.Util

@Composable
internal fun PlayingSeekbarWithText(musicState: MusicState, swatch: Palette.Swatch) {
  val musicVM = musicViewModel
  var dragging by remember {
    mutableStateOf(false)
  }

  val duration = musicState.song.duration
  var progress by remember {
    mutableFloatStateOf(musicVM.getProgress().toFloat() / duration)
  }

  val time by remember {
    derivedStateOf {
      if (duration <= 0) {
        Time("00:00", "00:00")
      } else {
        val elapsed = (progress * duration).toLong()
        val remaining = duration - elapsed
        Time(Util.getTime(elapsed), Util.getTime(remaining))
      }
    }
  }

  val playingTrackBackgroundColor = playingTrackBackgroundColor
  val baseProperties = defaultLineSliderProperties
  val sliderProperties = remember(swatch.rgb) {
    baseProperties.copy(
      trackBackgroundColor = playingTrackBackgroundColor,
      trackProgressColor = Color(swatch.rgb),
      trackHeight = 2.dp,
      thumbColor = Color(swatch.rgb),
      thumbWidth = 2.dp,
      thumbHeight = 6.dp,
      thumbShape = RectangleShape
    )
  }

  val textColor = remember {
    Color("#6b6b6b".toColorInt())
  }

  Row(
    horizontalArrangement = Arrangement.spacedBy(20.dp),
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(horizontal = 20.dp)
  ) {

    Text(
      text = time.elapsed,
      fontSize = 12.sp,
      modifier = Modifier.width(36.dp),
      color = textColor
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
      properties = sliderProperties
    )

    Text(
      text = time.remaining,
      fontSize = 12.sp,
      modifier = Modifier.width(36.dp),
      color = textColor
    )
  }

  LaunchedEffect(musicState.playing) {
    while (musicState.playing && isActive) {
      withFrameMillis {
        progress = musicVM.getProgress().toFloat() / duration
      }
      delay(500)
    }
  }
}

val playingTrackBackgroundColor: Color
  @Composable
  get() = Color(
    if (LocalTheme.current.isLight) {
      "#efeeed"
    } else {
      "#343438"
    }.toColorInt()
  )


private data class Time(val elapsed: String, val remaining: String)