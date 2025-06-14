package remix.myplayer.compose.ui.widget.popup

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.afollestad.materialdialogs.DialogAction.POSITIVE
import com.soundcloud.android.crop.Crop
import io.reactivex.functions.Consumer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.R
import remix.myplayer.bean.misc.CustomCover
import remix.myplayer.bean.mp3.APlayerModel
import remix.myplayer.bean.mp3.Song
import remix.myplayer.bean.mp3.popMenuItems
import remix.myplayer.bean.mp3.songIds
import remix.myplayer.bean.mp3.type
import remix.myplayer.compose.clickWithRipple
import remix.myplayer.compose.ui.theme.LocalTheme
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.helper.DeleteHelper
import remix.myplayer.helper.MusicServiceRemote.setPlayQueue
import remix.myplayer.misc.menu.LibraryListener.Companion.EXTRA_COVER
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService.Companion.EXTRA_POSITION
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.activity.base.BaseActivity
import remix.myplayer.ui.dialog.AddtoPlayListDialog
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.MusicUtil.makeCmdIntent
import remix.myplayer.util.RxUtil.applySingleScheduler
import remix.myplayer.util.SPUtil
import remix.myplayer.util.ToastUtil

@Composable
fun LibraryItemPopupButton(
  modifier: Modifier = Modifier,
  model: APlayerModel
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

    LibraryItemDropdownMenu(expanded, model) {
      expanded = false
    }

    Image(
      painter = painterResource(R.drawable.icon_player_more),
      contentDescription = "PopupButton",
      colorFilter = ColorFilter.tint(LocalTheme.current.libraryButton)
    )
  }
}

@Composable
fun LibraryItemDropdownMenu(
  expanded: Boolean,
  model: APlayerModel,
  onDismissRequest: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val items = model.popMenuItems()
  DropdownMenu(
    modifier = Modifier.wrapContentSize(Alignment.TopEnd),
    expanded = expanded,
    // TODO
//    offset = DpOffset(0.dp, -dimensionResource(R.dimen.item_list_btn_size)),
    containerColor = LocalTheme.current.dialogBackground,
    onDismissRequest = onDismissRequest
  ) {
    val activity = LocalActivity.current as? BaseActivity
    items.forEachIndexed { index, res ->
      DropdownMenuItem(
        text = { Text(stringResource(res), color = LocalTheme.current.textPrimary) },
        onClick = {
          onDismissRequest()
          scope.launch {
            val songs = withContext(Dispatchers.IO) {
              MediaStoreUtil.getSongsByIds(model.songIds())
            }
            handleClickMenuItem(activity ?: return@launch, songs, model, items[index])
          }
        }
      )
    }
  }
}

@SuppressLint("CheckResult")
private fun handleClickMenuItem(
  context: Context,
  songs: List<Song>,
  model: APlayerModel,
  titleId: Int
) {
  if (songs.isEmpty()) {
    return
  }
  val ids = songs.map { it.id }
  when (titleId) {
    //播放
    R.string.play -> {
      if (songs.isEmpty()) {
        ToastUtil.show(context, R.string.list_is_empty)
        return
      }
      setPlayQueue(
        songs, makeCmdIntent(Command.PLAYSELECTEDSONG)
          .putExtra(EXTRA_POSITION, 0)
      )
    }
    //添加到播放队列
    R.string.add_to_play_queue -> {
      if (songs.isEmpty()) {
        ToastUtil.show(context, R.string.list_is_empty)
        return
      }
      DatabaseRepository.getInstance()
        .insertToPlayQueue(ids)
        .compose(applySingleScheduler())
        .subscribe(Consumer {
          ToastUtil.show(context, context.getString(R.string.add_song_playqueue_success, it))
        })
    }
    //添加到播放列表
    R.string.add_to_playlist -> {
      AddtoPlayListDialog.newInstance(ids)
        .show((context as BaseActivity).supportFragmentManager, AddtoPlayListDialog::class.java.simpleName)
    }
    //删除
    R.string.delete -> {
      R.string.my_favorite
      if (model is PlayList && model.isFavorite()) {
        //我的收藏不可删除
        ToastUtil.show(context, R.string.mylove_cant_edit)
        return
      }
      val check = arrayOf(SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DELETE_SOURCE, false))
      Theme.getBaseDialog(context)
        .content(if (model is PlayList) R.string.confirm_delete_playlist else R.string.confirm_delete_from_library)
        .positiveText(R.string.confirm)
        .negativeText(R.string.cancel)
        .checkBoxPromptRes(R.string.delete_source, check[0]) { _, isChecked -> check[0] = isChecked }
        .onAny { _, which ->
          if (which == POSITIVE) {
            DeleteHelper.deleteSongs(
              context as BaseActivity,
              ids,
              check[0],
              if (model is PlayList) model.getKey().toLong() else -1,
              model is PlayList
            )
              .compose(applySingleScheduler())
              .subscribe({
                ToastUtil.show(context, if (it) R.string.delete_success else R.string.delete_error)
              }, {
                ToastUtil.show(context, R.string.delete_error)
              })
          }
        }
        .show()
    }
    //设置封面
    R.string.set_album_cover, R.string.set_artist_cover, R.string.set_playlist_cover -> {
      // TODO
      val customCover = CustomCover(model, model.type())
      val thumbIntent = (context as Activity).intent
      thumbIntent.putExtra(EXTRA_COVER, customCover)
      context.intent = thumbIntent
      Crop.pickImage(context, Crop.REQUEST_PICK)
    }
    //列表重命名
    R.string.rename -> {
      if (model !is PlayList) {
        return
      }
      if (model.isFavorite()) {
        //我的收藏不可删除
        ToastUtil.show(context, R.string.mylove_cant_edit)
        return
      }
      Theme.getBaseDialog(context)
        .title(R.string.rename)
        .input("", "", false) { _, input ->
          DatabaseRepository.getInstance()
            .getPlayList(model.getKey().toLong())
            .flatMap {
              DatabaseRepository.getInstance()
                .updatePlayList(it.copy(name = input.toString()))
            }
            .compose(applySingleScheduler())
            .subscribe({
              ToastUtil.show(context, R.string.save_success)
            }, {
              ToastUtil.show(context, R.string.save_error)
            })
        }
        .buttonRippleColor(ThemeStore.rippleColor)
        .positiveText(R.string.confirm)
        .negativeText(R.string.cancel)
        .show()
    }

    else -> {
    }
  }

}