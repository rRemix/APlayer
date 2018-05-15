package remix.myplayer.appwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.widget.RemoteViews;

import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.request.RemoteUriRequest;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.ui.activity.MainActivity;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.SPUtil;

import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/12/28 15:50
 */

public class BaseAppwidget extends AppWidgetProvider {
    public static final int SKIN_WHITE_1F = 1;//白色不带透明
    public static final int SKIN_TRANSPARENT = 2;//透明

    protected AppWidgetSkin mSkin;
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

    protected void buildAction(Context context, RemoteViews views) {
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

    protected void pushUpdate(Context context, int[] appWidgetId, RemoteViews remoteViews) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetId != null) {
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
            return;
        }
        appWidgetManager.updateAppWidget(new ComponentName(context, getClass()), remoteViews);
    }

    protected void updateRemoteViews(RemoteViews remoteViews,Song song){
        int skin = SPUtil.getValue(App.getContext(),SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.APP_WIDGET_SKIN,SKIN_WHITE_1F);
        mSkin = skin == SKIN_TRANSPARENT ? AppWidgetSkin.TRANSPARENT : AppWidgetSkin.WHITE_1F;
        updateBackground(remoteViews);
        updateTitle(remoteViews,song);
        updateArtist(remoteViews,song);
        updateSkin(remoteViews);
        updatePlayPause(remoteViews);
        updateLove(remoteViews,song);
        updateModel(remoteViews);
        updateNextAndPrev(remoteViews);
        updateProgress(remoteViews,song);
    }

    protected void updateSkin(RemoteViews remoteViews){
        Drawable skinDrawable = Theme.TintDrawable(R.drawable.widget_btn_skin, mSkin.getBtnColor());
        remoteViews.setImageViewBitmap(R.id.appwidget_skin,drawableToBitmap(skinDrawable));
    }

    protected void updateProgress(RemoteViews remoteViews, Song song) {
        //设置时间
        remoteViews.setTextColor(R.id.appwidget_progress,mSkin.getProgressColor());
        //进度
        remoteViews.setProgressBar(R.id.appwidget_seekbar,(int)song.getDuration(),MusicService.getProgress(),false);
    }

    protected void updateLove(RemoteViews remoteViews, Song song) {
        //是否收藏
        if(PlayListUtil.isLove(song.getId()) == PlayListUtil.EXIST){
            Drawable likeDrawable = Theme.TintDrawable(R.drawable.widget_btn_like_nor, mSkin.getBtnColor());
            remoteViews.setImageViewBitmap(R.id.appwidget_love,drawableToBitmap(likeDrawable));
        } else {
            remoteViews.setImageViewResource(R.id.appwidget_love, R.drawable.widget_btn_like_prs);
        }
    }

    protected void updateNextAndPrev(RemoteViews remoteViews) {
        //上下首歌曲
        Drawable nextDrawable = Theme.TintDrawable(R.drawable.widget_btn_next_normal,mSkin.getBtnColor());
        remoteViews.setImageViewBitmap(R.id.appwidget_next,drawableToBitmap(nextDrawable));
        Drawable prevDrawable = Theme.TintDrawable(R.drawable.widget_btn_previous_normal,mSkin.getBtnColor());
        remoteViews.setImageViewBitmap(R.id.appwidget_prev,drawableToBitmap(prevDrawable));
    }

    protected void updateModel(RemoteViews remoteViews) {
        //播放模式
        Drawable modelDrawable = Theme.TintDrawable(MusicService.getPlayModel() == Constants.PLAY_LOOP ?
                        R.drawable.widget_btn_loop_normal :  MusicService.getPlayModel() == Constants.PLAY_REPEATONE ? R.drawable.widget_btn_one_normal : R.drawable.widget_btn_shuffle_normal,
                mSkin.getBtnColor());
        remoteViews.setImageViewBitmap(R.id.appwidget_model,drawableToBitmap(modelDrawable));
    }

    protected void updatePlayPause(RemoteViews remoteViews) {
        //播放暂停按钮
        Drawable playPauseDrawable = Theme.TintDrawable(MusicService.isPlay() ? R.drawable.widget_btn_stop_normal : R.drawable.widget_btn_play_normal,mSkin.getBtnColor());
        remoteViews.setImageViewBitmap(R.id.appwidget_toggle,drawableToBitmap(playPauseDrawable));
    }

    protected void updateTitle(RemoteViews remoteViews, Song song) {
        //歌曲名
        remoteViews.setTextColor(R.id.appwidget_title, mSkin.getTitleColor());
        remoteViews.setTextViewText(R.id.appwidget_title,song.getTitle());
    }

    protected void updateArtist(RemoteViews remoteViews, Song song){
        //歌手名
        remoteViews.setTextColor(R.id.appwidget_artist,mSkin.getArtistColor());
        remoteViews.setTextViewText(R.id.appwidget_artist,song.getArtist());
    }

    protected void updateBackground(RemoteViews remoteViews) {
        remoteViews.setImageViewResource(R.id.appwidget_clickable,mSkin.getBackground());
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
