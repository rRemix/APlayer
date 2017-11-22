package remix.myplayer.service.notification;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.TextUtils;
import android.view.View;
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
import remix.myplayer.model.mp3.Song;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.activity.PlayerActivity;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.SPUtil;

import static remix.myplayer.service.MusicService.copy;

/**
 * Created by Remix on 2017/11/22.
 */

public class NotifyImpl extends Notify {
    private RemoteViews mRemoteView;
    private RemoteViews mRemoteBigView;

    public NotifyImpl(MusicService context) {
        super(context);
    }

    @Override
    public void update() {
        mRemoteBigView = new RemoteViews(mService.getPackageName(),  R.layout.notification_big);
        mRemoteView = new RemoteViews(mService.getPackageName(),R.layout.notification);
        boolean isPlay = MusicService.isPlay();

        if(!Global.isNotifyShowing() && !isPlay)
            return;

        buildAction(mService);
        buildNotification(mService);

        if((MusicService.getCurrentMP3() != null)) {
            boolean isSystemColor = SPUtil.getValue(mService,"Setting","IsSystemColor",true);

            Song temp = MusicService.getCurrentMP3();
            //设置歌手，歌曲名
            mRemoteBigView.setTextViewText(R.id.notify_song, temp.getTitle());
            mRemoteBigView.setTextViewText(R.id.notify_artist_album, temp.getArtist() + " - " + temp.getAlbum());

            mRemoteView.setTextViewText(R.id.notify_song,temp.getTitle());
            mRemoteView.setTextViewText(R.id.notify_artist_album,temp.getArtist() + " - " + temp.getAlbum());

            //设置了黑色背景
            if(!isSystemColor){
                mRemoteBigView.setTextColor(R.id.notify_song, ColorUtil.getColor(R.color.night_textcolor_primary));
                mRemoteView.setTextColor(R.id.notify_song, ColorUtil.getColor(R.color.night_textcolor_primary));
                //背景
                mRemoteBigView.setImageViewResource(R.id.notify_bg, R.drawable.bg_notification_black);
                mRemoteBigView.setViewVisibility(R.id.notify_bg, View.VISIBLE);
                mRemoteView.setImageViewResource(R.id.notify_bg, R.drawable.bg_notification_black);
                mRemoteView.setViewVisibility(R.id.notify_bg,View.VISIBLE);
            }
            //设置播放按钮
            if(!isPlay){
                mRemoteBigView.setImageViewResource(R.id.notify_play, R.drawable.notify_play);
                mRemoteView.setImageViewResource(R.id.notify_play, R.drawable.notify_play);
            }else{
                mRemoteBigView.setImageViewResource(R.id.notify_play, R.drawable.notify_pause);
                mRemoteView.setImageViewResource(R.id.notify_play, R.drawable.notify_pause);
            }
            //设置封面
            final int size = DensityUtil.dip2px(mService,120);
            final String uri = MediaStoreUtil.getImageUrl(temp.getAlbumId(),Constants.URL_ALBUM);
            ImageRequest imageRequest =
                    ImageRequestBuilder.newBuilderWithSource(!TextUtils.isEmpty(uri) ? Uri.parse(uri) : Uri.EMPTY)
                            .setResizeOptions(new ResizeOptions(size,size))
                            .build();
            DataSource<CloseableReference<CloseableImage>> dataSource = Fresco.getImagePipeline().fetchDecodedImage(imageRequest,this);

            dataSource.subscribe(new BaseBitmapDataSubscriber() {
                @Override
                protected void onNewResultImpl(Bitmap bitmap) {
                    try {
                        Bitmap result = copy(bitmap);
                        if(result != null) {
                            mRemoteBigView.setImageViewBitmap(R.id.notify_image, result);
                            mRemoteView.setImageViewBitmap(R.id.notify_image,result);
                        } else {
                            mRemoteBigView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day);
                            mRemoteView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day);
                        }
                    } catch (Exception e){
                        CommonUtil.uploadException("PushNotify Error",e);
                    } finally {

                        pushNotify();
                    }
                }

                @Override
                protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                    mRemoteBigView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day);
                    mRemoteView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day);
                    pushNotify();
                }
            }, CallerThreadExecutor.getInstance());
        }
    }

    private void buildNotification(Context context) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID);
        if(mNotification == null){
            mBuilder.setContent(mRemoteView)
                    .setCustomBigContentView(mRemoteBigView)
                    .setContentText("")
                    .setContentTitle("")
                    .setWhen(System.currentTimeMillis())
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setOngoing(MusicService.isPlay())
                    .setSmallIcon(R.drawable.notifbar_icon);
            mBuilder.setCustomBigContentView(mRemoteBigView);
            mBuilder.setCustomContentView(mRemoteView);
            //点击通知栏打开播放界面
            //后退回到主界面
            Intent result = new Intent(context,PlayerActivity.class);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(PlayerActivity.class);
            stackBuilder.addNextIntent(result);

            stackBuilder.editIntentAt(1).putExtra("Notify", true);
            stackBuilder.editIntentAt(0).putExtra("Notify", true);
            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );

//                Intent notifyIntent = new Intent(context,PlayerActivity.class);
//                notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                PendingIntent pendingIntent = PendingIntent.getActivity(context,0,notifyIntent,PendingIntent.FLAG_UPDATE_CURRENT);

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
