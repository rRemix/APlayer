package remix.myplayer.appwidgets.small

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import remix.myplayer.R
import remix.myplayer.appwidgets.AppWidgetSkin
import remix.myplayer.appwidgets.BaseAppwidget
import remix.myplayer.bean.mp3.Song
import remix.myplayer.service.MusicService
import remix.myplayer.util.Util

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/12/28 11:22
 */

class AppWidgetSmall : BaseAppwidget() {

  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    super.onUpdate(context, appWidgetManager, appWidgetIds)
    defaultAppWidget(context, appWidgetIds)
    val intent = Intent(MusicService.ACTION_WIDGET_UPDATE)
    intent.putExtra(EXTRA_WIDGET_NAME, this.javaClass.simpleName)
    intent.putExtra(EXTRA_WIDGET_IDS, appWidgetIds)
    Util.sendLocalBroadcast(intent)
  }

  private fun defaultAppWidget(context: Context, appWidgetIds: IntArray) {
    val remoteViews = RemoteViews(context.packageName, R.layout.app_widget_small)
    buildAction(context, remoteViews)
    pushUpdate(context, appWidgetIds, remoteViews)
  }

  override fun updateWidget(service: MusicService, appWidgetIds: IntArray?, reloadCover: Boolean) {
    val song = service.currentSong
    if (song == Song.EMPTY_SONG) {
      return
    }
    if (!hasInstances(service)) {
      return
    }
    val remoteViews = RemoteViews(service.packageName, R.layout.app_widget_small)
    buildAction(service, remoteViews)
    skin = AppWidgetSkin.WHITE_1F
    updateRemoteViews(service, remoteViews, song)
    //设置封面
    updateCover(service, remoteViews, appWidgetIds, reloadCover)
  }

  override fun partiallyUpdateWidget(service: MusicService) {
    val song = service.currentSong
    if (song == Song.EMPTY_SONG) {
      return
    }
    if (!hasInstances(service)) {
      return
    }
    val remoteViews = RemoteViews(service.packageName, R.layout.app_widget_small)
    buildAction(service, remoteViews)
    skin = AppWidgetSkin.WHITE_1F
    updateRemoteViews(service, remoteViews, song)

    val appIds = AppWidgetManager.getInstance(service).getAppWidgetIds(ComponentName(service, javaClass))
    pushPartiallyUpdate(service, appIds, remoteViews)
  }

  companion object {
    @Volatile
    private var INSTANCE: AppWidgetSmall? = null

    @JvmStatic
    fun getInstance(): AppWidgetSmall =
        INSTANCE ?: synchronized(this) {
          INSTANCE ?: AppWidgetSmall()
        }
  }
}
