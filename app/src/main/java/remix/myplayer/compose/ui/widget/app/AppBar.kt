package remix.myplayer.compose.ui.widget.app

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.bean.misc.Library
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.nav.LocalNavController
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.popup.ScreenPopupButton
import remix.myplayer.compose.viewmodel.SettingViewModel
import remix.myplayer.compose.viewmodel.TimerViewModel
import remix.myplayer.ui.activity.SearchActivity

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
private fun AppBarActions(vm: SettingViewModel = activityViewModel()) {
  val library by vm.currentLibrary.collectAsStateWithLifecycle()

  if (library.tag != Library.TAG_FOLDER && library.tag != Library.TAG_REMOTE) {
    ScreenPopupButton(library)
  }

  defaultActions.map { it ->
    IconButton(onClick = {
      it.action()
    }) {
      Icon(
        painter = painterResource(it.icon),
        contentDescription = it.contentDescription
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonAppBar(
  title: String,
  showBack: Boolean = true,
  actions: List<AppBarAction> = defaultActions
) {
  val navController = LocalNavController.current

  TopAppBar(
    title = { Text(title, color = Color.White, modifier = Modifier.padding(start = 16.dp)) },
    modifier = Modifier,
    navigationIcon = {
      if (showBack) {
        IconButton(onClick = {
          navController.popBackStack()
        }) {
          Icon(
            painter = painterResource(R.drawable.ic_arrow_back_white_24dp),
            contentDescription = "Back"
          )
        }
      }
    },
    actions = {
      actions.map {
        IconButton(onClick = it.action) {
          Icon(
            painter = painterResource(it.icon),
            contentDescription = it.contentDescription
          )
        }
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = LocalTheme.current.primary,
      scrolledContainerColor = LocalTheme.current.primary,
      navigationIconContentColor = Color.White,
      actionIconContentColor = Color.White,
    ),
  )
}

private val defaultActions: List<AppBarAction>
  @Composable
  get() {
    val context = LocalContext.current

    val timerVM = activityViewModel<TimerViewModel>()

    return listOf(
      AppBarAction(R.drawable.ic_timer_white_24dp, "Timer") {
        timerVM.showTimerDialog()
      },
      AppBarAction(R.drawable.ic_search_white_24dp, "Search") {
        context.startActivity(Intent(context, SearchActivity::class.java))
      })
  }


class AppBarAction(
  val icon: Int,
  val contentDescription: String? = null,
  val action: () -> Unit
)