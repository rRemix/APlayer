package remix.myplayer.ui.activity

import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.ActivityHistoryBinding
import remix.myplayer.db.room.AppDatabase
import remix.myplayer.helper.MusicServiceRemote.setPlayQueue
import remix.myplayer.helper.SortOrder
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.ui.adapter.SongAdapter
import remix.myplayer.ui.misc.MultipleChoice
import remix.myplayer.util.Constants
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.SPUtil

class HistoryActivity : MenuActivity() {

  private var job: Job? = null
  private lateinit var binding: ActivityHistoryBinding
  private val adapter by lazy {
    SongAdapter(R.layout.item_song_recycle, choice, binding.recyclerview)
  }

  private val choice by lazy {
    MultipleChoice<Song>(this, Constants.SONG)
  }

  override fun getMenuLayoutId(): Int {
    return R.menu.menu_history
  }

  override fun saveSortOrder(sortOrder: String) {
    SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.HISTORY_SORT_ORDER, sortOrder)
    loadHistory()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    super.onCreateOptionsMenu(menu)

    val sortOrder = SPUtil.getValue(
      this,
      SPUtil.SETTING_KEY.NAME,
      SPUtil.SETTING_KEY.HISTORY_SORT_ORDER,
      SortOrder.PLAY_COUNT_DESC
    )
    setUpMenuItem(menu, sortOrder)
    return true
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityHistoryBinding.inflate(layoutInflater)
    setContentView(binding.root)

    choice.adapter = adapter
    adapter.onItemClickListener = object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        val song = adapter.dataList[position]
        if (!choice.click(position, song)) {
          val songs = adapter.dataList
          if (songs.isEmpty()) {
            return
          }
          setPlayQueue(
            songs, MusicUtil.makeCmdIntent(Command.PLAYSELECTEDSONG)
              .putExtra(MusicService.EXTRA_POSITION, position)
          )
        }

      }

      override fun onItemLongClick(view: View, position: Int) {
        choice.longClick(position, adapter.dataList[position])
      }
    }

    binding.recyclerview.layoutManager = LinearLayoutManager(this)
    binding.recyclerview.itemAnimator = DefaultItemAnimator()
    binding.recyclerview.adapter = adapter

    setUpToolbar(getString(R.string.drawer_history))

    loadHistory()
  }

  private fun loadHistory() {
    job?.cancel()
    job = launch(Dispatchers.IO) {
      AppDatabase.getInstance(App.context.applicationContext)
        .historyDao()
        .selectAll(
          SPUtil.getValue(
            this@HistoryActivity,
            SPUtil.SETTING_KEY.NAME,
            SPUtil.SETTING_KEY.HISTORY_SORT_ORDER,
            SortOrder.PLAY_COUNT_DESC
          ) != SortOrder.PLAY_COUNT_DESC
        )
        .map {
          val songs = ArrayList<Song>()
          for (history in it) {
            val song = MediaStoreUtil.getSongById(history.audio_id)
            if (song != Song.EMPTY_SONG) {
              songs.add(song)
            }
          }
          songs
        }
        .collect {
          withContext(Dispatchers.Main) {
            adapter.setDataList(it)
          }
        }
    }
  }
}