package remix.myplayer.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Loader
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.ActivitySearchBinding
import remix.myplayer.misc.asynctask.AppWrappedAsyncTaskLoader
import remix.myplayer.misc.interfaces.LoaderIds
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.ui.adapter.SearchAdapter
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.MusicUtil
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util

/**
 * Created by taeja on 16-1-22.
 */
/**
 * 搜索界面，根据关键字，搜索歌曲名，艺术家，专辑中的记录
 */
class SearchActivity : LibraryActivity<Song, SearchAdapter>(), SearchView.OnQueryTextListener {
  private val allSongs: MutableList<Song> = ArrayList()
  lateinit var binding: ActivitySearchBinding

  //搜索的关键字
  private var key: String = ""
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivitySearchBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setUpToolbar("")

    loadSongs()
    adapter = SearchAdapter(choice, R.layout.item_search_reulst)
    choice.adapter = adapter
    adapter?.onItemClickListener = object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        adapter?.let { adapter ->
          if (position >= 0 && position < adapter.getDataList().size) {
            val song = adapter.getDataList()[position]
            if (!choice.click(position, song)) {
              Util.sendLocalBroadcast(MusicUtil.makeCmdIntent(Command.PLAY_TEMP)
                  .putExtra(MusicService.EXTRA_SONG, adapter.getDataList()[position]))
            }
          } else {
            ToastUtil.show(this@SearchActivity, R.string.illegal_arg)
          }
        }
      }

      override fun onItemLongClick(view: View, position: Int) {
        val song = adapter?.dataList?.get(position) ?: return
        if (choice.longClick(position, song)) {
          Util.hideKeyboard(window.decorView)
        }
      }
    }
    binding.searchResultNative.adapter = adapter
    binding.searchResultNative.layoutManager = LinearLayoutManager(this)
    binding.searchResultNative.itemAnimator = DefaultItemAnimator()
    updateUI()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    super.onCreateOptionsMenu(menu)
    val searchItem = menu.findItem(R.id.search)
    searchItem.expandActionView()
    val searchView = searchItem.actionView as SearchView
    searchView.queryHint = getString(R.string.search_hint)
    searchView.maxWidth = Int.MAX_VALUE

    //去掉搜索图标
    try {
      val mDrawable = SearchView::class.java.getDeclaredField("mSearchHintIcon")
      mDrawable.isAccessible = true
      val drawable = mDrawable[searchView] as Drawable
      drawable.setBounds(0, 0, 0, 0)
    } catch (e: Exception) {
      e.printStackTrace()
    }
    searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
      override fun onMenuItemActionExpand(item: MenuItem): Boolean {
        return true
      }

      override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
        onBackPressed()
        return false
      }
    })
    searchView.setQuery(key, false)
    searchView.post { searchView.setOnQueryTextListener(this@SearchActivity) }
    return true
  }

  override fun getMenuLayoutId(): Int {
    return R.menu.menu_search
  }

  override fun onLoadFinished(loader: Loader<List<Song>>, data: List<Song>?) {
    super.onLoadFinished(loader, data)
    updateUI()
  }

  /**
   * 更新界面
   */
  private fun updateUI() {
    adapter?.let { adapter ->
      binding.searchResultNative.visibility = if (adapter.getDataList().isNotEmpty()) View.VISIBLE else View.GONE
      binding.searchResultBlank.visibility = if (adapter.getDataList().isNotEmpty()) View.GONE else View.VISIBLE
    }
  }

  override val loader: Loader<List<Song>>
    get() = AsyncSearchLoader(this, key, allSongs)
  override val loaderId: Int = LoaderIds.ACTIVITY_SEARCH

  /**
   * 搜索歌曲名，专辑，艺术家中包含该关键的记录
   *
   * @param newKey 搜索关键字
   */
  private fun search(newKey: String) {
    this.key = newKey
    loaderManager.restartLoader(LoaderIds.ACTIVITY_SEARCH, null, this)
  }

  override fun onQueryTextSubmit(newKey: String): Boolean {
    if (newKey != key) {
      search(newKey)
      return true
    }
    return false
  }

  override fun onQueryTextChange(newKey: String): Boolean {
    if (newKey != key) {
      search(newKey)
      return true
    }
    return false
  }

  override fun onMediaStoreChanged() {
    super.onMediaStoreChanged()
    loadSongs()
  }

  override fun onResume() {
    super.onResume()
    loadSongs()
  }

  @SuppressLint("CheckResult")
  private fun loadSongs() {
    Completable
        .fromAction {
          allSongs.clear()
          allSongs.addAll(MediaStoreUtil.getAllSong())
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe {
          search(key)
        }
  }

  private class AsyncSearchLoader(context: Context, private val key: String, private val allSongs: List<Song>) : AppWrappedAsyncTaskLoader<List<Song>>(context) {
    override fun loadInBackground(): List<Song> {
      if (TextUtils.isEmpty(key) || allSongs.isEmpty()) {
        return ArrayList()
      }

      val songs = MediaStoreUtil.getSongs(MediaStore.Audio.Media.TITLE + " LIKE ? OR " +
          MediaStore.Audio.ArtistColumns.ARTIST + " LIKE ? OR " +
          MediaStore.Audio.AlbumColumns.ALBUM + " LIKE ?", arrayOf("%$key%", "%$key%", "%$key%"))

      val allSongs = ArrayList(allSongs)
      return songs.filter {
        allSongs.contains(it)
      }
    }
  }

}