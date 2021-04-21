package remix.myplayer.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.View
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import remix.myplayer.R
import remix.myplayer.db.room.DatabaseRepository.Companion.getInstance
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.request.network.RxUtil
import remix.myplayer.theme.Theme
import remix.myplayer.ui.adapter.AddToPlayListAdapter
import remix.myplayer.ui.dialog.base.BaseMusicDialog
import remix.myplayer.ui.fragment.PlayListFragment.AsyncPlayListLoader
import remix.myplayer.util.ToastUtil
import java.util.*

/**
 * Created by taeja on 16-2-1.
 */
/**
 * 将歌曲添加到播放列表的对话框
 */
class AddtoPlayListDialog : BaseMusicDialog(), LoaderManager.LoaderCallbacks<List<PlayList>> {

  private val adapter: AddToPlayListAdapter by lazy {
    AddToPlayListAdapter(R.layout.item_playlist_addto)
  }
  private var songIds: List<Int>? = null

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val rootView = requireActivity().layoutInflater.inflate(R.layout.dialog_addto_playlist, null)
    val dialog = Theme.getBaseDialog(activity)
        .customView(rootView, false)
        .build()

    songIds = arguments?.getSerializable(EXTRA_SONG_LIST) as List<Int>?
    if (songIds == null) {
      ToastUtil.show(context, R.string.add_song_playlist_error)
      dismiss()
    }
    adapter.onItemClickListener = object : OnItemClickListener {
      @SuppressLint("CheckResult")
      override fun onItemClick(view: View, position: Int) {
        val (playListId, name) = adapter.dataList[position]
        getInstance()
            .insertToPlayList(songIds ?: return, playListId)
            .compose(RxUtil.applySingleScheduler())
            .doFinally { dismiss() }
            .subscribe({ num: Int? -> ToastUtil.show(context, R.string.add_song_playlist_success, num, name) }
            ) { throwable: Throwable? -> ToastUtil.show(context, R.string.add_song_playlist_error) }
      }

      override fun onItemLongClick(view: View, position: Int) {}
    }

    val recyclerView = rootView.findViewById<RecyclerView>(R.id.playlist_addto_list)

    recyclerView.adapter = adapter
    recyclerView.itemAnimator = DefaultItemAnimator()
    recyclerView.layoutManager = LinearLayoutManager(context)
    loaderManager.initLoader<List<PlayList>>(LOADER_ID++, null, this)

    rootView.findViewById<View>(R.id.playlist_addto_new).setOnClickListener {
      getInstance()
          .getAllPlaylist()
          .compose(RxUtil.applySingleScheduler())
          .subscribe { playLists ->
            Theme.getBaseDialog(context)
                .title(R.string.new_playlist)
                .positiveText(R.string.create)
                .negativeText(R.string.cancel)
                .inputRange(1, 15)
                .input("", getString(R.string.local_list) + playLists.size) { dialog: MaterialDialog?, input: CharSequence ->
                  if (TextUtils.isEmpty(input)) {
                    ToastUtil.show(context, R.string.add_error)
                    return@input
                  }
                  getInstance()
                      .insertPlayList(input.toString())
                      .flatMap { newId ->
                        getInstance().insertToPlayList(songIds!!, newId.toLong())
                      }
                      .compose(RxUtil.applySingleScheduler())
                      .subscribe({ num: Int? ->
                        ToastUtil.show(context, R.string.add_playlist_success)
                        ToastUtil
                            .show(context, getString(R.string.add_song_playlist_success, num, input.toString()))
                      }, { throwable: Throwable? -> ToastUtil.show(context, R.string.add_error) })
                }
                .dismissListener { dialog: DialogInterface? -> dismiss() }
                .show()
          }
    }

    //改变高度，并置于底部
    dialog.window?.let { window ->
      window.setWindowAnimations(R.style.DialogAnimBottom)
      val display = requireActivity().windowManager.defaultDisplay
      val metrics = DisplayMetrics()
      display.getMetrics(metrics)
      val lp = window.attributes
      lp.width = metrics.widthPixels
      window.attributes = lp
      window.setGravity(Gravity.BOTTOM)
    }

    return dialog
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<List<PlayList>?> {
    return AsyncPlayListLoader(requireContext())
  }

  override fun onLoadFinished(loader: Loader<List<PlayList>?>, data: List<PlayList>?) {
    if (data == null) {
      return
    }
    adapter.setDataList(data)
  }

  override fun onLoaderReset(loader: Loader<List<PlayList>?>) {
    adapter.setDataList(null)
  }

  override fun onDestroy() {
    super.onDestroy()
    adapter.setDataList(null)
  }

  companion object {
    const val EXTRA_SONG_LIST = "list"
    fun newInstance(ids: List<Int>): AddtoPlayListDialog {
      val dialog = AddtoPlayListDialog()
      val arg = Bundle()
      arg.putSerializable(EXTRA_SONG_LIST, ArrayList<Any?>(ids))
      dialog.arguments = arg
      return dialog
    }

    private var LOADER_ID = 0
  }
}