package remix.myplayer.service.notification;

import android.annotation.TargetApi;
import android.app.Notification;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.request.RemoteUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
import remix.myplayer.util.DensityUtil;

import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * Created by Remix on 2017/11/22.
 */
@TargetApi(Build.VERSION_CODES.O)
public class NotifyImpl24 extends Notify {
    public NotifyImpl24(MusicService context) {
        super(context);
    }

    @Override
    public void updateForPlaying() {
        Song song = mService.getCurrentSong();

        //设置封面
        final int size = DensityUtil.dip2px(mService, 128);
        new RemoteUriRequest(getSearchRequestWithAlbumType(song), new RequestConfig.Builder(size, size).build()) {
            @Override
            public void onError(String errMsg) {
                Bitmap result = BitmapFactory.decodeResource(mService.getResources(), R.drawable.album_empty_bg_night);
                updateWithBitmap(result, song);
            }

            @Override
            public void onSuccess(Bitmap result) {
//                Bitmap result = copy(bitmap);
                if (result == null) {
                    result = BitmapFactory.decodeResource(mService.getResources(), R.drawable.album_empty_bg_night);
                }
                updateWithBitmap(result, song);
            }

        }.load();
    }

    private void updateWithBitmap(Bitmap bitmap, Song song) {
        int playPauseIcon = mService.isPlaying() ? R.drawable.ic_pause_black_24dp : R.drawable.ic_play_arrow_black_24dp;

        Intent deleteIntent = new Intent(MusicService.ACTION_CMD);
        deleteIntent.putExtra("Control", Command.CLOSE_NOTIFY);
        deleteIntent.putExtra("FromImpl24", true);

        Notification notification = new NotificationCompat.Builder(mService, PLAYING_NOTIFICATION_CHANNEL_ID)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.notifbar_icon)
                .addAction(R.drawable.ic_skip_previous_black_24dp, mService.getString(R.string.previous),
                        buildPendingIntent(mService, Command.PREV))
                .addAction(playPauseIcon, mService.getString(R.string.play_pause),
                        buildPendingIntent(mService, Command.TOGGLE))
                .addAction(R.drawable.ic_skip_next_black_24dp, mService.getString(R.string.next),
                        buildPendingIntent(mService, Command.NEXT))
                .addAction(R.drawable.ic_desktop_lyric_black_24dp, mService.getString(R.string.float_lrc),
                        buildPendingIntent(mService, Command.TOGGLE_FLOAT_LRC))
//                .setDeleteIntent(PendingIntent.getBroadcast(mService,3,deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentIntent(getContentIntent())
                .setContentTitle(song.getTitle())
                .setLargeIcon(bitmap)
                .setShowWhen(false)
                .setOngoing(mService.isPlaying())
                .setContentText(song.getArtist() + " - " + song.getAlbum())
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2)
                        .setMediaSession(mService.getMediaSession().getSessionToken()))
                .build();
        if (mIsStop)
            return;
        pushNotify(notification);
    }

}
