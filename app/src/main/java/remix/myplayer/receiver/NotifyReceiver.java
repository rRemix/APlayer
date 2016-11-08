package remix.myplayer.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.RemoteViews;

import remix.myplayer.R;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.activity.AudioHolderActivity;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
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
    private boolean mIsplay = false;
    private NotificationManager mNotificationManager;
    @Override
    public void onReceive(Context context, Intent intent) {
        UpdateNotify(context);
    }

    private void UpdateNotify(Context context) {
        boolean isBig = (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_NORMAL;

        mRemoteView = new RemoteViews(context.getPackageName(), isBig ? R.layout.notify_playbar_big : R.layout.notify_playbar);

        mIsplay = MusicService.getIsplay();

        if(!Global.isNotifyShowing() && !mIsplay)
            return;
        
        if((MusicService.getCurrentMP3() != null)) {
            boolean isSystemColor = SPUtil.getValue(context,"Setting","IsSystemColor",true);

            MP3Item temp = MusicService.getCurrentMP3();
            //设置歌手，歌曲名
            mRemoteView.setTextViewText(R.id.notify_song, temp.getTitle());
            mRemoteView.setTextColor( R.id.notify_song,isSystemColor ?
                    context.getResources().getColor(R.color.white_f2f2f2)
                    : context.getResources().getColor(R.color.white));

            mRemoteView.setTextViewText(R.id.notify_artist_album, temp.getArtist() + " - " + temp.getAlbum());

            //背景
            mRemoteView.setImageViewResource(R.id.notify_bg,isSystemColor ? R.drawable.bg_system : R.drawable.bg_black);

            //设置封面
            Bitmap bitmap = MediaStoreUtil.getAlbumBitmap(temp.getAlbumId(), true);
            if(bitmap != null)
                mRemoteView.setImageViewBitmap(R.id.notify_image,bitmap);
            else
                mRemoteView.setImageViewResource(R.id.notify_image,R.drawable.album_empty_bg_day);
            //设置播放按钮
            if(!mIsplay){
                mRemoteView.setImageViewResource(R.id.notify_play, R.drawable.notify_play);
            }else{
                mRemoteView.setImageViewResource(R.id.notify_play, R.drawable.notify_pause);
            }

            //添加Action
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
//                    .setLargeIcon(MediaStoreUtil.CheckBitmapByAlbumId((int)temp.getAlbumId(),false))
                    .setContent(mRemoteView)
                    .setContentText("")
                    .setContentTitle("")
                    .setWhen(System.currentTimeMillis())
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.notifbar_icon);

            //点击通知栏打开播放界面
            //后退回到主界面
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
            //根据分辨率设置布局
            if(isBig)
                mNotify.bigContentView = mRemoteView;
            else
                mNotify.contentView = mRemoteView;

            mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(0, mNotify);
            Global.setNotifyShowing(true);
        }

    }
}