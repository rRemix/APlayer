package remix.myplayer.compose.activity.base

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jaudiotagger.tag.FieldKey
import remix.myplayer.BuildConfig
import remix.myplayer.R
import remix.myplayer.helper.LanguageHelper.setLocal
import remix.myplayer.service.MusicService
import remix.myplayer.util.PermissionUtil
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util
import timber.log.Timber
import java.util.EnumMap

/**
 * Created by Remix on 2016/3/16.
 */
@SuppressLint("Registered")
open class BaseActivity : ComponentActivity(), CoroutineScope by MainScope() {

  private var isDestroyed = false
  var isForeground = false
    private set

  @JvmField
  protected var hasPermission = false

  val deleteSongLauncher =
    registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
      Timber.v("deleteSongLauncher resultCode: ${it.resultCode} data: ${it.data}")
      if (it.resultCode == RESULT_OK) {
        ToastUtil.show(this, R.string.delete_success)
      } else {
        ToastUtil.show(this, R.string.grant_delete_permission_tip)
      }
    }

  val writeSongLauncher =
    registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
      Timber.v("writeSongLauncher resultCode: ${it.resultCode} data: ${it.data}")
      lifecycleScope.launch {
        Util.saveAudioTag(this@BaseActivity, pendingWriteRequest ?: return@launch)
      }
    }

  var pendingWriteRequest: PendingWriteRequest? = null

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
    super.onCreate(savedInstanceState)
    //    //除PlayerActivity外静止横屏
//    if (!this.getClass().getSimpleName().equals(PlayerActivity.class.getSimpleName())) {
//      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//    } else{
//      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
//    }
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
//    when (requestCode) {
//      AudioTag.REQUEST_WRITE_PERMISSION ->
//        if (resultCode == Activity.RESULT_OK) {
//          audioTag?.saveTag()
//          audioTag = null
//        } else {
//          ToastUtil.show(this, R.string.grant_write_permission_tip)
//        }
//
//      MediaStoreUtil.REQUEST_DELETE_PERMISSION ->
//        if (resultCode == RESULT_OK) {
//          toDeleteSongs?.let {
//            MediaStoreUtil.deleteSource(this, it[0])
//            it.removeAt(0)
//            if (it.isNotEmpty()) {
//              MediaStoreUtil.deleteSource(this, it[0])
//            }
//          }
//        } else {
//          ToastUtil.show(this, R.string.grant_delete_permission_tip)
//        }
//    }
  }

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(setLocal(newBase))
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

data class PendingWriteRequest(
  val path: String,
  val fieldMap: EnumMap<FieldKey, String>
)