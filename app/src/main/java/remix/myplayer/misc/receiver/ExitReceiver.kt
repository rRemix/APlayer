package remix.myplayer.misc.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import remix.myplayer.misc.manager.ActivityManager
import remix.myplayer.misc.manager.ServiceManager
import remix.myplayer.service.MusicService
import timber.log.Timber

/**
 * Created by taeja on 16-2-16.
 */
/**
 * 接受程序退出的广播
 */
class ExitReceiver : BroadcastReceiver() {
  override fun onReceive(context: Context, intent: Intent) {
    Timber.v("开始关闭app")
    //停止所有service
    ServiceManager.stopAll()
    //      //关闭通知
//        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
    //停止摇一摇
//        ShakeDetector.getInstance().stopListen();
    //关闭所有activity
    ActivityManager.finishAll()
    Handler().postDelayed({
      Timber.tag(MusicService.TAG_LIFECYCLE).v("关闭App")
      System.exit(0)
    }, 1000)
    //        System.exit(0);
  }
}