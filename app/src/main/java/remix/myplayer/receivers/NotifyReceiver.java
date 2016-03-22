package remix.myplayer.receivers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.RemoteViews;

import remix.myplayer.R;
import remix.myplayer.activities.AudioHolderActivity;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.services.MusicService;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;

/**
 * Created by taeja on 16-2-4.
 */
public class NotifyReceiver extends BroadcastReceiver {
    private RemoteViews mRemoteView;
    private boolean mIsplay = false;
    private NotificationManager mNotificationManager;
    @Override
    public void onReceive(Context context, Intent intent) {

        UpdateNotify(context,intent.getBooleanExtra("FromMainActivity",false));
    }
    private void UpdateNotify(Context context,boolean frommainactivity) {
        mIsplay = MusicService.getIsplay();
        boolean isBig = context.getResources().getDisplayMetrics().widthPixels >= 1000;

        mRemoteView = new RemoteViews(context.getPackageName(), isBig ? R.layout.notify_playbar_big : R.layout.notify_playbar);

        if(frommainactivity && !MusicService.getIsplay())
            return;
        
        if((MusicService.getCurrentMP3() != null)) {
            MP3Info temp = MusicService.getCurrentMP3();
            //设置歌手，歌曲名
            mRemoteView.setTextViewText(R.id.notify_song, temp.getDisplayname());
            mRemoteView.setTextViewText(R.id.notify_artist_album, temp.getArtist() + "-" + temp.getAlbum());

            //设置封面
            Bitmap bitmap = DBUtil.CheckBitmapBySongId((int) temp.getId(), true);
            if(bitmap != null)
                mRemoteView.setImageViewBitmap(R.id.notify_image,bitmap);
            else
                mRemoteView.setImageViewResource(R.id.notify_image,R.drawable.default_recommend);
            //设置播放按钮
            if(!mIsplay){
                mRemoteView.setImageViewResource(R.id.notify_play, R.drawable.notifbar_btn_play);
            }else{
                mRemoteView.setImageViewResource(R.id.notify_play, R.drawable.notifbar_btn_stop);
            }


            Intent mButtonIntent = new Intent(Constants.CTL_ACTION);
            mButtonIntent.putExtra("FromNotify", true);
            //播放或者暂停
            mButtonIntent.putExtra("Control", Constants.PLAYORPAUSE);
            PendingIntent mIntent_Play = PendingIntent.getBroadcast(context, 1, mButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mRemoteView.setOnClickPendingIntent(R.id.notify_play, mIntent_Play);
            //下一首
            mButtonIntent.putExtra("Control", Constants.NEXT);
            PendingIntent mIntent_Next = PendingIntent.getBroadcast(context,2,mButtonIntent,PendingIntent.FLAG_UPDATE_CURRENT);
            mRemoteView.setOnClickPendingIntent(R.id.notify_next, mIntent_Next);
            //上一首
            if(isBig){
                mButtonIntent.putExtra("Control", Constants.PREV);
                PendingIntent mIntent_Prev = PendingIntent.getBroadcast(context, 2, mButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                mRemoteView.setOnClickPendingIntent(R.id.notify_prev,mIntent_Prev);
            }

            //关闭通知栏
            mButtonIntent.putExtra("Close", true);
            PendingIntent mIntent_Close = PendingIntent.getBroadcast(context, 4, mButtonIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mRemoteView.setOnClickPendingIntent(R.id.notify_close, mIntent_Close);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
//                    .setLargeIcon(DBUtil.CheckBitmapByAlbumId((int)temp.getAlbumId(),false))
                    .setContent(mRemoteView)
                    .setContentText("")
                    .setContentTitle("")
                    .setWhen(System.currentTimeMillis())
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setOngoing(true)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setSmallIcon(R.drawable.notifbar_icon);


            Intent result = new Intent(context,AudioHolderActivity.class);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(AudioHolderActivity.class);
            stackBuilder.addNextIntent(result);
            stackBuilder.editIntentAt(1).putExtra("Notify", true);
            stackBuilder.editIntentAt(0).putExtra("Notify", true);

            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );
            mBuilder.setContentIntent(resultPendingIntent);
            Notification mNotify = mBuilder.build();
            if(isBig)
                mNotify.bigContentView = mRemoteView;
            else
                mNotify.contentView = mRemoteView;
//            mNotify.contentIntent = resultPendingIntent;
            mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(0, mNotify);
        }

    }
}