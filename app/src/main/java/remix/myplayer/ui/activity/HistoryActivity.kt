package remix.myplayer.ui.activity

import android.content.Context
import android.content.Loader
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.ActivityHistoryBinding
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.helper.MusicServiceRemote.setPlayQueue
import remix.myplayer.misc.asynctask.AppWrappedAsyncTaskLoader
import remix.myplayer.misc.interfaces.LoaderIds.Companion.ACTIVITY_HISTORY
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.ui.adapter.SongAdapter
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.MusicUtil

class HistoryActivity : LibraryActivity<Song, SongAdapter>() {

  private lateinit var binding: ActivityHistoryBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityHistoryBinding.inflate(layoutInflater)
    setContentView(binding.root)

    mAdapter = SongAdapter(R.layout.item_song_recycle, mChoice, binding.recyclerview)
    mChoice.adapter = mAdapter
    mAdapter.setOnItemClickListener(object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        val song = mAdapter.datas[position]
        if (song != null && !mChoice.click(position, song)) {
          val songs = mAdapter.datas
          if (songs == null || songs.isEmpty()) {
            return
          }
          setPlayQueue(songs, MusicUtil.makeCmdIntent(Command.PLAYSELECTEDSONG)
              .putExtra(MusicService.EXTRA_POSITION, position))
        }
      }

      override fun onItemLongClick(view: View, position: Int) {
        mChoice.longClick(position, mAdapter.datas[position])
      }
    })

    binding.recyclerview.layoutManager = LinearLayoutManager(this)
    binding.recyclerview.itemAnimator = DefaultItemAnimator()
    binding.recyclerview.adapter = mAdapter

    setUpToolbar(getString(R.string.drawer_history))
  }

  override fun onLoadFinished(loader: Loader<List<Song>>, data: List<Song>?) {
    super.onLoadFinished(loader, data)
  }

  override fun getLoaderId(): Int {
    return ACTIVITY_HISTORY
  }

  override fun getLoader(): Loader<List<Song>> {
    return AsyncHistorySongLoader(this)
  }

  private class AsyncHistorySongLoader(context: Context) : AppWrappedAsyncTaskLoader<List<Song>>(context) {
    override fun loadInBackground(): List<Song> {
      return DatabaseRepository.getInstance().getHistorySongs().blockingGet()
    }
  }
}