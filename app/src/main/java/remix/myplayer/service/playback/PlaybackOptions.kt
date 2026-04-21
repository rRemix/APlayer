package remix.myplayer.service.playback

import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.data.prefs.SettingPrefs.Companion.REPEAT_MODE_ALL
import remix.myplayer.data.prefs.SettingPrefs.Companion.REPEAT_MODE_OFF
import remix.myplayer.data.prefs.SettingPrefs.Companion.REPEAT_MODE_ONE
import javax.inject.Inject
import javax.inject.Singleton

data class PlaybackOptionsState(
  val repeatMode: Int,
  val shuffleEnabled: Boolean,
)

@Singleton
class PlaybackOptions @Inject constructor(
  private val settingPrefs: SettingPrefs
) {

  val repeatMode: Int
    get() = settingPrefs.repeatMode

  val shuffleEnabled: Boolean
    get() = settingPrefs.shuffleEnabled

  fun set(repeatMode: Int, shuffleEnabled: Boolean): PlaybackOptionsState {
    val state = PlaybackOptionsState(repeatMode, shuffleEnabled)
    settingPrefs.repeatMode = state.repeatMode
    settingPrefs.shuffleEnabled = state.shuffleEnabled
    return state
  }

  fun toggleRepeat(): PlaybackOptionsState {
    return set(nextRepeatMode(repeatMode), shuffleEnabled)
  }

  fun toggleShuffle(): PlaybackOptionsState {
    return set(repeatMode, !shuffleEnabled)
  }

  fun setShuffleAll(): PlaybackOptionsState {
    return set(REPEAT_MODE_ALL, true)
  }

  companion object {

    fun nextRepeatMode(repeatMode: Int): Int {
      return when (repeatMode) {
        REPEAT_MODE_ALL -> REPEAT_MODE_ONE
        REPEAT_MODE_ONE -> REPEAT_MODE_OFF
        else -> REPEAT_MODE_ALL
      }
    }
  }
}
