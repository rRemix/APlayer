package remix.myplayer.service.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import remix.myplayer.data.prefs.SettingPrefs
import javax.inject.Inject

class PlaybackProgressSaver @Inject constructor(
  private val settingPrefs: SettingPrefs
) {

  private var progressJob: Job? = null

  fun start(scope: CoroutineScope, positionProvider: () -> Long) {
    if (progressJob?.isActive == true) {
      return
    }
    progressJob = scope.launch {
      while (isActive) {
        val progress = positionProvider()
        if (progress > 0) {
          settingPrefs.lastProgress = progress.toInt()
        }

        delay(INTERVAL_SAVE_PROGRESS)
      }
    }
  }

  fun stop() {
    progressJob?.cancel()
    progressJob = null
  }

  private companion object {
    const val INTERVAL_SAVE_PROGRESS = 1000L
  }
}
