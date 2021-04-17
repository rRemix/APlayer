package remix.myplayer.ui.activity

import android.content.Context
import android.content.Loader
import android.os.Bundle
import android.os.Message
import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.ActivityRecentlyBinding
import remix.myplayer.helper.MusicServiceRemote.setPlayQueue
import remix.myplayer.misc.asynctask.AppWrappedAsyncTaskLoader
import remix.myplayer.misc.handler.MsgHandler
import remix.myplayer.misc.handler.OnHandleMessage
import remix.myplayer.misc.interfaces.LoaderIds
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.ui.adapter.SongAdapter
import remix.myplayer.util.MediaStoreUtil.getLastAddedSong
import remix.myplayer.util.MusicUtil

/**
 * Created by taeja on 16-3-4.
 */
/**
 * 最近添加歌曲的界面 目前为最近7天添加
 */
class RecentlyActivity : LibraryActivity<Song, SongAdapter>() {
  private val binding: ActivityRecentlyBinding by lazy {
    ActivityRecentlyBinding.inflate(layoutInflater)
  }
  private val handler by lazy {
    MsgHandler(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)
    mAdapter = SongAdapter(R.layout.item_song_recycle, mChoice, binding.recyclerview)
    mChoice.adapter = mAdapter
    mAdapter?.onItemClickListener = object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        val song = mAdapter?.getDataList()?.get(position) ?: return

        if (!mChoice.click(position, song)) {
          val songs = mAdapter?.getDataList() ?: return
          if (songs.isEmpty()) {
            return
          }
          setPlayQueue(songs, MusicUtil.makeCmdIntent(Command.PLAYSELECTEDSONG)
              .putExtra(MusicService.EXTRA_POSITION, position))
        }
      }

      override fun onItemLongClick(view: View, position: Int) {
        mChoice.longClick(position, mAdapter!!.getDataList()[position])
      }
    }
    binding.recyclerview.layoutManager = LinearLayoutManager(this)
    binding.recyclerview.itemAnimator = DefaultItemAnimator()
    binding.recyclerview.adapter = mAdapter
    setUpToolbar(getString(R.string.recently))
  }

  override fun onLoadFinished(loader: Loader<List<Song>>, data: List<Song>?) {
    super.onLoadFinished(loader, data)
    if (data != null) {
      binding.recyclerview.visibility = if (data.isNotEmpty()) View.VISIBLE else View.GONE
      binding.recentlyPlaceholder.visibility = if (data.isNotEmpty()) View.GONE else View.VISIBLE
    } else {
      binding.recyclerview.visibility = View.GONE
      binding.recentlyPlaceholder.visibility = View.VISIBLE
    }
  }

  @OnHandleMessage
  fun handleMessage(msg: Message) {
    when (msg.what) {
      MSG_RESET_MULTI -> if (mAdapter != null) {
        mAdapter!!.notifyDataSetChanged()
      }
      MSG_UPDATE_ADAPTER -> if (mAdapter != null) {
        mAdapter!!.notifyDataSetChanged()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    handler.remove()
  }

  override fun onMediaStoreChanged() {
    super.onMediaStoreChanged()
  }

  override fun getLoader(): Loader<List<Song>> {
    return AsyncRecentlySongLoader(this)
  }

  override fun getLoaderId(): Int {
    return LoaderIds.ACTIVITY_RECENTLY
  }

  private class AsyncRecentlySongLoader(context: Context) : AppWrappedAsyncTaskLoader<List<Song>>(context) {
    override fun loadInBackground(): List<Song> {
      return lastAddedSongs
    }

    private val lastAddedSongs: List<Song>
      get() = getLastAddedSong()
  }

  companion object {
    val TAG = RecentlyActivity::class.java.simpleName
  }
}