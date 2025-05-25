package remix.myplayer.compose.ui.widget.app

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.bean.misc.Library
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.popup.ScreenPopupButton
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.ui.activity.SearchActivity
import remix.myplayer.ui.dialog.TimerDialog

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AppBar(
  scrollBehavior: TopAppBarScrollBehavior,
  drawerState: DrawerState
) {
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
    actions = { AppBarActions() })
}

@Composable
private fun AppBarActions(libraryVM: LibraryViewModel = viewModel()) {
  val library by libraryVM.currentLibrary.collectAsStateWithLifecycle()
  val activity = LocalActivity.current

  if (library.tag != Library.TAG_FOLDER && library.tag != Library.TAG_CLOUD) {
    ScreenPopupButton(library)
  }
  IconButton(onClick = {
    TimerDialog.newInstance()
      .show(
        (activity as FragmentActivity).supportFragmentManager,
        TimerDialog::class.java.simpleName
      )
  }) {
    Icon(
      painter = painterResource(R.drawable.ic_timer_white_24dp),
      contentDescription = "Timer"
    )
  }
  IconButton(onClick = { activity?.startActivity(Intent(activity, SearchActivity::class.java)) }) {
    Icon(
      painter = painterResource(R.drawable.ic_search_white_24dp),
      contentDescription = "Timer"
    )
  }
}
