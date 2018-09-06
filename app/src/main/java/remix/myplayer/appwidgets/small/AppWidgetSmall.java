package remix.myplayer.appwidgets.small;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import remix.myplayer.R;
import remix.myplayer.appwidgets.AppWidgetSkin;
import remix.myplayer.appwidgets.BaseAppwidget;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.service.MusicService;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/12/28 11:22
 */

public class AppWidgetSmall extends BaseAppwidget {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context, appWidgetIds);
        Intent intent = new Intent(MusicService.ACTION_WIDGET_UPDATE);
        intent.putExtra("WidgetName", "SmallWidget");
        intent.putExtra("WidgetIds", appWidgetIds);
        intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(intent);
    }

    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget_small);
        buildAction(context, remoteViews);
        pushUpdate(context, appWidgetIds, remoteViews);
    }

    @Override
    public void updateWidget(final MusicService service, final int[] appWidgetIds, boolean reloadCover) {
        final Song song = service.getCurrentSong();
        if (song == null || !hasInstances(service))
            return;
        final RemoteViews remoteViews = new RemoteViews(service.getPackageName(), R.layout.app_widget_small);
        buildAction(service, remoteViews);
        mSkin = AppWidgetSkin.WHITE_1F;
        updateRemoteViews(service,remoteViews, song);
        //设置封面
        updateCover(service, remoteViews, appWidgetIds, reloadCover);
    }
}
