package remix.myplayer.ui.screen.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.data.model.misc.Library
import remix.myplayer.ui.dialog.CreatePlayListDialog
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.app.BottomBar
import remix.myplayer.ui.widget.app.Drawer
import remix.myplayer.ui.widget.app.FAButton
import remix.myplayer.ui.widget.app.MultiSelectBar
import remix.myplayer.ui.widget.app.ViewPager
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.mainViewModel
import remix.myplayer.viewmodel.settingViewModel
import remix.myplayer.viewmodel.smbViewModel
import remix.myplayer.viewmodel.webDavViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun HomeScreen() {
  val mainVM = mainViewModel
  val libraryVM = libraryViewModel

  val multiSelectState by mainVM.multiSelectState.collectAsStateWithLifecycle()
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val scope = rememberCoroutineScope()

  BackHandler(enabled = drawerState.isOpen || multiSelectState.isShowing()) {
    if (drawerState.isOpen) {
      scope.launch {
        drawerState.close()
      }
    } else if (multiSelectState.isShowing()) {
      mainVM.closeMultiSelect()
    }
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = { Drawer(drawerState) }) {

    val libraries by settingViewModel.enabledLibraries.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState { libraries.size }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(
      flingAnimationSpec = null,
      snapAnimationSpec = null
    )

    val showMultiSelect by remember {
      derivedStateOf {
        multiSelectState.isShowInLibrary()
      }
    }

    Scaffold(
      Modifier
        .fillMaxSize()
        .nestedScroll(scrollBehavior.nestedScrollConnection),
      containerColor = LocalTheme.current.libraryBackground,
      topBar = {
        AnimatedContent(
          targetState = showMultiSelect,
          transitionSpec = {
            if (targetState) {
              slideInVertically() togetherWith slideOutVertically { height -> height / 2 }
            } else {
              slideInVertically { height -> height } togetherWith slideOutVertically()
            }
          }
        ) { isMultiSelect ->
          if (!isMultiSelect) {
            HomeAppBar(scrollBehavior, drawerState)
          } else {
            MultiSelectBar(
              state = multiSelectState,
              scrollBehavior = scrollBehavior,
            )
          }
        }
      },
      floatingActionButton = {
        val selectLibrary by remember(libraries) {
          derivedStateOf {
            libraries.getOrElse(pagerState.currentPage) { libraries.first() }
          }
        }

        CreatePlayListDialog()

        var showAddRemoteMenu by remember { mutableStateOf(false) }

        val webDavVM = webDavViewModel
        val smbVM = smbViewModel
        Column {
          if (showAddRemoteMenu) {
            DropdownMenu(
              expanded = true,
              containerColor = LocalTheme.current.dialogBackground,
              onDismissRequest = { showAddRemoteMenu = false }
            ) {
              DropdownMenuItem(
                text = {
                  Text(
                    stringResource(R.string.webdav),
                    color = LocalTheme.current.textPrimary
                  )
                },
                onClick = {
                  showAddRemoteMenu = false
                  webDavVM.showAddWebDavDialog()
                }
              )
              if (smbVM.supportSmb) {
                SmbDropDownMenu(smbVM) {
                  showAddRemoteMenu = false
                  smbVM.showAddSmbDialog()
                }
              }
            }
          }

          FAButton(
            selectLibrary.tag == Library.TAG_PLAYLIST || selectLibrary.tag == Library.TAG_REMOTE
          ) {
            if (mainVM.multiSelectState.value.isShowing()) {
              return@FAButton
            }

            if (selectLibrary.tag == Library.TAG_PLAYLIST) {
              libraryVM.showCreatePlaylistDialog()
            } else if (selectLibrary.tag == Library.TAG_REMOTE) {
              showAddRemoteMenu = true
            }
          }
        }

      })
    { contentPadding ->
      HomeContent(contentPadding, pagerState, libraries)
    }
  }
}

@Composable
private fun HomeContent(
  contentPadding: PaddingValues,
  pagerState: PagerState,
  libraries: List<Library>,
) {
  val scope = rememberCoroutineScope()
  val scrollToCurrentEvent = remember { MutableSharedFlow<Unit>() }

  Column(modifier = Modifier.padding(contentPadding)) {
    ScrollableTabRow(
      selectedTabIndex = pagerState.currentPage,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
          height = 3.dp,
          color = LocalTheme.current.primaryReverse
        )
      },
      edgePadding = 0.dp,
      containerColor = LocalTheme.current.primary
    ) {
      libraries.forEachIndexed { index, library ->
        val theme = LocalTheme.current
        var lastClickTime by remember { mutableLongStateOf(0L) }

        Tab(
          selected = pagerState.currentPage == index,
          onClick = {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < 300) {
              if (library.tag == Library.TAG_SONG) {
                scope.launch { scrollToCurrentEvent.emit(Unit) }
              }
              return@Tab
            }
            lastClickTime = currentTime
            scope.launch { pagerState.animateScrollToPage(index) }
          },
          text = { Text(stringResource(library.stringRes), maxLines = 1) },
          selectedContentColor = theme.primaryReverse,
          unselectedContentColor = colorResource(
            if (theme.isPrimaryCloseToWhite) R.color.dark_normal_tab_text_color else R.color.light_normal_tab_text_color
          )
        )
      }
    }

    ViewPager(
      modifier = Modifier.weight(1f),
      libraries = libraries,
      pagerState = pagerState,
      scrollToCurrentEvent = scrollToCurrentEvent
    )

    BottomBar()
  }
}

// 修改tab最小宽度
fun hackTabMinWidth() {
  try {
    Class
      .forName("androidx.compose.material3.TabRowKt")
      .getDeclaredField("ScrollableTabRowMinimumTabWidth")
      .apply {
        isAccessible = true
      }.set(null, 72f)
  } catch (e: Exception) {
    e.printStackTrace()
  }
}

