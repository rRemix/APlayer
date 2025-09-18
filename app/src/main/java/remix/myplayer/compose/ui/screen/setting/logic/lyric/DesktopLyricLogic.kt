package remix.myplayer.compose.ui.screen.setting.logic.lyric

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import dagger.hilt.android.EntryPointAccessors
import remix.myplayer.R
import remix.myplayer.compose.lyric.LyricsManagerEntryPoint
import remix.myplayer.compose.ui.screen.setting.SwitchPreference
import remix.myplayer.misc.floatpermission.FloatWindowManager
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util

@Composable
fun DesktopLyricLogic() {
  val context = LocalContext.current

  val lyricsManager = remember {
    EntryPointAccessors.fromApplication(
      context.applicationContext,
      LyricsManagerEntryPoint::class.java
    ).lyricsManager()
  }

  // TODO improve?
  val hasPermission = FloatWindowManager.getInstance().checkPermission(context)
  // 用户的选择
  var userSelect by remember { mutableStateOf(lyricsManager.isDesktopLyricEnabled) }
  // 当用户选择改变时执行检查逻辑
  var check by remember { mutableIntStateOf(0) }
  // 是否真正开启桌面歌词
  var desktopLyricOn by remember { mutableStateOf(userSelect && hasPermission) }

  val content by remember {
    derivedStateOf {
      context.getString(
        if (desktopLyricOn) R.string.opened_desktop_lrc else R.string.closed_desktop_lrc
      )
    }
  }

  SwitchPreference(
    stringResource(R.string.float_lrc),
    content,
    desktopLyricOn
  ) {
    userSelect = it
    check++
  }

  LaunchedEffect(check) {
    if (check == 0) {
      return@LaunchedEffect
    }
    if (userSelect && !hasPermission) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = "package:${context.packageName}".toUri()
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        Util.startActivitySafely(context, intent)
      }
      ToastUtil.show(context, R.string.plz_give_float_permission)
      userSelect = false
      return@LaunchedEffect
    }

    desktopLyricOn = userSelect
    if (desktopLyricOn == lyricsManager.isDesktopLyricEnabled) {
      return@LaunchedEffect
    }

    lyricsManager.setDesktopLyricEnabled(desktopLyricOn, context as? Activity)
  }
}