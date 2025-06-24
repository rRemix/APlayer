package remix.myplayer.compose.ui.widget.app

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.bean.misc.Library
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.clickableWithoutRipple
import remix.myplayer.compose.nav.LocalNavController
import remix.myplayer.compose.ui.dialog.NormalDialog
import remix.myplayer.compose.ui.dialog.rememberDialogState
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.compose.ui.widget.common.TextSecondary
import remix.myplayer.compose.ui.widget.popup.ScreenPopupButton
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.compose.viewmodel.SettingViewModel
import remix.myplayer.helper.SleepTimer.Companion.getMillisUntilFinish
import remix.myplayer.helper.SleepTimer.Companion.isTicking
import remix.myplayer.helper.SleepTimer.Companion.toggleTimer
import remix.myplayer.ui.activity.SearchActivity
import remix.myplayer.util.ToastUtil
import java.util.Timer
import kotlin.concurrent.fixedRateTimer

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppBar(
  scrollBehavior: TopAppBarScrollBehavior,
  drawerState: DrawerState
) {
  val scope = rememberCoroutineScope()

  TopAppBar(
    scrollBehavior = scrollBehavior,
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = LocalTheme.current.primary,
      scrolledContainerColor = LocalTheme.current.primary,
      navigationIconContentColor = Color.White,
      actionIconContentColor = Color.White,
    ),
    title = {},
    navigationIcon = {
      IconButton(onClick = { scope.launch { drawerState.open() } }) {
        Icon(Icons.Filled.Menu, contentDescription = "Menu")
      }
    },
    actions = { AppBarActions() })
}

