package remix.myplayer.service.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.request.network.RemoteUriRequest;
import remix.myplayer.service.MusicService;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;

import static remix.myplayer.service.MusicService.copy;
import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * Created by Remix on 2017/11/22.
 */
@TargetApi(Build.VERSION_CODES.O)
public class NotifyImpl24 extends Notify{
    public NotifyImpl24(MusicService context) {
        super(context);
    }

    @Override
    public void updateForPlaying() {
        Song song = MusicService.getCurrentMP3();
        if(song == null)
            return;

        //设置封面
        final int size = DensityUtil.dip2px(mService,128);
        new RemoteUriRequest(getSearchRequestWithAlbumType(song),new RequestConfig.Builder(size,size).build()){
            @Override
            public void onError(String errMsg) {
                Bitmap result = BitmapFactory.decodeResource(mService.getResources(), R.drawable.album_empty_bg_night);
                updateWithBitmap(result,song);
            }

            @Override
            public void onSuccess(Bitmap bitmap) {
                Bitmap result = copy(bitmap);
                if(result == null) {
                    result = BitmapFactory.decodeResource(mService.getResources(),R.drawable.album_empty_bg_night);
                }
                updateWithBitmap(result,song);
            }

        }.load();
    }

    private void updateWithBitmap(Bitmap bitmap,Song song){
        int playPauseIcon = MusicService.isPlay() ? R.drawable.ic_pause_black_24dp : R.drawable.ic_play_arrow_black_24dp;

        Intent deleteIntent = new Intent(MusicService.ACTION_CMD);
        deleteIntent.putExtra("Close", true);
        deleteIntent.putExtra("FromImpl24",true);

        Notification notification = new NotificationCompat.Builder(mService, PLAYING_NOTIFICATION_CHANNEL_ID)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.notifbar_icon)
                .addAction(R.drawable.ic_skip_previous_black_24dp, mService.getString(R.string.previous),
                        PendingIntent.getBroadcast(mService,0, getControlIntent(Constants.PREV), PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(playPauseIcon, mService.getString(R.string.play_pause),
                        PendingIntent.getBroadcast(mService,1, getControlIntent(Constants.TOGGLE), PendingIntent.FLAG_UPDATE_CURRENT))
                .addAction(R.drawable.ic_skip_next_black_24dp, mService.getString(R.string.next),
                        PendingIntent.getBroadcast(mService,2, getControlIntent(Constants.NEXT), PendingIntent.FLAG_UPDATE_CURRENT))
                .setDeleteIntent(PendingIntent.getBroadcast(mService,3,deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentIntent(getContentIntent())
                .setContentTitle(song.getTitle())
                .setLargeIcon(bitmap)
                .setShowWhen(false)
                .setOngoing(MusicService.isPlay())
                .setContentText(song.getArtist() + " - " + song.getAlbum())
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0,1,2)
                        .setMediaSession(mService.getMediaSession().getSessionToken()))
                .build();
        if(mIsStop)
            return;
        pushNotify(notification);
    }

    private Intent getControlIntent(int control){
        return new Intent(MusicService.ACTION_CMD).putExtra("FromNotify", true)
                .putExtra("Control",control);
    }

}
