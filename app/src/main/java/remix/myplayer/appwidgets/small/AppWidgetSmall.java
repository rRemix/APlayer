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
        defaultAppWidget(context,appWidgetIds);
        Intent intent = new Intent(MusicService.ACTION_WIDGET_UPDATE);
        intent.putExtra("WidgetName","SmallWidget");
        intent.putExtra("WidgetIds",appWidgetIds);
        intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(intent);
    }

    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget_small);
        buildAction(context,remoteViews);
        pushUpdate(context,appWidgetIds,remoteViews);
    }

    @Override
    public void updateWidget(final Context context,final int[] appWidgetIds, boolean reloadCover){
        final Song song = MusicService.getCurrentMP3();
        if(song == null || !hasInstances(context))
            return;
        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget_small);
        buildAction(context,remoteViews);
        mSkin = AppWidgetSkin.WHITE_1F;
        updateRemoteViews(remoteViews,song);
        //设置封面
        updateCover(context,remoteViews,appWidgetIds,reloadCover);
    }
}
