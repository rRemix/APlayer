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
import remix.myplayer.util.PlayListUtil;

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
        Intent intent = new Intent(Constants.WIDGET_UPDATE);
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
        MP3Item temp = MusicService.getCurrentMP3();
        if(temp == null || !hasInstances(context))
            return;
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget_big);
        buildAction(context,remoteViews);

        remoteViews.setTextViewText(R.id.notify_song, temp.getTitle());
        //播放暂停按钮
        remoteViews.setImageViewResource(R.id.appwidget_toggle,MusicService.isPlay() ? R.drawable.widget_btn_stop_normal : R.drawable.widget_btn_play_normal);
        //歌曲名和歌手名
        remoteViews.setTextViewText(R.id.appwidget_title,temp.getTitle());
        remoteViews.setTextViewText(R.id.appwidget_artist,temp.getArtist());
        //播放模式
        remoteViews.setImageViewResource(R.id.appwidget_model,MusicService.getPlayModel() == Constants.PLAY_LOOP ?
                R.drawable.widget_btn_loop_normal :  MusicService.getPlayModel() == Constants.PLAY_REPEATONE ? R.drawable.widget_btn_one_normal : R.drawable.widget_btn_shuffle_normal);
        //是否收藏
        remoteViews.setImageViewResource(R.id.appwidget_love,
                PlayListUtil.isLove(temp.getId()) == PlayListUtil.EXIST ? R.drawable.widget_btn_like_prs : R.drawable.widget_btn_like_nor);

        //设置时间
        long currentTime = MusicService.getProgress();
        long remainTime = temp.getDuration() - MusicService.getProgress();
        if(currentTime > 0 && remainTime > 0){
            remoteViews.setTextViewText(R.id.appwidget_progress, CommonUtil.getTime(currentTime) + "/" + CommonUtil.getTime(remainTime));
        }
        //进度
        remoteViews.setProgressBar(R.id.appwidget_seekbar,(int)temp.getDuration(),(int)currentTime,false);
        //设置封面
        updateCover(context,remoteViews,appWidgetIds,temp.getAlbumId(),reloadCover);
    }

    private void buildAction(Context context, RemoteViews views) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        views.setOnClickPendingIntent(R.id.appwidget_text_container, PendingIntent.getActivity(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT));
        ComponentName componentName = new ComponentName(context,MusicService.class);
        views.setOnClickPendingIntent(R.id.appwidget_toggle,buildPendingIntent(context,componentName,Constants.TOGGLE));
        views.setOnClickPendingIntent(R.id.appwidget_prev,buildPendingIntent(context,componentName, Constants.PREV));
        views.setOnClickPendingIntent(R.id.appwidget_next,buildPendingIntent(context,componentName,Constants.NEXT));
        views.setOnClickPendingIntent(R.id.appwidget_model,buildPendingIntent(context,componentName,Constants.CHANGE_MODEL));
        views.setOnClickPendingIntent(R.id.appwidget_love,buildPendingIntent(context,componentName,Constants.LOVE));
    }

}
