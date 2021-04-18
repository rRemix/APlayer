package remix.myplayer.ui.activity.base

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import remix.myplayer.BuildConfig
import remix.myplayer.helper.LanguageHelper.setLocal
import remix.myplayer.misc.manager.ActivityManager
import remix.myplayer.service.MusicService
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore.navigationBarColor
import remix.myplayer.theme.ThemeStore.sColoredNavigation
import remix.myplayer.theme.ThemeStore.statusBarColor
import remix.myplayer.theme.ThemeStore.themeRes
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.StatusBarUtil
import remix.myplayer.util.Util

/**
 * Created by Remix on 2016/3/16.
 */
@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {
  @JvmField
  protected var mContext: Context? = null
  private var mIsDestroyed = false
  protected var mIsForeground = false

  @JvmField
  protected var mHasPermission = false

  /**
   * 设置主题
   */
  protected open fun setUpTheme() {
    setTheme(themeRes)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    mContext = this
    mHasPermission = Util.hasPermissions(EXTERNAL_STORAGE_PERMISSIONS)
    //严格模式
    if (BuildConfig.DEBUG) {
//      StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
//          .detectDiskReads()
//          .detectDiskWrites()
//          .detectNetwork()
//          .detectCustomSlowCalls()
//          .penaltyLog()
//          .build());
//      StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
//          .detectAll()
//          .penaltyLog()
//          .penaltyDropBox()
//          .build());
    }
    setUpTheme()
    super.onCreate(savedInstanceState)
    //    //除PlayerActivity外静止横屏
//    if (!this.getClass().getSimpleName().equals(PlayerActivity.class.getSimpleName())) {
//      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//    } else{
//      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//    }
    //将该activity添加到ActivityManager,用于退出程序时关闭
    ActivityManager.AddActivity(this)
    setNavigationBarColor()
  }

  override fun setContentView(layoutResID: Int) {
    super.setContentView(layoutResID)
    setStatusBarColor()
    setStatusBarMode()
  }

  override fun setContentView(view: View) {
    super.setContentView(view)
    setStatusBarColor()
    setStatusBarMode()
  }

  protected open fun setStatusBarMode() {
    StatusBarUtil.setStatusBarModeAuto(this)
  }

  /**
   * 设置状态栏颜色
   */
  protected open fun setStatusBarColor() {
    StatusBarUtil.setColorNoTranslucent(this, statusBarColor)
  }

  /**
   * 设置导航栏颜色
   */
  protected open fun setNavigationBarColor() {
    //导航栏变色
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && sColoredNavigation) {
      val navigationColor = navigationBarColor
      window.navigationBarColor = navigationColor
      Theme.setLightNavigationbarAuto(this, ColorUtil.isColorLight(navigationColor))
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    ActivityManager.RemoveActivity(this)
    mIsDestroyed = true
  }

  override fun isDestroyed(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) super.isDestroyed() else mIsDestroyed
  }

  @SuppressLint("CheckResult")
  override fun onResume() {
    super.onResume()
    mIsForeground = true
    RxPermissions(this)
        .request(*EXTERNAL_STORAGE_PERMISSIONS)
        .subscribe { has: Boolean ->
          if (has != mHasPermission) {
            val intent = Intent(MusicService.PERMISSION_CHANGE)
            intent.putExtra(BaseMusicActivity.EXTRA_PERMISSION, has)
            Util.sendLocalBroadcast(intent)
          }
        }
  }

  override fun onPause() {
    super.onPause()
    mIsForeground = false
  }

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(setLocal(newBase))
  }

  companion object {
    val EXTERNAL_STORAGE_PERMISSIONS = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
  }
}