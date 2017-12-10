package remix.myplayer.appwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.widget.RemoteViews;

import remix.myplayer.R;
import remix.myplayer.model.mp3.Song;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.request.network.RemoteUriRequest;
import remix.myplayer.service.MusicService;
import remix.myplayer.util.DensityUtil;

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
                public void onSuccess(Bitmap bitmap) {
                    try {
                        if(mBitmap != null && !mBitmap.isRecycled()){
                            mBitmap.recycle();
                            mBitmap = null;
                        }
                        mBitmap = MusicService.copy(bitmap);
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
}
