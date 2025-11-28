package remix.myplayer.ui.screen.playing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.palette.graphics.Palette
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.app.ProgressAware
import remix.myplayer.ui.widget.common.LineSlider
import remix.myplayer.ui.widget.common.defaultLineSliderProperties
import remix.myplayer.util.Util
import remix.myplayer.viewmodel.playbackViewModel
import kotlin.math.roundToLong

@Composable
internal fun PlayingSeekbarWithText(
  swatch: Palette.Swatch
) {
  val playbackVM = playbackViewModel

  ProgressAware { progress, duration ->
    val seekBarUiState by playbackVM.seekBarUiState.collectAsStateWithLifecycle()

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
        value = if (seekBarUiState.interacting && seekBarUiState.uiProgress != null) {
          seekBarUiState.uiProgress!!.toFloat()
        } else {
          progress.toFloat()
        },
        onValueChange = { v ->
          playbackVM.setSeekbarUiState(v.roundToLong(), true)
        },
        onValueChangeFinished = {
          playbackVM.setProgress(playbackVM.seekBarUiState.value.uiProgress ?: progress)
          playbackVM.setSeekbarUiState(null, false)
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

    LaunchedEffect(seekBarUiState, progress) {
      time = if (duration <= 0) {
        Time("00:00", "00:00")
      } else {
        val elapsed = seekBarUiState.uiProgress ?: progress
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
