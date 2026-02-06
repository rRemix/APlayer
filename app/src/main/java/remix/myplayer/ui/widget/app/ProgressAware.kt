package remix.myplayer.ui.widget.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.service.playback.MusicStateSource

/**
 * 感知进度变化
 */

@Composable
fun ProgressAware(interval: Long = 0, content: @Composable (Long, Long) -> Unit) {
  val playbackState by MusicStateSource.playbackUiState.collectAsStateWithLifecycle()
  val progressState by MusicStateSource.progressState.collectAsStateWithLifecycle()

  val smoothPosition = rememberSmoothPosition(
    position = progressState.position,
    duration = progressState.duration,
    isPlaying = playbackState.isPlaying,
    speed = playbackState.speed,
    intervalMs = interval
  )

  if (playbackState.song.valid()) {
    val position = if (playbackState.isPlaying) smoothPosition else progressState.position
    content(position, progressState.duration)
  }
}
