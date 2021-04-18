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
import remix.myplayer.util.MusicUtil

class HistoryActivity : LibraryActivity<Song, SongAdapter>() {

  private lateinit var binding: ActivityHistoryBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityHistoryBinding.inflate(layoutInflater)
    setContentView(binding.root)

    mAdapter = SongAdapter(R.layout.item_song_recycle, mChoice, binding.recyclerview)
    mChoice.adapter = mAdapter
    mAdapter?.onItemClickListener = object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        mAdapter?.let { adapter ->
          val song = adapter.dataList[position]
          if (!mChoice.click(position, song)) {
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
        mChoice.longClick(position, mAdapter?.dataList?.get(position))
      }
    }

    binding.recyclerview.layoutManager = LinearLayoutManager(this)
    binding.recyclerview.itemAnimator = DefaultItemAnimator()
    binding.recyclerview.adapter = mAdapter

    setUpToolbar(getString(R.string.drawer_history))
  }

  override val loader: Loader<List<Song>>
    get() = AsyncHistorySongLoader(this)

  override val loaderId: Int = ACTIVITY_HISTORY

  private class AsyncHistorySongLoader(context: Context) : AppWrappedAsyncTaskLoader<List<Song>>(context) {
    override fun loadInBackground(): List<Song> {
      return DatabaseRepository.getInstance().getHistorySongs().blockingGet()
    }
  }
}