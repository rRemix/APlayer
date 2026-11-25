package remix.myplayer.ui.screen.setting.logic.play

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.ui.screen.setting.SwitchPreference
import remix.myplayer.ui.widget.common.LineSlider
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.defaultLineSliderProperties
import remix.myplayer.viewmodel.settingViewModel
import kotlin.math.roundToInt

@Composable
fun SkipIntroOutroLogic() {
  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()
  val play = settingState.play

  SwitchPreference(
    title = stringResource(R.string.play_skip_intro_outro),
    content = stringResource(R.string.play_skip_intro_outro_tip),
    checked = play.skipIntroOutroEnabled
  ) {
    settingVM.setSkipIntroOutroEnabled(it)
  }

  if (play.skipIntroOutroEnabled) {
    SkipSecondsSlider(
      title = stringResource(R.string.play_skip_intro_seconds),
      seconds = play.skipIntroSeconds,
      onSecondsChanged = settingVM::setSkipIntroSeconds
    )

    SkipSecondsSlider(
      title = stringResource(R.string.play_skip_outro_seconds),
      seconds = play.skipOutroSeconds,
      onSecondsChanged = settingVM::setSkipOutroSeconds
    )
  }
}

@Composable
private fun SkipSecondsSlider(
  title: String,
  seconds: Int,
  onSecondsChanged: (Int) -> Unit
) {
  var sliderValue by remember(seconds) { mutableFloatStateOf(seconds.toFloat()) }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 10.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      TextPrimary(
        title,
        modifier = Modifier.weight(1f),
        fontSize = 16.sp
      )
      TextPrimary(
        stringResource(R.string.play_skip_seconds_value, sliderValue.roundToInt()),
        fontSize = 16.sp
      )
    }

    LineSlider(
      modifier = Modifier
        .fillMaxWidth()
        .height(32.dp),
      value = sliderValue,
      onValueChange = { sliderValue = it },
      onValueChangeFinished = { onSecondsChanged(sliderValue.roundToInt()) },
      valueRange = SettingPrefs.SKIP_INTRO_OUTRO_MIN_SECONDS.toFloat()..
        SettingPrefs.SKIP_INTRO_OUTRO_MAX_SECONDS.toFloat(),
      steps = SettingPrefs.SKIP_INTRO_OUTRO_MAX_SECONDS -
        SettingPrefs.SKIP_INTRO_OUTRO_MIN_SECONDS - 1,
      properties = defaultLineSliderProperties.copy()
    )
  }
}
