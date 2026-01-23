package remix.myplayer.helper

import android.content.Context
import android.os.Process
import com.tencent.bugly.crashreport.CrashReport
import remix.myplayer.BuildConfig
import remix.myplayer.util.Util

object ThirdPartyInitializer {

  fun init(context: Context) {
    val buglyAppId = BuildConfig.BUGLY_APP_ID
    if (buglyAppId.isNotEmpty()) {
      // 获取当前包名
      val packageName = context.packageName
      // 获取当前进程名
      val processName = Util.getProcessName(Process.myPid())
      // 设置是否为上报进程
      val strategy = CrashReport.UserStrategy(context)
      strategy.appChannel = BuildConfig.FLAVOR
      strategy.isUploadProcess = processName == null || processName == packageName
      CrashReport.initCrashReport(context, buglyAppId, BuildConfig.DEBUG, strategy)
      CrashReport.setIsDevelopmentDevice(context, BuildConfig.DEBUG)
    }
  }
}