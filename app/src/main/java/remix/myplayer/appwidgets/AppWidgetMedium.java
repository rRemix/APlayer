package remix.myplayer.appwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import remix.myplayer.R;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.activity.MainActivity;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;

/**
 * Created by Remix on 2016/12/20.
 */

public class AppWidgetMedium extends BaseAppwidget {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        context.startService(new Intent(context,MusicService.class));
        mAppIds = appWidgetIds;
        mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget_medium);
        buildAction(context, mRemoteViews);
        pushUpdate(context,appWidgetIds,mRemoteViews);
        Intent intent = new Intent(Constants.WIDGET_UPDATE);
        intent.putExtra("WidgetName","MediumWidget");
        intent.putExtra("WidgetIds",appWidgetIds);
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.sendBroadcast(intent);
    }

    public void updateWidget(final Context context,boolean reloadCover){
        MP3Item temp = MusicService.getCurrentMP3();
        if(temp == null || !hasInstances(context))
            return;
        if(mRemoteViews == null) {
            mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget_medium);
            buildAction(context,mRemoteViews);
        }
        mRemoteViews.setTextViewText(R.id.notify_song, temp.getTitle());
        //播放暂停按钮
        mRemoteViews.setImageViewResource(R.id.appwidget_toggle,MusicService.isPlay() ? R.drawable.widget_btn_stop_normal : R.drawable.widget_btn_play_normal);
        //歌曲名和歌手名
        mRemoteViews.setTextViewText(R.id.appwidget_title,temp.getTitle());
        mRemoteViews.setTextViewText(R.id.appwidget_artist,temp.getArtist());
        //播放模式
        mRemoteViews.setImageViewResource(R.id.appwidget_model,MusicService.getPlayModel() == Constants.PLAY_LOOP ?
               R.drawable.play_btn_loop :  MusicService.getPlayModel() == Constants.PLAY_REPEATONE ? R.drawable.play_btn_loop_one : R.drawable.play_btn_shuffle);
        //是否收藏

        //设置时间
        long currentTime = MusicService.getProgress();
        long remainTime = temp.getDuration() - MusicService.getProgress();
        if(currentTime > 0 && remainTime > 0){
            mRemoteViews.setTextViewText(R.id.appwidget_progress, CommonUtil.getTime(currentTime) + "/" + CommonUtil.getTime(remainTime));
        }
        //进度
        mRemoteViews.setProgressBar(R.id.appwidget_seekbar,(int)temp.getDuration(),(int)currentTime,false);
        //设置封面
        updateCover(context,temp.getAlbumId(),reloadCover);
    }

    private void buildAction(Context context, RemoteViews views) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        views.setOnClickPendingIntent(R.id.appwidget_text_container,PendingIntent.getActivity(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT));
        ComponentName componentName = new ComponentName(context,MusicService.class);
        views.setOnClickPendingIntent(R.id.appwidget_toggle,buildPendingIntent(context,componentName,Constants.TOGGLE));
        views.setOnClickPendingIntent(R.id.appwidget_prev,buildPendingIntent(context,componentName, Constants.PREV));
        views.setOnClickPendingIntent(R.id.appwidget_next,buildPendingIntent(context,componentName,Constants.NEXT));
        views.setOnClickPendingIntent(R.id.appwidget_model,buildPendingIntent(context,componentName,Constants.CHANGE_MODEL));
    }



}
