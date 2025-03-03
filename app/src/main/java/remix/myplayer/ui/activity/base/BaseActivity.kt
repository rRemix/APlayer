package remix.myplayer.ui.activity.base

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
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
import timber.log.Timber

/**
 * Created by Remix on 2016/3/16.
 */
@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity(), CoroutineScope by MainScope() {
  private var isDestroyed = false
  protected var isForeground = false

  @JvmField
  protected var hasPermission = false

  var audioTag: AudioTag? = null

  var toDeleteSongs: ArrayList<Song>? = null

  private var dialog: Dialog? = null

  private val loadingDialog by lazy {
    Theme.getBaseDialog(this)
      .title(R.string.loading)
      .content(R.string.please_wait)
      .canceledOnTouchOutside(false)
      .progress(true, 0)
      .progressIndeterminateStyle(false).build()
  }

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

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !XXPermissions.isGranted(
        this,
        Manifest.permission.POST_NOTIFICATIONS
      )
    ) {
      XXPermissions.with(this)
        .permission(Permission.POST_NOTIFICATIONS)
        .request(object : OnPermissionCallback {
          override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
            Timber.v("request notification permission onGranted")
          }

          override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
            Timber.v("request notification permission onDenied: $doNotAskAgain")
          }
        })
    }

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
      Theme.setLightNavigationBarAuto(this, ColorUtil.isColorLight(navigationColor))
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    isDestroyed = true
    dialog?.dismiss()
  }

  override fun isDestroyed(): Boolean {
    return super.isDestroyed()
  }

  @SuppressLint("CheckResult")
  override fun onResume() {
    super.onResume()
    isForeground = true
    if (!hasPermission) {
      XXPermissions.with(this)
        .permission(*NECESSARY_PERMISSIONS)
        .request { _, allGranted ->
          Timber.v("request necessary permission: $allGranted")
          if (allGranted != hasPermission) {
            val intent = Intent(MusicService.PERMISSION_CHANGE)
            intent.putExtra(BaseMusicActivity.EXTRA_PERMISSION, allGranted)
            Util.sendLocalBroadcast(intent)
          }
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

  protected fun showLoading(): Dialog {
    if (loadingDialog.isShowing) {
      return loadingDialog
    }
    loadingDialog.show()
    return loadingDialog
  }

  protected fun dismissLoading() {
    if (loadingDialog.isShowing) {
      loadingDialog.dismiss()
    }
  }

  protected fun showDialog(newDialog: Dialog) {
    dialog?.dismiss()
    if (!isFinishing && !isDestroyed && hasWindowFocus()) {
      dialog = newDialog
      newDialog.show()
    }
  }

  companion object {
    val NECESSARY_PERMISSIONS =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Permission.READ_MEDIA_AUDIO, Permission.READ_MEDIA_IMAGES)
      } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
        arrayOf(
          Permission.READ_EXTERNAL_STORAGE,
          Permission.WRITE_EXTERNAL_STORAGE
        )
      } else {
        arrayOf(Permission.READ_EXTERNAL_STORAGE)
      }
  }
}