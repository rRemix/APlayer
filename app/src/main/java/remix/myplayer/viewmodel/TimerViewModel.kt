package remix.myplayer.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.helper.SleepTimer.Companion.getMillisUntilFinish
import remix.myplayer.helper.SleepTimer.Companion.isTicking
import remix.myplayer.helper.SleepTimer.Companion.toggleTimer
import remix.myplayer.ui.dialog.DialogState
import remix.myplayer.ui.nav.MessageNotifier
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
  private val savedStateHandle: SavedStateHandle,
  @param:ApplicationContext private val context: Context,
  val settingPrefs: SettingPrefs,
) : ViewModel() {

  private val _dialogState = DialogState(false)
  val dialogState = _dialogState

  fun showTimerDialog() {
    // 如果有默认设置并且没有开始计时，直接开始计时
    // 如果有默认设置但已经开始计时，显示该dialog
    if (_timerState.value.timerStartAuto && _timerState.value.timerDefaultDuration > 0) {
      if (!isTicking()) {
        updateState(durationInSec = _timerState.value.timerDefaultDuration)
        toggle()
        return
      }
    }

    if (isTicking()) {
      periodUpdateProgress()
    } else {
      cancelUpdateProgress()
      updateState(durationInSec = 0, progress = 0)
    }

    _dialogState.show()
  }

  private var progressJob: Job? = null
  fun periodUpdateProgress() {
    assert(progressJob?.isActive != true)
    progressJob = viewModelScope.launch {
      while (isActive) {
        val remainSecond = getMillisUntilFinish().toInt() / 1000
        updateState(durationInSec = remainSecond, progress = remainSecond)
        delay(1000)
      }
    }
  }

  fun cancelUpdateProgress() {
    progressJob?.cancel()
    progressJob = null
  }

  fun toggle() {
    if (_timerState.value.durationInSec <= 0 && !isTicking()) {
      MessageNotifier.show(R.string.plz_set_correct_time)
      return
    }
    toggleTimer((_timerState.value.durationInSec * 1000).toLong())
    updateState(positiveButtonText = if (isTicking()) R.string.cancel_timer else R.string.start_timer)
  }

  private val _timerState = MutableStateFlow(
    TimerState(
      timerStartAuto = settingPrefs.timerStartAuto,
      exitAfterTimerFinish = settingPrefs.exitAfterTimerFinish,
      timerDefaultDuration = settingPrefs.timerDefaultDuration
    )
  )
  val timerState = _timerState.asStateFlow()

  fun updateState(
    durationInSec: Int = _timerState.value.durationInSec,
    progress: Int = _timerState.value.progress,
    timerStartAuto: Boolean = _timerState.value.timerStartAuto,
    exitAfterTimerFinish: Boolean = _timerState.value.exitAfterTimerFinish,
    positiveButtonText: Int = _timerState.value.positiveButtonText,
    timerDefaultDuration: Int = _timerState.value.timerDefaultDuration
  ) {
    settingPrefs.timerStartAuto = timerStartAuto && timerDefaultDuration > 0
    settingPrefs.timerDefaultDuration = timerDefaultDuration
    _timerState.value = _timerState.value.copy(
      durationInSec = durationInSec,
      progress = progress,
      timerStartAuto = timerStartAuto,
      exitAfterTimerFinish = exitAfterTimerFinish,
      positiveButtonText = positiveButtonText,
      timerDefaultDuration = timerDefaultDuration
    )
  }
}

data class TimerState(
  val durationInSec: Int = 0,
  val progress: Int = 0,
  val timerStartAuto: Boolean,
  val exitAfterTimerFinish: Boolean,
  val positiveButtonText: Int = R.string.start_timer,
  val timerDefaultDuration: Int
)
