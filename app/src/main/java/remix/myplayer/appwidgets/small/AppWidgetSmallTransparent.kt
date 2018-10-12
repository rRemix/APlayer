package remix.myplayer.appwidgets.small

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import remix.myplayer.R
import remix.myplayer.appwidgets.AppWidgetSkin
import remix.myplayer.appwidgets.BaseAppwidget
import remix.myplayer.service.MusicService

class AppWidgetSmallTransparent : BaseAppwidget() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        defaultAppWidget(context, appWidgetIds)
        val intent = Intent(MusicService.ACTION_WIDGET_UPDATE)
        intent.putExtra("WidgetName", "SmallWidgetTransparent")
        intent.putExtra("WidgetIds", appWidgetIds)
        intent.flags = Intent.FLAG_RECEIVER_REGISTERED_ONLY
        context.sendBroadcast(intent)
    }

    private fun defaultAppWidget(context: Context, appWidgetIds: IntArray) {
        val remoteViews = RemoteViews(context.packageName, R.layout.app_widget_small_transparent)
        buildAction(context, remoteViews)
        pushUpdate(context, appWidgetIds, remoteViews)
    }

    override fun updateWidget(service: MusicService, appWidgetIds: IntArray?, reloadCover: Boolean) {
        val song = service.currentSong
        if (!hasInstances(service))
            return
        val remoteViews = RemoteViews(service.packageName, R.layout.app_widget_small_transparent)
        buildAction(service, remoteViews)
        mSkin = AppWidgetSkin.TRANSPARENT
        updateRemoteViews(service, remoteViews, song)
        //设置封面
        updateCover(service, remoteViews, appWidgetIds, reloadCover)
    }
}
