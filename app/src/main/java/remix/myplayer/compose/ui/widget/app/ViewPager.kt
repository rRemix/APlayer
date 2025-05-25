package remix.myplayer.compose.ui.widget.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import remix.myplayer.bean.misc.Library
import remix.myplayer.compose.ui.widget.library.screen.AlbumScreen
import remix.myplayer.compose.ui.widget.library.screen.SongScreen
import remix.myplayer.compose.viewmodel.LibraryViewModel

@Composable
fun ViewPager(
  modifier: Modifier = Modifier,
  libraries: List<Library>,
  pagerState: PagerState,
  viewModel: LibraryViewModel = viewModel()
) {

  HorizontalPager(
    modifier = modifier,
    state = pagerState,
    beyondViewportPageCount = 1
  ) { page ->
    when (libraries[page].tag) {
      Library.TAG_SONG -> SongScreen()
      Library.TAG_ALBUM -> AlbumScreen()
      else -> PageContent("Page: ${libraries[page]}")
    }
  }

  LaunchedEffect(pagerState.currentPage) {
    viewModel.changeLibrary(libraries[pagerState.currentPage])
  }
}

@Composable
fun PageContent(data: String) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.LightGray)
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = data,
      fontSize = 24.sp,
      fontWeight = FontWeight.Bold,
      modifier = Modifier
        .padding(bottom = 16.dp)
        .fillMaxSize()
    )
  }
}
