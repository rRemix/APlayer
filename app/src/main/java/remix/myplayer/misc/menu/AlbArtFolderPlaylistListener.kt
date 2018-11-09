package remix.myplayer.misc.menu

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.widget.PopupMenu
import android.view.MenuItem
import com.afollestad.materialdialogs.DialogAction.POSITIVE
import com.soundcloud.android.crop.Crop
import remix.myplayer.Global
import remix.myplayer.R
import remix.myplayer.bean.misc.CustomThumb
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.dialog.AddtoPlayListDialog
import remix.myplayer.util.*
import remix.myplayer.util.MediaStoreUtil.*
import java.util.*

/**
 * Created by taeja on 16-1-25.
 */
class AlbArtFolderPlaylistListener(private val context: Context, //专辑id 艺术家id 歌曲id 文件夹position
                                   private val id: Int, //0:专辑 1:歌手 2:文件夹 3:播放列表
                                   private val type: Int, //专辑名 艺术家名 文件夹position或者播放列表名字
                                   private val key: String) : PopupMenu.OnMenuItemClickListener {

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val ids = when (type) {
            Constants.ALBUM, Constants.ARTIST //专辑或者艺术家
            -> getSongIds((if (type == Constants.ALBUM) MediaStore.Audio.Media.ALBUM_ID else MediaStore.Audio.Media.ARTIST_ID) + "=" + id, null)
            Constants.PLAYLIST //播放列表
            -> PlayListUtil.getSongIds(id)
            Constants.FOLDER //文件夹
            -> getSongIdsByParentId(id)
            else -> emptyList<Int>()
        }

        //根据不同参数获得歌曲id列表
        when (item.itemId) {
            //播放
            R.id.menu_play -> {
                if (ids == null || ids.isEmpty()) {
                    ToastUtil.show(context, R.string.list_is_empty)
                    return true
                }
                val intent = Intent(MusicService.ACTION_CMD)
                val arg = Bundle()
                arg.putInt("Control", Command.PLAYSELECTEDSONG)
                arg.putInt("Position", 0)
                intent.putExtras(arg)
                MusicServiceRemote.setPlayQueue(ids, intent)
            }
            //添加到播放队列
            R.id.menu_add_to_play_queue -> {
                if (ids == null || ids.isEmpty()) {
                    ToastUtil.show(context, R.string.list_is_empty)
                    return true
                }
                ToastUtil.show(context, context.getString(R.string.add_song_playqueue_success, MusicService.AddSongToPlayQueue(ids)))
            }
            //添加到播放列表
            R.id.menu_add_to_playlist -> {
                val intentAdd = Intent(context, AddtoPlayListDialog::class.java)
                val ardAdd = Bundle()
                ardAdd.putSerializable("list", ArrayList(ids!!))
                intentAdd.putExtras(ardAdd)
                context.startActivity(intentAdd)
            }
            //删除
            R.id.menu_delete -> {
                if (id == Global.MyLoveID && type == Constants.PLAYLIST) {
                    ToastUtil.show(context, context.getString(R.string.mylove_cant_edit))
                    return true
                }

                Theme.getBaseDialog(context)
                        .content(if (type == Constants.PLAYLIST) R.string.confirm_delete_playlist else R.string.confirm_delete_from_library)
                        .positiveText(R.string.confirm)
                        .negativeText(R.string.cancel)
                        .checkBoxPromptRes(R.string.delete_source, SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DELETE_SOURCE, false), null)
                        .onAny { dialog, which ->
                            if (which == POSITIVE) {
                                if (type != Constants.PLAYLIST) {
                                    ToastUtil.show(context, if (MediaStoreUtil.delete(id, type, dialog.isPromptCheckBoxChecked) > 0) R.string.delete_success else R.string.delete_error)
                                } else {
                                    if (dialog.isPromptCheckBoxChecked) {
                                        MediaStoreUtil.delete(getSongsByIds(ids), true)
                                    }
                                    ToastUtil.show(context, if (PlayListUtil.deletePlayList(id)) R.string.delete_success else R.string.delete_error)
                                }
                            }
                        }
                        .show()
            }
            //设置封面
            R.id.menu_album_thumb -> {
                val thumbBean = CustomThumb(id, type, key)
                val thumbIntent = (context as Activity).intent
                thumbIntent.putExtra("thumb", thumbBean)
                context.intent = thumbIntent
                Crop.pickImage(context, Crop.REQUEST_PICK)
            }
            //列表重命名
            R.id.menu_playlist_rename -> {
                if (id == Global.MyLoveID && type == Constants.PLAYLIST) {
                    ToastUtil.show(context, context.getString(R.string.mylove_cant_edit))
                    return true
                }
                Theme.getBaseDialog(context)
                        .title(R.string.rename)
                        .input("", "", false) { dialog, input -> ToastUtil.show(context, context.getString(if (PlayListUtil.rename(id, input.toString())) R.string.save_success else R.string.save_error)) }
                        .buttonRippleColor(ThemeStore.getRippleColor())
                        .positiveText(R.string.confirm)
                        .negativeText(R.string.cancel)
                        .show()
            }
            else -> {
            }
        }
        return true
    }

}
