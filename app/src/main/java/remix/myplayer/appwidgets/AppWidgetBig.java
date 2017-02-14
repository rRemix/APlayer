package remix.myplayer.appwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
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
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.activity.MainActivity;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.MediaStoreUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/1/23 10:58
 */

public class AppWidgetBig extends BaseAppwidget {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        context.startService(new Intent(context,MusicService.class));
        mAppIds = appWidgetIds;
        mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget_medium);
        buildAction(context, mRemoteViews);
        pushUpdate(context,appWidgetIds,mRemoteViews);
        Intent intent = new Intent(Constants.WIDGET_UPDATE);
        intent.putExtra("WidgetName","BigWidget");
        intent.putExtra("WidgetIds",appWidgetIds);
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.sendBroadcast(intent);
    }


    public void updateWidget(final Context context){
        MP3Item temp = MusicService.getCurrentMP3();
        if(temp == null || !hasInstances(context))
            return;
        if(mRemoteViews == null) {
            mRemoteViews = new RemoteViews(context.getPackageName(), R.layout.app_widget_medium);
            buildAction(context,mRemoteViews);
        }
        mRemoteViews.setTextViewText(R.id.notify_song, temp.getTitle());
        //播放暂停按钮
        mRemoteViews.setImageViewResource(R.id.appwidget_toggle,MusicService.isPlay() ? R.drawable.notify_pause : R.drawable.notify_play);
        //歌曲名和歌手名
        mRemoteViews.setTextViewText(R.id.appwidget_title,temp.getTitle());
        mRemoteViews.setTextViewText(R.id.appwidget_artist,temp.getArtist());

        //设置封面
        int size = DensityUtil.dip2px(context,110);
        final ImageRequest imageRequest =
                ImageRequestBuilder.newBuilderWithSource(Uri.parse(MediaStoreUtil.getImageUrl(temp.getAlbumId(),Constants.URL_ALBUM)))
                        .setResizeOptions(new ResizeOptions(size,size))
                        .build();
        DataSource<CloseableReference<CloseableImage>> dataSource = Fresco.getImagePipeline().fetchDecodedImage(imageRequest,this);

//        dataSource.subscribe(new BaseDataSubscriber<CloseableReference<CloseableImage>>() {
//            @Override
//            protected void onNewResultImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
//                if(!dataSource.isFinished())
//                    return;
//                CloseableReference<CloseableImage> result = dataSource.getResult();
//                if(result != null ){
//                    try {
//                        CloseableImage closeableImage = result.get();
//                        if(closeableImage instanceof CloseableBitmap){
//                            Bitmap bitmap = Bitmap.createBitmap(((CloseableBitmap) closeableImage).getUnderlyingBitmap());
//                            if(bitmap != null) {
//                                mRemoteViews.setImageViewBitmap(R.id.appwidget_image, bitmap);
//                            } else {
//                                mRemoteViews.setImageViewResource(R.id.appwidget_image, R.drawable.album_empty_bg_day);
//                            }
//                            pushUpdate(context,mAppIds,mRemoteViews);
//                        }
//                    }catch (Exception e){
//                        LogUtil.e(e.toString());
//                    }
//                }
//            }
//            @Override
//            protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
//                pushUpdate(context,mAppIds,mRemoteViews);
//            }
//        }, CallerThreadExecutor.getInstance());

        dataSource.subscribe(new BaseBitmapDataSubscriber() {
            @Override
            protected void onNewResultImpl(Bitmap bitmap) {
                try {
                    Bitmap result = Bitmap.createBitmap(bitmap);
                    if(result != null) {
                        mRemoteViews.setImageViewBitmap(R.id.appwidget_image, result);
                    } else {
                        mRemoteViews.setImageViewResource(R.id.appwidget_image, R.drawable.album_empty_bg_day);
                    }
                    pushUpdate(context,mAppIds,mRemoteViews);
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            @Override
            protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                mRemoteViews.setImageViewResource(R.id.appwidget_image, R.drawable.album_empty_bg_day);
                pushUpdate(context,mAppIds,mRemoteViews);
            }
        }, CallerThreadExecutor.getInstance());
    }

    private void buildAction(Context context, RemoteViews views) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        views.setOnClickPendingIntent(R.id.appwidget_text_container, PendingIntent.getActivity(context,0,intent,PendingIntent.FLAG_UPDATE_CURRENT));
        ComponentName componentName = new ComponentName(context,MusicService.class);
        views.setOnClickPendingIntent(R.id.appwidget_toggle,buildPendingIntent(context,componentName,Constants.TOGGLE));
        views.setOnClickPendingIntent(R.id.appwidget_prev,buildPendingIntent(context,componentName, Constants.PREV));
        views.setOnClickPendingIntent(R.id.appwidget_next,buildPendingIntent(context,componentName,Constants.NEXT));
    }

}