@Composable
private fun AppBarActions(vm: SettingViewModel = activityViewModel()) {
  val library by vm.currentLibrary.collectAsStateWithLifecycle()

  if (library.tag != Library.TAG_FOLDER && library.tag != Library.TAG_REMOTE) {
    ScreenPopupButton(library)
  }

  defaultActions.map { it ->
    IconButton(onClick = {
      it.action()
    }) {
      Icon(
        painter = painterResource(it.icon),
        contentDescription = it.contentDescription
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonAppBar(
  title: String,
  showBack: Boolean = true,
  actions: List<AppBarAction> = defaultActions
) {
  val navController = LocalNavController.current

  TopAppBar(
    title = { Text(title, color = Color.White, modifier = Modifier.padding(start = 16.dp)) },
    modifier = Modifier,
    navigationIcon = {
      if (showBack) {
        IconButton(onClick = {
          navController.popBackStack()
        }) {
          Icon(
            painter = painterResource(R.drawable.ic_arrow_back_white_24dp),
            contentDescription = "Back"
          )
        }
      }
    },
    actions = {
      actions.map {
        IconButton(onClick = it.action) {
          Icon(
            painter = painterResource(it.icon),
            contentDescription = it.contentDescription
          )
        }
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = LocalTheme.current.primary,
      scrolledContainerColor = LocalTheme.current.primary,
      navigationIconContentColor = Color.White,
      actionIconContentColor = Color.White,
    ),
  )
}

private var updateTimer: Timer? = null
private val defaultActions: List<AppBarAction>
  @Composable
  get() {
    val context = LocalContext.current
    val setting = activityViewModel<LibraryViewModel>().settingPrefs

    val tipDialogState = rememberDialogState(false)
    NormalDialog(
      dialogState = tipDialogState,
      titleRes = R.string.timer_default_info_title,
      contentRes = R.string.timer_default_info_content,
      positiveRes = R.string.close,
      onPositive = {

      }
    )

    var durationInSec by rememberSaveable {
      mutableIntStateOf(0)
    }
    var progress by rememberSaveable {
      mutableIntStateOf(0)
    }
    var timerStartAuto by rememberSaveable {
      mutableStateOf(setting.timerStartAuto)
    }
    var exitAfterTimerFinish by rememberSaveable {
      mutableStateOf(setting.exitAfterTimerFinish)
    }
    var positiveText by rememberSaveable {
      mutableIntStateOf(R.string.start_timer)
    }
    var minuteText by rememberSaveable {
      mutableStateOf("00")
    }
    var secondText by rememberSaveable {
      mutableStateOf("00")
    }

    fun updateTextAndProgress(second: Int) {
      val min = second / 60
      val sec = second % 60

      minuteText = min.toString().padStart(2, '0')
      secondText = sec.toString().padStart(2, '0')
      progress = second
    }

    fun toggle() {
      if (durationInSec <= 0 && !isTicking()) {
        ToastUtil.show(context, R.string.plz_set_correct_time)
        return
      }
      toggleTimer((durationInSec * 1000).toLong())
      positiveText = if (isTicking()) R.string.cancel_timer else R.string.start_timer
    }

    val dialogState = rememberDialogState(false)
    NormalDialog(
      dialogState = dialogState,
      titleRes = R.string.timer,
      titleAlignment = Alignment.CenterHorizontally,
      onPositive = {
        toggle()
      },
      positiveRes = positiveText,
      custom = {
        LazyColumn(horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .weight(1f, false)) {
          item {
            Box(
              modifier = Modifier.padding(top = 24.dp, bottom = 36.dp),
              contentAlignment = Alignment.Center
            ) {
              CircleSeekBar(Modifier.size(180.dp), progress = progress) { current, max ->
                durationInSec = current / 60 * 60
                updateTextAndProgress(durationInSec)
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
                checked = timerStartAuto,
                colors = SwitchDefaults.colors().copy(
                  checkedTrackColor = LocalTheme.current.secondary,
                  uncheckedTrackColor = Color.Transparent
                ),
                onCheckedChange = { isChecked ->
                  if (isChecked) {
                    if (durationInSec > 0) {
                      ToastUtil.show(context, R.string.set_success)
                      setting.timerStartAuto = true
                      setting.timerDefaultDuration = durationInSec
                      timerStartAuto = true
                    } else {
                      ToastUtil.show(context, R.string.plz_set_correct_time)
                    }
                  } else {
                    ToastUtil.show(context, R.string.cancel_success)
                    setting.timerStartAuto = false
                    setting.timerDefaultDuration = -1
                    timerStartAuto = false
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
                checked = exitAfterTimerFinish,
                colors = SwitchDefaults.colors().copy(
                  checkedTrackColor = LocalTheme.current.secondary,
                  uncheckedTrackColor = Color.Transparent
                ),
                onCheckedChange = {
                  setting.exitAfterTimerFinish = it
                  exitAfterTimerFinish = it
                },
                modifier = Modifier.scale(0.8f)
              )
            }
          }
        }
      },
      onDismissRequest = {
        updateTimer?.cancel()
        updateTimer = null
      }
    )

    return listOf(
      AppBarAction(R.drawable.ic_timer_white_24dp, "Timer") {
        // 如果有默认设置并且没有开始计时，直接开始计时
        // 如果有默认设置但已经开始计时，显示该dialog
        if (timerStartAuto && setting.timerDefaultDuration > 0) {
          if (!isTicking()) {
            durationInSec = setting.timerDefaultDuration
            toggle()
            return@AppBarAction
          }
        }

        if (isTicking()) {
          updateTimer?.cancel()
          updateTimer = fixedRateTimer(period = 1000L) {
            val remainSecond = getMillisUntilFinish().toInt() / 1000
            updateTextAndProgress(remainSecond)
          }
        } else {
          updateTimer?.cancel()
          updateTimer = null
          updateTextAndProgress(0)
        }

        dialogState.show()
      },
      AppBarAction(R.drawable.ic_search_white_24dp, "Search") {
        context.startActivity(Intent(context, SearchActivity::class.java))
      })
  }


class AppBarAction(
  val icon: Int,
  val contentDescription: String? = null,
  val action: () -> Unit
)