package remix.myplayer.ui.activity

import android.content.Context
import android.content.Intent
import android.content.Loader
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.ActivitySongChooseBinding
import remix.myplayer.db.room.DatabaseRepository.Companion.getInstance
import remix.myplayer.misc.asynctask.AppWrappedAsyncTaskLoader
import remix.myplayer.misc.interfaces.LoaderIds
import remix.myplayer.misc.interfaces.OnSongChooseListener
import remix.myplayer.util.RxUtil
import remix.myplayer.theme.ThemeStore.materialPrimaryColor
import remix.myplayer.theme.ThemeStore.textColorPrimaryReverse
import remix.myplayer.ui.adapter.SongChooseAdapter
import remix.myplayer.util.MediaStoreUtil.getAllSong
import remix.myplayer.util.ToastUtil

/**
 * @ClassName SongChooseActivity
 * @Description 新建列表后添加歌曲
 * @Author Xiaoborui
 * @Date 2016/10/21 09:34
 */
class SongChooseActivity : LibraryActivity<Song, SongChooseAdapter>() {
  lateinit var binding: ActivitySongChooseBinding

  private var mPlayListID = 0
  private var mPlayListName: String? = null
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivitySongChooseBinding.inflate(layoutInflater)
    setContentView(binding.root)
    mPlayListID = intent.getIntExtra(EXTRA_ID, -1)
    if (mPlayListID <= 0) {
      ToastUtil.show(this, R.string.add_error, Toast.LENGTH_SHORT)
      return
    }
    mPlayListName = intent.getStringExtra(EXTRA_NAME)
    adapter = SongChooseAdapter(R.layout.item_song_choose, OnSongChooseListener { isValid: Boolean ->
      binding.confirm.alpha = if (isValid) 1.0f else 0.6f
      binding.confirm.isClickable = isValid
    })
    binding.recyclerview.layoutManager = LinearLayoutManager(this)
    binding.recyclerview.adapter = adapter
    binding.recyclerview.itemAnimator = DefaultItemAnimator()
    binding.confirm.alpha = 0.6f
    binding.header.setBackgroundColor(materialPrimaryColor)
    for (view in arrayOf(binding.confirm, binding.cancel, binding.title)) {
      view.setTextColor(textColorPrimaryReverse)
    }
    binding.confirm.setOnClickListener { v: View -> onClick(v) }
    binding.cancel.setOnClickListener { v: View -> onClick(v) }
  }

  fun onClick(v: View) {
    val id = v.id
    if (id == R.id.cancel) {
      finish()
    } else if (id == R.id.confirm) {
      if (adapter?.checkedSong?.size == 0) {
        ToastUtil.show(this, R.string.choose_no_song)
        return
      }
      getInstance()
          .insertToPlayList(adapter!!.checkedSong, mPlayListID.toLong())
          .compose(RxUtil.applySingleScheduler())
          .subscribe({ num: Int? ->
            ToastUtil.show(this, getString(R.string.add_song_playlist_success, num,
                mPlayListName))
            finish()
          }) { throwable: Throwable? -> finish() }
    }
  }

  override val loaderId: Int = LoaderIds.ACTIVITY_SONGCHOOSE

  override val loader: Loader<List<Song>>
    get() = AsyncSongLoader(this)

  override fun saveSortOrder(sortOrder: String) {

  }

  private class AsyncSongLoader(context: Context) : AppWrappedAsyncTaskLoader<List<Song>>(context) {
    override fun loadInBackground(): List<Song> {
      return getAllSong()
    }
  }

  companion object {
    val TAG = SongChooseActivity::class.java.simpleName
    const val EXTRA_NAME = "PlayListName"
    const val EXTRA_ID = "PlayListID"
    fun start(context: Context, playListId: Int, playListName: String?) {
      val intent = Intent(context, SongChooseActivity::class.java)
      intent.putExtra(EXTRA_ID, playListId)
      intent.putExtra(EXTRA_NAME, playListName)
      context.startActivity(intent)
    }
  }
}