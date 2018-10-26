package remix.myplayer.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import com.afollestad.materialdialogs.DialogAction
import remix.myplayer.App
import remix.myplayer.Global
import remix.myplayer.R
import remix.myplayer.bean.mp3.*
import remix.myplayer.misc.getSongIds
import remix.myplayer.service.MusicService
import remix.myplayer.theme.Theme
import remix.myplayer.theme.Theme.getBaseDialog
import remix.myplayer.ui.activity.MainActivity
import remix.myplayer.ui.adapter.*
import remix.myplayer.ui.widget.MultiPopupWindow
import remix.myplayer.ui.widget.TipPopupwindow
import remix.myplayer.util.*

class MultipleChoice<T>(private val activity: Activity, val type: Int) : View.OnClickListener {
    //所有选中的position
    private val checkPos = ArrayList<Int>()
    //所有选中的参数 歌曲 专辑 艺术家 播放列表 文件夹
    private val checkParam = ArrayList<T>()
    //是否正在显示顶部菜单
    var isActive: Boolean = false
    var adapter: RecyclerView.Adapter<*>? = null
    private var popup: MultiPopupWindow? = null
    var extra: Int = 0


    private fun getSongs(): List<Song> {
        val ids = getSongIds()
        val songs = ArrayList<Song>()
        ids.forEach {
            songs.add(MediaStoreUtil.getSongById(it))
        }
        return songs
    }

    private fun getSongIds(): List<Int> {
        if (checkParam.isEmpty())
            return emptyList()
        val ids = ArrayList<Int>()
        when (type) {
            Constants.SONG, Constants.PLAYLISTSONG -> {
                checkParam.forEach {
                    ids.add((it as Song).Id)
                }
            }
            Constants.ALBUM -> {
                checkParam.forEach {
                    ids.addAll((it as Album).getSongIds())
                }
            }
            Constants.ARTIST -> {
                checkParam.forEach {
                    ids.addAll((it as Artist).getSongIds())
                }
            }
            Constants.PLAYLIST -> {
                checkParam.forEach {
                    ids.addAll((it as PlayList).getSongIds())
                }
            }
            Constants.FOLDER -> {
                checkParam.forEach {
                    ids.addAll((it as Folder).getSongIds())
                }
            }

        }
        return ids
    }

