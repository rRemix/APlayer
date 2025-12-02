package remix.myplayer.ui.widget.lyric

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.ui.dialog.ColorSpace
import remix.myplayer.ui.widget.common.LineSlider
import remix.myplayer.ui.widget.common.defaultLineSliderProperties

@Composable
internal fun FontColorContainer(
  color: Color,
  onInteractingStatusChange: (Boolean) -> Unit,
  onValueChange: (ColorSpace, Float) -> Unit
) {
  // 监听slider交互，取消延迟隐藏
  val rSliderSource = remember { MutableInteractionSource() }
  val gSliderSource = remember { MutableInteractionSource() }
  val bSliderSource = remember { MutableInteractionSource() }

  rememberSlidersInteractionListener(
    listOf(rSliderSource, gSliderSource, bSliderSource),
    onInteractingStatusChange
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    ColorSlider(ColorSpace.Red, color, rSliderSource) {
      onValueChange(ColorSpace.Red, it)
    }
    ColorSlider(ColorSpace.Green, color, gSliderSource) {
      onValueChange(ColorSpace.Green, it)
    }
    ColorSlider(ColorSpace.Blue, color, bSliderSource) {
      onValueChange(ColorSpace.Blue, it)
    }
  }
}

@Composable
private fun ColorSlider(
  space: ColorSpace,
  color: Color,
  interactionSource: MutableInteractionSource,
  onValueChange: (Float) -> Unit
) {
  Row(
    modifier = Modifier.padding(horizontal = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(space.text, color = colorResource(R.color.desktop_lyrics_control_color))
    LineSlider(
      modifier = Modifier
        .weight(1f)
        .height(24.dp),
      interactionSource = interactionSource,
      value = space.value(color),
      onValueChange = onValueChange,
      properties = defaultLineSliderProperties.copy(
        trackProgressColor = color,
        thumbColor = color
      )
    )
  }
}