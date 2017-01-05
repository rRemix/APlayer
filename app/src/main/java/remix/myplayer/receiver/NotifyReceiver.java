package remix.myplayer.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.RemoteViews;

import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableBitmap;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import remix.myplayer.R;
import remix.myplayer.fragment.BottomActionBarFragment;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.activity.PlayerActivity;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.SPUtil;

/**
 * Created by taeja on 16-2-4.
 */

/**
 * 接收更新通知栏的广播
 * 当用户播放任意一首歌曲时，显示通知栏
 */
public class NotifyReceiver extends BroadcastReceiver {
    private RemoteViews mRemoteView;
    private RemoteViews mRemoteBigView;
    private boolean mIsplay = false;
    private Notification mNotification;
    private NotificationManager mNotificationManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        UpdateNotify(context);
    }

    private void UpdateNotify(final Context context) {
        mRemoteBigView = new RemoteViews(context.getPackageName(),  R.layout.notify_playbar_big);
        mRemoteView = new RemoteViews(context.getPackageName(),R.layout.notify_playbar);
        mIsplay = MusicService.getIsplay();

        if(!Global.isNotifyShowing() && !mIsplay)
            return;

        if((MusicService.getCurrentMP3() != null)) {
            boolean isSystemColor = SPUtil.getValue(context,"Setting","IsSystemColor",true);

            MP3Item temp = MusicService.getCurrentMP3();
            //设置歌手，歌曲名
            mRemoteBigView.setTextViewText(R.id.notify_song, temp.getTitle());
            mRemoteBigView.setTextViewText(R.id.notify_artist_album, temp.getArtist() + " - " + temp.getAlbum());
            mRemoteBigView.setTextColor(R.id.notify_song, ColorUtil.getColor(isSystemColor ? R.color.day_textcolor_primary : R.color.night_textcolor_primary));

            mRemoteView.setTextViewText(R.id.notify_song,temp.getTitle());
            mRemoteView.setTextColor(R.id.notify_song, ColorUtil.getColor(isSystemColor ? R.color.day_textcolor_primary : R.color.night_textcolor_primary));
            mRemoteView.setTextViewText(R.id.notify_artist_album,temp.getArtist() + " - " + temp.getAlbum());
            //背景
            mRemoteBigView.setImageViewResource(R.id.notify_bg,isSystemColor ? R.drawable.bg_system : R.drawable.bg_black);
            mRemoteView.setImageViewResource(R.id.notify_bg,isSystemColor ? R.drawable.bg_system : R.drawable.bg_black);
            //设置播放按钮
            if(!mIsplay){
                mRemoteBigView.setImageViewResource(R.id.notify_play, R.drawable.notify_play);
                mRemoteView.setImageViewResource(R.id.notify_play, R.drawable.notify_play);
            }else{
                mRemoteBigView.setImageViewResource(R.id.notify_play, R.drawable.notify_pause);
                mRemoteView.setImageViewResource(R.id.notify_play, R.drawable.notify_pause);
            }
            //设置封面
            int size = DensityUtil.dip2px(context,120);
            ImageRequest imageRequest =
                    ImageRequestBuilder.newBuilderWithSource(Uri.parse("file://" + MediaStoreUtil.getImageUrl(temp.getAlbumId() + "",Constants.URL_ALBUM)))
                    .setResizeOptions(new ResizeOptions(size,size))
                    .setProgressiveRenderingEnabled(true)
                    .build();
            DataSource<CloseableReference<CloseableImage>> dataSource = Fresco.getImagePipeline().fetchDecodedImage(imageRequest,this);

//            dataSource.subscribe(new BaseDataSubscriber<CloseableReference<CloseableImage>>() {
//                @Override
//                protected void onNewResultImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
//                    if(!dataSource.isFinished())
//                        return;
//                    CloseableReference<CloseableImage> result = dataSource.getResult();
//                    if(result != null){
//                        try {
//                            CloseableImage closeableImage = result.get();
//                            if(closeableImage instanceof CloseableBitmap){
//                                Bitmap bitmap = Bitmap.createBitmap(((CloseableBitmap) closeableImage).getUnderlyingBitmap());
//                                if(bitmap != null) {
//                                    mRemoteBigView.setImageViewBitmap(R.id.notify_image, bitmap);
//                                    mRemoteView.setImageViewBitmap(R.id.notify_image,bitmap);
//                                } else {
//                                    mRemoteBigView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day);
//                                    mRemoteView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day);
//                                }
//                                pushNotify(context);
//                            }
//                        }catch (Exception e){
//                            LogUtil.e(e.toString());
//                        }
//                    }
//                }
//                @Override
//                protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
//                    pushNotify(context);
//                }
//            }, CallerThreadExecutor.getInstance());

            dataSource.subscribe(new BaseBitmapDataSubscriber() {
                @Override
                protected void onNewResultImpl(Bitmap bitmap) {
                    Bitmap result = Bitmap.createBitmap(bitmap);
                    if(result != null) {
                        mRemoteBigView.setImageViewBitmap(R.id.notify_image, result);
                        mRemoteView.setImageViewBitmap(R.id.notify_image,result);
                    } else {
                        mRemoteBigView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day);
                        mRemoteView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day);
                    }
                    pushNotify(context);
                }
                @Override
                protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                    pushNotify(context);
                }
            }, CallerThreadExecutor.getInstance());
        }

    }

    private void pushNotify(Context context) {
        buildAction(context);
        buildNotitication(context);
        if(mNotificationManager == null)
            mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mNotification);
        Global.setNotifyShowing(true);
    }

    private void buildNotitication(Context context) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context);
        if(mNotification == null){
            mBuilder.setContent(mRemoteView)
                    .setCustomBigContentView(mRemoteBigView)
                    .setContentText("")
                    .setContentTitle("")
                    .setWhen(System.currentTimeMillis())
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setOngoing(mIsplay)
                    .setSmallIcon(R.drawable.notifbar_icon);
            //点击通知栏打开播放界面
            //后退回到主界面
            Intent result = new Intent(context,PlayerActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(PlayerActivity.class);
            stackBuilder.addNextIntent(result);
            stackBuilder.editIntentAt(1).putExtra("Notify", true);
            stackBuilder.editIntentAt(1).putExtra("Rect", BottomActionBarFragment.getCoverRect());
            stackBuilder.editIntentAt(0).putExtra("Notify", true);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            mNotification = mBuilder.build();
        } else {
            mNotification.bigContentView = mRemoteBigView;
            mNotification.contentView = mRemoteView;
        }
    }

    private void buildAction(Context context) {
        //添加Action
        Intent actionIntent = new Intent(Constants.CTL_ACTION);
        actionIntent.putExtra("FromNotify", true);
        //播放或者暂停
        actionIntent.putExtra("Control", Constants.TOGGLE);
        PendingIntent playIntent = PendingIntent.getBroadcast(context, 1, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteBigView.setOnClickPendingIntent(R.id.notify_play, playIntent);
        mRemoteView.setOnClickPendingIntent(R.id.notify_play,playIntent);
        //下一首
        actionIntent.putExtra("Control", Constants.NEXT);
        PendingIntent nextIntent = PendingIntent.getBroadcast(context,2,actionIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteBigView.setOnClickPendingIntent(R.id.notify_next, nextIntent);
        mRemoteView.setOnClickPendingIntent(R.id.notify_next, nextIntent);
        //上一首
        actionIntent.putExtra("Control", Constants.PREV);
        PendingIntent prevIntent = PendingIntent.getBroadcast(context, 3, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteBigView.setOnClickPendingIntent(R.id.notify_prev,prevIntent);

        //关闭通知栏
        actionIntent.putExtra("Close", true);
        PendingIntent closeIntent = PendingIntent.getBroadcast(context, 4, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteBigView.setOnClickPendingIntent(R.id.notify_close, closeIntent);
        mRemoteView.setOnClickPendingIntent(R.id.notify_close,closeIntent);
    }
}