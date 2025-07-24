package remix.myplayer.compose.ui.widget.popup

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
import com.soundcloud.android.crop.Crop
import remix.myplayer.R
import remix.myplayer.bean.misc.CustomCover
import remix.myplayer.bean.mp3.APlayerModel
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.activity.base.BaseActivity
import remix.myplayer.compose.clickWithRipple
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.compose.ui.theme.popupButton
import remix.myplayer.compose.viewmodel.libraryViewModel
import remix.myplayer.compose.viewmodel.musicViewModel
import remix.myplayer.compose.viewmodel.settingViewModel
import remix.myplayer.misc.menu.LibraryListener.Companion.EXTRA_COVER
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService.Companion.EXTRA_SONG
import remix.myplayer.util.Constants
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.Util

@Composable
fun SongPopupButton(
  modifier: Modifier = Modifier,
  song: Song,
  parent: APlayerModel
) {
  var expanded by remember { mutableStateOf(false) }
  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .clickWithRipple {
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
      R.string.set_album_cover,
      R.string.collect,
      R.string.share,
      R.string.ring,
      R.string.delete
    )
  val activity = LocalActivity.current as? BaseActivity
  val settingVM = settingViewModel
  val musicVM = musicViewModel
  val libraryVM = libraryViewModel

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
              musicVM.insertToQueue(listOf(song.id))
            }

            R.string.song_detail -> {
              settingVM.showSongDetailDialog(song)
            }

            R.string.song_edit -> {
              if (song.isLocal()) {
                settingVM.showSongEditDialog(song)
              }
            }

            R.string.set_album_cover -> {
              val customCover = CustomCover(song, Constants.ALBUM)
              val coverIntent = activity.intent
              coverIntent.putExtra(EXTRA_COVER, customCover)
              activity.intent = coverIntent
              Crop.pickImage(activity, Crop.REQUEST_PICK)
            }

            R.string.collect -> {
              val favorite =
                libraryVM.playLists.value.firstOrNull { it.isFavorite() } ?: return@DropdownMenuItem

              libraryVM.addSongsToPlayList(listOf(song.id), favorite.name)
            }

            R.string.ring -> {
              MediaStoreUtil.setRing(activity, song.id)
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
