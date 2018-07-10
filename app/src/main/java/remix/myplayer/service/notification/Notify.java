package remix.myplayer.service.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.app.TaskStackBuilder;

import remix.myplayer.R;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.activity.PlayerActivity;
import remix.myplayer.util.Global;
import remix.myplayer.util.LogUtil;

/**
 * Created by Remix on 2017/11/22.
 */

public abstract class Notify {
    MusicService mService;
    private NotificationManager mNotificationManager;

    private int mNotifyMode = NOTIFY_MODE_BACKGROUND;

    private static final int NOTIFY_MODE_FOREGROUND = 1;
    private static final int NOTIFY_MODE_BACKGROUND = 2;

    static final String PLAYING_NOTIFICATION_CHANNEL_ID = "playing_notification";
    private static final int PLAYING_NOTIFICATION_ID = 1;

//    Notification mNotification;
    boolean mIsStop;

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
        NotificationChannel playingNotificationChannel = new NotificationChannel(PLAYING_NOTIFICATION_CHANNEL_ID,mService.getString(R.string.playing_notification), NotificationManager.IMPORTANCE_LOW);
        playingNotificationChannel.setShowBadge(false);
        playingNotificationChannel.enableLights(false);
        playingNotificationChannel.enableVibration(false);
        playingNotificationChannel.setDescription(mService.getString(R.string.playing_notification_description));
        mNotificationManager.createNotificationChannel(playingNotificationChannel);
    }

    public abstract void updateForPlaying();

    void pushNotify(Notification notification) {
        final int newNotifyMode;
        if (MusicService.isPlay()) {
            newNotifyMode = NOTIFY_MODE_FOREGROUND;
        } else{
            newNotifyMode = NOTIFY_MODE_BACKGROUND;
        }

        if (mNotifyMode != newNotifyMode && newNotifyMode == NOTIFY_MODE_BACKGROUND) {
//            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
                mService.stopForeground(false);
        }
        if (newNotifyMode == NOTIFY_MODE_FOREGROUND) {
            LogUtil.d("ServiceLifeCycle","启动前台服务");
            mService.startForeground(PLAYING_NOTIFICATION_ID, notification);
        } else  {
            mNotificationManager.notify(PLAYING_NOTIFICATION_ID, notification);
        }

        mNotifyMode = newNotifyMode;
        Global.setNotifyShowing(true);
    }

    /**
     * 取消通知栏
     */
    public void cancelPlayingNotify(){
        mService.stopForeground(true);
        mNotificationManager.cancel(PLAYING_NOTIFICATION_ID);
//        mIsStop = true;
//        mNotifyMode = NOTIFY_MODE_NONE;
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

    PendingIntent buildPendingIntent(Context context, int operation) {
        Intent intent = new Intent(MusicService.ACTION_CMD);
        intent.putExtra("Control",operation);
        intent.setComponent(new ComponentName(context,MusicService.class));
        intent.putExtra("FromNotify",true);

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O){
            return PendingIntent.getService(context, operation, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }else{
            if(operation != Command.TOGGLE_FLOAT_LRC){
                return PendingIntent.getForegroundService(context, operation, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }else{
                PendingIntent.getBroadcast(context,operation,intent,PendingIntent.FLAG_UPDATE_CURRENT);
            }
        }

        return PendingIntent.getService(context, operation, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
