package remix.myplayer.ui.widget.app

import android.content.ComponentName
import android.content.Intent
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
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
import remix.myplayer.data.prefs.ThemePrefs.Companion.BLACK
import remix.myplayer.data.prefs.ThemePrefs.Companion.DARK
import remix.myplayer.data.prefs.ThemePrefs.Companion.LIGHT
import remix.myplayer.misc.receiver.ExitReceiver
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.RouteHistory
import remix.myplayer.ui.nav.RouteLastAdded
import remix.myplayer.ui.nav.RouteSetting
import remix.myplayer.ui.theme.AppTheme
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.widget.common.TextPrimary
import remix.myplayer.ui.widget.library.GlideCover
import remix.myplayer.util.Constants
import remix.myplayer.util.ext.isPortraitOrientation
import remix.myplayer.viewmodel.PlaybackViewModel
import remix.myplayer.viewmodel.playbackViewModel

private val drawerTitles = mutableListOf(
  R.string.drawer_song,
  R.string.drawer_history,
  R.string.drawer_recently_add,
  R.string.drawer_setting,
  R.string.exit
)

private val drawerIcons = mutableListOf(
  R.drawable.ic_library_music_24dp,
  R.drawable.ic_history_24dp,
  R.drawable.ic_recent_24dp,
  R.drawable.ic_settings_24dp,
  R.drawable.ic_exit_to_app_24dp
)

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun Drawer(drawerState: DrawerState, vm: PlaybackViewModel = playbackViewModel) {
  val navController = LocalNavController.current
  val context = LocalContext.current
  val theme = LocalTheme.current

  val drawerDefault = colorResource(
    when (theme.theme) {
      LIGHT -> R.color.drawer_default_light
      DARK -> R.color.drawer_default_dark
      BLACK -> R.color.drawer_default_black
      else -> throw IllegalArgumentException("unknown theme: $theme")
    }
  )
  val drawerEffect = colorResource(
    when (theme.theme) {
      LIGHT -> R.color.drawer_effect_light
      DARK -> R.color.drawer_effect_dark
      BLACK -> R.color.drawer_effect_black
      else -> throw IllegalArgumentException("unknown theme: $theme")
    }
  )

  ModalDrawerSheet(
    modifier = Modifier
      .width(264.dp)
      .fillMaxHeight(),
    drawerShape = RectangleShape,
    drawerContainerColor = drawerDefault,
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
      val playbackState by vm.playbackUiState.collectAsStateWithLifecycle()
      val isPortrait = context.isPortraitOrientation()
      GlideCover(
        model = playbackState.song,
        circle = false,
        modifier = Modifier
          .padding(if (isPortrait) 20.dp else 12.dp)
          .size(if (isPortrait) 128.dp else 98.dp)
      )
      Text(
        modifier = Modifier
          .background(
            color = AppTheme.darkenColor(theme.primary),
            shape = RoundedCornerShape(4.dp)
          )
          .width(170.dp)
          .padding(horizontal = 8.dp, vertical = 6.dp),
        text = stringResource(R.string.play_now, playbackState.song.title),
        textAlign = TextAlign.Center,
        color = theme.primaryReverse,
        fontSize = if (isPortrait) 14.sp else 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      Spacer(modifier = Modifier.height(if (isPortrait) 20.dp else 12.dp))
    }

    val scope = rememberCoroutineScope()
    LazyColumn(modifier = Modifier.background(drawerDefault)) {
      itemsIndexed(drawerTitles) { index, item ->

        NavigationDrawerItem(
          label = {
            TextPrimary(
              modifier = Modifier.padding(start = 4.dp),
              text = stringResource(drawerTitles[index]),
              fontSize = 16.sp
            )
          },
          selected = index == 0,
          onClick = {
            when (item) {
              // 歌曲库
              R.string.drawer_song -> scope.launch { drawerState.close() }
              // 历史
              R.string.drawer_history -> navController.navigate(RouteHistory)
              // 最近添加
              R.string.drawer_recently_add -> navController.navigate(RouteLastAdded)
              // 设置
              R.string.drawer_setting -> navController.navigate(RouteSetting)
              // 退出
              R.string.exit -> {
                context.sendBroadcast(
                  Intent(Constants.ACTION_EXIT)
                    .setComponent(ComponentName(context, ExitReceiver::class.java))
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
            selectedContainerColor = drawerEffect,
            unselectedContainerColor = drawerDefault
          )
        )
      }

      item {
        Spacer(modifier = Modifier.weight(1f))
      }
    }

  }
}
