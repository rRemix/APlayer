package remix.myplayer.ui.activity

import android.content.Context
import android.content.Intent
import android.content.Loader
import android.os.Bundle
import android.os.Message
import android.text.TextUtils
import android.view.Menu
import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import remix.myplayer.R
import remix.myplayer.bean.mp3.APlayerModel
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.ActivityChildHolderBinding
import remix.myplayer.db.room.DatabaseRepository.Companion.getInstance
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.helper.MusicServiceRemote.getCurrentSong
import remix.myplayer.helper.MusicServiceRemote.isPlaying
import remix.myplayer.helper.MusicServiceRemote.setPlayQueue
import remix.myplayer.helper.SortOrder
import remix.myplayer.misc.asynctask.AppWrappedAsyncTaskLoader
import remix.myplayer.misc.handler.MsgHandler
import remix.myplayer.misc.handler.OnHandleMessage
import remix.myplayer.misc.interfaces.LoaderIds
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.theme.ThemeStore.accentColor
import remix.myplayer.ui.adapter.ChildHolderAdapter
import remix.myplayer.ui.fragment.BottomActionBarFragment
import remix.myplayer.ui.misc.MultipleChoice
import remix.myplayer.util.*
import remix.myplayer.util.MediaStoreUtil.getSongsByArtistIdOrAlbumId
import remix.myplayer.util.MediaStoreUtil.getSongsByGenreId
import remix.myplayer.util.MediaStoreUtil.getSongsByParentPath
import remix.myplayer.util.SPUtil.SETTING_KEY
import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by Remix on 2015/12/4.
 */
/**
 * 专辑、艺术家、文件夹、播放列表详情
 */
class ChildHolderActivity : LibraryActivity<Song, ChildHolderAdapter>() {
  private val binding by lazy {
    ActivityChildHolderBinding.inflate(layoutInflater)
  }

  //获得歌曲信息列表的参数
  private var type = 0
  private var key: String = ""
  private var title: String = ""

  //当前排序
  private var sortOrder: String = ""
  private val refreshHandler by lazy {
    MsgHandler(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(binding.root)

    //参数id，类型，标题
    key = intent.getStringExtra(EXTRA_KEY) ?: ""
    title = intent.getStringExtra(EXTRA_TITLE) ?: ""
    type = intent.getIntExtra(EXTRA_TYPE, -1)
    if (key.isEmpty() || title.isEmpty() || type == -1) {
      ToastUtil.show(this, R.string.illegal_arg)
      finish()
      return
    }
    choice = MultipleChoice(this,
        if (type == Constants.PLAYLIST) Constants.PLAYLISTSONG else Constants.SONG)
    adapter = ChildHolderAdapter(R.layout.item_song_recycle, type, title, choice, binding.childHolderRecyclerView)
    choice.adapter = adapter
    if (TextUtils.isDigitsOnly(key)) {
      choice.extra = key.toLong()
    }
    adapter?.onItemClickListener = object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        val song = adapter?.dataList?.get(position)
        if (isPlaying() && song == getCurrentSong()) {
          val bottomActionBarFragment = supportFragmentManager
              .findFragmentByTag("BottomActionBarFragment") as BottomActionBarFragment?
          bottomActionBarFragment?.startPlayerActivity()
        } else {
          if (!choice.click(position, song)) {
            val songs = adapter?.dataList
            if (songs.isNullOrEmpty()) {
              return
            }
            //设置正在播放列表
            setPlayQueue(songs, MusicUtil.makeCmdIntent(Command.PLAYSELECTEDSONG)
                .putExtra(MusicService.EXTRA_POSITION, position))
          }
        }
      }

      override fun onItemLongClick(view: View, position: Int) {
        choice.longClick(position, adapter!!.dataList[position])
      }
    }
    binding.childHolderRecyclerView.layoutManager = LinearLayoutManager(this)
    binding.childHolderRecyclerView.itemAnimator = DefaultItemAnimator()
    binding.childHolderRecyclerView.adapter = adapter
    val accentColor = accentColor
    binding.childHolderRecyclerView.setBubbleColor(accentColor)
    binding.childHolderRecyclerView.setHandleColor(accentColor)
    binding.childHolderRecyclerView.setBubbleTextColor(ColorUtil.getColor(if (ColorUtil.isColorLight(accentColor)) R.color.light_text_color_primary else R.color.dark_text_color_primary))

