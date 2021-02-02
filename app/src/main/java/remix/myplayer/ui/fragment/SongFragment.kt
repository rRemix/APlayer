package remix.myplayer.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import butterknife.BindView
import kotlinx.android.synthetic.main.fragment_song.*
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.helper.MusicServiceRemote.getCurrentSong
import remix.myplayer.helper.MusicServiceRemote.isPlaying
import remix.myplayer.helper.MusicServiceRemote.setPlayQueue
import remix.myplayer.misc.asynctask.WrappedAsyncTaskLoader
import remix.myplayer.misc.interfaces.LoaderIds
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.ui.activity.MainActivity
import remix.myplayer.ui.adapter.SongAdapter
import remix.myplayer.ui.widget.fastcroll_recyclerview.LocationRecyclerView
import remix.myplayer.util.MediaStoreUtil.getAllSong
import remix.myplayer.util.MusicUtil

/**
 * Created by Remix on 2015/11/30.
 */
/**
 * 全部歌曲的Fragment
 */
class SongFragment : LibraryFragment<Song, SongAdapter>() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mPageName = TAG
  }

  override val layoutID: Int = R.layout.fragment_song

  override fun initAdapter() {
    mAdapter = SongAdapter(R.layout.item_song_recycle, mChoice, location_recyclerView)
    mAdapter?.setOnItemClickListener(object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        val song = mAdapter?.datas?.get(position) ?: return
        if (userVisibleHint && mChoice?.click(position, song) == false) {
          if (isPlaying() && song == getCurrentSong()) {
            if (requireActivity() is MainActivity) {
              (requireActivity() as MainActivity).toPlayerActivity()
            }
          } else {
            //设置正在播放列表
            val songs = mAdapter?.datas
            if (songs == null || songs.isEmpty()) {
              return
            }
            setPlayQueue(songs, MusicUtil.makeCmdIntent(Command.PLAYSELECTEDSONG)
                .putExtra(MusicService.EXTRA_POSITION, position))
          }
        }
      }

      override fun onItemLongClick(view: View, position: Int) {
        if (userVisibleHint) {
          mChoice?.longClick(position, mAdapter?.datas?.get(position))
        }
      }
    })
  }

  override fun initView() {
    location_recyclerView.layoutManager = LinearLayoutManager(context)
    location_recyclerView.itemAnimator = DefaultItemAnimator()
    location_recyclerView.adapter = mAdapter
    location_recyclerView.setHasFixedSize(true)
  }

  override fun loader(): Loader<List<Song>> {
    return AsyncSongLoader(mContext)
  }

  override val loaderId: Int = LoaderIds.SONG_FRAGMENT

  override val adapter: SongAdapter? = mAdapter

  override fun onMetaChanged() {
    super.onMetaChanged()
    mAdapter?.updatePlayingSong()
  }

  fun scrollToCurrent() {
    location_recyclerView.smoothScrollToCurrentSong(mAdapter?.datas ?: return)
  }

  private class AsyncSongLoader(context: Context?) : WrappedAsyncTaskLoader<List<Song>>(context) {
    override fun loadInBackground(): List<Song> {
      return getAllSong()
    }
  }

  companion object {
    val TAG = SongFragment::class.java.simpleName
  }
}