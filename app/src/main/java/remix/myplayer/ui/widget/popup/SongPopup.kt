package remix.myplayer.ui.widget.popup

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import remix.myplayer.R
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Song
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService.Companion.EXTRA_SONG
import remix.myplayer.ui.activity.base.BaseActivity
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.RouteCustomCoverCrop
import remix.myplayer.ui.nav.RouteTagEdit
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.theme.popupButton
import remix.myplayer.util.Constants
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.Util
import remix.myplayer.util.ext.clickWithRipple
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.playbackViewModel
import remix.myplayer.viewmodel.settingViewModel
import remix.myplayer.viewmodel.tagEditViewModel

@Composable
fun SongPopupButton(
  modifier: Modifier = Modifier,
  song: Song,
  parent: APlayerModel,
  enabled: Boolean = true
) {
  var expanded by remember { mutableStateOf(false) }
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .clickWithRipple(enabled = enabled) {
        expanded = !expanded
      }
      .size(dimensionResource(id = R.dimen.item_list_btn_size))
  ) {
    SongDropdownMenu(expanded, song, parent) {
      expanded = false
    }

    Image(
      painter = painterResource(R.drawable.icon_player_more),
      contentDescription = "song button",
      colorFilter = ColorFilter.tint(LocalTheme.current.popupButton())
    )
  }
}

@Composable
private fun SongDropdownMenu(
  expanded: Boolean,
  song: Song,
  parent: APlayerModel,
  onDismissRequest: () -> Unit
) {
  val menuItems =
    listOf(
      R.string.add_to_next_song,
      R.string.add_to_play_queue,
      R.string.add_to_playlist,
      R.string.song_detail,
      R.string.song_edit,
//      R.string.set_album_cover,
      R.string.collect,
      R.string.share,
      R.string.ring,
      R.string.delete
    )
  val activity = LocalActivity.current as? BaseActivity
  val settingVM = settingViewModel
  val tagEditVM = tagEditViewModel
  val playbackVM = playbackViewModel
  val libraryVM = libraryViewModel
  val nav = LocalNavController.current

  DropdownMenu(
    modifier = Modifier.wrapContentSize(Alignment.TopEnd),
    expanded = expanded,
    // TODO
//    offset = DpOffset(0.dp, -dimensionResource(R.dimen.item_list_btn_size)),
    containerColor = LocalTheme.current.dialogBackground,
    onDismissRequest = onDismissRequest
  ) {
    menuItems.forEachIndexed { _, res ->
      DropdownMenuItem(
        text = { Text(stringResource(res), color = LocalTheme.current.textPrimary) },
        onClick = {
          onDismissRequest()

          if (activity == null) {
            return@DropdownMenuItem
          }

          when (res) {
            R.string.add_to_next_song -> {
              Util.sendLocalBroadcast(
                MusicUtil.makeCmdIntent(Command.ADD_TO_NEXT_SONG)
                  .putExtra(EXTRA_SONG, song)
              )
            }

            R.string.add_to_playlist -> {
              settingVM.showAddSongToPlayListDialog(listOf(song.id), "")
            }

            R.string.add_to_play_queue -> {
              playbackVM.insertToQueue(listOf(song))
            }

            R.string.song_detail -> {
              settingVM.showSongDetailDialog(song)
            }

            R.string.song_edit -> {
              if (song.isLocal()) {
                tagEditVM.startTagEdit(song)
                nav.navigate(RouteTagEdit)
              }
            }

            R.string.set_album_cover -> {
              nav.navigate("${RouteCustomCoverCrop}/${song.albumId}/${Constants.ALBUM}")
            }

            R.string.collect -> {
              val favorite =
                libraryVM.playLists.value.firstOrNull { it.isFavorite() } ?: return@DropdownMenuItem

              libraryVM.addSongsToPlayList(listOf(song.id), favorite.name)
            }

            R.string.ring -> {
              MusicUtil.setRing(activity, song.id)
            }

            R.string.share -> {
              activity.startActivity(
                Intent.createChooser(Util.createShareSongFileIntent(song, activity), null)
              )
            }

            R.string.delete -> {
              settingVM.showDeleteSongDialog(listOf(song), parent = parent)
            }
          }
        }
      )
    }
  }

}
