package remix.myplayer.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import remix.myplayer.ui.theme.LocalTheme

/**
 * 为 LazyColumn 添加简单的垂直滚动条
 */
@Composable
fun Modifier.verticalScrollbar(
  state: LazyListState,
  width: Dp = 8.dp,
  height: Dp = 48.dp,
  cornerRadius: Dp = width / 2,
  color: Color = LocalTheme.current.primary,
): Modifier {
  var isVisible by remember { mutableStateOf(false) }

  LaunchedEffect(state.isScrollInProgress) {
    if (state.isScrollInProgress) {
      isVisible = true
    } else {
      delay(500)
      isVisible = false
    }
  }

  val alpha by animateFloatAsState(
    targetValue = if (isVisible) 1f else 0f,
    animationSpec = tween(durationMillis = 300),
    label = "ScrollbarAlpha"
  )

  return drawWithContent {
    drawContent()

    if (alpha > 0f) {
      val visibleItemsInfo = state.layoutInfo.visibleItemsInfo
      val totalItemsCount = state.layoutInfo.totalItemsCount
      val viewportHeight = this.size.height

      if (visibleItemsInfo.isNotEmpty() && totalItemsCount > 0) {
        // 假定所有item的高度都一样
        val estimatedItemSize = visibleItemsInfo.sumOf { it.size } / visibleItemsInfo.size.toFloat()
        val totalContentHeight = estimatedItemSize * totalItemsCount

        if (totalContentHeight <= viewportHeight) return@drawWithContent

        // 计算当前和总的偏移
        val currentScrollOffset =
          state.firstVisibleItemIndex * estimatedItemSize + state.firstVisibleItemScrollOffset
        val maxScrollOffset = (totalContentHeight - viewportHeight).coerceAtLeast(1f)

        // 计算indicator偏移
        val fraction = (currentScrollOffset / maxScrollOffset).coerceIn(0f, 1f)
        val indicatorOffsetY = fraction * (viewportHeight - height.toPx())

        drawRoundRect(
          color = color,
          topLeft = Offset(this.size.width - width.toPx(), indicatorOffsetY),
          size = Size(width.toPx(), height.toPx()),
          cornerRadius = CornerRadius(cornerRadius.toPx()),
          alpha = alpha
        )
      }
    }
  }
}

/**
 * 为 LazyVerticalGrid 添加简单的垂直滚动条
 */
@Composable
fun Modifier.verticalScrollbar(
  state: LazyGridState,
  width: Dp = 8.dp,
  height: Dp = 40.dp,
  cornerRadius: Dp = width / 2,
  color: Color = LocalTheme.current.primary,
): Modifier {
  var isVisible by remember { mutableStateOf(false) }

  LaunchedEffect(state.isScrollInProgress) {
    if (state.isScrollInProgress) {
      isVisible = true
    } else {
      delay(500)
      isVisible = false
    }
  }

  val alpha by animateFloatAsState(
    targetValue = if (isVisible) 1f else 0f,
    animationSpec = tween(durationMillis = 300),
    label = "ScrollbarAlpha"
  )

  return drawWithContent {
    drawContent()

    if (alpha > 0f) {
      val visibleItemsInfo = state.layoutInfo.visibleItemsInfo
      val totalItemsCount = state.layoutInfo.totalItemsCount
      val viewportHeight = this.size.height

      if (visibleItemsInfo.isNotEmpty() && totalItemsCount > 0) {
        // 假定所有item的大小都一样
        val estimatedItemHeight = visibleItemsInfo.sumOf { it.size.height } / visibleItemsInfo.size.toFloat()
        val estimatedItemWidth = visibleItemsInfo.sumOf { it.size.width } / visibleItemsInfo.size.toFloat()

        if (estimatedItemHeight > 0 && estimatedItemWidth > 0) {
          // 计算列数、总行数和总内容高度
          val spanCount = (this.size.width / estimatedItemWidth).toInt().coerceAtLeast(1)
          val totalRows = (totalItemsCount + spanCount - 1) / spanCount
          val totalContentHeight = totalRows * estimatedItemHeight

          if (totalContentHeight <= viewportHeight) return@drawWithContent

          // 计算当前和总的偏移
          val currentScrollOffset =
            (state.firstVisibleItemIndex / spanCount) * estimatedItemHeight + state.firstVisibleItemScrollOffset
          val maxScrollOffset = (totalContentHeight - viewportHeight).coerceAtLeast(1f)

          // 计算indicator偏移
          val fraction = (currentScrollOffset / maxScrollOffset).coerceIn(0f, 1f)
          val indicatorOffsetY = fraction * (viewportHeight - height.toPx())

          drawRoundRect(
            color = color,
            topLeft = Offset(this.size.width - width.toPx(), indicatorOffsetY),
            size = Size(width.toPx(), height.toPx()),
            cornerRadius = CornerRadius(cornerRadius.toPx()),
            alpha = alpha
          )
        }
      }
    }
  }
}


@Composable
fun Modifier.clickWithRipple(
  circle: Boolean = true,
  enabled: Boolean = true,
  onClick: () -> Unit
): Modifier {
  var modifier = this
  if (circle) {
    modifier = modifier.clip(CircleShape)
  }
  return modifier.clickable(
    enabled = enabled,
    interactionSource = remember { MutableInteractionSource() },
    indication = ripple(color = LocalTheme.current.ripple), onClick = onClick
  )
}

@Composable
fun Modifier.clickableWithoutRipple(
  interactionSource: MutableInteractionSource = MutableInteractionSource(),
  enabled: Boolean = true,
  onClick: () -> Unit
) = this.clickable(
  enabled = enabled,
  interactionSource = interactionSource,
  indication = null,
) {
  onClick()
}