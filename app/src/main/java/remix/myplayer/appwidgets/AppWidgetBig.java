package remix.myplayer.appwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.activity.MainActivity;
import remix.myplayer.util.Constants;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.Util;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/1/23 10:58
 */

public class AppWidgetBig extends BaseAppwidget {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context,appWidgetIds);
        Intent intent = new Intent(MusicService.ACTION_WIDGET_UPDATE);
        intent.putExtra("WidgetName","BigWidget");
        intent.putExtra("WidgetIds",appWidgetIds);
        intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(intent);
    }

    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget_big);
        buildAction(context,remoteViews);
        pushUpdate(context,appWidgetIds,remoteViews);
    }

    public void updateWidget(final Context context,final int[] appWidgetIds, boolean reloadCover){
        final Song song = MusicService.getCurrentMP3();
        if(song == null || !hasInstances(context))
            return;
        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget_big);
        buildAction(context,remoteViews);
        updateRemoteViews(remoteViews,song);
        //设置时间
        long currentTime = MusicService.getProgress();
        if(currentTime > 0){
            remoteViews.setTextViewText(R.id.appwidget_progress, Util.getTime(currentTime));
        }
        //设置封面
        updateCover(context,remoteViews,appWidgetIds,reloadCover);
    }

    private void buildAction(Context context, RemoteViews views) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        ComponentName componentName = new ComponentName(context,MusicService.class);
        views.setOnClickPendingIntent(R.id.appwidget_toggle,buildPendingIntent(context,componentName,Constants.TOGGLE));
        views.setOnClickPendingIntent(R.id.appwidget_prev,buildPendingIntent(context,componentName, Constants.PREV));
        views.setOnClickPendingIntent(R.id.appwidget_next,buildPendingIntent(context,componentName,Constants.NEXT));
        views.setOnClickPendingIntent(R.id.appwidget_model,buildPendingIntent(context,componentName,Constants.CHANGE_MODEL));
        views.setOnClickPendingIntent(R.id.appwidget_love,buildPendingIntent(context,componentName,Constants.LOVE));
        views.setOnClickPendingIntent(R.id.appwidget_skin,buildPendingIntent(context,componentName,Constants.UPDATE_APPWIDGET));
        Intent action = new Intent(context, MainActivity.class);
        action.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        views.setOnClickPendingIntent(R.id.appwidget_clickable, PendingIntent.getActivity(context, 0, action, 0));
    }

}
