package remix.myplayer.appwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.widget.RemoteViews;

import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.request.network.RemoteUriRequest;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.Util;

import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/12/28 15:50
 */

public class BaseAppwidget extends AppWidgetProvider {
    protected Bitmap mBitmap;

    protected PendingIntent buildPendingIntent(Context context,ComponentName componentName,int operation) {
        Intent intent = new Intent(MusicService.ACTION_APPWIDGET_OPERATE);
        intent.putExtra("Control",operation);
        intent.setComponent(componentName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return PendingIntent.getForegroundService(context, operation, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            return PendingIntent.getService(context, operation, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
//        return PendingIntent.getService(context, operation, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    protected boolean hasInstances(Context context) {
        int[] appIds = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, getClass()));
        return appIds != null && appIds.length > 0;
    }

    protected void updateCover(final Context context, final RemoteViews remoteViews,final int[] appWidgetIds, boolean reloadCover){
        Song song = MusicService.getCurrentMP3();
        if(song == null)
            return;
        //设置封面
        if(!reloadCover){
            if(mBitmap != null && !mBitmap.isRecycled()) {
                remoteViews.setImageViewBitmap(R.id.appwidget_image, mBitmap);
            } else {
                remoteViews.setImageViewResource(R.id.appwidget_image, R.drawable.album_empty_bg_night);
            }
            pushUpdate(context,appWidgetIds,remoteViews);
        } else {
            final int size = DensityUtil.dip2px(context,72);

            new RemoteUriRequest(getSearchRequestWithAlbumType(song),new RequestConfig.Builder(size,size).build()){
                @Override
                public void onError(String errMsg) {
                    if(mBitmap != null && !mBitmap.isRecycled()){
                        mBitmap.recycle();
                    }
                    mBitmap = null;
                    remoteViews.setImageViewResource(R.id.appwidget_image, R.drawable.album_empty_bg_day);
                    pushUpdate(context,appWidgetIds,remoteViews);
                }

                @Override
                public void onSuccess(Bitmap result) {
                    try {
                        if(mBitmap != null && !mBitmap.isRecycled()){
                            mBitmap.recycle();
                            mBitmap = null;
                        }
                        mBitmap = MusicService.copy(result);
                        if(mBitmap != null) {
                            remoteViews.setImageViewBitmap(R.id.appwidget_image, mBitmap);
                        } else {
                            remoteViews.setImageViewResource(R.id.appwidget_image, R.drawable.album_empty_bg_day);
                        }

                    } catch (Exception e){
                        e.printStackTrace();
                    } finally {
                        pushUpdate(context,appWidgetIds,remoteViews);
                    }
                }
            }.load();
        }
    }


    protected void pushUpdate(Context context, int[] appWidgetId, RemoteViews remoteViews) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetId != null) {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
            return;
        }
        appWidgetManager.updateAppWidget(new ComponentName(context, getClass()), remoteViews);
    }

    protected void updateProgress(RemoteViews remoteViews, Song song,boolean transParent) {
        //设置时间
        if(transParent){
            remoteViews.setTextColor(R.id.appwidget_progress,Color.WHITE);
        } else {
            remoteViews.setTextColor(R.id.appwidget_progress,ColorUtil.getColor(R.color.appwidget_progress_color));
        }
        //进度
        remoteViews.setProgressBar(R.id.appwidget_seekbar,(int)song.getDuration(),MusicService.getProgress(),false);
    }

    protected void updateLove(RemoteViews remoteViews, Song song, boolean transParent) {
        //是否收藏
        if(PlayListUtil.isLove(song.getId()) == PlayListUtil.EXIST ){
            if(transParent){
                Drawable likeDrawable = Theme.TintDrawable(R.drawable.widget_btn_like_nor, Color.WHITE);
                remoteViews.setImageViewBitmap(R.id.appwidget_love,drawableToBitmap(likeDrawable));
            } else {
                remoteViews.setImageViewResource(R.id.appwidget_love,R.drawable.widget_btn_like_nor);
            }
        } else {
            remoteViews.setImageViewResource(R.id.appwidget_love, R.drawable.widget_btn_like_prs);
        }
    }

    protected void updateNextAndPrev(RemoteViews remoteViews, boolean transParent) {
        //上下首歌曲
        if(transParent){
            Drawable nextDrawable = Theme.TintDrawable(R.drawable.widget_btn_next_normal, Color.WHITE);
            remoteViews.setImageViewBitmap(R.id.appwidget_next,drawableToBitmap(nextDrawable));
            Drawable prevDrawable = Theme.TintDrawable(R.drawable.widget_btn_previous_normal,Color.WHITE);
            remoteViews.setImageViewBitmap(R.id.appwidget_prev,drawableToBitmap(prevDrawable));
        }
    }

    protected void updateModel(RemoteViews remoteViews, boolean transParent) {
        //播放模式
        if(transParent){
            Drawable modelDrawable = Theme.TintDrawable(MusicService.getPlayModel() == Constants.PLAY_LOOP ?
                            R.drawable.widget_btn_loop_normal :  MusicService.getPlayModel() == Constants.PLAY_REPEATONE ? R.drawable.widget_btn_one_normal : R.drawable.widget_btn_shuffle_normal,
                    Color.WHITE);
            remoteViews.setImageViewBitmap(R.id.appwidget_model,drawableToBitmap(modelDrawable));
        } else {
            remoteViews.setImageViewResource(R.id.appwidget_model,MusicService.getPlayModel() == Constants.PLAY_LOOP ?
                    R.drawable.widget_btn_loop_normal :  MusicService.getPlayModel() == Constants.PLAY_REPEATONE ? R.drawable.widget_btn_one_normal : R.drawable.widget_btn_shuffle_normal);
        }
    }

    protected void updatePlayPause(RemoteViews remoteViews, boolean transParent) {
        //播放暂停按钮
        if(transParent){
            Drawable playPauseDrawable = Theme.TintDrawable(MusicService.isPlay() ? R.drawable.widget_btn_stop_normal : R.drawable.widget_btn_play_normal, Color.WHITE);
            remoteViews.setImageViewBitmap(R.id.appwidget_toggle,drawableToBitmap(playPauseDrawable));
        } else {
            remoteViews.setImageViewResource(R.id.appwidget_toggle,MusicService.isPlay() ? R.drawable.widget_btn_stop_normal : R.drawable.widget_btn_play_normal);
        }
    }

    protected void updateTitle(RemoteViews remoteViews, Song song, boolean transParent) {
        //歌曲名
        if(transParent){
            remoteViews.setTextColor(R.id.appwidget_title, Color.WHITE);
        } else {
            remoteViews.setTextColor(R.id.appwidget_title, ColorUtil.getColor(R.color.appwidget_title_color));
        }
        remoteViews.setTextViewText(R.id.appwidget_title,song.getTitle());
    }

    protected void updateArtist(RemoteViews remoteViews, Song song, boolean transParent){
        //歌手名
        if(transParent){
            remoteViews.setTextColor(R.id.appwidget_artist,ColorUtil.getColor(R.color.day_scan_track_color));
        } else {
            remoteViews.setTextColor(R.id.appwidget_artist,ColorUtil.getColor(R.color.appwidget_artist_color));
        }
        remoteViews.setTextViewText(R.id.appwidget_artist,song.getArtist());
    }

    protected void updateBackground(RemoteViews remoteViews, boolean transParent) {
        if(transParent){
            remoteViews.setImageViewResource(R.id.appwidget_clickable,R.drawable.bg_corner_app_widget_white_0f);
        } else {
            remoteViews.setImageViewResource(R.id.appwidget_clickable,R.drawable.bg_corner_app_widget_white_1f);
        }
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        // 取 drawable 的长宽
        int w = drawable.getIntrinsicWidth();
        int h = drawable.getIntrinsicHeight();

        // 取 drawable 的颜色格式
        Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                : Bitmap.Config.RGB_565;
        // 建立对应 bitmap
        Bitmap bitmap = Bitmap.createBitmap(w, h, config);
        // 建立对应 bitmap 的画布
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        // 把 drawable 内容画到画布中
        drawable.draw(canvas);
        return bitmap;
    }
}
