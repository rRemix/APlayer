package remix.myplayer.misc.menu

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.provider.MediaStore
import android.view.MenuItem
import androidx.appcompat.widget.PopupMenu
import com.afollestad.materialdialogs.DialogAction.POSITIVE
import com.soundcloud.android.crop.Crop
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import remix.myplayer.R
import remix.myplayer.bean.misc.CustomCover
import remix.myplayer.bean.mp3.APlayerModel
import remix.myplayer.bean.mp3.Genre
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.helper.DeleteHelper
import remix.myplayer.helper.MusicServiceRemote.setPlayQueue
import remix.myplayer.misc.getSongIds
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService.Companion.EXTRA_POSITION
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.activity.base.BaseActivity
import remix.myplayer.ui.dialog.AddtoPlayListDialog
import remix.myplayer.util.Constants
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.MediaStoreUtil.getSongIds
import remix.myplayer.util.MediaStoreUtil.getSongsByParentPath
import remix.myplayer.util.MusicUtil.makeCmdIntent
import remix.myplayer.util.RxUtil.applySingleScheduler
import remix.myplayer.util.SPUtil
import remix.myplayer.util.ToastUtil

/**
 * Created by taeja on 16-1-25.
 */
class LibraryListener(private val context: Context,
                      private val model: APlayerModel,
                      private val type: Int,
                      private val extra: String) : PopupMenu.OnMenuItemClickListener {

  private fun getSongIdSingle(): Single<List<Long>> {
    return Single.fromCallable {
      when (type) {
        //专辑或者艺术家
        Constants.ALBUM, Constants.ARTIST -> getSongIds((if (type == Constants.ALBUM) MediaStore.Audio.Media.ALBUM_ID else MediaStore.Audio.Media.ARTIST_ID) + "=" + model.getKey(), null)
        //播放列表
        Constants.PLAYLIST -> DatabaseRepository.getInstance().getPlayList(model.getKey().toLong())
            .map {
              it.audioIds.toList()
            }
            .blockingGet()
        //文件夹
        Constants.FOLDER -> getSongsByParentPath(model.getKey()).map { it.id }
        //流派
        Constants.GENRE -> (model as Genre).getSongIds()
        else -> emptyList()
      }
    }
  }

  @SuppressLint("CheckResult")
  override fun onMenuItemClick(item: MenuItem): Boolean {
    getSongIdSingle()
        .map {
          MediaStoreUtil.getSongsByIds(it)
        }
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .subscribe(Consumer { songs ->
          val ids = songs.map { it.id }
          when (item.itemId) {
            //播放
            R.id.menu_play -> {
              if (songs == null || songs.isEmpty()) {
                ToastUtil.show(context, R.string.list_is_empty)
                return@Consumer
              }
              setPlayQueue(songs, makeCmdIntent(Command.PLAYSELECTEDSONG)
                  .putExtra(EXTRA_POSITION, 0))
            }
            //添加到播放队列
            R.id.menu_add_to_play_queue -> {
              if (songs == null || songs.isEmpty()) {
                ToastUtil.show(context, R.string.list_is_empty)
                return@Consumer
              }
              DatabaseRepository.getInstance()
                  .insertToPlayQueue(ids)
                  .compose(applySingleScheduler())
                  .subscribe(Consumer {
                    ToastUtil.show(context, context.getString(R.string.add_song_playqueue_success, it))
                  })
            }
            //添加到播放列表
            R.id.menu_add_to_playlist -> {
              AddtoPlayListDialog.newInstance(ids)
                  .show((context as BaseActivity).supportFragmentManager, AddtoPlayListDialog::class.java.simpleName)
            }
            //删除
            R.id.menu_delete -> {
              R.string.my_favorite
              if (extra == context.getString(R.string.my_favorite)) {
                //我的收藏不可删除
                ToastUtil.show(context, R.string.mylove_cant_edit)
                return@Consumer
              }
              val check = arrayOf(SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DELETE_SOURCE, false))
              Theme.getBaseDialog(context)
                  .content(if (type == Constants.PLAYLIST) R.string.confirm_delete_playlist else R.string.confirm_delete_from_library)
                  .positiveText(R.string.confirm)
                  .negativeText(R.string.cancel)
                  .checkBoxPromptRes(R.string.delete_source, check[0]) { buttonView, isChecked -> check[0] = isChecked }
                  .onAny { dialog, which ->
                    if (which == POSITIVE) {
                      DeleteHelper.deleteSongs(context as BaseActivity, ids, check[0], if (type == Constants.PLAYLIST) model.getKey().toLong() else -1, type == Constants.PLAYLIST)
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
            R.id.menu_album_thumb -> {
              val customCover = CustomCover(model, type)
              val thumbIntent = (context as Activity).intent
              thumbIntent.putExtra(EXTRA_COVER, customCover)
              context.intent = thumbIntent
              Crop.pickImage(context, Crop.REQUEST_PICK)
            }
            //列表重命名
            R.id.menu_playlist_rename -> {
              if (extra == context.getString(R.string.my_favorite)) {
                //我的收藏不可删除
                ToastUtil.show(context, R.string.mylove_cant_edit)
                return@Consumer
              }
              Theme.getBaseDialog(context)
                  .title(R.string.rename)
                  .input("", "", false) { dialog, input ->
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
        })


    return true
  }

  companion object{
    const val EXTRA_COVER = "cover"
  }

}
