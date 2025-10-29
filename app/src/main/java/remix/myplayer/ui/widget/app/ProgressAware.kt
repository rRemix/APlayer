package remix.myplayer.ui.widget.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import remix.myplayer.viewmodel.playbackViewModel

/**
 * 感知进度变化
 */

@Composable
fun ProgressAware(interval: Long = 100, content: @Composable (Long, Long) -> Unit) {
  val playbackVM = playbackViewModel
  val playbackState by playbackVM.playbackState.collectAsStateWithLifecycle()

  var progress by remember {
    mutableLongStateOf(0)
  }

  val duration = playbackState.song.duration

  if (playbackState.song.valid()) {
    content(progress, duration)
  }

  LaunchedEffect(playbackState.song, playbackState.playing) {
    do {
      delay(interval)
      withFrameMillis {
        progress = playbackVM.getProgress().toLong()
      }
    } while (isActive && playbackState.playing && playbackState.song.valid())
  }
}