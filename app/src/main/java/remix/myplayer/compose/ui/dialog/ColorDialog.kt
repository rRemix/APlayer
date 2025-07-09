package remix.myplayer.compose.ui.dialog

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import remix.myplayer.R
import remix.myplayer.compose.ui.theme.APlayerTheme
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.misc.toHexString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorDialog(
  dialogState: DialogState,
  initialColor: Color,
  onDismissRequest: (() -> Unit)? = {},
  onColorChange: (Color) -> Unit,
  onPositive: () -> Unit
) {
  NormalDialog(
    dialogState = dialogState,
    onDismissRequest = onDismissRequest,
    titleRes = R.string.custom,
    positiveRes = R.string.confirm,
    onPositive = onPositive,

    custom = {
      val theme = LocalTheme.current
      var text by remember {
        mutableStateOf(initialColor.toHexString())
      }

      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
          modifier = Modifier
            .padding(top = 24.dp)
            .fillMaxWidth()
            .height(120.dp)
            .background(initialColor)
        )

        Row(
          modifier = Modifier.padding(top = 24.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          TextPrimary("#", modifier = Modifier.padding(end = 6.dp))

          val interactionSource = remember { MutableInteractionSource() }
          BasicTextField(
            value = text,
            onValueChange = {
              text = it.filter {
                it.isDigit() || it in 'a'..'f' || it in 'A'..'F'
              }
                .take(6)
                .uppercase()
              if (text.length == 6) {
                onColorChange(Color("#$text".toColorInt()))
              }
            },
            modifier = Modifier.width(IntrinsicSize.Min),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            interactionSource = interactionSource,
            textStyle = TextStyle(fontSize = 18.sp)
          ) { innerTextField ->
            TextFieldDefaults.DecorationBox(
              value = text,
              visualTransformation = VisualTransformation.None,
              innerTextField = innerTextField,
              singleLine = true,
              enabled = true,
              colors = TextFieldDefaults.colors().copy(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                cursorColor = initialColor,
                focusedTextColor = theme.textPrimary,
                unfocusedTextColor = theme.textPrimary,
                focusedIndicatorColor = initialColor,
                unfocusedIndicatorColor = initialColor
              ),
              interactionSource = interactionSource,
              contentPadding = PaddingValues(0.dp)
            )
          }
        }
      }

      SliderWithText(ColorSpace.Red, initialColor) {
        onColorChange(initialColor.copy(red = it))
      }
      SliderWithText(ColorSpace.Green, initialColor) {
        onColorChange(initialColor.copy(green = it))
      }
      SliderWithText(ColorSpace.Blue, initialColor) {
        onColorChange(initialColor.copy(blue = it))
      }
    }
  )
}


private enum class ColorSpace(val text: String) {
  Red("R"),
  Green("G"),
  Blue("B");

  fun value(color: Color): Float {
    return when (this) {
      Red -> color.red
      Green -> color.green
      Blue -> color.blue
    }
  }
}

@Composable
private fun SliderWithText(space: ColorSpace, color: Color, onValueChange: (Float) -> Unit) {
  Row(
    modifier = Modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center
  ) {
    TextPrimary(space.text)
    LineSlider(
      value = space.value(color),
      onValueChange = onValueChange,
      modifier = Modifier
        .padding(horizontal = 12.dp)
        .height(36.dp)
        .weight(1f),
      trackBackgroundColor = color,
      thumbColor = color,
    )
    TextPrimary(
      text = (space.value(color) * 255).toInt().toString(),
      modifier = Modifier.width(24.dp)
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineSlider(
  value: Float,
  onValueChange: (Float) -> Unit,
  onValueChangeFinished: () -> Unit = {},
  modifier: Modifier = Modifier,
  interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
  steps: Int = 0,
  valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
  trackHeight: Dp = 4.dp,
  trackBackgroundColor: Color = LocalTheme.current.primary,
  trackProgressColor: Color = LocalTheme.current.primary,
  thumbWidth: Dp = 12.dp,
  thumbHeight: Dp = 12.dp,
  thumbShape: Shape = CircleShape,
  thumbColor: Color = LocalTheme.current.primary,
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
        trackHeight = trackHeight,
        background = trackBackgroundColor,
        progress = trackProgressColor
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
          .width(thumbWidth)
          .height(thumbHeight)
          .scale(thumbScaleAnimValue)
          .indication(interactionSource, ripple(color = theme.ripple, bounded = false))
          .hoverable(interactionSource = interactionSource)
          .background(thumbColor, thumbShape)
      )
    }
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Track(
  sliderState: SliderState,
  trackHeight: Dp = 4.dp,
  background: Color = Color((if (LocalTheme.current.isLight) "#ffe0e0e0" else "#ff424242").toColorInt()),
  progress: Color = LocalTheme.current.primary
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
    progress = Color.Red
  )
}

@Composable
@Preview(showBackground = true)
private fun LineSliderPreview() {
  APlayerTheme {
    var sliderPosition by remember { mutableFloatStateOf(15f) }

    LineSlider(sliderPosition, modifier = Modifier.height(40.dp), onValueChange = {
      sliderPosition = it
    }, valueRange = 0f..255f, steps = 0)
  }
}
