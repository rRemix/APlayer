package remix.myplayer.ui.screen

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.data.bean.misc.Library
import remix.myplayer.ui.dialog.InputDialog
import remix.myplayer.ui.dialog.rememberDialogState
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.RouteSongChoose
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.app.BottomBar
import remix.myplayer.ui.widget.app.Drawer
import remix.myplayer.ui.widget.app.FAButton
import remix.myplayer.ui.widget.app.MultiSelectBar
import remix.myplayer.ui.widget.app.ViewPager
import remix.myplayer.ui.widget.common.defaultAppBarActions
import remix.myplayer.ui.widget.popup.ScreenPopupButton
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.mainViewModel
import remix.myplayer.viewmodel.settingViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun HomeScreen() {
  val mainVM = mainViewModel
  val libraryVM = libraryViewModel
  val navController = LocalNavController.current
  val context = LocalContext.current

  val multiSelectState by mainVM.multiSelectState.collectAsStateWithLifecycle()
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val scope = rememberCoroutineScope()

  BackPressHandler(enabled = drawerState.isOpen || multiSelectState.isShowing()) {
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

    val libraries by settingViewModel.allLibraries.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState { libraries.size }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

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
        val showFb by remember {
          derivedStateOf {
            pagerState.currentPage == libraries.indexOfFirst {
              it.tag == Library.TAG_PLAYLIST
            }
          }
        }

        var text by rememberSaveable {
          mutableStateOf("")
        }
        val dialogState = rememberDialogState(false)

        InputDialog(
          dialogState = dialogState,
          title = stringResource(R.string.new_playlist),
          positive = stringResource(R.string.create),
          text = text,
          onDismissRequest = {
            text = ""
          },
          onValueChange = {
            text = it
          }
        ) {
          libraryVM.insertPlayList(it) { id ->
            if (id > 0) {
              navController.navigate("$RouteSongChoose/${id}/$it")
            }
          }
        }

        FAButton(showFb) {
          if (mainVM.multiSelectState.value.isShowing()) {
            return@FAButton
          }

          text = "${context.getString(R.string.local_list)}${libraryVM.playLists.value.size}"
          dialogState.show()
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
  val scrollToTopEvent = remember { MutableSharedFlow<Unit>() }

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
                scope.launch { scrollToTopEvent.emit(Unit) }
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
      scrollToTopEvent = scrollToTopEvent
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

@Composable
fun BackPressHandler(
  enabled: Boolean = true,
  onBackPressed: () -> Unit
) {
  val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
  val backCallback = remember {
    object : OnBackPressedCallback(enabled) {
      override fun handleOnBackPressed() {
        onBackPressed()
      }
    }
  }

  LaunchedEffect(enabled) {
    backCallback.isEnabled = enabled
  }

  DisposableEffect(dispatcher) {
    dispatcher?.addCallback(backCallback)
    onDispose {
      backCallback.remove()
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeAppBar(
  scrollBehavior: TopAppBarScrollBehavior,
  drawerState: DrawerState
) {
  val library by settingViewModel.currentLibrary.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()

  TopAppBar(
    scrollBehavior = scrollBehavior,
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = LocalTheme.current.primary,
      scrolledContainerColor = LocalTheme.current.primary,
      navigationIconContentColor = Color.White,
      actionIconContentColor = Color.White,
    ),
    title = {},
    navigationIcon = {
      IconButton(onClick = { scope.launch { drawerState.open() } }) {
        Icon(Icons.Filled.Menu, contentDescription = "Menu")
      }
    },
    actions = {
      if (library.tag != Library.TAG_FOLDER && library.tag != Library.TAG_REMOTE) {
        ScreenPopupButton(library)
      }

      defaultAppBarActions.map { it ->
        IconButton(onClick = {
          it.action()
        }) {
          Icon(
            painter = painterResource(it.icon),
            contentDescription = it.contentDescription
          )
        }
      }
    })
}