package remix.myplayer.compose.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.compose.activity.base.BaseMusicActivity
import remix.myplayer.compose.nav.AppNav
import remix.myplayer.compose.nav.LocalNavController
import remix.myplayer.compose.ui.dialog.dismissLoading
import remix.myplayer.compose.ui.dialog.showLoading
import remix.myplayer.compose.ui.theme.APlayerTheme
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.theme.LocalThemeController
import remix.myplayer.compose.ui.theme.ThemeController
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.compose.viewmodel.MainViewModel
import remix.myplayer.compose.viewmodel.MusicViewModel
import remix.myplayer.compose.viewmodel.ProvideViewModels
import remix.myplayer.misc.update.DownloadService
import remix.myplayer.misc.update.DownloadService.Companion.ACTION_DISMISS_DIALOG
import remix.myplayer.misc.update.DownloadService.Companion.ACTION_DOWNLOAD_COMPLETE
import remix.myplayer.misc.update.DownloadService.Companion.ACTION_SHOW_DIALOG
import remix.myplayer.theme.Theme
import remix.myplayer.util.Util.registerLocalReceiver
import remix.myplayer.util.Util.unregisterLocalReceiver
import java.io.File
import java.lang.ref.WeakReference
import javax.inject.Inject

@AndroidEntryPoint
class ComposeActivity : BaseMusicActivity() {

  private val libraryViewModel: LibraryViewModel by viewModels()
  private val musicViewModel: MusicViewModel by viewModels()
  private val mainViewModel: MainViewModel by viewModels()

  @Inject
  lateinit var themeController: ThemeController

  private val receiver by lazy {
    MainReceiver(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    addMusicServiceEventListener(libraryViewModel)
    addMusicServiceEventListener(musicViewModel)

    enableEdgeToEdge(
      navigationBarStyle = SystemBarStyle.auto(
        android.graphics.Color.WHITE,
        android.graphics.Color.WHITE
      )
    )
    setContent {
      AppCompositionLocalProvider(themeController) {
        val theme = LocalTheme.current
        val color = if (theme.coloredNaviBar) {
          theme.primary
        } else {
          Color.White
        }
        // TODO
        window.navigationBarColor = color.toArgb()
        Theme.setLightNavigationBarAuto(this, theme.isPrimaryLight)

        APlayerTheme {
          AppNav()
        }
      }
    }

    registerLocalReceiver(receiver, IntentFilter().apply {
      addAction(ACTION_DOWNLOAD_COMPLETE)
      addAction(ACTION_SHOW_DIALOG)
      addAction(ACTION_DISMISS_DIALOG)
    })

    lifecycleScope.launch {
      delay(5000)
      mainViewModel.checkInAppUpdate()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    unregisterLocalReceiver(receiver)
  }

  private class MainReceiver(act: ComposeActivity) : BroadcastReceiver() {

    private val ref: WeakReference<ComposeActivity> = WeakReference(act)

    override fun onReceive(context: Context, intent: Intent?) {
      if (intent == null) {
        return
      }
      val action = intent.action
      if (action.isNullOrEmpty()) {
        return
      }
      val act = ref.get() ?: return
      when (action) {
        ACTION_DOWNLOAD_COMPLETE -> {
          val apkFile = File(intent.getStringExtra(DownloadService.EXTRA_PATH)!!)
          val apkUri = ("file://${apkFile.absolutePath}").toUri()
          val intent: Intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val apkUri = FileProvider.getUriForFile(
              context,
              "${context.packageName}.ota_update_provider",
              apkFile
            )
            Intent(Intent.ACTION_INSTALL_PACKAGE).setData(apkUri)
              .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          } else {
            Intent(Intent.ACTION_VIEW).setDataAndType(
              apkUri,
              "application/vnd.android.package-archive"
            )
              .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          }
          act.startActivity(intent)
        }

        ACTION_SHOW_DIALOG -> showLoading(false, context.getString(R.string.updating))
        ACTION_DISMISS_DIALOG -> dismissLoading()
      }

    }
  }
}

@Composable
fun AppCompositionLocalProvider(
  themeController: ThemeController,
  content: @Composable (() -> Unit)
) {
  CompositionLocalProvider(
    LocalThemeController provides themeController,
    LocalTheme provides themeController.appTheme,
    LocalNavController provides rememberNavController()
  ) {
    ProvideViewModels {
      content()
    }
  }
}