package remix.myplayer.misc.receiver

import remix.myplayer.misc.manager.APlayerActivityManager.Companion.finishAll
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import timber.log.Timber
import remix.myplayer.misc.manager.ServiceManager
import remix.myplayer.service.MusicService
import kotlin.system.exitProcess

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
    ServiceManager.StopAll()
    //      //关闭通知
//        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
    //停止摇一摇
//        ShakeDetector.getInstance().stopListen();
    //关闭所有activity
    finishAll()
    Handler(Looper.getMainLooper()).postDelayed({
      Timber.tag(MusicService.TAG_LIFECYCLE).v("关闭App")
      exitProcess(0)
    }, 1000)
    //        System.exit(0);
  }
}