    private fun delete() {
        val title = when (type) {
            Constants.PLAYLIST -> activity.getString(R.string.confirm_delete_playlist)
            Constants.PLAYLISTSONG -> activity.getString(R.string.confirm_delete_from_playlist)
            else -> activity.getString(R.string.confirm_delete_from_library)
        }
        Theme.getBaseDialog(activity)
                .content(title)
                .positiveText(R.string.confirm)
                .negativeText(R.string.cancel)
                .checkBoxPromptRes(R.string.delete_source, SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DELETE_SOURCE, false), null)
                .onAny { dialog, which ->
                    if (which == DialogAction.POSITIVE) {
                        val num = if (type != Constants.PLAYLISTSONG || dialog.isPromptCheckBoxChecked) {
                            MediaStoreUtil.delete(getSongs(), dialog.isPromptCheckBoxChecked)
                        } else {
                            PlayListUtil.deleteMultiSongs(getSongIds(), extra)
                        }
                        ToastUtil.show(activity, activity.getString(R.string.delete_multi_song, num))
                        close()
                    }
                }
                .show()
    }

    @SuppressLint("CheckResult")
    private fun addToPlayQueue() {
        ToastUtil.show(activity, activity.getString(R.string.add_song_playqueue_success, MusicService.AddSongToPlayQueue(getSongIds())))
        close()
    }

    private fun addToPlayList() {
        //获得所有播放列表的信息
        val playListInfoList = PlayListUtil.getAllPlayListInfo()
        val playlistNameList = ArrayList<String>()
        if (playListInfoList == null)
            return
        for (i in playListInfoList.indices) {
            playlistNameList.add(playListInfoList[i].Name)
        }

        getBaseDialog(activity)
                .title(R.string.add_to_playlist)
                .items(playlistNameList)
                .itemsCallback { dialog, view, which, text ->
                    val num = PlayListUtil.addMultiSongs(getSongIds(), playListInfoList[which].Name, playListInfoList[which]._Id)
                    ToastUtil.show(activity, activity.getString(R.string.add_song_playlist_success, num, playListInfoList[which].Name))
                    close()
                }
                .neutralText(R.string.create_playlist)
                .onNeutral { dialog, which ->
                    getBaseDialog(activity)
                            .title(R.string.new_playlist)
                            .positiveText(R.string.create)
                            .negativeText(R.string.cancel)
                            .content(R.string.input_playlist_name)
                            .inputRange(1, 15)
                            .input("", activity.getString(R.string.local_list) + Global.PlayList.size) { _, input ->
                                if (!TextUtils.isEmpty(input)) {
                                    val newPlayListId = PlayListUtil.addPlayList(input.toString())
                                    ToastUtil.show(activity, when {
                                        newPlayListId > 0 -> R.string.add_playlist_success
                                        newPlayListId == -1 -> R.string.add_playlist_error
                                        else -> R.string.playlist_already_exist
                                    }, Toast.LENGTH_SHORT)
                                    if (newPlayListId < 0) {
                                        return@input
                                    }
                                    val num = PlayListUtil.addMultiSongs(getSongIds(), input.toString(), newPlayListId)
                                    ToastUtil.show(activity, activity.getString(R.string.add_song_playlist_success, num, input.toString()))
                                    close()
                                }
                            }
                            .show()
                }
                .build().show()
    }

    fun click(pos: Int, data: T): Boolean {
        if (!isActive)
            return false
        changeData(pos, data)
        closeIfNeed()
        adapter?.notifyItemChanged(if (isLibraryAdapter()) pos + 1 else pos)
        return true
    }

    fun longClick(pos: Int, data: T): Boolean {
        //某个列表已经处于多选状态
        if (!isActive && isActiveSomeWhere) {
            return false
        }
        if (!isActive) {
            open()
            isActive = true
            isActiveSomeWhere = true
            Util.vibrate(App.getContext(), 150)
        }

        changeData(pos, data)
        closeIfNeed()
        adapter?.notifyItemChanged(if (isLibraryAdapter()) pos + 1 else pos)
        return true
    }

    private fun isLibraryAdapter(): Boolean {
        return (adapter is SongAdapter || adapter is AlbumAdapter || adapter is ArtistAdapter
                || adapter is PlayListAdapter || adapter is ChildHolderAdapter)
    }

    private fun closeIfNeed() {
        if (checkPos.isEmpty()) {
            close()
        }
    }

    private fun clearCheck() {
        checkPos.clear()
        checkParam.clear()
        adapter?.notifyDataSetChanged()
    }

    private fun changeData(pos: Int, data: T) {
        if (checkPos.contains(pos)) {
            checkPos.remove(pos)
            checkParam.remove(data)
        } else {
            checkPos.add(pos)
            checkParam.add(data)
        }
    }


    fun open() {
        popup = MultiPopupWindow(activity)
        popup?.show(View(activity))
        if (SPUtil.getValue(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.FIRST_SHOW_MULTI, true)) {
            SPUtil.putValue(activity, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.FIRST_SHOW_MULTI, false)
            val tipPopup = TipPopupwindow(activity)
            tipPopup.show(activity.window.decorView)
        }
        //MainActivity显示分割线
        popup?.contentView?.findViewById<View>(R.id.multi_divider)?.visibility = if (activity is MainActivity) View.VISIBLE else View.GONE
        popup?.contentView?.findViewById<View>(R.id.multi_playqueue)?.setOnClickListener(this)
        popup?.contentView?.findViewById<View>(R.id.multi_delete)?.setOnClickListener(this)
        popup?.contentView?.findViewById<View>(R.id.multi_playlist)?.setOnClickListener(this)
        popup?.contentView?.findViewById<View>(R.id.multi_close)?.setOnClickListener(this)
    }

    fun close() {
        isActive = false
        isActiveSomeWhere = false
        popup?.dismiss()
        popup = null
        clearCheck()
    }

    fun isPositionCheck(pos: Int): Boolean {
        return checkPos.contains(pos)
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.multi_playqueue -> {
                addToPlayQueue()
            }
            R.id.multi_delete -> {
                delete()
            }
            R.id.multi_playlist -> {
                addToPlayList()
            }
            R.id.multi_close -> {
                close()
            }
        }
    }

    companion object {
        @JvmStatic
        var isActiveSomeWhere = false
    }
}