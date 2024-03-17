package remix.myplayer.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.loader.content.Loader
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.FragmentSongBinding
import remix.myplayer.helper.MusicServiceRemote.getCurrentSong
import remix.myplayer.helper.MusicServiceRemote.isPlaying
import remix.myplayer.helper.MusicServiceRemote.setPlayQueue
import remix.myplayer.misc.asynctask.WrappedAsyncTaskLoader
import remix.myplayer.misc.interfaces.LoaderIds
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.activity.MainActivity
import remix.myplayer.ui.adapter.SongAdapter
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.MediaStoreUtil.getAllSong
import remix.myplayer.util.MusicUtil

/**
 * Created by Remix on 2015/11/30.
 */
/**
 * 全部歌曲的Fragment
 */
class SongFragment : LibraryFragment<Song, SongAdapter, FragmentSongBinding>() {
  override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentSongBinding
    get() = FragmentSongBinding::inflate

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pageName = TAG
  }

  override fun initAdapter() {
    adapter = SongAdapter(R.layout.item_song_recycle, multiChoice, binding.locationRecyclerView)
    adapter.onItemClickListener = object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        val song = adapter.dataList[position]
        if (userVisibleHint && !multiChoice.click(position, song)) {
          if (isPlaying() && song == getCurrentSong()) {
            if (requireActivity() is MainActivity) {
              (requireActivity() as MainActivity).toPlayerActivity()
            }
          } else {
            //设置正在播放列表
            val songs = adapter.dataList
            if (songs.isEmpty()) {
              return
            }
            setPlayQueue(songs, MusicUtil.makeCmdIntent(Command.PLAYSELECTEDSONG)
                .putExtra(MusicService.EXTRA_POSITION, position))
          }
        }
      }

      override fun onItemLongClick(view: View, position: Int) {
        if (userVisibleHint) {
          multiChoice.longClick(position, adapter.dataList.get(position))
        }
      }
    }
  }

  override fun initView() {
    binding.locationRecyclerView.layoutManager = LinearLayoutManager(context)
    binding.locationRecyclerView.itemAnimator = DefaultItemAnimator()
    binding.locationRecyclerView.adapter = adapter
    binding.locationRecyclerView.setHasFixedSize(true)

    val accentColor = ThemeStore.accentColor
    binding.locationRecyclerView.setBubbleColor(accentColor)
    binding.locationRecyclerView.setHandleColor(accentColor)
    binding.locationRecyclerView.setBubbleTextColor(
      if (ColorUtil.isColorLight(accentColor)) {
        ColorUtil.getColor(R.color.light_text_color_primary)
      } else {
        ColorUtil.getColor(R.color.dark_text_color_primary)
      }
    )
  }

  override fun loader(): Loader<List<Song>> {
    return AsyncSongLoader(requireContext())
  }

  override val loaderId: Int = LoaderIds.FRAGMENT_SONG

//  override val adapter: SongAdapter? = adapter

  override fun onMetaChanged() {
    super.onMetaChanged()
    adapter.updatePlayingSong()
  }

  fun scrollToCurrent() {
    binding.locationRecyclerView.smoothScrollToCurrentSong(adapter.dataList)
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