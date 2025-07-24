package remix.myplayer.compose.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import remix.myplayer.R
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.common.LineSlider
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.compose.ui.widget.common.defaultLineSliderProperties
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
