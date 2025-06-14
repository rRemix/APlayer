package remix.myplayer.compose.ui.widget.popup

import android.annotation.SuppressLint
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
import com.afollestad.materialdialogs.DialogAction.POSITIVE
import com.soundcloud.android.crop.Crop
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.misc.CustomCover
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.clickWithRipple
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.helper.DeleteHelper
import remix.myplayer.misc.menu.LibraryListener.Companion.EXTRA_COVER
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService.Companion.EXTRA_SONG
import remix.myplayer.theme.Theme
import remix.myplayer.ui.activity.base.BaseActivity
import remix.myplayer.ui.dialog.AddtoPlayListDialog
import remix.myplayer.ui.misc.AudioTag
import remix.myplayer.util.Constants
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.RxUtil.applySingleScheduler
import remix.myplayer.util.SPUtil
import remix.myplayer.util.SPUtil.SETTING_KEY
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util

@Composable
fun SongPopupButton(
  modifier: Modifier = Modifier,
  song: Song,
  isDeletePlayList: Boolean = false,
  playListName: String = ""
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
    SongDropdownMenu(expanded, song, isDeletePlayList, playListName) {
      expanded = false
    }

    Image(
      painter = painterResource(R.drawable.icon_player_more),
      contentDescription = "song button",
      colorFilter = ColorFilter.tint(LocalTheme.current.libraryButton)
    )
  }
}

@Composable
private fun SongDropdownMenu(
  expanded: Boolean,
  song: Song,
  isDeletePlayList: Boolean = false,
  playListName: String = "",
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
          handleClickMenuItem(activity, res, song, isDeletePlayList, playListName)
        }
      )
    }
  }
}

@SuppressLint("CheckResult")
private fun handleClickMenuItem(
  activity: BaseActivity?,
  id: Int,
  song: Song,
  isDeletePlayList: Boolean,
  playListName: String
) {
  if (activity == null) {
    return
  }

  val tag = AudioTag(activity, song)
  when (id) {
    R.string.add_to_next_song -> {
      Util.sendLocalBroadcast(
        MusicUtil.makeCmdIntent(Command.ADD_TO_NEXT_SONG)
          .putExtra(EXTRA_SONG, song)
      )
    }

    R.string.add_to_playlist -> {
      val fm = activity.supportFragmentManager
      if (!fm.isDestroyed && !fm.isStateSaved) {
        AddtoPlayListDialog.newInstance(listOf(song.id))
          .show(fm, AddtoPlayListDialog::class.java.simpleName)
      }
    }

    R.string.add_to_play_queue -> {
      DatabaseRepository.getInstance()
        .insertToPlayQueue(listOf(song.id))
        .compose(applySingleScheduler())
        .subscribe { it ->
          ToastUtil.show(
            activity,
            activity.getString(R.string.add_song_playqueue_success, it)
          )
        }
    }

    R.string.song_detail -> {
      tag.detail()
    }

    R.string.song_edit -> {
      tag.edit()
    }

    R.string.set_album_cover -> {
      val customCover = CustomCover(song, Constants.ALBUM)
      val coverIntent = activity.intent
      coverIntent.putExtra(EXTRA_COVER, customCover)
      activity.intent = coverIntent
      Crop.pickImage(activity, Crop.REQUEST_PICK)
    }

    R.string.collect -> {
      DatabaseRepository.getInstance()
        .insertToPlayList(listOf(song.id), activity.getString(R.string.my_favorite))
        .compose<Int>(applySingleScheduler<Int>())
        .subscribe(
          { _ ->
            ToastUtil.show(
              activity,
              activity.getString(
                R.string.add_song_playlist_success,
                1,
                activity.getString(R.string.my_favorite)
              )
            )
          },
          { _ -> ToastUtil.show(activity, R.string.add_song_playlist_error) })
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
      val title = activity.getString(
        R.string.confirm_delete_from_playlist_or_library,
        if (isDeletePlayList) playListName else "曲库"
      )
      val check =
        arrayOf(SPUtil.getValue(App.context, SETTING_KEY.NAME, SETTING_KEY.DELETE_SOURCE, false));
      Theme.getBaseDialog(activity)
        .content(title)
        .positiveText(R.string.confirm)
        .negativeText(R.string.cancel)
        .checkBoxPromptRes(R.string.delete_source, check[0]) { _, isChecked ->
          check[0] = isChecked
        }
        .onAny { _, which ->
          if (which == POSITIVE) {
            DeleteHelper
              .deleteSong(activity, song.id, check[0], isDeletePlayList, playListName)
              .subscribe({ success ->
                ToastUtil.show(
                  activity,
                  if (success) R.string.delete_success else R.string.delete_error
                )
              }, { ToastUtil.show(activity, R.string.delete_error) })
          }
        }
        .show()
    }
  }
}