package remix.myplayer.compose.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.activityViewModel
import remix.myplayer.compose.clickWithRipple
import remix.myplayer.compose.nav.LocalNavController
import remix.myplayer.compose.ui.theme.APlayerTheme
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.common.TextPrimary
import remix.myplayer.compose.ui.widget.common.TextSecondary
import remix.myplayer.compose.ui.widget.library.GlideCover
import remix.myplayer.compose.viewmodel.LibraryViewModel
import remix.myplayer.util.ToastUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongChooseScreen(id: Long, name: String, vm: LibraryViewModel = activityViewModel()) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val theme = LocalTheme.current
  val songs by vm.songs.collectAsStateWithLifecycle()
  val nav = LocalNavController.current

  val selectedIds = remember {
    mutableStateSetOf<Long>()
  }

  Scaffold(
    topBar = {
      CenterAlignedTopAppBar(
        expandedHeight = 56.dp,
        title = {
          Text(
            stringResource(R.string.choose_song),
            color = theme.textPrimaryReverse,
            fontSize = 16.sp
          )
        },
        navigationIcon = {
          Box(
            modifier = Modifier
              .width(70.dp)
              .fillMaxHeight()
              .clickWithRipple(circle = false) {
                nav.popBackStack()
              },
            contentAlignment = Alignment.Center
          ) {
            Text(
              stringResource(R.string.cancel),
              color = theme.textPrimaryReverse,
              fontSize = 14.sp
            )
          }
        },
        actions = {
          Box(
            modifier = Modifier
              .width(70.dp)
              .fillMaxHeight()
              .clickWithRipple(circle = false) {
                if (selectedIds.isEmpty()) {
                  ToastUtil.show(context, R.string.choose_no_song)
                  return@clickWithRipple
                }

                scope.launch {
                  val count = vm.addSongsToPlayList(selectedIds.toList(), id)
                  ToastUtil.show(context, context.getString(R.string.add_song_playlist_success, count, name))
                  nav.popBackStack()
                }
              },
            contentAlignment = Alignment.Center
          ) {
            Text(
              stringResource(R.string.confirm),
              color = theme.textPrimaryReverse,
              fontSize = 14.sp
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = LocalTheme.current.primary,
          scrolledContainerColor = LocalTheme.current.primary,
          navigationIconContentColor = Color.White,
          actionIconContentColor = Color.White,
        ),
      )
    },
    containerColor = LocalTheme.current.mainBackground,
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
    ) {
      itemsIndexed(songs, key = { _, song ->
        song.id
      }) { pos, song ->
        ListSong(song = song, checked = selectedIds.contains(song.id)) {
          if (it) {
            selectedIds.add(song.id)
          } else {
            selectedIds.remove(song.id)
          }
        }
      }
    }
  }
}

@Composable
private fun ListSong(song: Song, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  Row(
    modifier = Modifier
      .height(56.dp)
      .fillMaxWidth()
      .clickWithRipple(false) {
        onCheckedChange(!checked)
      },
    verticalAlignment = Alignment.CenterVertically
  ) {
    GlideCover(
      modifier = Modifier
        .padding(horizontal = 8.dp)
        .size(42.dp), model = song, circle = false
    )

    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
      TextPrimary(song.showName)
      Spacer(Modifier.height(4.dp))
      TextSecondary(song.artist)
    }

    Checkbox(modifier = Modifier.padding(horizontal = 16.dp), checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Preview(showBackground = true)
@Composable
fun SongChooseScreenPreview() {
  APlayerTheme {
    SongChooseScreen(0, "")
  }
}