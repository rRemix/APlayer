package remix.myplayer.service.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.TaskStackBuilder;

import remix.myplayer.R;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.activity.PlayerActivity;
import remix.myplayer.util.Global;

/**
 * Created by Remix on 2017/11/22.
 */

public abstract class Notify {
    protected MusicService mService;
    private NotificationManager mNotificationManager;

    private static final int IDLE_DELAY = 5 * 60 * 1000;
    private long mLastPlayedTime;
    private int mNotifyMode = NOTIFY_MODE_NONE;
    protected long mNotificationPostTime = 0;
    private static final int NOTIFY_MODE_NONE = 0;
    private static final int NOTIFY_MODE_FOREGROUND = 1;
    private static final int NOTIFY_MODE_BACKGROUND = 2;

    static final String PLAYING_NOTIFICATION_CHANNEL_ID = "playing_notification";
    static final int PLAYING_NOTIFICATION_ID = 1;

    protected Notification mNotification;

    Notify(MusicService context){
        mService = context;
        init();
    }

    private void init() {
        mNotificationManager = (NotificationManager) mService.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            createNotificationChannel();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel(){
        NotificationChannel notificationChannel = new NotificationChannel(PLAYING_NOTIFICATION_CHANNEL_ID,mService.getString(R.string.playing_notification), NotificationManager.IMPORTANCE_LOW);
        notificationChannel.setShowBadge(false);
        notificationChannel.enableLights(false);
        notificationChannel.enableVibration(false);
        notificationChannel.setDescription(mService.getString(R.string.playing_notification_description));
        mNotificationManager.createNotificationChannel(notificationChannel);
    }

    public abstract void update();

    void pushNotify() {
        final int newNotifyMode;
        if (MusicService.isPlay()) {
            newNotifyMode = NOTIFY_MODE_FOREGROUND;
        } else if (recentlyPlayed()) {
            newNotifyMode = NOTIFY_MODE_BACKGROUND;
        } else {
            newNotifyMode = NOTIFY_MODE_NONE;
        }

        mNotificationManager.notify(PLAYING_NOTIFICATION_ID, mNotification);
        if (mNotifyMode != newNotifyMode) {
            if (mNotifyMode == NOTIFY_MODE_FOREGROUND) {
                mService.stopForeground(newNotifyMode == NOTIFY_MODE_NONE);
            } else if (newNotifyMode == NOTIFY_MODE_NONE) {
                mNotificationManager.cancel(PLAYING_NOTIFICATION_ID);
            }
        }
        if (newNotifyMode == NOTIFY_MODE_FOREGROUND) {
            mService.startForeground(PLAYING_NOTIFICATION_ID, mNotification);
        } else if (newNotifyMode == NOTIFY_MODE_BACKGROUND) {
            mNotificationManager.notify(PLAYING_NOTIFICATION_ID, mNotification);
        }

        //记录最后一次播放的时间
        mLastPlayedTime = System.currentTimeMillis();
        mNotifyMode = newNotifyMode;
        Global.setNotifyShowing(true);
    }

    /**
     * 取消通知栏
     */
    public void cancel(){
        mService.stopForeground(true);
        mNotificationManager.cancel(PLAYING_NOTIFICATION_ID);
        mNotifyMode = NOTIFY_MODE_NONE;
    }

    /**
     * @return 最近是否在播放
     */
    private boolean recentlyPlayed() {
        return MusicService.isPlay() || System.currentTimeMillis() - mLastPlayedTime < IDLE_DELAY;
    }

    PendingIntent getContentIntent(){
        Intent result = new Intent(mService,PlayerActivity.class);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(mService);
        stackBuilder.addParentStack(PlayerActivity.class);
        stackBuilder.addNextIntent(result);

        stackBuilder.editIntentAt(1).putExtra("Notify", true);
        stackBuilder.editIntentAt(0).putExtra("Notify", true);
        return stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
    }
}
