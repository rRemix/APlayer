package remix.myplayer.services;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.List;

import remix.myplayer.R;
import remix.myplayer.activities.AudioHolderActivity;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
/**
 * Created by Remix on 2016/2/16.
 */
public class NotifyService extends Service {
    private final static String TAG = "NotifyService";
    private NotifyReceiver mNotifyReceiver;
    public static NotifyService mInstance;
    private Context mContext;
    private boolean mIsForeground = true;
    @Override
    public void onCreate(){
        super.onCreate();
        mContext = getApplicationContext();
        mInstance = this;
        mNotifyReceiver = new NotifyReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        filter.addAction(Constants.NOTIFY);
        registerReceiver(mNotifyReceiver,filter);

        new Thread(){
            @Override
            public void run(){
                String packagename = getApplicationContext().getPackageName();
                //每0.1秒检测是否app切换到前台，如果是，关闭通知栏
                while(true){
                    try {
                        ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
                        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
                        if (!tasks.isEmpty()) {
                            ComponentName topActivity = tasks.get(0).topActivity;
                            if (topActivity.getPackageName().equals(packagename)) {
                                NotificationManager mNotificationManager =
                                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                mNotificationManager.cancel(0);
                                mIsForeground = false;
                            }else {
                                mIsForeground = true;
                            }
                        }
                        sleep(100);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

            }
        }.start();
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
        private RemoteViews mRemoteView;
        private boolean mIsplay = false;
        @Override
        public void onReceive(Context context, Intent intent) {
            UpdateNotify();
        }
        private void UpdateNotify() {
            mIsplay = MusicService.getIsplay();
            Log.d(TAG,"isplay=" + mIsplay);
            Log.d(TAG,"isShow=" + mIsForeground);
            mRemoteView = new RemoteViews(mContext.getPackageName(), R.layout.notify_playbar);


            if(mIsForeground || (MusicService.getCurrentMP3() != null && mIsplay)) {
                MP3Info temp = MusicService.getCurrentMP3();
                //设置歌手，歌曲名
                mRemoteView.setTextViewText(R.id.notify_song, temp.getDisplayname());
                mRemoteView.setTextViewText(R.id.notify_artist, temp.getArtist());
                //设置封面
                Bitmap bitmap = DBUtil.CheckBitmapBySongId((int) temp.getId(), true);
                if(bitmap != null)
                    mRemoteView.setImageViewBitmap(R.id.notify_image,bitmap);
                else
                    mRemoteView.setImageViewResource(R.id.notify_image,R.drawable.song_artist_empty_bg);
                //设置播放按钮
                if(!mIsplay){
                    mRemoteView.setImageViewResource(R.id.notify_play, R.drawable.notifbar_btn_play);
                }else{
                    mRemoteView.setImageViewResource(R.id.notify_play, R.drawable.notifbar_btn_stop);
                }

                Intent mButtonIntent = new Intent(Constants.CTL_ACTION);
                mButtonIntent.putExtra("FromNotify", true);
                mButtonIntent.putExtra("Control", Constants.PLAYORPAUSE);
                PendingIntent mIntent_Play = PendingIntent.getBroadcast(mContext,1,mButtonIntent,PendingIntent.FLAG_UPDATE_CURRENT);
                mRemoteView.setOnClickPendingIntent(R.id.notify_play,mIntent_Play);

                mButtonIntent.putExtra("Control", Constants.NEXT);
                PendingIntent mIntent_Next = PendingIntent.getBroadcast(mContext,2,mButtonIntent,PendingIntent.FLAG_UPDATE_CURRENT);
                mRemoteView.setOnClickPendingIntent(R.id.notify_next,mIntent_Next);

                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(mContext)
                        .setContent(mRemoteView)
                        .setWhen(System.currentTimeMillis())
                        .setPriority(Notification.PRIORITY_DEFAULT)
                        .setOngoing(mIsplay)
                        .setSmallIcon(R.drawable.notifbar_icon);
//                    .setLargeIcon(DBUtil.CheckBitmapByAlbumId((int)MusicService.getCurrentMP3().getAlbumId(),false))
//                    .setStyle(new android.support.v7.app.NotificationCompat.MediaStyle().setMediaSession(MusicService.mMediaSession.getSessionToken()));

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
}
