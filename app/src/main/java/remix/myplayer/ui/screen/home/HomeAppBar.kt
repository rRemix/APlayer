package remix.myplayer.ui.screen.home

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import remix.myplayer.data.model.misc.Library
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.defaultAppBarActions
import remix.myplayer.ui.widget.popup.ScreenPopupButton
import remix.myplayer.viewmodel.settingViewModel


@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun HomeAppBar(
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
      if (library.tag != Library.TAG_REMOTE) {
        ScreenPopupButton(library)
      }

      defaultAppBarActions.map {
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
