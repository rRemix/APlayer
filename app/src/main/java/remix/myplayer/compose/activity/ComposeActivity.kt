package remix.myplayer.compose.activity

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import remix.myplayer.compose.activity.base.BaseMusicActivity
import remix.myplayer.compose.nav.AppNav
import remix.myplayer.compose.nav.LocalNavController
import remix.myplayer.compose.ui.theme.APlayerTheme
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.theme.LocalThemeController
import remix.myplayer.compose.ui.theme.ThemeController
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.compose.viewmodel.MusicViewModel
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.service.MusicService
import remix.myplayer.theme.Theme
import javax.inject.Inject

@AndroidEntryPoint
class ComposeMainActivity : BaseMusicActivity() {

  private val libraryViewModel: LibraryViewModel by viewModels()
  private val musicViewModel: MusicViewModel by viewModels()

  @Inject
  lateinit var themeController: ThemeController

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

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
  }

  override fun onPermissionChanged(has: Boolean) {
    super.onPermissionChanged(has)
    fetchMedia()
  }

  override fun onServiceConnected(service: MusicService) {
    super.onServiceConnected(service)
    onMetaChanged()
  }

  override fun onMediaStoreChanged() {
    super.onMediaStoreChanged()
    fetchMedia()
  }

  override fun onMetaChanged() {
    super.onMetaChanged()
    musicViewModel.setCurrentSong(MusicServiceRemote.getCurrentSong())
    musicViewModel.setPlaying(MusicServiceRemote.isPlaying())
  }

  override fun onPlayStateChange() {
    super.onPlayStateChange()
    musicViewModel.setPlaying(MusicServiceRemote.isPlaying())
  }

  private fun fetchMedia() {
    if (hasPermission) {
      libraryViewModel.fetchMedia()
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
    content()
  }
}