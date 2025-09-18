package remix.myplayer.compose.ui.widget.app

import androidx.compose.foundation.layout.Box
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
import remix.myplayer.compose.viewmodel.musicViewModel

/**
 * 感知进度变化
 */

@Composable
fun ProgressAware(interval: Long = 100, content: @Composable (Long, Long) -> Unit) {
  val musicVM = musicViewModel
  val musicState by musicVM.musicState.collectAsStateWithLifecycle()

  var progress by remember {
    mutableLongStateOf(0)
  }

  val duration = musicState.song.duration

  content(progress, duration)

  LaunchedEffect(musicState.song, musicState.playing) {
    while (isActive && musicState.playing) {
      delay(interval)
      withFrameMillis {
        progress = musicVM.getProgress().toLong()
      }
    }
  }
}