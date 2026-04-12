package remix.myplayer.ui.widget.popup

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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.data.db.room.entity.PlayList
import remix.myplayer.data.model.audio.APlayerModel
import remix.myplayer.data.model.audio.Album
import remix.myplayer.data.model.audio.Artist
import remix.myplayer.data.model.audio.Folder
import remix.myplayer.data.model.audio.Genre
import remix.myplayer.data.model.audio.type
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService.Companion.EXTRA_POSITION
import remix.myplayer.service.MusicServiceRemote.setPlayQueue
import remix.myplayer.ui.nav.LocalNavController
import remix.myplayer.ui.nav.MessageNotifier
import remix.myplayer.ui.nav.RouteCustomCoverCrop
import remix.myplayer.ui.theme.LocalTheme
import remix.myplayer.ui.theme.popupButton
import remix.myplayer.util.MusicUtil.makeCmdIntent
import remix.myplayer.util.ext.clickWithRipple
import remix.myplayer.viewmodel.libraryViewModel
import remix.myplayer.viewmodel.playbackViewModel
import remix.myplayer.viewmodel.settingViewModel

@Composable
fun LibraryItemPopupButton(
  modifier: Modifier = Modifier,
  model: APlayerModel,
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

    LibraryItemDropdownMenu(expanded, model) {
      expanded = false
    }

    Image(
      painter = painterResource(R.drawable.icon_player_more),
      contentDescription = "PopupButton",
      colorFilter = ColorFilter.tint(LocalTheme.current.popupButton())
    )
  }
}

@Composable
fun LibraryItemDropdownMenu(
  expanded: Boolean,
  model: APlayerModel,
  onDismissRequest: () -> Unit
) {
  val nav = LocalNavController.current
  val scope = rememberCoroutineScope()
  val libraryVM = libraryViewModel
  val playbackVM = playbackViewModel
  val settingVM = settingViewModel
  val items = model.popMenuItems()

  DropdownMenu(
    modifier = Modifier.wrapContentSize(Alignment.TopEnd),
    expanded = expanded,
    // TODO
//    offset = DpOffset(0.dp, -dimensionResource(R.dimen.item_list_btn_size)),
    containerColor = LocalTheme.current.dialogBackground,
    onDismissRequest = onDismissRequest
  ) {
    items.forEachIndexed { _, res ->
      DropdownMenuItem(
        text = { Text(stringResource(res), color = LocalTheme.current.textPrimary) },
        onClick = {
          onDismissRequest()
          scope.launch {
            val songs = withContext(Dispatchers.IO) {
              libraryVM.loadSongsByModels(listOf(model))
            }

//            if (songs.isEmpty()) {
//              return@launch
//            }
            val ids = songs.map { it.id }
            when (res) {
              // 播放
              R.string.play -> {
                if (songs.isEmpty()) {
                  MessageNotifier.show(R.string.list_is_empty)
                  return@launch
                }
                setPlayQueue(
                  songs, makeCmdIntent(Command.PLAY_AT)
                    .putExtra(EXTRA_POSITION, 0)
                )
              }
              // 添加到播放队列
              R.string.add_to_play_queue -> {
                if (songs.isEmpty()) {
                  MessageNotifier.show(R.string.list_is_empty)
                  return@launch
                }

                playbackVM.insertToQueue(songs)
              }
              // 添加到播放列表
              R.string.add_to_playlist -> {
                settingVM.showAddSongToPlayListDialog(ids, "")
              }
              // 删除
              R.string.delete -> {
                if (model is PlayList && model.isFavorite()) {
                  MessageNotifier.show(R.string.mylove_cant_edit)
                  return@launch
                }
                settingVM.showDeleteSongDialog(
                  listOf(model),
                  if (model is PlayList) R.string.confirm_delete_playlist else R.string.confirm_delete_from_library
                )
              }
              // 设置封面
              R.string.set_album_cover, R.string.set_artist_cover, R.string.set_playlist_cover -> {
                nav.navigate("${RouteCustomCoverCrop}/${model.getKey().toLong()}/${model.type()}")
              }
              // 列表重命名
              R.string.rename -> {
                if (model !is PlayList) {
                  return@launch
                }
                if (model.isFavorite()) {
                  // 我的收藏不可删除
                  MessageNotifier.show(R.string.mylove_cant_edit)
                  return@launch
                }

                settingVM.showReNamePlayListDialog(model)
              }

              else -> {
              }
            }
          }
        }
      )
    }
  }
}

private fun APlayerModel.popMenuItems(): List<Int> {
  return when (this) {
    is Album -> listOf(
      R.string.play,
      R.string.add_to_play_queue,
      R.string.add_to_playlist,
//      R.string.set_album_cover,
      R.string.delete
    )

    is Artist -> listOf(
      R.string.play,
      R.string.add_to_play_queue,
      R.string.add_to_playlist,
//      R.string.set_artist_cover,
      R.string.delete
    )

    is PlayList -> listOf(
      R.string.play,
      R.string.add_to_play_queue,
      R.string.add_to_playlist,
//      R.string.set_playlist_cover,
      R.string.rename,
      R.string.delete
    )

    is Genre -> listOf(
      R.string.play,
      R.string.add_to_play_queue,
      R.string.add_to_playlist
    )

    is Folder -> listOf(
      R.string.play,
      R.string.add_to_play_queue,
      R.string.add_to_playlist,
      R.string.delete
    )

    else -> throw IllegalArgumentException("unknown model: $this")
  }
}
