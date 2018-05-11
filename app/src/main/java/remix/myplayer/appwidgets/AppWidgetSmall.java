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

    public void updateWidget(final Context context,final int[] appWidgetIds, boolean reloadCover){
        final Song song = MusicService.getCurrentMP3();
        if(song == null || !hasInstances(context))
            return;
        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget_small);
        buildAction(context,remoteViews);
        updateRemoteViews(remoteViews,song);
//        remoteViews.setTextViewText(R.id.notify_song, temp.getTitle());
//        //播放暂停按钮
//        remoteViews.setImageViewResource(R.id.appwidget_toggle,MusicService.isPlay() ? R.drawable.widget_btn_stop_normal : R.drawable.widget_btn_play_normal);
//        //歌曲名和歌手名
//        remoteViews.setTextViewText(R.id.appwidget_title,temp.getTitle());
//        //是否收藏
//        remoteViews.setImageViewResource(R.id.appwidget_love,
//                PlayListUtil.isLove(temp.getId()) == PlayListUtil.EXIST ? R.drawable.widget_btn_like_prs : R.drawable.widget_btn_like_nor);
//        //播放模式
//        remoteViews.setImageViewResource(R.id.appwidget_model,MusicService.getPlayModel() == Constants.PLAY_LOOP ?
//                R.drawable.widget_btn_loop_normal :  MusicService.getPlayModel() == Constants.PLAY_REPEATONE ? R.drawable.widget_btn_one_normal : R.drawable.widget_btn_shuffle_normal);
//        //是否收藏
//        remoteViews.setImageViewResource(R.id.appwidget_love,
//                PlayListUtil.isLove(temp.getId()) == PlayListUtil.EXIST ? R.drawable.widget_btn_like_prs : R.drawable.widget_btn_like_nor);
//        //进度
//        remoteViews.setProgressBar(R.id.appwidget_seekbar,(int)temp.getDuration(), MusicService.getProgress(),false);

        //设置封面
        updateCover(context,remoteViews,appWidgetIds,reloadCover);
    }

    private void buildAction(Context context, RemoteViews views) {
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
