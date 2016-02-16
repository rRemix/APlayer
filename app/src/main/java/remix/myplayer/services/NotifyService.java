package remix.myplayer.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.service.notification.StatusBarNotification;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.RemoteViews;

import remix.myplayer.R;
import remix.myplayer.activities.AudioHolderActivity;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2016/2/16.
 */
public class NotifyService extends Service {
    private NotifyReceiver mNotifyReceiver;
    public static NotifyService mInstance;

//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        return super.onStartCommand(intent, flags, startId);
//    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        mNotifyReceiver = new NotifyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(Utility.NOTIFY);
        registerReceiver(mNotifyReceiver,filter);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mNotifyReceiver);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class NotifyReceiver extends BroadcastReceiver {
        private Handler mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }
        };
        private RemoteViews mRemoteView;
        @Override
        public void onReceive(Context context, Intent intent) {
            if(MusicService.getCurrentMP3() == null || !MusicService.getIsplay()) {
                return;
            }
            mRemoteView = new RemoteViews(context.getPackageName(), R.layout.notify_playbar);
            UpdateUI();

            Intent mButtonIntent = new Intent(Utility.CTL_ACTION);
            mButtonIntent.putExtra("Control", Utility.PLAY);
            PendingIntent mIntent_Play = PendingIntent.getBroadcast(context,1,mButtonIntent,PendingIntent.FLAG_UPDATE_CURRENT);
            mRemoteView.setOnClickPendingIntent(R.id.notify_play,mIntent_Play);

            mButtonIntent.putExtra("Control",Utility.NEXT);
            PendingIntent mIntent_Next = PendingIntent.getBroadcast(context,2,mButtonIntent,PendingIntent.FLAG_UPDATE_CURRENT);
            mRemoteView.setOnClickPendingIntent(R.id.notify_next,mIntent_Next);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                    .setContent(mRemoteView)
                    .setWhen(System.currentTimeMillis())
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.app_icon);

            Intent result = new Intent(context,AudioHolderActivity.class);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(AudioHolderActivity.class);
            stackBuilder.addNextIntent(result);
            stackBuilder.editIntentAt(1).putExtra("Notify", true);
            stackBuilder.editIntentAt(0).putExtra("Notify",true);

            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );

            mBuilder.setContentIntent(resultPendingIntent);

            NotificationManager mNotificationManager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(0, mBuilder.build());

        }

        private void UpdateUI() {
            //设置歌手，歌曲名
            mRemoteView.setTextViewText(R.id.notify_song, MusicService.getCurrentMP3().getDisplayname());
            mRemoteView.setTextViewText(R.id.notify_artist, MusicService.getCurrentMP3().getArtist());
            //设置封面
            Bitmap bitmap = Utility.CheckBitmapBySongId((int) MusicService.getCurrentMP3().getId(), true);
            if(bitmap != null)
                mRemoteView.setImageViewBitmap(R.id.notify_image,bitmap);
            else
                mRemoteView.setImageViewResource(R.id.notify_image,R.drawable.default_recommend);
            //设置播放按钮
            if(!MusicService.getIsplay()){
                mRemoteView.setImageViewResource(R.id.notify_play, R.drawable.bt_lockscreen_play_nor);
            }else{
                mRemoteView.setImageViewResource(R.id.notify_play, R.drawable.bt_lockscreen_pause_nor);
            }
        }
    }

}
