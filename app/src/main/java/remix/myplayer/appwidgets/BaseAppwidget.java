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
import remix.myplayer.appwidgets.medium.AppWidgetMedium;
import remix.myplayer.appwidgets.small.AppWidgetSmall;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.request.RemoteUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.activity.MainActivity;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.PlayListUtil;

import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/12/28 15:50
 */

public abstract class BaseAppwidget extends AppWidgetProvider {
    public static final int SKIN_WHITE_1F = 1;//白色不带透明
    public static final int SKIN_TRANSPARENT = 2;//透明
    private static final String TAG = "桌面部件";

    protected AppWidgetSkin mSkin;
    protected Bitmap mBitmap;
    private static int IMAGE_SIZE_BIG = DensityUtil.dip2px(App.getContext(),270);
    private static int IMAGE_SIZE_MEDIUM = DensityUtil.dip2px(App.getContext(),72);

    protected PendingIntent buildPendingIntent(Context context,ComponentName componentName,int operation) {
        Intent intent = new Intent(MusicService.ACTION_APPWIDGET_OPERATE);
        intent.putExtra("Control",operation);
        intent.setComponent(componentName);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return PendingIntent.getForegroundService(context, operation, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            return PendingIntent.getService(context, operation, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    protected PendingIntent buildSkinPendingIntent(Context context, String widgetName){
        Intent intent = new Intent(MusicService.ACTION_WIDGET_UPDATE);
        intent.putExtra("WidgetName",widgetName);
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        return PendingIntent.getBroadcast(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
    }

    protected PendingIntent buildTimerPendingIntent(Context context){
        Intent intent = new Intent(MusicService.ACTION_CMD);
        intent.putExtra("Control",Command.TOGGLE_TIMER);
        return PendingIntent.getBroadcast(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);
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
                LogUtil.d(TAG,"复用Bitmap: " + mBitmap);
                remoteViews.setImageViewBitmap(R.id.appwidget_image, mBitmap);
            } else {
                LogUtil.d(TAG,"Bitmap复用失败: " + mBitmap);
                remoteViews.setImageViewResource(R.id.appwidget_image, R.drawable.album_empty_bg_night);
            }
            pushUpdate(context,appWidgetIds,remoteViews);
        } else {
            final int size = this.getClass().getSimpleName().contains("Big") ? IMAGE_SIZE_BIG : IMAGE_SIZE_MEDIUM ;
            new RemoteUriRequest(getSearchRequestWithAlbumType(song),new RequestConfig.Builder(size, size).build()){
                @Override
                public void onError(String errMsg) {
                    LogUtil.d(TAG,"onError: " + errMsg + " --- 清空bitmap: " + mBitmap);
//                    Recycler.recycleBitmap(mBitmap);
                    mBitmap = null;
                    remoteViews.setImageViewResource(R.id.appwidget_image, R.drawable.album_empty_bg_day);
                    pushUpdate(context,appWidgetIds,remoteViews);
                }

                @Override
                public void onSuccess(Bitmap result) {
                    try {
                        if(result != mBitmap && mBitmap != null){
                            LogUtil.d(TAG,"onSuccess --- 回收Bitmap: " + mBitmap);
//                            Recycler.recycleBitmap(mBitmap);
                            mBitmap = null;
                        }
//                        mBitmap = MusicService.copy(result);
                        mBitmap = result;
                        LogUtil.d(TAG,"onSuccess --- 获取Bitmap: " + mBitmap);
                        if(mBitmap != null) {
                            remoteViews.setImageViewBitmap(R.id.appwidget_image, mBitmap);
                        } else {
                            remoteViews.setImageViewResource(R.id.appwidget_image, R.drawable.album_empty_bg_day);
                        }

                    } catch (Exception e){
                        LogUtil.d(TAG,"onSuccess --- 发生异常: " + e);
                    } finally {
                        pushUpdate(context,appWidgetIds,remoteViews);
                    }
                }
            }.load();
        }
    }

    protected void buildAction(Context context, RemoteViews views) {
        ComponentName componentName = new ComponentName(context,MusicService.class);
        views.setOnClickPendingIntent(R.id.appwidget_toggle,buildPendingIntent(context,componentName, Command.TOGGLE));
        views.setOnClickPendingIntent(R.id.appwidget_prev,buildPendingIntent(context,componentName, Command.PREV));
        views.setOnClickPendingIntent(R.id.appwidget_next,buildPendingIntent(context,componentName, Command.NEXT));
        views.setOnClickPendingIntent(R.id.appwidget_model,buildPendingIntent(context,componentName, Command.CHANGE_MODEL));
        views.setOnClickPendingIntent(R.id.appwidget_love,buildPendingIntent(context,componentName, Command.LOVE));

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            final String widgetName = this instanceof AppWidgetSmall ? "SmallWidget" : this instanceof AppWidgetMedium ? "MediumWidget" : "BigWidget";
            views.setOnClickPendingIntent(R.id.appwidget_skin, buildSkinPendingIntent(context,widgetName));
            views.setOnClickPendingIntent(R.id.appwidget_timer,buildTimerPendingIntent(context));
        }else{
            views.setOnClickPendingIntent(R.id.appwidget_skin,buildPendingIntent(context,componentName,Constants.UPDATE_APPWIDGET));
            views.setOnClickPendingIntent(R.id.appwidget_timer,buildPendingIntent(context,componentName, Command.TOGGLE_TIMER));
        }

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
//        int skin = SPUtil.getValue(App.getContext(),SPUtil.SETTING_KEY.NAME,SPUtil.SETTING_KEY.APP_WIDGET_SKIN,SKIN_WHITE_1F);
//        mSkin = skin == SKIN_TRANSPARENT ? AppWidgetSkin.TRANSPARENT : AppWidgetSkin.WHITE_1F;
//        updateBackground(remoteViews);
        updateTitle(remoteViews,song);
        updateArtist(remoteViews,song);
//        updateSkin(remoteViews);
        updatePlayPause(remoteViews);
        updateLove(remoteViews,song);
        updateModel(remoteViews);
        updateNextAndPrev(remoteViews);
        updateProgress(remoteViews,song);
        updateTimer(remoteViews);
    }

    private void updateTimer(RemoteViews remoteViews) {
        remoteViews.setImageViewResource(R.id.appwidget_timer,mSkin.getTimerRes());
    }

//    protected void updateSkin(RemoteViews remoteViews){
//        Drawable skinDrawable = Theme.TintDrawable(R.drawable.widget_btn_skin, mSkin.getBtnColor());
//        remoteViews.setImageViewBitmap(R.id.appwidget_skin,drawableToBitmap(skinDrawable));
//    }

    protected void updateProgress(RemoteViews remoteViews, Song song) {
        //设置时间
        remoteViews.setTextColor(R.id.appwidget_progress,mSkin.getProgressColor());
        //进度
        remoteViews.setProgressBar(R.id.appwidget_seekbar,(int)song.getDuration(),MusicService.getProgress(),false);
    }

    protected void updateLove(RemoteViews remoteViews, Song song) {
        //是否收藏
        if(PlayListUtil.isLove(song.getId()) != PlayListUtil.EXIST){
            remoteViews.setImageViewResource(R.id.appwidget_love,mSkin.getLoveRes());
        } else {
            remoteViews.setImageViewResource(R.id.appwidget_love, mSkin.getLovedRes());
        }
    }

    protected void updateNextAndPrev(RemoteViews remoteViews) {
        //上下首歌曲
        remoteViews.setImageViewResource(R.id.appwidget_next,mSkin.getNextRes());
        remoteViews.setImageViewResource(R.id.appwidget_prev,mSkin.getPrevRes());
    }

    protected void updateModel(RemoteViews remoteViews) {
        //播放模式
        remoteViews.setImageViewResource(R.id.appwidget_model, mSkin.getModeRes());
    }

    protected void updatePlayPause(RemoteViews remoteViews) {
        //播放暂停按钮
        remoteViews.setImageViewResource(R.id.appwidget_toggle,mSkin.getPlayPauseRes());
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

    public abstract void updateWidget(final Context context,final int[] appWidgetIds, boolean reloadCover);
}
