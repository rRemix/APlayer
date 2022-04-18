package remix.myplayer.ui.misc

import android.annotation.SuppressLint
import android.app.Activity
import android.text.TextUtils
import android.view.View
import android.widget.PopupMenu
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.mp3.Album
import remix.myplayer.bean.mp3.Artist
import remix.myplayer.bean.mp3.Folder
import remix.myplayer.bean.mp3.Song
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.misc.getSongIds
import remix.myplayer.theme.Theme
import remix.myplayer.theme.Theme.getBaseDialog
import remix.myplayer.ui.activity.base.BaseActivity
import remix.myplayer.ui.adapter.*
import remix.myplayer.ui.widget.MultiPopupWindow
import remix.myplayer.util.*
import remix.myplayer.util.RxUtil.applySingleScheduler
import java.lang.ref.WeakReference

class MultipleChoice<T>(activity: Activity, val type: Int) {
  private val activityRef = WeakReference(activity)
  private val disposableContainer = CompositeDisposable()

  private val databaseRepository = DatabaseRepository.getInstance()

  //所有选中的position
  private val checkPos = ArrayList<Int>()

  //所有选中的参数 歌曲 专辑 艺术家 播放列表 文件夹
  private val checkParam = ArrayList<T>()

  //是否正在显示顶部菜单
  var isActive: Boolean = false
  var adapter: BaseAdapter<T, *>? = null
  private var popup: MultiPopupWindow? = null
  var extra: Long = 0

  private fun getSongsSingle(ids: List<Long>): Single<List<Song>> {
    return Single.fromCallable {
      val songs = ArrayList<Song>()
      ids.forEach {
        songs.add(MediaStoreUtil.getSongById(it))
      }
      songs
    }
  }

