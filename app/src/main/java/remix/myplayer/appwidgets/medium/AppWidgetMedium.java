package remix.myplayer.appwidgets.medium;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import remix.myplayer.R;
import remix.myplayer.appwidgets.AppWidgetSkin;
import remix.myplayer.appwidgets.BaseAppwidget;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.service.MusicService;
import remix.myplayer.util.Util;

/**
 * Created by Remix on 2016/12/20.
 */

public class AppWidgetMedium extends BaseAppwidget {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context, appWidgetIds);
        Intent intent = new Intent(MusicService.ACTION_WIDGET_UPDATE);
        intent.putExtra("WidgetName", "MediumWidget");
        intent.putExtra("WidgetIds", appWidgetIds);
        intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(intent);
    }

    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget_medium);
        buildAction(context, remoteViews);
        pushUpdate(context, appWidgetIds, remoteViews);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    @Override
    public void updateWidget(final MusicService service, final int[] appWidgetIds, boolean reloadCover) {
        final Song song = service.getCurrentSong();
        if (song == null || !hasInstances(service))
            return;
        final RemoteViews remoteViews = new RemoteViews(service.getPackageName(), R.layout.app_widget_medium);
        buildAction(service, remoteViews);
        mSkin = AppWidgetSkin.WHITE_1F;
        updateRemoteViews(service, remoteViews, song);
        //设置时间
        long currentTime = service.getProgress();
        long remainTime = song.getDuration() - service.getProgress();
        if (currentTime > 0 && remainTime > 0) {
            remoteViews.setTextViewText(R.id.appwidget_progress, Util.getTime(currentTime) + "/" + Util.getTime(remainTime));
        }
        //设置封面
        updateCover(service, remoteViews, appWidgetIds, reloadCover);
    }
}
