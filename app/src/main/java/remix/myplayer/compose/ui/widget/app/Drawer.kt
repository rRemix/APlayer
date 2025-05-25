package remix.myplayer.compose.ui.widget.app

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
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.ui.theme.AppTheme
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.compose.ui.widget.library.GlideCover
import remix.myplayer.compose.viewmodel.MusicViewModel

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
fun Drawer(viewModel: MusicViewModel = viewModel()) {
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
      val currentSong by viewModel.currentSong.observeAsState(Song.EMPTY_SONG)
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
      drawerTitles.mapIndexed { index, _ ->
        NavigationDrawerItem(
          label = {
            TextPrimary(
              modifier = Modifier.padding(start = 4.dp),
              text = stringResource(drawerTitles[index])
            )
          },
          selected = selectDrawer == index,
          onClick = { selectDrawer = index },
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