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

public class AppWidgetMediumTransparent extends BaseAppwidget {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context,appWidgetIds);
        Intent intent = new Intent(MusicService.ACTION_WIDGET_UPDATE);
        intent.putExtra("WidgetName","MediumWidgetTransparent");
        intent.putExtra("WidgetIds",appWidgetIds);
        intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(intent);
    }

    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget_medium_transparent);
        buildAction(context,remoteViews);
        pushUpdate(context,appWidgetIds,remoteViews);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    @Override
    public void updateWidget(final Context context,final int[] appWidgetIds, boolean reloadCover){
        final Song song = MusicService.getCurrentMP3();
        if(song == null || !hasInstances(context))
            return;
        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget_medium_transparent);
        buildAction(context,remoteViews);
        mSkin = AppWidgetSkin.TRANSPARENT;
        updateRemoteViews(remoteViews,song);
        //设置时间
        long currentTime = MusicService.getProgress();
        long remainTime = song.getDuration() - MusicService.getProgress();
        if(currentTime > 0 && remainTime > 0){
            remoteViews.setTextViewText(R.id.appwidget_progress, Util.getTime(currentTime) + "/" + Util.getTime(remainTime));
        }
        //设置封面
        updateCover(context,remoteViews,appWidgetIds,reloadCover);
    }
}

