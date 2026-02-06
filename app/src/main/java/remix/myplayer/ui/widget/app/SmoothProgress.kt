package remix.myplayer.ui.widget.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.isActive

@Composable
fun rememberSmoothPosition(
  position: Long,
  duration: Long,
  isPlaying: Boolean,
  speed: Float,
  intervalMs: Long = 0
): Long {
  // 基准值
  var basePosition by remember { mutableLongStateOf(position) }
  // 基准时间
  var baseTimeNs by remember { mutableLongStateOf(0L) }
  // 平滑后的进度
  var smoothPosition by remember { mutableLongStateOf(position) }

  // 收到真实进度时重置基准
  LaunchedEffect(position, isPlaying, speed) {
    val now = withFrameNanos { it }
    basePosition = position
    baseTimeNs = now
    smoothPosition = position
  }

  // 逐帧插值 按时间外推平滑进度
  LaunchedEffect(isPlaying, speed, duration, intervalMs) {
    if (!isPlaying) {
      smoothPosition = basePosition
      return@LaunchedEffect
    }

    var lastFrameNs = 0L
    while (isActive) {
      val frameTime = withFrameNanos { it }
      if (intervalMs > 0) {
        if (lastFrameNs != 0L && (frameTime - lastFrameNs) < intervalMs * 1_000_000L) {
          continue
        }
        lastFrameNs = frameTime
      }

      val elapsedMs = (frameTime - baseTimeNs) / 1_000_000L
      val estimated = basePosition + (elapsedMs * speed).toLong()
      smoothPosition = if (duration > 0L) {
        estimated.coerceIn(0L, duration)
      } else {
        maxOf(0L, estimated)
      }
    }
  }

  return if (isPlaying) smoothPosition else position
}
