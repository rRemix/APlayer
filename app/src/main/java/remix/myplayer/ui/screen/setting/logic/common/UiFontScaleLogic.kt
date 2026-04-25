package remix.myplayer.ui.screen.setting.logic.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import remix.myplayer.ui.dialog.NormalDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.screen.setting.NormalPreference
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.LineSlider
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.ui.widget.common.defaultLineSliderProperties
import remix.myplayer.viewmodel.settingViewModel
import java.util.Locale
import kotlin.math.roundToInt

private val uiFontScaleSliderSteps =
  (
    (SettingPrefs.UI_FONT_SCALE_MAX - SettingPrefs.UI_FONT_SCALE_MIN) /
      SettingPrefs.UI_FONT_SCALE_STEP
    ).roundToInt() - 1

@Composable
fun UiFontScaleLogic() {
  val settingVM = settingViewModel
  val settingState = settingVM.settingsState.collectAsStateWithLifecycle()
  val scale = SettingPrefs.normalizeUiFontScale(settingState.value.common.uiFontScale)
  val dialogState = rememberDialogState(false)
  var selectedScale by remember { mutableFloatStateOf(scale) }

  NormalPreference(
    title = stringResource(R.string.ui_font_size),
    content = formatUiFontScale(scale)
  ) {
    selectedScale = scale
    dialogState.show()
  }

  NormalDialog(
    dialogState = dialogState,
    titleRes = R.string.ui_font_size,
    positiveRes = R.string.confirm,
    onPositive = {
      settingVM.setUiFontScale(selectedScale)
    },
    neutralRes = R.string.reset,
    onNeutral = {
      selectedScale = SettingPrefs.UI_FONT_SCALE_DEFAULT
    },
    negativeRes = R.string.cancel,
    custom = {
      UiFontScaleSelector(
        appliedScale = scale,
        scale = selectedScale,
        onValueChange = {
          selectedScale = SettingPrefs.normalizeUiFontScale(it)
        }
      )
    }
  )
}

@Composable
private fun UiFontScaleSelector(
  appliedScale: Float,
  scale: Float,
  onValueChange: (Float) -> Unit
) {
  val theme = LocalTheme.current

  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      TextPrimary(
        text = stringResource(R.string.ui_font_size_preview),
        fontSize = (16f * scale / appliedScale).sp,
        maxLine = Int.MAX_VALUE
      )
      TextSecondary(
        text = formatUiFontScale(scale),
        fontSize = 14.sp
      )
    }

    LineSlider(
      modifier = Modifier
        .fillMaxWidth()
        .height(32.dp),
      value = scale,
      onValueChange = onValueChange,
      valueRange = SettingPrefs.UI_FONT_SCALE_MIN..SettingPrefs.UI_FONT_SCALE_MAX,
      steps = uiFontScaleSliderSteps,
      properties = defaultLineSliderProperties.copy(
        trackProgressColor = theme.primary,
        thumbColor = theme.primary
      )
    )
  }
}

private fun formatUiFontScale(scale: Float): String {
  return String.format(
    Locale.getDefault(),
    "%.0f%%",
    SettingPrefs.normalizeUiFontScale(scale) * 100f
  )
}
