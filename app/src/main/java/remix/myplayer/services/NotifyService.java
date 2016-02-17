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
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.widget.RemoteViews;

import remix.myplayer.R;
import remix.myplayer.activities.AudioHolderActivity;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.infos.MP3Info;

/**
 * Created by Remix on 2016/2/16.
 */
public class NotifyService extends Service {
    private NotifyReceiver mNotifyReceiver;
    public static NotifyService mInstance;
    private Context mContext;
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        return super.onStartCommand(intent, flags, startId);
//    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        mInstance = this;
        mNotifyReceiver = new NotifyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(Constants.NOTIFY);
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
    public void UpdateNotify(){
        mNotifyReceiver.UpdateNotify();
    }


    class NotifyReceiver extends BroadcastReceiver {
        private Handler mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                UpdateNotify();
            }
        };
        private RemoteViews mRemoteView;
        @Override
        public void onReceive(Context context, Intent intent) {
            UpdateNotify();
        }

        private void UpdateNotify() {
            mRemoteView = new RemoteViews(mContext.getPackageName(), R.layout.notify_playbar);
            MP3Info temp = MusicService.getCurrentMP3();
            if(temp == null)
                return;
            //设置歌手，歌曲名
            mRemoteView.setTextViewText(R.id.notify_song, temp.getDisplayname());
            mRemoteView.setTextViewText(R.id.notify_artist, temp.getArtist());
            //设置封面
            Bitmap bitmap = DBUtil.CheckBitmapBySongId((int) temp.getId(), true);
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

            Intent mButtonIntent = new Intent(Constants.CTL_ACTION);
            mButtonIntent.putExtra("FromNotify", true);
            mButtonIntent.putExtra("Control", Constants.PLAY);
            PendingIntent mIntent_Play = PendingIntent.getBroadcast(mContext,1,mButtonIntent,PendingIntent.FLAG_UPDATE_CURRENT);
            mRemoteView.setOnClickPendingIntent(R.id.notify_play,mIntent_Play);

            mButtonIntent.putExtra("Control", Constants.NEXT);
            PendingIntent mIntent_Next = PendingIntent.getBroadcast(mContext,2,mButtonIntent,PendingIntent.FLAG_UPDATE_CURRENT);
            mRemoteView.setOnClickPendingIntent(R.id.notify_next,mIntent_Next);

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext)
                    .setContent(mRemoteView)
                    .setWhen(System.currentTimeMillis())
                    .setPriority(Notification.PRIORITY_DEFAULT)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.app_icon);

            Intent result = new Intent(mContext,AudioHolderActivity.class);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(mContext);
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
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(0, mBuilder.build());
        }
    }

}
