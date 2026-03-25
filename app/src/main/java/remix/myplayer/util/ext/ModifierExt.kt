package remix.myplayer.util.ext

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import remix.myplayer.ui.theme.LocalTheme
import kotlin.math.abs
import kotlin.math.roundToInt

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
  val targetScroll = remember { mutableStateOf<Float?>(null) }

  LaunchedEffect(state) {
    snapshotFlow { targetScroll.value }
      .collectLatest { progress ->
        if (progress != null) {
          val visibleItemsInfo = state.layoutInfo.visibleItemsInfo
          val totalItemsCount = state.layoutInfo.totalItemsCount

          if (visibleItemsInfo.isNotEmpty() && totalItemsCount > 0) {
            val estimatedItemSize = visibleItemsInfo.sumOf { it.size } / visibleItemsInfo.size.toFloat()
            if (estimatedItemSize > 0f) {
              val exactIndex = progress * totalItemsCount
              val index = exactIndex.toInt()
              val offset = ((exactIndex - index) * estimatedItemSize).roundToInt()
              state.scrollToItem(index, -offset)
            }
          }
        }
      }
  }

  return verticalScrollbarImpl(
    isScrollInProgress = state.isScrollInProgress,
    width = width,
    height = height,
    cornerRadius = cornerRadius,
    color = color,
    onDrag = { progress ->
      targetScroll.value = progress
    },
    calculateScrollMetrics = { _ ->
      val visibleItemsInfo = state.layoutInfo.visibleItemsInfo
      val totalItemsCount = state.layoutInfo.totalItemsCount

      if (visibleItemsInfo.isNotEmpty() && totalItemsCount > 0) {
        val estimatedItemSize = visibleItemsInfo.sumOf { it.size } / visibleItemsInfo.size.toFloat()
        val totalContentHeight = estimatedItemSize * totalItemsCount
        val currentScrollOffset =
          state.firstVisibleItemIndex * estimatedItemSize + state.firstVisibleItemScrollOffset
        totalContentHeight to currentScrollOffset
      } else null
    }
  )
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
  val targetScroll = remember { mutableStateOf<Float?>(null) }

  LaunchedEffect(state) {
    snapshotFlow { targetScroll.value }
      .collectLatest { progress ->
        if (progress != null) {
          val visibleItemsInfo = state.layoutInfo.visibleItemsInfo
          val totalItemsCount = state.layoutInfo.totalItemsCount

          if (visibleItemsInfo.isNotEmpty() && totalItemsCount > 0) {
            val estimatedItemHeight =
              visibleItemsInfo.sumOf { it.size.height } / visibleItemsInfo.size.toFloat()
            val estimatedItemWidth =
              visibleItemsInfo.sumOf { it.size.width } / visibleItemsInfo.size.toFloat()

            if (estimatedItemHeight > 0 && estimatedItemWidth > 0) {
              val spanCount =
                (state.layoutInfo.viewportSize.width / estimatedItemWidth).toInt().coerceAtLeast(1)
              val totalRows = (totalItemsCount + spanCount - 1) / spanCount

              val exactRow = progress * totalRows
              val row = exactRow.toInt()
              val offset = ((exactRow - row) * estimatedItemHeight).roundToInt()
              state.scrollToItem(row * spanCount, -offset)
            }
          }
        }
      }
  }

  return verticalScrollbarImpl(
    isScrollInProgress = state.isScrollInProgress,
    width = width,
    height = height,
    cornerRadius = cornerRadius,
    color = color,
    onDrag = { progress ->
      targetScroll.value = progress
    },
    calculateScrollMetrics = { viewportSize ->
      val visibleItemsInfo = state.layoutInfo.visibleItemsInfo
      val totalItemsCount = state.layoutInfo.totalItemsCount

      if (visibleItemsInfo.isNotEmpty() && totalItemsCount > 0) {
        val estimatedItemHeight =
          visibleItemsInfo.sumOf { it.size.height } / visibleItemsInfo.size.toFloat()
        val estimatedItemWidth =
          visibleItemsInfo.sumOf { it.size.width } / visibleItemsInfo.size.toFloat()

        if (estimatedItemHeight > 0 && estimatedItemWidth > 0) {
          val spanCount = (viewportSize.width / estimatedItemWidth).toInt().coerceAtLeast(1)
          val totalRows = (totalItemsCount + spanCount - 1) / spanCount
          val totalContentHeight = totalRows * estimatedItemHeight

          val currentScrollOffset =
            (state.firstVisibleItemIndex / spanCount) * estimatedItemHeight + state.firstVisibleItemScrollOffset
          totalContentHeight to currentScrollOffset
        } else null
      } else null
    }
  )
}

@Composable
private fun Modifier.verticalScrollbarImpl(
  isScrollInProgress: Boolean,
  width: Dp,
  height: Dp,
  cornerRadius: Dp,
  color: Color,
  onDrag: (progress: Float) -> Unit,
  calculateScrollMetrics: (viewportSize: Size) -> Pair<Float, Float>?
): Modifier {
  var isVisible by remember { mutableStateOf(false) }
  var isDragging by remember { mutableStateOf(false) }
  val density = LocalDensity.current

  LaunchedEffect(isScrollInProgress, isDragging) {
    if (isScrollInProgress || isDragging) {
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

  return this
    .pointerInput(Unit) {
      awaitEachGesture {
        val indicatorHeightPx = with(density) { height.toPx() }
        val touchSlop = viewConfiguration.touchSlop

        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

        if (down.position.x < size.width - with(density) { width.toPx() + 16.dp.toPx() }) {
          return@awaitEachGesture
        }

        var dragStarted = false

        do {
          val event = awaitPointerEvent(pass = PointerEventPass.Initial)
          val change = event.changes.firstOrNull { it.id == down.id }

          if (change == null || !change.pressed) {
            isDragging = false
            break
          }

          val totalDragY = change.position.y - down.position.y
          val totalDragX = change.position.x - down.position.x
          if (!dragStarted) {
            val isVerticalDrag =
              abs(totalDragY) >= touchSlop && abs(totalDragY) > abs(totalDragX)
            if (!isVerticalDrag) {
              continue
            }

            dragStarted = true
            isDragging = true
          }

          val trackHeight = size.height.toFloat()
          val maxOffset = trackHeight - indicatorHeightPx
          if (maxOffset > 0) {
            val thumbTop = (change.position.y - indicatorHeightPx / 2f).coerceIn(0f, maxOffset)
            val progress = thumbTop / maxOffset
            onDrag(progress)
          }

          change.consume()
        } while (true)
      }
    }
    .drawWithContent {
      drawContent()

      if (alpha > 0f) {
        val metrics = calculateScrollMetrics(this.size)
        if (metrics != null) {
          val (totalContentHeight, currentScrollOffset) = metrics
          val viewportHeight = this.size.height

          if (totalContentHeight > viewportHeight) {
            val maxScrollOffset = (totalContentHeight - viewportHeight).coerceAtLeast(1f)
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
