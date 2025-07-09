package remix.myplayer.compose.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.clickableWithoutRipple
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.app.CircleSeekBar
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.compose.ui.widget.common.TextSecondary
import remix.myplayer.compose.viewmodel.TimerViewModel
import remix.myplayer.util.ToastUtil

@Composable
fun TimerDialog() {
  val context = LocalContext.current
  val timerVM = activityViewModel<TimerViewModel>()
  val timerState by timerVM.timerState.collectAsStateWithLifecycle()

  val tipDialogState = rememberDialogState(false)
  NormalDialog(
    dialogState = tipDialogState,
    titleRes = R.string.timer_default_info_title,
    contentRes = R.string.timer_default_info_content,
    positiveRes = R.string.close,
    onPositive = {
    }
  )

  val minuteText by remember {
    derivedStateOf {
      (timerState.progress / 60).toString().padStart(2, '0')
    }
  }
  val secondText by remember {
    derivedStateOf {
      (timerState.progress % 60).toString().padStart(2, '0')
    }
  }

  NormalDialog(
    dialogState = timerVM.dialogState,
    titleRes = R.string.timer,
    titleAlignment = Alignment.CenterHorizontally,
    onPositive = {
      timerVM.toggle()
    },
    positiveRes = timerState.positiveButtonText,
    custom = {
      LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .weight(1f, false)
      ) {
        item {
          Box(
            modifier = Modifier.padding(top = 24.dp, bottom = 36.dp),
            contentAlignment = Alignment.Center
          ) {
            CircleSeekBar(Modifier.size(180.dp), progress = timerState.progress) { current, max ->
              val durationInSec = current / 60 * 60
              timerVM.updateState(durationInSec = durationInSec, progress = durationInSec)
            }

            @Composable
            fun TextWithLine(text: String) {
              Box(
                modifier = Modifier
                  .background(Color.Transparent, RoundedCornerShape(1.dp))
                  .border(1.dp, LocalTheme.current.textSecondary)
                  .padding(1.dp),
                contentAlignment = Alignment.Center
              ) {
                TextPrimary(text, modifier = Modifier, fontSize = 24.sp)
                Spacer(
                  modifier = Modifier
                    .matchParentSize()
                    .requiredHeight(1.dp)
                    .background(LocalTheme.current.dialogBackground)
                    .align(Alignment.Center)
                )
              }
            }

            Row(
              modifier = Modifier
                .wrapContentSize(), verticalAlignment = Alignment.CenterVertically
            ) {
              TextWithLine(minuteText)
              TextSecondary(":", modifier = Modifier.padding(horizontal = 4.dp), fontSize = 20.sp)
              TextWithLine(secondText)
            }
          }
        }

        item {
          Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .padding(end = 20.dp)
          ) {
            Icon(
              modifier = Modifier
                .padding(end = 4.dp)
                .clickableWithoutRipple {
                  tipDialogState.show()
                },
              painter = painterResource(R.drawable.ic_info_24dp),
              contentDescription = "TimerTip"
            )

            TextPrimary(
              stringResource(R.string.as_default),
              modifier = Modifier.padding(end = 8.dp)
            )

            Switch(
              checked = timerState.timerStartAuto,
              colors = SwitchDefaults.colors().copy(
                checkedTrackColor = LocalTheme.current.secondary,
                uncheckedTrackColor = Color.Transparent
              ),
              onCheckedChange = { isChecked ->
                if (isChecked) {
                  if (timerState.durationInSec > 0) {
                    ToastUtil.show(context, R.string.set_success)
                    timerVM.updateState(
                      timerStartAuto = true,
                      timerDefaultDuration = timerState.durationInSec
                    )
                  } else {
                    ToastUtil.show(context, R.string.plz_set_correct_time)
                  }
                } else {
                  ToastUtil.show(context, R.string.cancel_success)
                  timerVM.updateState(timerStartAuto = false, timerDefaultDuration = -1)
                }

              },
              modifier = Modifier.scale(0.9f)
            )
          }
        }

        item {
          Row(
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 16.dp, end = 20.dp)
          ) {
            TextPrimary(
              stringResource(R.string.timer_pending_close),
              modifier = Modifier.padding(end = 8.dp)
            )
            Switch(
              checked = timerState.exitAfterTimerFinish,
              colors = SwitchDefaults.colors().copy(
                checkedTrackColor = LocalTheme.current.secondary,
                uncheckedTrackColor = Color.Transparent
              ),
              onCheckedChange = {
                timerVM.updateState(exitAfterTimerFinish = it)
              },
              modifier = Modifier.scale(0.8f)
            )
          }
        }
      }
    },
    onDismissRequest = {
      timerVM.cancelUpdateProgress()
    }
  )
}