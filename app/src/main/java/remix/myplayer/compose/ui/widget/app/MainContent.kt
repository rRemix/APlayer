package remix.myplayer.compose.ui.widget.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import remix.myplayer.bean.misc.Library
import remix.myplayer.compose.ui.theme.LocalTheme

@Composable
fun MainContent(
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
          color = LocalTheme.current.primaryReverse)
      },
      edgePadding = 0.dp,
      containerColor = LocalTheme.current.primary
    ) {
      libraries.forEachIndexed { index, library ->
        Tab(
          selected = pagerState.currentPage == index,
          onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
          text = { Text(stringResource(library.stringRes), maxLines = 1) },
          selectedContentColor = LocalTheme.current.primaryReverse,
          unselectedContentColor = LocalTheme.current.tabText
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