    //标题
    var title = ""
    if (type != Constants.FOLDER) {
      if (title.contains("unknown")) {
        if (type == Constants.ARTIST) {
          title = getString(R.string.unknown_artist)
        } else if (type == Constants.ALBUM) {
          title = getString(R.string.unknown_album)
        }
      }
    } else {
      title = title.substring(title.lastIndexOf("/") + 1)
    }
    //初始化toolbar
    setUpToolbar(title)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    super.onCreateOptionsMenu(menu)
    sortOrder = when (type) {
      Constants.PLAYLIST -> {
        SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.CHILD_PLAYLIST_SONG_SORT_ORDER,
            SortOrder.SONG_A_Z)
      }
      Constants.ALBUM -> {
        SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.CHILD_ALBUM_SONG_SORT_ORDER,
            SortOrder.TRACK_NUMBER)
      }
      Constants.ARTIST -> {
        SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.CHILD_ARTIST_SONG_SORT_ORDER,
            SortOrder.SONG_A_Z)
      }
      else -> {
        SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.CHILD_FOLDER_SONG_SORT_ORDER,
            SortOrder.SONG_A_Z)
      }
    }
    if (TextUtils.isEmpty(sortOrder)) {
      return true
    }
    setUpMenuItem(menu, sortOrder)
    return true
  }

  override fun saveSortOrder(sortOrder: String) {
    var update = false
    if (type == Constants.PLAYLIST) {
      //手动排序或者排序发生变化
      if (sortOrder.equals(SortOrder.PLAYLIST_SONG_CUSTOM, ignoreCase = true) ||
          !this.sortOrder.equals(sortOrder, ignoreCase = true)) {
        //选择的是手动排序
        if (sortOrder.equals(SortOrder.PLAYLIST_SONG_CUSTOM, ignoreCase = true)) {
          CustomSortActivity.start(this, intent.getSerializableExtra(EXTRA_MODEL) as PlayList, ArrayList(adapter!!.dataList))
        } else {
          update = true
        }
      }
    } else {
      //排序发生变化
      if (!this.sortOrder.equals(sortOrder, ignoreCase = true)) {
        update = true
      }
    }
    when (type) {
      Constants.PLAYLIST -> {
        SPUtil.putValue(this, SETTING_KEY.NAME, SETTING_KEY.CHILD_PLAYLIST_SONG_SORT_ORDER, sortOrder)
      }
      Constants.ALBUM -> {
        SPUtil.putValue(this, SETTING_KEY.NAME, SETTING_KEY.CHILD_ALBUM_SONG_SORT_ORDER, sortOrder)
      }
      Constants.ARTIST -> {
        SPUtil.putValue(this, SETTING_KEY.NAME, SETTING_KEY.CHILD_ARTIST_SONG_SORT_ORDER, sortOrder)
      }
      Constants.GENRE -> {
        SPUtil.putValue(this, SETTING_KEY.NAME, SETTING_KEY.CHILD_GENRE_SONG_SORT_ORDER, sortOrder)
      }
      else -> {
        SPUtil.putValue(this, SETTING_KEY.NAME, SETTING_KEY.CHILD_FOLDER_SONG_SORT_ORDER, sortOrder)
      }
    }
    this.sortOrder = sortOrder
    if (update) {
      onMediaStoreChanged()
    }
  }

  override fun getMenuLayoutId(): Int {
    return if (type == Constants.PLAYLIST) R.menu.menu_child_for_playlist else if (type == Constants.ALBUM) R.menu.menu_child_for_album else if (type == Constants.ARTIST) R.menu.menu_child_for_artist else R.menu.menu_child_for_folder
  }

  override val loaderId: Int = LoaderIds.ACTIVITY_CHILDHOLDER

  override val loader: Loader<List<Song>>
    get() = AsyncChildSongLoader(this)


  override fun onLoadFinished(loader: Loader<List<Song>>, data: List<Song>?) {
    super.onLoadFinished(loader, data)
    binding.childholderItemNum.text = getString(R.string.song_count, data?.size ?: 0)
  }

  override fun onServiceConnected(service: MusicService) {
    super.onServiceConnected(service)
  }

  override fun onPlayListChanged(name: String) {
    super.onPlayListChanged(name)
    if (name == PlayList.TABLE_NAME) {
      onMediaStoreChanged()
    }
  }

  override fun onMediaStoreChanged() {
    super.onMediaStoreChanged()
  }

  override fun onTagChanged(oldSong: Song, newSong: Song) {
    super.onTagChanged(oldSong, newSong)
    //    if (mType == Constants.ARTIST || mType == Constants.ALBUM) {
//      mId = mType == Constants.ARTIST ? newSong.getArtistId() : newSong.getAlbumId();
//      Title = mType == Constants.ARTIST ? newSong.getArtist() : newSong.getAlbum();
//      mToolBar.setTitle(Title);
//    }
  }

  /**
   * 根据参数(专辑id 歌手id 文件夹名 播放列表名)获得对应的歌曲信息列表
   *
   * @return 对应歌曲信息列表
   */
  private val songs: List<Song>
    get() {
      if (TextUtils.isEmpty(key)) {
        return Collections.emptyList()
      }
      when (type) {
        Constants.ALBUM -> return getSongsByArtistIdOrAlbumId(key.toLong(), Constants.ALBUM)
        Constants.ARTIST -> return getSongsByArtistIdOrAlbumId(key.toLong(), Constants.ARTIST)
        Constants.GENRE -> return getSongsByGenreId(key.toLong(), SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.CHILD_GENRE_SONG_SORT_ORDER, SortOrder.SONG_A_Z))
        Constants.FOLDER -> return getSongsByParentPath(key)
        Constants.PLAYLIST -> return getInstance()
            .getPlayList(key.toLong())
            .flatMap { playList: PlayList? ->
              getInstance()
                  .getPlayListSongs(this, playList!!, false)
            }
            .blockingGet()
      }
      return ArrayList()
    }

  override fun onDestroy() {
    super.onDestroy()
    refreshHandler.remove()
  }

  @OnHandleMessage
  fun handleInternal(msg: Message) {
    when (msg.what) {
      MSG_RESET_MULTI -> adapter!!.notifyDataSetChanged()
    }
  }

  override fun onMetaChanged() {
    super.onMetaChanged()
    if (adapter != null) {
      adapter!!.updatePlayingSong()
    }
  }

  private class AsyncChildSongLoader(childHolderActivity: ChildHolderActivity) : AppWrappedAsyncTaskLoader<List<Song>>(childHolderActivity) {
    private val ref: WeakReference<ChildHolderActivity> = WeakReference(childHolderActivity)
    override fun loadInBackground(): List<Song> {
      return childSongs
    }

    private val childSongs: List<Song>
      get() {
        val activity = ref.get()
        var songs: List<Song>? = ArrayList()
        if (activity != null) {
          songs = activity.songs
        }
        return songs ?: ArrayList()
      }

  }

  companion object {
    private const val EXTRA_KEY = "key"
    private const val EXTRA_TYPE = "type"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_MODEL = "model"

    fun start(context: Context, type: Int, key: String?, title: String?, model: APlayerModel? = null) {
      val intent = Intent(context, ChildHolderActivity::class.java)
          .putExtra(EXTRA_KEY, key)
          .putExtra(EXTRA_TYPE, type)
          .putExtra(EXTRA_TITLE, title)
      if (model != null) {
        intent.putExtra(EXTRA_MODEL, model)
      }
      context.startActivity(intent)
    }
  }
}