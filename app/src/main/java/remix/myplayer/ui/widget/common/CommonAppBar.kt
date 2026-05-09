package remix.myplayer.ui.widget.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import remix.myplayer.R
import remix.myplayer.helper.SleepTimer
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.RouteSearch
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.viewmodel.timerViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonAppBar(
  title: String?,
  showBack: Boolean = true,
  onBack: (() -> Unit)? = null,
  actions: @Composable () -> Unit
) {
  val navController = LocalNavController.current

  TopAppBar(
    title = {
      if (!title.isNullOrEmpty()) {
        Text(
          title,
          color = Color.White,
          fontSize = 18.sp,
          modifier = Modifier.padding(start = 16.dp)
        )
      }
    },
    modifier = Modifier,
    navigationIcon = {
      if (showBack) {
        IconButton(onClick = {
          if (onBack != null) {
            onBack.invoke()
          } else {
            navController.popBackStack()
          }
        }) {
          Icon(
            painter = painterResource(R.drawable.ic_arrow_back_white_24dp),
            contentDescription = "Back"
          )
        }
      }
    },
    actions = {
      actions()
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = LocalTheme.current.primary,
      scrolledContainerColor = LocalTheme.current.primary,
      navigationIconContentColor = Color.White,
      actionIconContentColor = Color.White,
    ),
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonAppBar(
  title: String,
  showBack: Boolean = true,
  onBack: (() -> Unit)? = null,
  actions: List<AppBarAction> = defaultAppBarActions
) {
  CommonAppBar(
    title,
    showBack,
    onBack,
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
  )
}

val defaultAppBarActions: List<AppBarAction>
  @Composable
  get() {
    val timerVM = timerViewModel
    val nav = LocalNavController.current
    val timerRunning by SleepTimer.runningState.collectAsStateWithLifecycle()
    val timerIcon = if (timerRunning) R.drawable.ic_timer_on_24dp else R.drawable.ic_timer_white_24dp

    return listOf(
      AppBarAction(timerIcon, "Timer") {
        timerVM.showTimerDialog()
      },
      AppBarAction(R.drawable.ic_search_white_24dp, "Search") {
        nav.navigate(RouteSearch)
      })
  }


class AppBarAction(
  val icon: Int,
  val contentDescription: String? = null,
  val action: () -> Unit
)
