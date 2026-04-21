package remix.myplayer.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import remix.myplayer.R
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.LineSlider
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.defaultLineSliderProperties
import remix.myplayer.util.ext.isPortraitOrientation
import remix.myplayer.util.ext.isTablet
import remix.myplayer.util.ext.toHexString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorDialog(
  dialogState: DialogState,
  initialColor: Color,
  titleRes: Int,
  onDismissRequest: (() -> Unit)? = {},
  onColorChange: (Color) -> Unit,
  onPositive: () -> Unit
) {
  val isLandscape = !LocalContext.current.isPortraitOrientation()
  val isTablet = LocalContext.current.isTablet()

  NormalDialog(
    dialogState = dialogState,
    onDismissRequest = onDismissRequest,
    titleRes = titleRes,
    positiveRes = R.string.confirm,
    onPositive = onPositive,
    usePlatformDefaultWidth = !isLandscape,
    custom = {
      val theme = LocalTheme.current
      var text by remember(initialColor) {
        mutableStateOf(initialColor.toHexString())
      }
      val interactionSource = remember { MutableInteractionSource() }

      @Composable
      fun ColorTextInput(modifier: Modifier = Modifier) {
        Row(
          modifier = modifier,
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          TextPrimary("#", modifier = Modifier.padding(end = 6.dp))

          BasicTextField(
            value = text,
            onValueChange = { input ->
              text = input.filter { char ->
                char.isDigit() || char in 'a'..'f' || char in 'A'..'F'
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

      if (isLandscape) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .then(
              if (isTablet) Modifier.height(180.dp)
              else Modifier.weight(1f)
            ),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
          verticalAlignment = Alignment.Top
        ) {
          Box(
            modifier = Modifier
              .weight(0.45f)
              .fillMaxHeight()
              .background(initialColor)
          )

          Column(
            modifier = Modifier
              .weight(0.55f)
              .fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
          ) {
            ColorTextInput()

            val sliderHeight = if (isTablet) 36.dp else 16.dp
            ColorSpace.entries.forEach { space ->
              SliderWithText(space, initialColor, sliderHeight) {
                onColorChange(space.copy(initialColor, it))
              }
            }
          }
        }
      } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Box(
            modifier = Modifier
              .padding(top = 24.dp)
              .fillMaxWidth()
              .height(120.dp)
              .background(initialColor)
          )
          ColorTextInput(modifier = Modifier.padding(top = 24.dp))
        }

        ColorSpace.entries.forEach { space ->
          SliderWithText(space, initialColor) {
            onColorChange(space.copy(initialColor, it))
          }
        }
      }
    }
  )
}

enum class ColorSpace(val text: String) {
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

  fun copy(color: Color, value: Float): Color {
    return when (this) {
      Red -> color.copy(red = value)
      Green -> color.copy(green = value)
      Blue -> color.copy(blue = value)
    }
  }
}

@Composable
private fun SliderWithText(
  space: ColorSpace,
  color: Color,
  height: Dp = 36.dp,
  onValueChange: (Float) -> Unit
) {
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
        .height(height)
        .weight(1f),
      properties = defaultLineSliderProperties.copy(
        trackProgressColor = color,
        thumbColor = color
      ),
    )
    TextPrimary(
      text = (space.value(color) * 255).toInt().toString(),
      modifier = Modifier.width(24.dp)
    )
  }
}
