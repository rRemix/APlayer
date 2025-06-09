package remix.myplayer.compose.ui.widget.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import remix.myplayer.compose.ui.theme.APlayerTheme
import remix.myplayer.compose.ui.theme.LocalTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun CircleSeekBar(
  modifier: Modifier,
  trackWidth: Dp = 4.dp,
  thumbRadius: Dp = 8.dp,
  progressMax: Int = 7200,
  progress: Int = 0,
  onProgressChange: ((current: Int, max: Int) -> Unit) = { _, _ ->

  }
) {
  val theme = LocalTheme.current

  var canvasCenter by remember {
    mutableStateOf(Offset.Zero)
  }
  var canvasSize by remember {
    mutableStateOf(Size.Zero)
  }
  val thumbRadiusPx = with(LocalDensity.current) {
    thumbRadius.toPx()
  }
  var thumbIsPressed by remember {
    mutableStateOf(false)
  }

  fun isTouchValid(offset: Offset): Boolean {
    val radius = canvasSize.width / 2 - thumbRadiusPx
    val distance = sqrt(
      (offset.x - canvasCenter.x).toDouble().pow(2.0) + (offset.y - canvasCenter.y).toDouble()
        .pow(2.0)
    )
    return distance >= radius - thumbRadiusPx
  }

  fun calculate(offset: Offset) {
    var rad = atan2(
      (offset.y - canvasCenter.y).toDouble(), (offset.x - canvasCenter.x).toDouble()
    )
    // 转换角度，以 12 点方向为 0 度
    rad += if (rad >= -0.5 * PI) {
      0.5 * PI
    } else {
      2.5 * PI
    }
    // 更新进度
    onProgressChange((rad / (2.0 * PI) * progressMax).toInt(), progressMax)
  }

  Canvas(
    modifier = modifier
      .pointerInput(Unit) {
        detectTapGestures {
          if (isTouchValid(it)) {
            calculate(it)
          }
        }
      }
      .pointerInput(Unit) {
        detectDragGestures(onDragStart = {
          thumbIsPressed = isTouchValid(it)
        }) { change, dragAmount ->
          if (thumbIsPressed) {
            calculate(change.position)
          }

        }
      }) {
    canvasCenter = center
    canvasSize = size

    drawCircle(
      color = theme.secondary.copy(alpha = 0.24f),
      style = Stroke(width = trackWidth.toPx())
    )

    drawArc(
      color = theme.secondary,
      style = Stroke(width = trackWidth.toPx(), cap = StrokeCap.Round),
      startAngle = -90f,
      sweepAngle = (progress * 360.0 / progressMax).toFloat(),
      useCenter = false
    )

    val rad = progress * PI * 2 / progressMax
    drawCircle(
      color = theme.secondary,
      radius = thumbRadius.toPx(),
      center = Offset(
        (center.x + sin(rad) * size.width / 2).toFloat(),
        (center.y + (-cos(rad)) * size.width / 2).toFloat()
      ),
      style = Fill
    )
  }
}


@Preview(showBackground = true)
@Composable
fun CircleSeekBarPreview() {
  APlayerTheme {
    CircleSeekBar(modifier = Modifier.size(180.dp))
  }
}