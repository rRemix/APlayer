package remix.myplayer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import remix.myplayer.ui.appwidgets.BaseAppwidget
import remix.myplayer.ui.appwidgets.big.AppWidgetBig
import remix.myplayer.ui.appwidgets.medium.AppWidgetMedium
import remix.myplayer.ui.appwidgets.medium.AppWidgetMediumTransparent
import remix.myplayer.ui.appwidgets.small.AppWidgetSmall
import remix.myplayer.ui.appwidgets.small.AppWidgetSmallTransparent
import remix.myplayer.ui.theme.ThemeController
import remix.myplayer.util.Util.isAppOnForeground
import timber.log.Timber
import javax.inject.Inject

class AppWidgetUpdater @Inject constructor(
  private val themeController: ThemeController
) {

  val receiver: BroadcastReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      val name = intent.getStringExtra(BaseAppwidget.EXTRA_WIDGET_NAME)
      val appIds = intent.getIntArrayExtra(BaseAppwidget.EXTRA_WIDGET_IDS)
      Timber.v("name: $name appIds: $appIds")
      widgets[name]?.updateWidget(context, appIds, true, primaryColor())
    }
  }

  private val widgets = hashMapOf(
    APPWIDGET_BIG to AppWidgetBig.getInstance(),
    APPWIDGET_MEDIUM to AppWidgetMedium.getInstance(),
    APPWIDGET_MEDIUM_TRANSPARENT to AppWidgetMediumTransparent.getInstance(),
    APPWIDGET_SMALL to AppWidgetSmall.getInstance(),
    APPWIDGET_SMALL_TRANSPARENT to AppWidgetSmallTransparent.getInstance()
  )

  private var desktopWidgetJob: Job? = null

  fun updateWidget(context: Context, scope: CoroutineScope, isPlaying: Boolean, screenOn: Boolean) {
    // 暂停停止更新进度条和时间
    if (!isPlaying) {
      // 暂停后不再持续更新，但需要完整刷新一次
      fullUpdate(context)
      stop()
    } else if (screenOn) {
      fullUpdate(context)
      // 开始播放后更新进度条和时间
      start(context, scope)
    }
  }

  fun partiallyUpdateWidget(context: Context, force: Boolean = false) {
    // app在前台不用更新
    if (!isAppOnForeground || force) {
      widgets.forEach {
        it.value.partiallyUpdateWidget(context, primaryColor())
      }
    }
  }

  fun stop() {
    desktopWidgetJob?.cancel()
    desktopWidgetJob = null
  }

  private fun fullUpdate(context: Context) {
    val primaryColor = primaryColor()
    widgets.forEach {
      it.value.updateWidget(context, null, true, primaryColor)
    }
  }

  private fun start(context: Context, scope: CoroutineScope) {
    if (desktopWidgetJob?.isActive == true) {
      return
    }
    desktopWidgetJob = scope.launch {
      while (isActive) {
        partiallyUpdateWidget(context)
        delay(INTERVAL_UPDATE_APPWIDGET)
      }
    }
  }

  private fun primaryColor(): Int {
    return themeController.appTheme.primary.toArgb()
  }

  private companion object {
    private const val APPWIDGET_BIG = "AppWidgetBig"
    private const val APPWIDGET_MEDIUM = "AppWidgetMedium"
    private const val APPWIDGET_SMALL = "AppWidgetSmall"
    private const val APPWIDGET_MEDIUM_TRANSPARENT = "AppWidgetMediumTransparent"
    private const val APPWIDGET_SMALL_TRANSPARENT = "AppWidgetSmallTransparent"
    private const val INTERVAL_UPDATE_APPWIDGET = 1000L
  }
}
