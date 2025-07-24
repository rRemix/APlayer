package remix.myplayer.compose.ui.widget.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import remix.myplayer.compose.ui.theme.APlayerTheme
import remix.myplayer.compose.ui.theme.LocalTheme

@Immutable
data class LineSliderProperties(
  val trackHeight: Dp,
  val trackProgressColor: Color,
  val trackBackgroundColor: Color,
  val thumbWidth: Dp,
  val thumbHeight: Dp,
  val thumbShape: Shape,
  val thumbColor: Color,
)

val defaultLineSliderProperties
  @Composable
  get() = LineSliderProperties(
    trackHeight = 4.dp,
    trackProgressColor = LocalTheme.current.primary,
    trackBackgroundColor = Color((if (LocalTheme.current.isLight) "#ffe0e0e0" else "#ff424242").toColorInt()),
    thumbWidth = 12.dp,
    thumbHeight = 12.dp,
    thumbShape = CircleShape,
    thumbColor = LocalTheme.current.primary,
  )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineSlider(
  modifier: Modifier = Modifier,
  value: Float,
  onValueChange: (Float) -> Unit,
  onValueChangeFinished: () -> Unit = {},
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  steps: Int = 0,
  valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
  properties: LineSliderProperties = defaultLineSliderProperties
) {
  val theme = LocalTheme.current

  val sliderAnimValue by animateFloatAsState(targetValue = value)

  Slider(
    value = sliderAnimValue,
    onValueChange = onValueChange,
    onValueChangeFinished = onValueChangeFinished,
    modifier = modifier,
    interactionSource = interactionSource,
    steps = steps,
    valueRange = valueRange,
    track = { sliderState ->
      Track(
        sliderState,
        trackHeight = properties.trackHeight,
        background = properties.trackBackgroundColor,
        progress = properties.trackProgressColor
      )
    },
    thumb = {
      val interactions = remember { mutableStateListOf<Interaction>() }
      LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
          when (interaction) {
            is PressInteraction.Press -> interactions.add(interaction)
            is PressInteraction.Release -> interactions.remove(interaction.press)
            is PressInteraction.Cancel -> interactions.remove(interaction.press)
            is DragInteraction.Start -> interactions.add(interaction)
            is DragInteraction.Stop -> interactions.remove(interaction.start)
            is DragInteraction.Cancel -> interactions.remove(interaction.start)
          }
        }
      }

      val thumbScaleAnimValue by animateFloatAsState(
        targetValue = if (interactions.isNotEmpty()) 1.3f else 1f
      )

      Box(
        modifier = Modifier
          .fillMaxHeight()
          .wrapContentSize(Alignment.Center)
          .width(properties.thumbWidth)
          .height(properties.thumbHeight)
          .scale(thumbScaleAnimValue)
          .indication(interactionSource, ripple(color = theme.ripple, bounded = false))
          .hoverable(interactionSource = interactionSource)
          .background(properties.thumbColor, properties.thumbShape)
      )
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Track(
  sliderState: SliderState,
  trackHeight: Dp = 4.dp,
  background: Color,
  progress: Color
) {
  Spacer(
    modifier = Modifier
      .fillMaxWidth()
      .height(trackHeight)
      .background(background)
  )
  Spacer(
    modifier = Modifier
      .fillMaxWidth(fraction = sliderState.value / sliderState.valueRange.endInclusive)
      .height(trackHeight)
      .background(progress)
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview(showBackground = true)
private fun TrackPreview() {
  Track(
    SliderState(
      value = 25f,
      valueRange = 0f..255f
    ),
    progress = Color.Red,
    background = Color((if (LocalTheme.current.isLight) "#ffe0e0e0" else "#ff424242").toColorInt())
  )
}

@Composable
@Preview(showBackground = true)
private fun LineSliderPreview() {
  APlayerTheme {
    var sliderPosition by remember { mutableFloatStateOf(15f) }

    LineSlider(modifier = Modifier.height(40.dp), value = sliderPosition, onValueChange = {
      sliderPosition = it
    }, valueRange = 0f..255f, steps = 0)
  }
}