  private fun getSongIdSingle(): Single<List<Long>> {
    return Single.fromCallable {
      val ids = ArrayList<Long>()
      if (checkParam.isEmpty()) {
        return@fromCallable ids
      }

      when (type) {
        Constants.SONG, Constants.PLAYLISTSONG -> {
          checkParam.forEach {
            ids.add((it as Song).id)
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
            ids.addAll((it as PlayList).audioIds)
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

  private fun getSongIds(): List<Long> {
    if (checkParam.isEmpty())
      return emptyList()
    val ids = ArrayList<Long>()
    when (type) {
      Constants.SONG, Constants.PLAYLISTSONG -> {
        checkParam.forEach {
          ids.add((it as Song).id)
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
          ids.addAll((it as PlayList).audioIds.toList())
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
    val context = activityRef.get() ?: return
    val title = when (type) {
      Constants.PLAYLIST -> context.getString(R.string.confirm_delete_playlist)
      Constants.PLAYLISTSONG -> context.getString(R.string.confirm_delete_from_playlist)
      else -> context.getString(R.string.confirm_delete_from_library)
    }

    val dialog = Theme.getLoadingDialog(context, context.getString(R.string.deleting)).build()
    val checked = arrayOf(SPUtil.getValue(App.context, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DELETE_SOURCE, false))
    getBaseDialog(context)
        .content(title)
        .positiveText(R.string.confirm)
        .negativeText(R.string.cancel)
        .checkBoxPromptRes(R.string.delete_source, checked[0]) { buttonView, isChecked -> checked[0] = isChecked }
        .onPositive { _, which ->
          val disposable = getSongIdSingle()
              .flatMap { ids ->
                getSongsSingle(ids)
              }
              .flatMap { songs ->
                deleteSingle(checked[0], songs)
              }
              .subscribeOn(Schedulers.io())
              .observeOn(AndroidSchedulers.mainThread())
              .doOnSubscribe {
                dialog.show()
              }
              .doFinally {
                if (dialog.isShowing) {
                  dialog.dismiss()
                }
                close()
              }
              .subscribe { count ->
                ToastUtil.show(context, context.getString(R.string.delete_multi_song, count))
              }
          disposableContainer.add(disposable)
        }.show()
  }

  private fun deleteSingle(deleteSource: Boolean, songs: List<Song>): Single<Int> {
    return Single
        .fromCallable {
          when (type) {
            Constants.PLAYLIST -> { //删除播放列表
              if (deleteSource) {
                MediaStoreUtil.delete(activityRef.get() as BaseActivity, songs, true)
              }

              checkParam.forEach {
                val playlist = it as PlayList
                if (playlist.name != activityRef.get()?.getString(R.string.my_favorite)) {
                  databaseRepository.deletePlayList((it as PlayList).id).subscribe()
                }
              }

              songs.size
            }
            Constants.PLAYLISTSONG -> { //删除播放列表内歌曲
              if (deleteSource) {
                MediaStoreUtil.delete(activityRef.get() as BaseActivity, songs, true)
              } else {
                databaseRepository.deleteFromPlayList(songs.map { it.id }, extra).blockingGet()
              }
            }
            else -> {
              MediaStoreUtil.delete(activityRef.get() as BaseActivity, songs, deleteSource)
            }
          }
        }
  }

  @SuppressLint("CheckResult")
  private fun addToPlayQueue() {
    val context = activityRef.get() ?: return
    val dialog = Theme.getLoadingDialog(context, context.getString(R.string.adding)).build()

    val disposable = getSongIdSingle()
        .flatMap {
          databaseRepository.insertToPlayQueue(it)
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe {
          dialog.show()
        }
        .doFinally {
          if (dialog.isShowing) {
            dialog.dismiss()
          }
          close()
        }
        .subscribe(Consumer {
          val activity = activityRef.get() ?: return@Consumer
          ToastUtil.show(activity, activity.getString(R.string.add_song_playqueue_success, it))
        })
    disposableContainer.add(disposable)
  }

  @SuppressLint("CheckResult")
  private fun addToPlayList() {
    val activity = activityRef.get() ?: return
    //获得所有播放列表的信息
    val disposable = databaseRepository.getAllPlaylist()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ playLists ->
          getBaseDialog(activity)
              .title(R.string.add_to_playlist)
              .items(playLists.map {
                it.name
              })
              .itemsCallback { dialog, view, which, text ->
                //直接添加到已有的播放列表
                getSongIdSingle()
                    .flatMap {
                      databaseRepository.insertToPlayList(it, playLists[which].name)
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .doFinally {
                      close()
                    }
                    .subscribe(Consumer {
                      ToastUtil.show(activity, activity.getString(R.string.add_song_playlist_success, it, playLists[which].name))
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
                    .input("", activity.getString(R.string.local_list) + playLists.size) { _, input ->
                      if (TextUtils.isEmpty(input)) {
                        ToastUtil.show(activity, R.string.add_error)
                        return@input
                      }
                      databaseRepository
                          .insertPlayList(input.toString())
                          .flatMap {
                            getSongIdSingle()
                          }
                          .flatMap {
                            databaseRepository.insertToPlayList(it, input.toString())
                          }
                          .compose(applySingleScheduler())
                          .doFinally {
                            close()
                          }
                          .subscribe({
                            ToastUtil.show(activity, R.string.add_playlist_success)
                            ToastUtil.show(activity, activity.getString(R.string.add_song_playlist_success, it, input.toString()))
                          }, {
                            ToastUtil.show(activity, activity.getString(R.string.add_error))
                          })
                    }
                    .show()
              }
              .build().show()
        }, {
          ToastUtil.show(activity, activity.getString(R.string.add_error))
        })
    disposableContainer.add(disposable)
  }

  private fun selectAll() {
    checkPos.clear()
    checkParam.clear()
    val datas = adapter!!.dataList
    checkPos.addAll(0 until datas.size)
    checkParam.addAll(datas)
    updateTitle()
    adapter?.notifyDataSetChanged()
  }

  fun click(pos: Int, data: T?): Boolean {
    if (data == null) {
      return false
    }
    if (!isActive)
      return false
    changeData(pos, data)
    closeIfNeed()
    updateTitle()
    adapter?.notifyItemChanged(if (isLibraryAdapter()) pos + 1 else pos)
    return true
  }

  fun longClick(pos: Int, data: T?): Boolean {
    if (data == null) {
      return false
    }
    //某个列表已经处于多选状态
    if (!isActive && isActiveSomeWhere) {
      return false
    }
    if (!isActive) {
      open()
      isActive = true
      isActiveSomeWhere = true
      Util.vibrate(App.context, 100)
    }

    changeData(pos, data)
    closeIfNeed()
    updateTitle()
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

  private fun updateTitle() {
    val activity = activityRef.get() ?: return
    popup?.binding?.multiTitle?.text =
        activity.getString(R.string.song_list_select_title_format, checkPos.size)
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
    val activity = activityRef.get() ?: return
    if (activity.isDestroyed || activity.isFinishing || !activity.hasWindowFocus()) {
      return
    }
    popup = MultiPopupWindow(activity)
    popup!!.binding.multiClose.setOnClickListener { close() }
    popup!!.binding.multiPlaylist.setOnClickListener { addToPlayList() }
    popup!!.binding.multiQueue.setOnClickListener { addToPlayQueue() }
    popup!!.binding.multiDelete.setOnClickListener { delete() }
    popup!!.binding.multiMore.setOnClickListener {
      PopupMenu(activity, popup!!.binding.multiMore).run {
        inflate(R.menu.menu_multi_select_more)
        setOnMenuItemClickListener {
          when (it.itemId) {
            R.id.select_all -> selectAll()
          }
          true
        }
        if (!activity.isFinishing && !activity.isDestroyed && activity.hasWindowFocus()) {
          show()
        }
      }
    }
    popup!!.show(View(activity))
  }

  fun close() {
    disposableContainer.clear()
    isActive = false
    isActiveSomeWhere = false
    popup?.dismiss()
    popup = null
    clearCheck()
  }

  fun isPositionCheck(pos: Int): Boolean {
    return checkPos.contains(pos)
  }

  override fun toString(): String {
    return "MultipleChoice(activity=${activityRef.get()}, type=$type, checkPos=$checkPos, checkParam=$checkParam, isActive=$isActive, adapter=$adapter, popup=$popup, extra=$extra)"
  }


  companion object {
    @JvmStatic
    var isActiveSomeWhere = false
  }
}

