package remix.myplayer.appwidgets.medium

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
 * Created by Remix on 2016/12/20.
 */

class AppWidgetMedium : BaseAppwidget() {

  override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
    super.onUpdate(context,appWidgetManager,appWidgetIds)
    defaultAppWidget(context, appWidgetIds)
    val intent = Intent(MusicService.ACTION_WIDGET_UPDATE)
    intent.putExtra(EXTRA_WIDGET_NAME, this.javaClass.simpleName)
    intent.putExtra(EXTRA_WIDGET_IDS, appWidgetIds)
    Util.sendLocalBroadcast(intent)
  }

  private fun defaultAppWidget(context: Context, appWidgetIds: IntArray) {
    val remoteViews = RemoteViews(context.packageName, R.layout.app_widget_medium)
    buildAction(context, remoteViews)
    pushUpdate(context, appWidgetIds, remoteViews)
  }


  override fun updateWidget(service: MusicService, appWidgetIds: IntArray?, reloadCover: Boolean) {
    val song = service.currentSong
    if(song == Song.EMPTY_SONG){
      return
    }
    if(!hasInstances(service)){
      return
    }
    val remoteViews = RemoteViews(service.packageName, R.layout.app_widget_medium)
    buildAction(service, remoteViews)
    skin = AppWidgetSkin.WHITE_1F
    updateRemoteViews(service, remoteViews, song)
    //设置时间
    val currentTime = service.progress.toLong()
    val remainTime = song.getDuration() - service.progress
    if (currentTime > 0 && remainTime > 0) {
      remoteViews.setTextViewText(R.id.appwidget_progress, Util.getTime(currentTime) + "/" + Util.getTime(remainTime))
    }
    //设置封面
    updateCover(service, remoteViews, appWidgetIds, reloadCover)
  }

  override fun partiallyUpdateWidget(service: MusicService){
    val song = service.currentSong
    if(song == Song.EMPTY_SONG){
      return
    }
    if(!hasInstances(service)){
      return
    }
    val remoteViews = RemoteViews(service.packageName, R.layout.app_widget_medium)
    buildAction(service, remoteViews)
    skin = AppWidgetSkin.WHITE_1F
    updateRemoteViews(service, remoteViews, song)
    //设置时间
    val currentTime = service.progress.toLong()
    val remainTime = song.getDuration() - service.progress
    if (currentTime > 0 && remainTime > 0) {
      remoteViews.setTextViewText(R.id.appwidget_progress, Util.getTime(currentTime) + "/" + Util.getTime(remainTime))
    }
    val appIds = AppWidgetManager.getInstance(service).getAppWidgetIds(ComponentName(service, javaClass))
    pushPartiallyUpdate(service,appIds,remoteViews)
  }

  companion object {
    @Volatile
    private var INSTANCE: AppWidgetMedium? = null

    @JvmStatic
    fun getInstance(): AppWidgetMedium =
        INSTANCE ?: synchronized(this) {
          INSTANCE ?: AppWidgetMedium()
        }
  }
}
