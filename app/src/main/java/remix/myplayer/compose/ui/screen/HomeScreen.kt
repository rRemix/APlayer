package remix.myplayer.compose.ui.screen

import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.bean.misc.Library
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.app.AppBar
import remix.myplayer.compose.ui.widget.app.BottomBar
import remix.myplayer.compose.ui.widget.app.Drawer
import remix.myplayer.compose.ui.widget.app.FAButton
import remix.myplayer.compose.ui.widget.app.ViewPager
import remix.myplayer.compose.viewmodel.SettingViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun HomeScreen(vm: SettingViewModel = activityViewModel()) {
  val drawerState = rememberDrawerState(DrawerValue.Closed)
  val scope = rememberCoroutineScope()

  BackPressHandler(enabled = drawerState.isOpen) {
    if (drawerState.isOpen) {
      scope.launch {
        drawerState.close()
      }
    }
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = { Drawer(drawerState) }) {

    val libraries by vm.allLibraries.collectAsStateWithLifecycle()
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
        Tab(
          selected = pagerState.currentPage == index,
          onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
          text = { Text(stringResource(library.stringRes), maxLines = 1) },
          selectedContentColor = theme.primaryReverse,
          unselectedContentColor = colorResource(
            if (theme.isPrimaryCloseToWhite) R.color.dark_normal_tab_text_color else R.color.light_normal_tab_text_color)
        )
      }
    }

    ViewPager(
      modifier = Modifier.weight(1f),
      libraries = libraries,
      pagerState = pagerState
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
private fun BackPressHandler(
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