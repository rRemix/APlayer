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
import remix.myplayer.services.MusicService;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;

/**
 * Created by taeja on 16-2-4.
 */
public class NotifyReceiver extends BroadcastReceiver
{

    private RemoteViews mRemoteView;
    @Override
    public void onReceive(Context context, Intent intent) {
        if(MusicService.getCurrentMP3() == null || !MusicService.getIsplay()) {
            return;
        }

        mRemoteView = new RemoteViews(context.getPackageName(),R.layout.notify_playbar);
        //设置歌手，歌曲名
        mRemoteView.setTextViewText(R.id.notify_song, MusicService.getCurrentMP3().getDisplayname());
        mRemoteView.setTextViewText(R.id.notify_artist, MusicService.getCurrentMP3().getArtist());
        //设置封面
        Bitmap bitmap = DBUtil.CheckBitmapBySongId((int)MusicService.getCurrentMP3().getId(),true);
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
        mButtonIntent.putExtra("Control", Constants.PLAYORPAUSE);
        PendingIntent mIntent_Play = PendingIntent.getBroadcast(context,1,mButtonIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteView.setOnClickPendingIntent(R.id.notify_play,mIntent_Play);

        mButtonIntent.putExtra("Control", Constants.NEXT);
        PendingIntent mIntent_Next = PendingIntent.getBroadcast(context,2,mButtonIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        mRemoteView.setOnClickPendingIntent(R.id.notify_next,mIntent_Next);

//        ActivityManager manager = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE) ;
//        List<ActivityManager.RunningTaskInfo> runningTaskInfos = manager.getRunningTasks(1) ;
//        String activity;
//        if(runningTaskInfos != null)
//            activity = (runningTaskInfos.get(0).topActivity).toString() ;

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setContent(mRemoteView)
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setOngoing(true)
                .setSmallIcon(R.drawable.app_icon);

        Intent result = new Intent(context,AudioHolderActivity.class);
//        result.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(AudioHolderActivity.class);
        stackBuilder.addNextIntent(result);
        stackBuilder.editIntentAt(1).putExtra("Notify",true);


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
}
