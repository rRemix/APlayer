package remix.myplayer.appwidgets.medium

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import remix.myplayer.R
import remix.myplayer.appwidgets.AppWidgetSkin
import remix.myplayer.appwidgets.BaseAppwidget
import remix.myplayer.service.MusicService
import remix.myplayer.util.Util

class AppWidgetMediumTransparent : BaseAppwidget() {
  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    defaultAppWidget(context, appWidgetIds)
    val intent = Intent(MusicService.ACTION_WIDGET_UPDATE)
    intent.putExtra("WidgetName", "MediumWidgetTransparent")
    intent.putExtra("WidgetIds", appWidgetIds)
    intent.flags = Intent.FLAG_RECEIVER_REGISTERED_ONLY
    context.sendBroadcast(intent)
  }

  private fun defaultAppWidget(context: Context, appWidgetIds: IntArray) {
    val remoteViews = RemoteViews(context.packageName, R.layout.app_widget_medium_transparent)
    buildAction(context, remoteViews)
    pushUpdate(context, appWidgetIds, remoteViews)
  }

  override fun onReceive(context: Context, intent: Intent) {
    super.onReceive(context, intent)
  }

  override fun updateWidget(service: MusicService, appWidgetIds: IntArray?, reloadCover: Boolean) {
    val song = service.currentSong
    if (song == null || !hasInstances(service))
      return
    val remoteViews = RemoteViews(service.packageName, R.layout.app_widget_medium_transparent)
    buildAction(service, remoteViews)
    skin = AppWidgetSkin.TRANSPARENT
    updateRemoteViews(service, remoteViews, song)
    //设置时间
    val currentTime = service.progress.toLong()
    val remainTime = song.duration - service.progress
    if (currentTime > 0 && remainTime > 0) {
      remoteViews.setTextViewText(R.id.appwidget_progress, Util.getTime(currentTime) + "/" + Util.getTime(remainTime))
    }
    //设置封面
    updateCover(service, remoteViews, appWidgetIds, reloadCover)
  }
}

