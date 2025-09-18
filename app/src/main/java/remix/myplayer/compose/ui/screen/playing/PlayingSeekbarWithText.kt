package remix.myplayer.compose.ui.screen.playing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.palette.graphics.Palette
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.app.ProgressAware
import remix.myplayer.compose.ui.widget.common.LineSlider
import remix.myplayer.compose.ui.widget.common.defaultLineSliderProperties
import remix.myplayer.compose.viewmodel.musicViewModel
import remix.myplayer.util.Util

@Composable
internal fun PlayingSeekbarWithText(swatch: Palette.Swatch) {
  val musicVM = musicViewModel

  ProgressAware { progress, duration ->
    var dragging by remember {
      mutableStateOf(false)
    }

    var uiProgress by remember {
      mutableLongStateOf(0)
    }

    var time by remember {
      mutableStateOf(Time("00:00", "00:00"))
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
        maxLines = 1,
        modifier = Modifier.width(36.dp),
        color = textColor
      )

      LineSlider(
        value = (if (dragging) uiProgress else progress).toFloat(),
        onValueChange = {
          dragging = true
          uiProgress = it.toLong()
        },
        onValueChangeFinished = {
          musicVM.setProgress(uiProgress.toLong())
          dragging = false
        },
        valueRange = 0f..duration.toFloat(),

        modifier = Modifier
          .height(12.dp)
          .weight(1f),
        properties = sliderProperties
      )

      Text(
        text = time.remaining,
        fontSize = 12.sp,
        maxLines = 1,
        modifier = Modifier.width(36.dp),
        color = textColor
      )
    }

    LaunchedEffect(uiProgress, progress) {
      time = if (duration <= 0) {
        Time("00:00", "00:00")
      } else {
        val elapsed = if (dragging) uiProgress.toLong() else progress
        val remaining = duration - elapsed
        Time(Util.getTime(elapsed), Util.getTime(remaining))
      }
    }
  }
}

internal val playingTrackBackgroundColor: Color
  @Composable
  get() = Color(
    if (LocalTheme.current.isLight) {
      "#efeeed"
    } else {
      "#343438"
    }.toColorInt()
  )


private data class Time(val elapsed: String, val remaining: String)