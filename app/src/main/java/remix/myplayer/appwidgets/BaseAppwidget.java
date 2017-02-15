package remix.myplayer.appwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.RemoteViews;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import remix.myplayer.R;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.MediaStoreUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/12/28 15:50
 */

public class BaseAppwidget extends AppWidgetProvider {
//    protected int[] mAppIds;
    protected Bitmap mBitmap;

    protected PendingIntent buildPendingIntent(Context context,ComponentName componentName,int operation) {
        Intent intent = new Intent(Constants.CTL_ACTION);
        intent.putExtra("Control",operation);
        intent.putExtra("FromWidget",true);
        intent.setComponent(componentName);
        return PendingIntent.getService(context,operation,intent,PendingIntent.FLAG_UPDATE_CURRENT);
    }

    protected boolean hasInstances(Context context) {
        int[] appIds = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, getClass()));
        return appIds != null && appIds.length > 0;
    }

    protected void updateCover(final Context context, final RemoteViews remoteViews,final int[] appWidgetIds,int albumId, boolean reloadCover){
        //设置封面
        if(!reloadCover){
            if(mBitmap != null) {
                remoteViews.setImageViewBitmap(R.id.appwidget_image, mBitmap);
            } else {
                remoteViews.setImageViewResource(R.id.appwidget_image, R.drawable.album_empty_bg_day);
            }
            pushUpdate(context,appWidgetIds,remoteViews);
        } else {
            int size = DensityUtil.dip2px(context,72);
            final ImageRequest imageRequest =
                    ImageRequestBuilder.newBuilderWithSource(Uri.parse( MediaStoreUtil.getImageUrl(albumId,Constants.URL_ALBUM)))
                            .setResizeOptions(new ResizeOptions(size,size))
                            .build();
            DataSource<CloseableReference<CloseableImage>> dataSource = Fresco.getImagePipeline().fetchDecodedImage(imageRequest,this);

            dataSource.subscribe(new BaseBitmapDataSubscriber() {
                @Override
                protected void onNewResultImpl(Bitmap bitmap) {
                    try {
                        if(mBitmap != null && !mBitmap.isRecycled()){
                            mBitmap.recycle();
                            mBitmap = null;
                        }
                        mBitmap = Bitmap.createBitmap(bitmap);
                        if(mBitmap != null) {
                            remoteViews.setImageViewBitmap(R.id.appwidget_image, mBitmap);
                        } else {
                            remoteViews.setImageViewResource(R.id.appwidget_image, R.drawable.album_empty_bg_day);
                        }
                        pushUpdate(context,appWidgetIds,remoteViews);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
                @Override
                protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                    if(mBitmap != null && !mBitmap.isRecycled()){
                        mBitmap.recycle();
                    }
                    mBitmap = null;
                    remoteViews.setImageViewResource(R.id.appwidget_image, R.drawable.album_empty_bg_day);
                    pushUpdate(context,appWidgetIds,remoteViews);
                }
            }, CallerThreadExecutor.getInstance());
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
