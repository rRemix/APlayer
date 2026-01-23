package remix.myplayer.ui.screen

import android.media.audiofx.AudioEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import remix.myplayer.R
import remix.myplayer.helper.EQHelper
import remix.myplayer.service.MusicServiceRemote
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.CommonAppBar
import remix.myplayer.ui.widget.common.LineSlider
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.common.TextSecondary
import remix.myplayer.ui.widget.common.defaultLineSliderProperties
import remix.myplayer.util.ext.CenterInBox
import remix.myplayer.util.ext.clickWithRipple
import java.text.DecimalFormat
import kotlin.math.roundToInt

@Composable
fun EQScreen() {
  val context = LocalContext.current
  val nav = LocalNavController.current

  val sessionId = MusicServiceRemote.getAudioSessionId()
  if (sessionId == AudioEffect.ERROR_BAD_VALUE || sessionId == null) {
    MessageNotifier.show(R.string.no_audio_ID)
    nav.popBackStack()
    return
  }

  // 初始化两次
  if (!EQHelper.init(context, sessionId, true)) {
    MessageNotifier.show(R.string.eq_initial_failed)
    nav.popBackStack()
    return
  }

  // UI状态
  var enableEq by remember { mutableStateOf(EQHelper.enable) }
  val bandNumber = EQHelper.bandNumber.toInt()
  val minLevel = EQHelper.minLevel
  val maxLevel = EQHelper.maxLevel
  val levelRange = (maxLevel - minLevel).toFloat()

  // 频段显示的最小/最大文本
  val decimalFormat = remember { DecimalFormat("+#;-#") }
  val minLevelText = decimalFormat.format(minLevel / 100)
  val maxLevelText = decimalFormat.format(maxLevel / 100)

  // 频段滑条的当前值
  val bandProgress = remember(bandNumber, minLevel) {
    mutableStateListOf<Float>().apply {
      repeat(bandNumber) { i ->
        add((EQHelper.getBandLevel(i) - minLevel).toFloat())
      }
    }
  }

  var bassStrength by remember { mutableFloatStateOf(EQHelper.bassBoostStrength.toFloat()) }

  Scaffold(
    topBar = { CommonAppBar(title = stringResource(R.string.eq), actions = emptyList()) },
    containerColor = LocalTheme.current.mainBackground,
  ) { contentPadding ->
    Column(
      modifier = Modifier
        .padding(contentPadding)
        .fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        // 开关
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          TextSecondary(stringResource(R.string.use_eq), fontSize = 14.sp)
          Switch(
            modifier = Modifier.scale(0.8f),
            checked = enableEq,
            colors = SwitchDefaults.colors().copy(
              checkedTrackColor = LocalTheme.current.secondary,
              uncheckedTrackColor = Color.Transparent
            ),
            onCheckedChange = { checked ->
              enableEq = checked
              EQHelper.updateEnable(checked)
              // 低音增强的可用性受 enable 和硬件支持影响
              if (!EQHelper.isBassBoostEnabled) {
                bassStrength = EQHelper.bassBoostStrength.toFloat()
              }
            }
          )
        }

        // 低音增强
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
        ) {
          TextSecondary(stringResource(R.string.bass_boost), fontSize = 14.sp)
          // 低音增强滑条
          LineSlider(
            modifier = Modifier
              .fillMaxWidth()
              .height(24.dp),
            value = bassStrength,
            onValueChange = { v ->
              bassStrength = v
              if (EQHelper.isBassBoostEnabled) {
                EQHelper.bassBoostStrength = v.roundToInt()
              }
            },
            valueRange = 0f..1000f,
            properties = defaultLineSliderProperties.copy(),
            enabled = EQHelper.isBassBoostEnabled
          )
        }

        // 频段列表
        repeat(bandNumber) { index ->
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            TextPrimary("${EQHelper.getCenterFreq(index)} mHz")
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              TextPrimary(minLevelText)
              // 频段滑条
              LineSlider(
                modifier = Modifier
                  .weight(1f)
                  .height(24.dp),
                value = bandProgress[index],
                onValueChange = { v ->
                  bandProgress[index] = v
                  if (enableEq) {
                    EQHelper.setBandLevel(index, (v + minLevel).roundToInt())
                  }
                },
                valueRange = 0f..levelRange,
                properties = defaultLineSliderProperties.copy(),
                enabled = enableEq
              )
              TextPrimary(maxLevelText)
            }
          }
        }
      }

      // 重置按钮
      Spacer(modifier = Modifier.height(16.dp))

      CenterInBox(
        modifier = Modifier
          .padding(bottom = 8.dp)
          .width(200.dp)
          .background(LocalTheme.current.primary, RoundedCornerShape(6.dp))
          .padding(vertical = 4.dp)
          .clickWithRipple(false, enabled = enableEq) {
            EQHelper.reset()
            repeat(bandNumber) { i ->
              bandProgress[i] = (EQHelper.getBandLevel(i) - minLevel).toFloat()
            }
            bassStrength = EQHelper.bassBoostStrength.toFloat()
          }
      ) {
        Text(
          stringResource(R.string.reset),
          textAlign = TextAlign.Center,
          color = Color.White
        )
      }
    }
  }
}