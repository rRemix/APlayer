package remix.myplayer.ui.screen.setting.logic.lyric

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.misc.floatpermission.FloatWindowManager
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.screen.setting.SwitchPreference
import remix.myplayer.util.Util
import remix.myplayer.viewmodel.settingViewModel

@Composable
fun DesktopLyricLogic() {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val settingVM = settingViewModel
  val settingState by settingVM.settingsState.collectAsStateWithLifecycle()

  var uiState by remember {
    mutableStateOf(
      DesktopLyricUiState(
        hasPermission = FloatWindowManager.getInstance().checkPermission(context),
        userWantsEnabled = settingState.lyric.desktopLyricEnabled,
        isWaitingForPermission = false
      )
    )
  }

  // 是否真正开启桌面歌词
  val isActuallyEnabled = uiState.userWantsEnabled && uiState.hasPermission

  SwitchPreference(
    title = stringResource(R.string.float_lrc),
    content = stringResource(
      if (isActuallyEnabled) R.string.opened_desktop_lrc
      else R.string.closed_desktop_lrc
    ),
    checked = isActuallyEnabled
  ) { wantsEnabled ->
    uiState = uiState.copy(userWantsEnabled = wantsEnabled)
  }

  LaunchedEffect(uiState.userWantsEnabled, uiState.hasPermission) {
    when {
      // 尝试开启但没有权限
      uiState.userWantsEnabled && !uiState.hasPermission -> {
        requestOverlayPermission(context)
        uiState = uiState.copy(
          userWantsEnabled = false, // 重置用户选择
          isWaitingForPermission = true
        )
      }
      // 保存
      isActuallyEnabled != settingState.lyric.desktopLyricEnabled -> {
        settingVM.setDesktopLyricEnabled(isActuallyEnabled, context as? Activity)
      }
    }
  }

  // 处理悬浮窗权限授予后的逻辑
  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) {
        val currentPermission = FloatWindowManager.getInstance().checkPermission(context)

        uiState = uiState.copy(hasPermission = currentPermission)

        // 如果之前在等待权限且现在获得了权限，自动开启
        if (uiState.isWaitingForPermission && currentPermission) {
          uiState = uiState.copy(
            userWantsEnabled = true,
            isWaitingForPermission = false
          )
        }
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }
}

/**
 * 桌面歌词UI状态
 */
private data class DesktopLyricUiState(
  val hasPermission: Boolean,
  val userWantsEnabled: Boolean,
  val isWaitingForPermission: Boolean
)

/**
 * 请求悬浮窗权限
 */
private fun requestOverlayPermission(context: Context) {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
      data = "package:${context.packageName}".toUri()
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    Util.startActivitySafely(context, intent)
  }
  MessageNotifier.show(R.string.plz_give_float_permission)
}