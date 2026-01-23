package remix.myplayer.ui.screen.playing

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import remix.myplayer.R
import remix.myplayer.lyric.LyricLine
import remix.myplayer.lyric.PerWordLyricLine
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.ui.widget.lyric.LyricMultiLine
import remix.myplayer.util.ext.clickWithRipple
import remix.myplayer.viewmodel.playbackViewModel
import kotlin.math.max
import kotlin.math.roundToInt

const val DEFAULT_TEXT_SIZE = 15f
private val DEFAULT_ANIM_SPEC =
  tween<Float>(durationMillis = 400, easing = FastOutSlowInEasing)
private const val HIGHLIGHT_SCALE = 1.2f
private const val LYRIC_WIDTH_FRACTION = 1f / HIGHLIGHT_SCALE

@Composable
internal fun LyricContainer(
  modifier: Modifier,
  lyrics: List<LyricLine>,
  rawProgress: Long,
  rawDuration: Long,
  offset: Long,
  fontScale: Float
) {
  val seekBarUiState by playbackViewModel.seekBarUiState.collectAsStateWithLifecycle()
  val progress = (seekBarUiState.uiProgress ?: rawProgress) + offset
  val duration = rawDuration + offset

  val playbackVM = playbackViewModel
  val scrollState = rememberScrollState()
  var highlightIndex by remember(lyrics) {
    mutableIntStateOf(0)
  }

  // 用户是否在滑动
  var dragging by remember {
    mutableStateOf(false)
  }
  // 是否允许外部更新进度
  var allowProgressUpdates by remember {
    mutableStateOf(true)
  }

  var viewportHeightPx by remember { mutableIntStateOf(0) }
  // 记录每行的布局的坐标和尺寸
  val lineBounds = remember(lyrics) {
    mutableStateListOf<LineBounds?>().apply { repeat(lyrics.size) { add(null) } }
  }

  val fontSize = (fontScale * DEFAULT_TEXT_SIZE).sp

  Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
    Column(
      modifier = modifier
        .fillMaxSize()
        .onSizeChanged { viewportHeightPx = it.height }
        .nestedScroll(remember {
          object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
              if (source == NestedScrollSource.UserInput && available.y != 0f) {
                dragging = true
              }
              return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
              // 惯性滚动结束后，认为不再滑动
              dragging = false
              return super.onPostFling(consumed, available)
            }
          }
        })
        .verticalScroll(scrollState),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // 顶部占位，保证高亮行有空间滚动到视口中间
      Spacer(Modifier.height(with(LocalDensity.current) { (viewportHeightPx / 2).toDp() }))

      lyrics.forEachIndexed { index, line ->
        val isHighLight = index == highlightIndex
        val scale by animateFloatAsState(
          targetValue = if (isHighLight) HIGHLIGHT_SCALE else 1.0f,
          animationSpec = DEFAULT_ANIM_SPEC
        )
        Column(
          modifier = Modifier
            // 预留缩放后的空间，避免高亮时长句被裁切
            .fillMaxWidth(LYRIC_WIDTH_FRACTION)
            .onGloballyPositioned { c ->
              lineBounds[index] = LineBounds(
                c.positionInParent().y,
                c.size.height.toFloat()
              )
            }
            .graphicsLayer(scaleX = scale, scaleY = scale),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          if (isHighLight) {
            val endTime = max(line.time, lyrics.getOrNull(index + 1)?.time ?: duration)

            LyricMultiLine(
              LocalTheme.current.textPrimary,
              LocalTheme.current.textSecondary,
              fontSize = fontSize,
              // 如果是逐行歌词并且允许更新进度则分开绘制，否则只绘制已唱
              if (allowProgressUpdates && line is PerWordLyricLine) {
                line.getProgress(
                  progress.coerceIn(line.time, endTime),
                  endTime
                )
              } else null,
              line,
            )
          } else {
            TextSecondary(
              line.content,
              fontSize = fontSize,
              maxLine = Int.MAX_VALUE,
              textAlign = TextAlign.Center
            )
          }

          if (!line.translation.isNullOrEmpty()) {
            TextSecondary(
              line.translation ?: "",
              fontSize = 15.sp,
              maxLine = Int.MAX_VALUE,
              textAlign = TextAlign.Center
            )
          }
        }
      }

      // 底部占位，保证高亮行有空间滚动到视口中间
      Spacer(Modifier.height(with(LocalDensity.current) { (viewportHeightPx / 2).toDp() }))
    }

    if (!allowProgressUpdates) {
      Box(
        modifier = Modifier
          .padding(horizontal = 8.dp)
          .fillMaxWidth()
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Image(
            modifier = Modifier
              .clickWithRipple {
                playbackVM.setProgress(lyrics[highlightIndex].time)
                allowProgressUpdates = true
              },
            painter = painterResource(R.drawable.ic_play_arrow_black_24dp),
            contentDescription = "LyricPlayFromLine",
            colorFilter = ColorFilter.tint(LocalTheme.current.textSecondary)
          )

          HorizontalDivider(
            modifier = Modifier
              .fillMaxWidth(),
            thickness = 1.dp,
            color = Color.Black
          )
        }

        var viewHeight by remember { mutableIntStateOf(0) }
        TextSecondary(
          lyrics[highlightIndex].formattedTime,
          modifier = Modifier
            .align(Alignment.CenterEnd)
            .onGloballyPositioned {
              viewHeight = it.size.height
            }
            .offset(y = (-viewHeight / 3).dp)
        )
      }
    }

  }

  // 滚动到视图中心
  LaunchedEffect(highlightIndex, viewportHeightPx, lineBounds.getOrNull(highlightIndex)) {
    if (lyrics.isEmpty() || viewportHeightPx == 0) return@LaunchedEffect
    val bound = lineBounds.getOrNull(highlightIndex) ?: return@LaunchedEffect
    val target = ((bound.top + bound.height / 2f) - viewportHeightPx / 2f).roundToInt()
    // 根据seekbar状态决定是否需要动画滚动
    if (!seekBarUiState.interacting) {
      scrollState.animateScrollTo(target, DEFAULT_ANIM_SPEC)
    } else {
      scrollState.scrollTo(target)
    }
  }

  // 进度更新
  LaunchedEffect(progress, lyrics) {
    if (!dragging && allowProgressUpdates) {
      val newIndex =
        lyrics.binarySearchBy(progress) { it.time }.let { if (it < 0) -(it + 1) - 1 else it }
      if (newIndex != highlightIndex) {
        highlightIndex = newIndex.coerceAtLeast(0)
      }
    }
  }

  // 停止滑动2s后才能更新进度
  LaunchedEffect(dragging) {
    if (dragging) {
      allowProgressUpdates = false
      return@LaunchedEffect
    }
    delay(2000)
    allowProgressUpdates = true
  }

  // 滑动结束200ms后滚动到最近行
  LaunchedEffect(dragging) {
    if (viewportHeightPx == 0 || dragging) {
      return@LaunchedEffect
    }

    delay(DEFAULT_ANIM_SPEC.durationMillis.toLong())
    val y = scrollState.value + viewportHeightPx / 2f
    var targetLine: Int = -1
    var targetBound: LineBounds? = null
    var minDistance = Float.POSITIVE_INFINITY
    for (i in lyrics.indices) {
      val bound = lineBounds.getOrNull(i) ?: return@LaunchedEffect
      if (y >= bound.top && y <= bound.bottom) {
        targetLine = i
        targetBound = bound
        break
      }

      val distance = if (y < bound.top) (bound.top - y) else (y - bound.bottom)
      if (distance < minDistance) {
        targetLine = i
        targetBound = bound
        minDistance = distance
      }
    }
    check(targetLine != -1)

    // 如果高亮行改变直接滑动，否则计算下滑动距离
    if (highlightIndex != targetLine) {
      highlightIndex = targetLine
    } else if (targetBound != null) {
      scrollState.animateScrollTo(
        ((targetBound.top + targetBound.height / 2f) - viewportHeightPx / 2f).roundToInt(),
        DEFAULT_ANIM_SPEC
      )
    }
  }
}

private data class LineBounds(val top: Float, val height: Float) {

  val bottom = top + height
  val mid = top + height / 2
}
