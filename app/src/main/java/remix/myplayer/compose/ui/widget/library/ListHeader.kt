package remix.myplayer.compose.ui.widget.library

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.widget.app.clickableWithoutRipple
import remix.myplayer.helper.MusicServiceRemote.setPlayQueue
import remix.myplayer.service.Command
import remix.myplayer.ui.adapter.HeaderAdapter
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.ToastUtil

@Composable
fun SongListHeader(songs: List<Song>) {
  val context = LocalContext.current
  Row(
    modifier = Modifier
      .height(48.dp)
      .fillMaxWidth()
      .background(LocalTheme.current.mainBackground)
      .clickableWithoutRipple(remember { MutableInteractionSource() }) {
        if (songs.isEmpty()) {
          ToastUtil.show(context, R.string.no_song)
          return@clickableWithoutRipple
        }
        setPlayQueue(songs, MusicUtil.makeCmdIntent(Command.NEXT, true))
      },
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      modifier = Modifier.padding(start = 16.dp, end = 8.dp),
      painter = painterResource(R.drawable.ic_shuffle_white_24dp),
      tint = LocalTheme.current.secondary,
      contentDescription = "ListHeaderIcon"
    )
    Text(
      text = stringResource(R.string.play_random, songs.size),
      color = LocalTheme.current.textSecondary
    )
  }
}

@Composable
fun ModeHeader(grid: Boolean, onClick: (mode: Int) -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .background(LocalTheme.current.mainBackground),
    horizontalArrangement = Arrangement.End,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(
      modifier = Modifier.clickableWithoutRipple(interactionSource = remember { MutableInteractionSource() }) {
        onClick(HeaderAdapter.GRID_MODE)
      },
      painter = painterResource(R.drawable.ic_apps_white_24dp),
      contentDescription = "ModeGrid",
      tint = Color(if (grid) LocalTheme.current.secondary.toArgb() else ColorUtil.getColor(R.color.default_model_button_color))
    )
    Icon(
      modifier = Modifier
        .padding(horizontal = 18.dp)
        .clickableWithoutRipple(interactionSource = remember { MutableInteractionSource() }) {
          onClick(HeaderAdapter.LIST_MODE)
        } ,
      painter = painterResource(R.drawable.ic_format_list_bulleted_white_24dp),
      contentDescription = "ModeList",
      tint = Color(if (!grid) LocalTheme.current.secondary.toArgb() else ColorUtil.getColor(R.color.default_model_button_color))
    )
  }
}