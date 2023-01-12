package remix.myplayer.ui.activity.base

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.tbruyelle.rxpermissions2.RxPermissions
import remix.myplayer.BuildConfig
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.helper.LanguageHelper.setLocal
import remix.myplayer.service.MusicService
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore.navigationBarColor
import remix.myplayer.theme.ThemeStore.sColoredNavigation
import remix.myplayer.theme.ThemeStore.statusBarColor
import remix.myplayer.theme.ThemeStore.themeRes
import remix.myplayer.ui.misc.AudioTag
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.PermissionUtil
import remix.myplayer.util.StatusBarUtil
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util

/**
 * Created by Remix on 2016/3/16.
 */
@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {
  private var isDestroyed = false
  protected var isForeground = false

  @JvmField
  protected var hasPermission = false

  var audioTag: AudioTag? = null

  var toDeleteSongs: ArrayList<Song>? = null

  /**
   * 设置主题
   */
  protected open fun setUpTheme() {
    setTheme(themeRes)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    hasPermission = PermissionUtil.hasNecessaryPermission()
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
    isDestroyed = true
  }

  override fun isDestroyed(): Boolean {
    return super.isDestroyed()
  }

  @SuppressLint("CheckResult")
  override fun onResume() {
    super.onResume()
    isForeground = true
    RxPermissions(this)
        .request(*NECESSARY_PERMISSIONS)
        .subscribe { has: Boolean ->
          if (has != hasPermission) {
            val intent = Intent(MusicService.PERMISSION_CHANGE)
            intent.putExtra(BaseMusicActivity.EXTRA_PERMISSION, has)
            Util.sendLocalBroadcast(intent)
          }
        }
  }

  override fun onPause() {
    super.onPause()
    isForeground = false
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      AudioTag.REQUEST_WRITE_PERMISSION ->
        if (resultCode == Activity.RESULT_OK) {
          audioTag?.saveTag()
          audioTag = null
        } else {
          ToastUtil.show(this, R.string.grant_write_permission_tip)
        }
      MediaStoreUtil.REQUEST_DELETE_PERMISSION ->
        if (resultCode == Activity.RESULT_OK) {
          toDeleteSongs?.let {
            MediaStoreUtil.deleteSource(this, it[0])
            it.removeAt(0)
            if (it.isNotEmpty()) {
              MediaStoreUtil.deleteSource(this, it[0])
            }
          }
        } else {
          ToastUtil.show(this, R.string.grant_delete_permission_tip)
        }
    }
  }

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(setLocal(newBase))
  }

  companion object {
    val NECESSARY_PERMISSIONS =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_IMAGES)
      } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
      } else {
        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
      }
  }
}