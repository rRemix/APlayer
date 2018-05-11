package remix.myplayer.service.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.view.View;
import android.widget.RemoteViews;

import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.request.network.RemoteUriRequest;
import remix.myplayer.service.MusicService;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.SPUtil;

import static remix.myplayer.service.MusicService.copy;
import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

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
    public void updateForPlaying() {
        mRemoteBigView = new RemoteViews(mService.getPackageName(),R.layout.notification_big);
        mRemoteView = new RemoteViews(mService.getPackageName(),R.layout.notification);
        boolean isPlay = MusicService.isPlay();

        buildAction(mService);
        Notification notification = buildNotification(mService);

        if((MusicService.getCurrentMP3() != null)) {
            boolean isSystemColor = SPUtil.getValue(mService,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.NOTIFY_SYSTEM_COLOR,Build.VERSION.SDK_INT < Build.VERSION_CODES.N);

            Song song = MusicService.getCurrentMP3();
            //设置歌手，歌曲名
            mRemoteBigView.setTextViewText(R.id.notify_song, song.getTitle());
            mRemoteBigView.setTextViewText(R.id.notify_artist_album, song.getArtist() + " - " + song.getAlbum());

            mRemoteView.setTextViewText(R.id.notify_song,song.getTitle());
            mRemoteView.setTextViewText(R.id.notify_artist_album,song.getArtist() + " - " + song.getAlbum());

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
            final int size = DensityUtil.dip2px(mService,128);

            new RemoteUriRequest(getSearchRequestWithAlbumType(song),new RequestConfig.Builder(size,size).build()){
                @Override
                public void onError(String errMsg) {
                    mRemoteBigView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day);
                    mRemoteView.setImageViewResource(R.id.notify_image, R.drawable.album_empty_bg_day);
                    pushNotify(notification);
                }

                @Override
                public void onSuccess(Bitmap bitmap) {
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
                        LogUtil.d("Notify",e.toString());
                    } finally {
                        pushNotify(notification);
                    }
                }

            }.load();
        }
    }

    private Notification buildNotification(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, PLAYING_NOTIFICATION_CHANNEL_ID);
        builder.setContent(mRemoteView)
                .setCustomBigContentView(mRemoteBigView)
                .setContentText("")
                .setContentTitle("")
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(MusicService.isPlay())
                .setContentIntent(getContentIntent())
                .setSmallIcon(R.drawable.notifbar_icon);
        builder.setCustomBigContentView(mRemoteBigView);
        builder.setCustomContentView(mRemoteView);
        return builder.build();
    }

    private void buildAction(Context context) {
        //添加Action
        //切换
        PendingIntent playIntent = buildPendingIntent(context, Constants.TOGGLE);
        mRemoteBigView.setOnClickPendingIntent(R.id.notify_play,playIntent);
        mRemoteView.setOnClickPendingIntent(R.id.notify_play,playIntent);
        //下一首
        PendingIntent nextIntent = buildPendingIntent(context, Constants.NEXT);
        mRemoteBigView.setOnClickPendingIntent(R.id.notify_next, nextIntent);
        mRemoteView.setOnClickPendingIntent(R.id.notify_next, nextIntent);
        //上一首
        PendingIntent prevIntent = buildPendingIntent(context, Constants.PREV);
        mRemoteBigView.setOnClickPendingIntent(R.id.notify_prev,prevIntent);

        //关闭通知栏
        PendingIntent closeIntent = buildPendingIntent(context,Constants.CLOSE_NOTIFY);
        mRemoteBigView.setOnClickPendingIntent(R.id.notify_close, closeIntent);
        mRemoteView.setOnClickPendingIntent(R.id.notify_close,closeIntent);
    }

}
