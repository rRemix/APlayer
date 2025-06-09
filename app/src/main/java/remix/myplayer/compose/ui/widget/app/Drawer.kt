package remix.myplayer.compose.ui.widget.app

import android.content.ComponentName
import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.nav.LocalNavController
import remix.myplayer.compose.nav.RouteSetting
import remix.myplayer.compose.ui.theme.AppTheme
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.compose.ui.widget.library.GlideCover
import remix.myplayer.compose.viewmodel.MusicViewModel
import remix.myplayer.misc.receiver.ExitReceiver
import remix.myplayer.ui.activity.HistoryActivity
import remix.myplayer.ui.activity.RecentlyActivity
import remix.myplayer.ui.activity.SupportActivity
import remix.myplayer.util.Constants

private val drawerTitles = intArrayOf(
  R.string.drawer_song,
  R.string.drawer_history,
  R.string.drawer_recently_add,
  R.string.support_develop,
  R.string.drawer_setting,
  R.string.exit
)
private val drawerIcons = intArrayOf(
  R.drawable.ic_library_music_24dp,
  R.drawable.ic_history_24dp,
  R.drawable.ic_recent_24dp,
  R.drawable.ic_favorite_24dp,
  R.drawable.ic_settings_24dp,
  R.drawable.ic_exit_to_app_24dp
)

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun Drawer(drawerState: DrawerState, vm: MusicViewModel = activityViewModel()) {
  val navController = LocalNavController.current

  val theme = LocalTheme.current
  ModalDrawerSheet(
    modifier = Modifier
      .width(264.dp)
      .fillMaxHeight(),
    drawerShape = RectangleShape,
    drawerContainerColor = theme.drawerDefault,
    windowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Start)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(theme.primary),
      verticalArrangement = Arrangement.Center,
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Spacer(modifier = Modifier.height(with(LocalDensity.current) {
        WindowInsets.systemBars.getTop(this).toDp()
      }))
      val currentSong by vm.currentSong.collectAsStateWithLifecycle()
      GlideCover(
        model = currentSong,
        circle = false,
        modifier = Modifier
          .padding(top = 20.dp, bottom = 20.dp)
          .size(128.dp)
      )
      Text(
        modifier = Modifier
          .background(
            color = AppTheme.darkenColor(theme.primary),
            shape = RoundedCornerShape(4.dp)
          )
          .width(170.dp)
          .padding(horizontal = 8.dp, vertical = 6.dp),
        text = stringResource(R.string.play_now, currentSong.title),
        textAlign = TextAlign.Center,
        color = theme.primaryReverse,
        fontSize = 14.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(20.dp))
    }

    var selectDrawer by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.background(theme.drawerDefault)) {
      val activity = LocalActivity.current
      val scope = rememberCoroutineScope()
      drawerTitles.mapIndexed { index, _ ->
        NavigationDrawerItem(
          label = {
            TextPrimary(
              modifier = Modifier.padding(start = 4.dp),
              text = stringResource(drawerTitles[index]),
              fontSize = 16.sp
            )
          },
          selected = selectDrawer == index,
          onClick = {
            selectDrawer = index

            val activity = activity ?: return@NavigationDrawerItem
            when (index) {
              // 歌曲库
              0 -> scope.launch { drawerState.close() }
              // 历史
              1 -> activity.startActivity(Intent(activity, HistoryActivity::class.java))
              // 最近添加
              2 -> activity.startActivity(Intent(activity, RecentlyActivity::class.java))
              // 捐赠
              3 -> activity.startActivity(Intent(activity, SupportActivity::class.java))
              // 设置
              4 -> navController.navigate(RouteSetting)
              // 退出
              5 -> {
                activity.sendBroadcast(
                  Intent(Constants.ACTION_EXIT)
                    .setComponent(ComponentName(activity, ExitReceiver::class.java))
                )
              }
            }
          },
          icon = {
            Icon(
              modifier = Modifier.padding(start = 8.dp),
              painter = painterResource(drawerIcons[index]),
              contentDescription = null,
              tint = theme.primary
            )
          },
          shape = RectangleShape,
          colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = theme.drawerEffect,
            unselectedContainerColor = theme.drawerDefault
          )
        )
      }
      Spacer(modifier = Modifier.weight(1f))
    }
  }
}