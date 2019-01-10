package remix.myplayer.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import remix.myplayer.App
import remix.myplayer.Global
import remix.myplayer.R
import remix.myplayer.bean.mp3.*
import remix.myplayer.misc.getSongIds
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

  private fun getSongsSingle(ids: List<Int>): Single<List<Song>> {
    return Single.fromCallable {
      val songs = ArrayList<Song>()
      ids.forEach {
        songs.add(MediaStoreUtil.getSongById(it))
      }
      songs
    }
  }

  private fun getSongIdSingle(): Single<List<Int>> {
    return Single.fromCallable {
      val ids = ArrayList<Int>()
      if (checkParam.isEmpty()) {
        return@fromCallable ids
      }

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
      ids
    }
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

  @SuppressLint("CheckResult")
  private fun delete() {
    val title = when (type) {
      Constants.PLAYLIST -> activity.getString(R.string.confirm_delete_playlist)
      Constants.PLAYLISTSONG -> activity.getString(R.string.confirm_delete_from_playlist)
      else -> activity.getString(R.string.confirm_delete_from_library)
    }

    getSongIdSingle()
        .flatMap { getSongsSingle(it) }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doFinally {
          close()
        }
        .subscribe { songs ->
          Theme.getBaseDialog(activity)
              .content(title)
              .positiveText(R.string.confirm)
              .negativeText(R.string.cancel)
              .checkBoxPromptRes(R.string.delete_source, SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DELETE_SOURCE, false), null)
              .onPositive { dialog, which ->
                deleteSingle(dialog, songs)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(Consumer {
                      ToastUtil.show(activity, activity.getString(R.string.delete_multi_song, it))

                    })

              }.show()
        }

  }

  private fun deleteSingle(dialog: MaterialDialog, songs: List<Song>): Single<Int> {
    return Single
        .fromCallable {
          when (type) {
            Constants.PLAYLIST -> { //删除播放列表
              if (dialog.isPromptCheckBoxChecked) {
                MediaStoreUtil.delete(songs, true)
              }
              checkParam.forEach { PlayListUtil.deletePlayList((it as PlayList)._Id) }
              songs.size
            }
            Constants.PLAYLISTSONG -> { //删除播放列表内歌曲
              if (dialog.isPromptCheckBoxChecked) {
                MediaStoreUtil.delete(songs, true)
              } else {
                PlayListUtil.deleteMultiSongs(songs.map { it.Id }, extra)
              }
            }
            else -> {
              MediaStoreUtil.delete(songs, dialog.isPromptCheckBoxChecked)
            }
          }
        }
  }

  @SuppressLint("CheckResult")
  private fun addToPlayQueue() {
    getSongIdSingle()
        .map {
          PlayListUtil.addMultiSongs(it, Constants.PLAY_QUEUE)
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doFinally {
          close()
        }
        .subscribe(Consumer {
          ToastUtil.show(activity, activity.getString(R.string.add_song_playqueue_success, it))
        })
  }

  @SuppressLint("CheckResult")
  private fun addToPlayList() {
    getSongIdSingle()
        .subscribe(Consumer {
          LogUtil.d("", it.toString())
        })
    //获得所有播放列表的信息
    Single
        .fromCallable {
          PlayListUtil.getAllPlayListInfo()
        }
        .filter {
          it.size > 0
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ playListInfoList ->
          getBaseDialog(activity)
              .title(R.string.add_to_playlist)
              .items(playListInfoList.map {
                it.Name
              })
              .itemsCallback { dialog, view, which, text ->
                //直接添加到已有的播放列表
                getSongIdSingle()
                    .map {
                      PlayListUtil.addMultiSongs(it, playListInfoList[which].Name, playListInfoList[which]._Id)
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .doFinally {
                      close()
                    }
                    .subscribe(Consumer {
                      ToastUtil.show(activity, activity.getString(R.string.add_song_playlist_success, it, playListInfoList[which].Name))
                    })
              }
              .neutralText(R.string.create_playlist)
              .onNeutral { dialog, which ->
                //新建后再添加到新的列表
                getBaseDialog(activity)
                    .title(R.string.new_playlist)
                    .positiveText(R.string.create)
                    .negativeText(R.string.cancel)
                    .content(R.string.input_playlist_name)
                    .inputRange(1, 15)
                    .input("", activity.getString(R.string.local_list) + Global.PlayList.size) { _, input ->
                      if (!TextUtils.isEmpty(input)) {
                        Single
                            .fromCallable {
                              val newPlaylistId = PlayListUtil.addPlayList(input.toString())
                              if (newPlaylistId == -2) {
                                throw IllegalArgumentException(activity.getString(R.string.playlist_already_exist))
                              } else if (newPlaylistId < 0) {
                                throw IllegalArgumentException(activity.getString(R.string.add_playlist_error))
                              }
                              newPlaylistId
                            }
                            .filter {
                              it > 0
                            }
                            .map {
                              PlayListUtil.addMultiSongs(getSongIds(), input.toString(), it)
                            }
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .doFinally {
                              close()
                            }
                            .subscribe({
                              ToastUtil.show(activity, activity.getString(R.string.add_song_playlist_success, it, input.toString()))
                            }, {
                              if (it is IllegalArgumentException) {
                                ToastUtil.show(activity, it.message)
                              } else {
                                ToastUtil.show(activity, activity.getString(R.string.add_error))
                              }
                            })

                      }
                    }
                    .show()
              }
              .build().show()
        }, {
          ToastUtil.show(activity, activity.getString(R.string.add_error))
        })
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
      Util.vibrate(App.getContext(), 100)
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

  override fun toString(): String {
    return "MultipleChoice(activity=$activity, type=$type, checkPos=$checkPos, checkParam=$checkParam, isActive=$isActive, adapter=$adapter, popup=$popup, extra=$extra)"
  }


  companion object {
    @JvmStatic
    var isActiveSomeWhere = false
  }
}