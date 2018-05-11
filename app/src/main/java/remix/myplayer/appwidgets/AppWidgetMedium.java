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
 * Created by Remix on 2016/12/20.
 */

public class AppWidgetMedium extends BaseAppwidget {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        defaultAppWidget(context,appWidgetIds);
        Intent intent = new Intent(MusicService.ACTION_WIDGET_UPDATE);
        intent.putExtra("WidgetName","MediumWidget");
        intent.putExtra("WidgetIds",appWidgetIds);
        intent.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        context.sendBroadcast(intent);
    }

    private void defaultAppWidget(Context context, int[] appWidgetIds) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget_medium);
        buildAction(context,remoteViews);
        pushUpdate(context,appWidgetIds,remoteViews);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
    }

    public void updateWidget(final Context context,final int[] appWidgetIds, boolean reloadCover){
        final Song song = MusicService.getCurrentMP3();
        if(song == null || !hasInstances(context))
            return;
        final RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget_medium);
        buildAction(context,remoteViews);
        updateRemoteViews(remoteViews,song);
        //设置时间
        long currentTime = MusicService.getProgress();
        long remainTime = song.getDuration() - MusicService.getProgress();
        if(currentTime > 0 && remainTime > 0){
            remoteViews.setTextViewText(R.id.appwidget_progress, Util.getTime(currentTime) + "/" + Util.getTime(remainTime));
        }

//        boolean transParent = SPUtil.getValue(context,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.APP_WIDGET_SKIN,true);
//        boolean transParent = true;
//        if(transParent){
//            remoteViews.setImageViewResource(R.id.appwidget_clickable,R.drawable.bg_corner_app_widget_transparent);
//        } else {
//            remoteViews.setImageViewResource(R.id.appwidget_clickable,R.drawable.bg_corner_app_widget_white_1f);
//        }
//
//        remoteViews.setTextViewText(R.id.notify_song, temp.getTitle());
//        //播放暂停按钮
//        if(transParent){
//            Drawable playPauseDrawable = Theme.TintDrawable(MusicService.isPlay() ? R.drawable.widget_btn_stop_normal : R.drawable.widget_btn_play_normal, Color.WHITE);
//            remoteViews.setImageViewBitmap(R.id.appwidget_toggle,drawableToBitmap(playPauseDrawable));
//        } else {
//            remoteViews.setImageViewResource(R.id.appwidget_toggle,MusicService.isPlay() ? R.drawable.widget_btn_stop_normal : R.drawable.widget_btn_play_normal);
//        }
//
//        //歌曲名和歌手名
//        if(transParent){
//            remoteViews.setTextColor(R.id.appwidget_title,Color.WHITE);
////            remoteViews.setTextColor(R.id.appwidget_artist,Color.WHITE);
//            remoteViews.setTextColor(R.id.appwidget_progress,Color.WHITE);
//        } else {
//            remoteViews.setTextColor(R.id.appwidget_title, ColorUtil.getColor(R.color.appwidget_title_color));
////            remoteViews.setTextColor(R.id.appwidget_artist,ColorUtil.getColor(R.color.appwidget_artist_color));
//            remoteViews.setTextColor(R.id.appwidget_progress,ColorUtil.getColor(R.color.appwidget_progress_color));
//        }
//        remoteViews.setTextViewText(R.id.appwidget_title,temp.getTitle());
//        remoteViews.setTextViewText(R.id.appwidget_artist,temp.getArtist());
//        //播放模式
//        if(transParent){
//            Drawable modelDrawable = Theme.TintDrawable(MusicService.getPlayModel() == Constants.PLAY_LOOP ?
//                    R.drawable.widget_btn_loop_normal :  MusicService.getPlayModel() == Constants.PLAY_REPEATONE ? R.drawable.widget_btn_one_normal : R.drawable.widget_btn_shuffle_normal,
//                    Color.WHITE);
//            remoteViews.setImageViewBitmap(R.id.appwidget_model,drawableToBitmap(modelDrawable));
//        } else {
//            remoteViews.setImageViewResource(R.id.appwidget_model,MusicService.getPlayModel() == Constants.PLAY_LOOP ?
//                    R.drawable.widget_btn_loop_normal :  MusicService.getPlayModel() == Constants.PLAY_REPEATONE ? R.drawable.widget_btn_one_normal : R.drawable.widget_btn_shuffle_normal);
//        }
//        //上下首歌曲
//        if(transParent){
//            Drawable nextDrawable = Theme.TintDrawable(R.drawable.widget_btn_next_normal,Color.WHITE);
//            remoteViews.setImageViewBitmap(R.id.appwidget_next,drawableToBitmap(nextDrawable));
//            Drawable prevDrawable = Theme.TintDrawable(R.drawable.widget_btn_previous_normal,Color.WHITE);
//            remoteViews.setImageViewBitmap(R.id.appwidget_prev,drawableToBitmap(prevDrawable));
//        }
//        //是否收藏
//        if(PlayListUtil.isLove(temp.getId()) == PlayListUtil.EXIST ){
//            if(transParent){
//                Drawable likeDrawable = Theme.TintDrawable(R.drawable.widget_btn_like_nor,Color.WHITE);
//                remoteViews.setImageViewBitmap(R.id.appwidget_love,drawableToBitmap(likeDrawable));
//            } else {
//                remoteViews.setImageViewResource(R.id.appwidget_love,R.drawable.widget_btn_like_nor);
//            }
//        } else {
//            remoteViews.setImageViewResource(R.id.appwidget_love, R.drawable.widget_btn_like_prs);
//        }
//
//
//        //设置时间
//        long currentTime = MusicService.getProgress();
//        long remainTime = temp.getDuration() - MusicService.getProgress();
//        if(currentTime > 0 && remainTime > 0){
//            remoteViews.setTextViewText(R.id.appwidget_progress, Util.getTime(currentTime) + "/" + Util.getTime(remainTime));
//        }
//        //进度
//        remoteViews.setProgressBar(R.id.appwidget_seekbar,(int)temp.getDuration(),(int)currentTime,false);
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
