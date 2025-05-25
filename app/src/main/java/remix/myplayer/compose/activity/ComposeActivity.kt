package remix.myplayer.compose.activity

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import dagger.hilt.android.AndroidEntryPoint
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.theme.LocalThemeController
import remix.myplayer.compose.ui.theme.ThemeController
import remix.myplayer.compose.ui.widget.app.AppBar
import remix.myplayer.compose.ui.widget.app.Drawer
import remix.myplayer.compose.ui.widget.app.FAButton
import remix.myplayer.compose.ui.widget.app.MainContent
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.compose.viewmodel.MusicViewModel
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.ui.activity.base.BaseMusicActivity
import javax.inject.Inject

@AndroidEntryPoint
class ComposeActivity : BaseMusicActivity() {
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
      AppThemeProvider(themeController) {
        APlayerApp()
      }
    }
    libraryViewModel.loadInit(hasPermission)
  }

  override fun onPermissionChanged(has: Boolean) {
    super.onPermissionChanged(has)
    fetchMedia()
  }

  override fun onMediaStoreChanged() {
    super.onMediaStoreChanged()
    fetchMedia()
  }

  override fun onMetaChanged() {
    super.onMetaChanged()
    musicViewModel.setCurrentSong(MusicServiceRemote.getCurrentSong())
  }

  override fun onPlayStateChange() {
    super.onPlayStateChange()
    musicViewModel.setPlaying(MusicServiceRemote.isPlaying())
  }

  private fun fetchMedia() {
    if (hasPermission) {
      libraryViewModel.fetchSongs()
      libraryViewModel.fetchAlbums()
    }
  }

}

@Composable
fun AppThemeProvider(
  themeController: ThemeController,
  content: @Composable (() -> Unit)
) {
  CompositionLocalProvider(
    LocalTheme provides themeController.appTheme,
    LocalThemeController provides themeController
  ) {
    content()
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun APlayerApp() {
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val viewModel: LibraryViewModel = viewModel()
  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = { Drawer() }) {

    val libraries by viewModel.allLibraries.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState { libraries.size }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
      Modifier
        .fillMaxSize()
        .nestedScroll(scrollBehavior.nestedScrollConnection),
      containerColor = LocalTheme.current.libraryBackground,
      topBar = { AppBar(scrollBehavior, drawerState) },
      floatingActionButton = {
        FAButton(pagerState, libraries)
      })
    { contentPadding ->
      MainContent(contentPadding, pagerState, libraries)
    }
  }
}

@Preview(showBackground = true)
@Composable
fun AppPreview() {
  APlayerApp()
}