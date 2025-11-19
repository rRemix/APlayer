package remix.myplayer.ui.widget.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.service.playback.MusicStateSource

/**
 * 感知进度变化
 */

@Composable
fun ProgressAware(interval: Long = 50, content: @Composable (Long, Long) -> Unit) {
//  val playbackVM = playbackViewModel
  val playbackState by MusicStateSource.playbackUiState.collectAsStateWithLifecycle()
  val progressState by MusicStateSource.progressState.collectAsStateWithLifecycle()

  if (playbackState.song.valid()) {
    content(progressState.position, progressState.duration)
  }

}