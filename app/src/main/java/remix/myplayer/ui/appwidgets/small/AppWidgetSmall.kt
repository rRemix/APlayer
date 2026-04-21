package remix.myplayer.ui.appwidgets.small

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import remix.myplayer.R
import remix.myplayer.data.model.audio.Song
import remix.myplayer.service.MusicService
import remix.myplayer.ui.appwidgets.AppWidgetSkin
import remix.myplayer.ui.appwidgets.BaseAppwidget
import remix.myplayer.util.Util

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/12/28 11:22
 */

class AppWidgetSmall : BaseAppwidget() {

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
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

  override fun updateWidget(
    context: Context,
    appWidgetIds: IntArray?,
    reloadCover: Boolean,
    primaryColor: Int
  ) {
    val song = playbackState.song
    if (song == Song.EMPTY_SONG) {
      return
    }
    if (!hasInstances(context)) {
      return
    }
    val remoteViews = RemoteViews(context.packageName, R.layout.app_widget_small)
    buildAction(context, remoteViews)
    skin = AppWidgetSkin.WHITE_1F
    updateRemoteViews(context, remoteViews, song, primaryColor)
    //设置封面
    updateCover(context, remoteViews, appWidgetIds, reloadCover)
  }

  override fun partiallyUpdateWidget(context: Context, primaryColor: Int) {
    val song = playbackState.song
    if (song == Song.EMPTY_SONG) {
      return
    }
    if (!hasInstances(context)) {
      return
    }
    val remoteViews = RemoteViews(context.packageName, R.layout.app_widget_small)
    buildAction(context, remoteViews)
    skin = AppWidgetSkin.WHITE_1F
    updateRemoteViews(context, remoteViews, song, primaryColor)

    val appIds =
      AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, javaClass))
    pushPartiallyUpdate(context, appIds, remoteViews)
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
