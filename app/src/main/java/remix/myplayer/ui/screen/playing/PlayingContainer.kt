package remix.myplayer.ui.screen.playing

import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.util.ThemeUtil
import remix.myplayer.viewmodel.playbackViewModel
import remix.myplayer.viewmodel.settingViewModel

@Composable
fun PlayingContainer(content: @Composable () -> Unit) {
  val settingState by settingViewModel.settingsState.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val theme = LocalTheme.current
  val initialColor = Color(
    ThemeUtil.resolveColor(
      context,
      R.attr.colorSurface,
      if (theme.isLight) Color.White.value.toInt() else Color.Black.value.toInt()
    )
  )

  if (!theme.isLight) {
    val background = theme.mainBackground
    val brush = Brush.verticalGradient(colors = listOf(background, background))
    Container(brush = brush, content = content)
    return
  }

  when (settingState.playingScreen.background) {
    SettingPrefs.BACKGROUND_ADAPTIVE_COLOR -> {
      val swatch by playbackViewModel.swatch.collectAsStateWithLifecycle()
      val color = remember(initialColor) { Animatable(initialValue = initialColor) }
      val brush = Brush.verticalGradient(colors = listOf(color.value, initialColor))
      Container(brush = brush, content = content)

      LaunchedEffect(swatch, initialColor) {
        color.animateTo(Color(swatch.rgb), animationSpec = tween(600))
      }
    }

    SettingPrefs.BACKGROUND_THEME -> {
      val brush = Brush.verticalGradient(colors = listOf(theme.primary, initialColor))
      Container(brush = brush, content = content)
    }

    else -> {
      Container(brush = null, content = content)
    }
  }
}

/**
 * 系统自由小窗 / 悬浮小窗底部的安全间距。
 *
 * 小窗模式下 WindowInsets.navigationBars 不报告 inset（小窗内无系统导航栏），
 * 但国产 ROM（MIUI/EMUI/ColorOS 等）会在小窗底部渲染一条手势 / 拖拽条，
 * 直接盖住播放界面底部的 PlayingUtilityBar。多窗口模式下补一个固定底部间距来避让。
 */
private val SmallWindowBottomSafePadding = 24.dp

@Composable
private fun Container(
  brush: Brush?,
  content: @Composable () -> Unit
) {
  // 多窗口 / 系统小窗下 navigationBars 不报告 inset，但国产 ROM 会在小窗底部画手势 / 拖拽条盖住内容；
  // 保证底部总留白 ≥ SmallWindowBottomSafePadding（safeDrawing 已给的 + 额外补的，不重复叠加）。
  val inMultiWindow = rememberInMultiWindowMode()
  val safeBottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
  val extraBottom = if (inMultiWindow) {
    (SmallWindowBottomSafePadding - safeBottom).coerceAtLeast(0.dp)
  } else {
    0.dp
  }
  // 背景在 safeDrawingPadding 之前，铺满全屏（沉浸式）；内容由 safeDrawingPadding 内缩避开 insets。
  val modifier = Modifier
    .fillMaxSize()
    .then(
      if (brush != null) Modifier.background(
        brush = brush,
        shape = RectangleShape
      ) else Modifier
    )
    .safeDrawingPadding()
    .padding(bottom = extraBottom)

  Column(modifier = modifier) {
    content()
  }
}

/**
 * 当前是否处于系统多窗口 / 自由小窗模式。
 *
 * isInMultiWindowMode 本身不是 Compose State，不会主动触发重组；这里以 LocalConfiguration 为 key
 * remember，进入 / 退出小窗时窗口尺寸变化触发 configuration change → 重组重读最新值。
 * API 24 以下不存在多窗口，固定返回 false。
 */
@Composable
private fun rememberInMultiWindowMode(): Boolean {
  val activity = LocalActivity.current ?: return false
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
  val configuration = LocalConfiguration.current
  return remember(activity, configuration) { activity.isInMultiWindowMode }
}
