package remix.myplayer.appwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.RemoteViews;

import remix.myplayer.R;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.activity.MainActivity;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;

/**
 * Created by Remix on 2016/12/20.
 */

public class AppWidgetBig extends BaseAppwidget {
    private static int[] mAppIds;
    private RemoteViews mRemoteViews;
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        mAppIds = appWidgetIds;
        mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget_big);
        buildAction(context, mRemoteViews);
        pushUpdate(context,appWidgetIds,mRemoteViews);
        Intent intent = new Intent(Constants.WIDGET_UPDATE);
        intent.putExtra("WidgetName","BigWidget");
        intent.putExtra("WidgetIds",appWidgetIds);
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.sendBroadcast(intent);
    }

    public void updateWidget(Context context){
        MP3Item temp = MusicService.getCurrentMP3();
        if(temp == null)
            return;
        if(mRemoteViews == null)
            mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget_big);
        mRemoteViews.setTextViewText(R.id.notify_song, temp.getTitle());
        //设置封面
        Bitmap bitmap = MediaStoreUtil.getAlbumBitmap(temp.getAlbumId(), true);
        if(bitmap != null) {
            mRemoteViews.setImageViewBitmap(R.id.appwidget_image,bitmap);
        } else {
            mRemoteViews.setImageViewResource(R.id.appwidget_image, R.drawable.album_empty_bg_day);
        }

        //播放暂停按钮
        mRemoteViews.setImageViewResource(R.id.appwidget_toggle,MusicService.getIsplay() ? R.drawable.notify_pause : R.drawable.notify_play);
        //歌曲名和歌手名
        mRemoteViews.setTextViewText(R.id.appwidget_title,temp.getTitle());
        mRemoteViews.setTextViewText(R.id.appwidget_artist,temp.getArtist());
        pushUpdate(context,mAppIds,mRemoteViews);
    }

    private void buildAction(Context context, RemoteViews views) {
        ComponentName componentName = new ComponentName(context,MusicService.class);
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent activityIntent = PendingIntent.getActivity(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.appwidget_text_container,activityIntent);
        views.setOnClickPendingIntent(R.id.appwidget_prev,buildPendingIntent(context, Constants.PREV,componentName));
        views.setOnClickPendingIntent(R.id.appwidget_toggle,buildPendingIntent(context,Constants.PLAYORPAUSE,componentName));
        views.setOnClickPendingIntent(R.id.appwidget_next,buildPendingIntent(context,Constants.NEXT,componentName));
    }

    private void pushUpdate(Context context, int[] appWidgetId, RemoteViews remoteViews) {
        AppWidgetManager localAppWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetId != null) {
            localAppWidgetManager.updateAppWidget(appWidgetId, remoteViews);
            return;
        }
        localAppWidgetManager.updateAppWidget(new ComponentName(context, getClass()), remoteViews);
    }


}
