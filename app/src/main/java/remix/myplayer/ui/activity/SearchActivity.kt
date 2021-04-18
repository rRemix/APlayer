package remix.myplayer.ui.activity

import android.content.Context
import android.content.Loader
import android.database.Cursor
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
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.ActivitySearchBinding
import remix.myplayer.misc.asynctask.AppWrappedAsyncTaskLoader
import remix.myplayer.misc.interfaces.LoaderIds
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.ui.adapter.SearchAdapter
import remix.myplayer.util.*
import remix.myplayer.util.MediaStoreUtil.baseSelection
import remix.myplayer.util.MediaStoreUtil.getSongInfo
import java.util.*

/**
 * Created by taeja on 16-1-22.
 */
/**
 * 搜索界面，根据关键字，搜索歌曲名，艺术家，专辑中的记录
 */
class SearchActivity : LibraryActivity<Song, SearchAdapter>(), SearchView.OnQueryTextListener {
  lateinit var binding: ActivitySearchBinding

  //搜索的关键字
  private var mkey: String = ""
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivitySearchBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setUpToolbar("")
    mAdapter = SearchAdapter(R.layout.item_search_reulst)
    mAdapter?.onItemClickListener = object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        mAdapter?.let { adapter ->
          if (position >= 0 && position < adapter.getDataList().size) {
            Util.sendLocalBroadcast(MusicUtil.makeCmdIntent(Command.PLAY_TEMP)
                .putExtra(MusicService.EXTRA_SONG, adapter.getDataList()[position]))
          } else {
            ToastUtil.show(this@SearchActivity, R.string.illegal_arg)
          }
        }

      }

      override fun onItemLongClick(view: View, position: Int) {}
    }
    binding.searchResultNative.adapter = mAdapter
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
    searchView.setQuery(mkey, false)
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
    mAdapter?.let { adapter ->
      binding.searchResultNative.visibility = if (adapter.getDataList().isNotEmpty()) View.VISIBLE else View.GONE
      binding.searchResultBlank.visibility = if (adapter.getDataList().isNotEmpty()) View.GONE else View.VISIBLE
    }
  }

  override val loader: Loader<List<Song>>
    get() = AsyncSearchLoader(this, mkey)
  override val loaderId: Int = LoaderIds.ACTIVITY_SEARCH

  /**
   * 搜索歌曲名，专辑，艺术家中包含该关键的记录
   *
   * @param key 搜索关键字
   */
  private fun search(key: String) {
    mkey = key
    loaderManager.restartLoader(LoaderIds.ACTIVITY_SEARCH, null, this)
  }

  override fun onQueryTextSubmit(key: String): Boolean {
    if (key != mkey) {
      search(key)
      return true
    }
    return false
  }

  override fun onQueryTextChange(key: String): Boolean {
    if (key != mkey) {
      search(key)
      return true
    }
    return false
  }

  private class AsyncSearchLoader(context: Context, private val key: String) : AppWrappedAsyncTaskLoader<List<Song>>(context) {
    override fun loadInBackground(): List<Song> {
      if (TextUtils.isEmpty(key)) {
        return ArrayList()
      }
      var cursor: Cursor? = null
      val songs: MutableList<Song> = ArrayList()
      try {
        val selection = (MediaStore.Audio.Media.TITLE + " like ? " + "or " + MediaStore.Audio.Media.ARTIST
            + " like ? "
            + "or " + MediaStore.Audio.Media.ALBUM + " like ? and " + baseSelection)
        cursor = context.contentResolver
            .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                selection, arrayOf("%$key%", "%$key%", "%$key%"), null)
        if (cursor != null && cursor.count > 0) {
          val blackList = SPUtil.getStringSet(App.context, SPUtil.SETTING_KEY.NAME,
              SPUtil.SETTING_KEY.BLACKLIST_SONG)
          while (cursor.moveToNext()) {
            if (!blackList
                    .contains(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID)))) {
              songs.add(getSongInfo(cursor))
            }
          }
        }
      } finally {
        if (cursor != null && !cursor.isClosed) {
          cursor.close()
        }
      }
      return songs
    }
  }

}