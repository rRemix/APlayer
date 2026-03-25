package remix.myplayer.ui.widget.lyric

import androidx.annotation.DrawableRes
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.ui.widget.common.LineSlider
import remix.myplayer.ui.widget.common.defaultLineSliderProperties
import remix.myplayer.util.ext.clickWithRipple

@Composable
internal fun FontSizeContainer(
  firstLineSize: Float,
  secondLineSize: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  sliderTintColor: Color,
  onInteractingStatusChange: (Boolean) -> Unit,
  onValueChange: (Int, Float) -> Unit
) {
  // 监听slider交互，取消延迟隐藏
  val firstSliderSource = remember { MutableInteractionSource() }
  val secondSliderSource = remember { MutableInteractionSource() }

  rememberSlidersInteractionListener(
    listOf(firstSliderSource, secondSliderSource),
    onInteractingStatusChange
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    FontSizeSetting(
      icon = R.drawable.ic_looks_one_24dp,
      desc = "DkpFirstLineFontSize",
      value = firstLineSize,
      valueRange = valueRange,
      sliderTintColor = sliderTintColor,
      interactionSource = firstSliderSource,
      onValueChange = {
        onValueChange(0, it)
      }
    )
    FontSizeSetting(
      icon = R.drawable.ic_looks_two_24dp,
      desc = "DkpSecondLineFontSize",
      value = secondLineSize,
      valueRange = valueRange,
      sliderTintColor = sliderTintColor,
      interactionSource = secondSliderSource,
      onValueChange = {
        onValueChange(1, it)
      }
    )
  }
}

@Composable
private fun FontSizeSetting(
  @DrawableRes icon: Int,
  desc: String,
  value: Float,
  valueRange: ClosedFloatingPointRange<Float>,
  sliderTintColor: Color,
  interactionSource: MutableInteractionSource,
  onValueChange: (Float) -> Unit
) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    SliderIcon(icon, desc)
    SliderIcon(R.drawable.ic_text_decrease_24dp, "${desc}_decrease") {
      val newValue = value - 1
      if (newValue in valueRange) {
        onValueChange(newValue)
      }
    }

    LineSlider(
      modifier = Modifier
        .weight(1f)
        .height(24.dp),
      interactionSource = interactionSource,
      value = value,
      onValueChange = onValueChange,
      valueRange = valueRange,
      properties = defaultLineSliderProperties.copy(
        trackProgressColor = sliderTintColor,
        thumbColor = sliderTintColor
      )
    )

    SliderIcon(R.drawable.ic_text_increase_24dp, "${desc}_increase") {
      val newValue = value + 1
      if (newValue in valueRange) {
        onValueChange(newValue)
      }
    }
  }
}

@Composable
internal fun SliderIcon(@DrawableRes icon: Int, desc: String, onClick: (() -> Unit)? = null) {
  val modifier = if (onClick != null) {
    Modifier.clickWithRipple {
      onClick.invoke()
    }
  } else {
    Modifier
  }

  Icon(
    modifier = modifier
      .size(dimensionResource(R.dimen.desktop_lyrics_slider_icon_size))
      .padding(dimensionResource(R.dimen.desktop_lyrics_slider_icon_padding)),
    painter = painterResource(icon),
    contentDescription = desc,
    tint = colorResource(R.color.desktop_lyrics_control_color)
  )

}

@Composable
@Preview(showBackground = true)
fun SliderPreview() {
  var value by remember {
    mutableFloatStateOf(0f)
  }
  LineSlider(
    modifier = Modifier
      .height(24.dp),
    steps = 24,
    value = value,
    onValueChange = {
      value = it
    },
    valueRange = 8f..32f,
    properties = defaultLineSliderProperties
  )
}
