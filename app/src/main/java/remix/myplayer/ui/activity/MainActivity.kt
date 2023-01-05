package remix.myplayer.ui.activity


import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.text.TextUtils
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager.widget.ViewPager
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.signature.ObjectKey
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringSystem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.soundcloud.android.crop.Crop
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_timer.view.*
import kotlinx.android.synthetic.main.navigation_header.*
import remix.myplayer.App
import remix.myplayer.App.Companion.IS_GOOGLEPLAY
import remix.myplayer.R
import remix.myplayer.bean.misc.CustomCover
import remix.myplayer.bean.misc.Library
import remix.myplayer.bean.mp3.Song
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.glide.GlideApp
import remix.myplayer.glide.UriFetcher
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.helper.SortOrder
import remix.myplayer.misc.cache.DiskCache
import remix.myplayer.misc.handler.MsgHandler
import remix.myplayer.misc.handler.OnHandleMessage
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.misc.menu.LibraryListener.Companion.EXTRA_COVER
import remix.myplayer.misc.receiver.ExitReceiver
import remix.myplayer.misc.update.DownloadService
import remix.myplayer.misc.update.DownloadService.Companion.ACTION_DISMISS_DIALOG
import remix.myplayer.misc.update.DownloadService.Companion.ACTION_DOWNLOAD_COMPLETE
import remix.myplayer.misc.update.DownloadService.Companion.ACTION_SHOW_DIALOG
import remix.myplayer.misc.update.UpdateAgent
import remix.myplayer.misc.update.UpdateListener
import remix.myplayer.service.MusicService
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.adapter.DrawerAdapter
import remix.myplayer.ui.adapter.MainPagerAdapter
import remix.myplayer.ui.fragment.*
import remix.myplayer.ui.misc.DoubleClickListener
import remix.myplayer.ui.misc.MultipleChoice
import remix.myplayer.util.*
import remix.myplayer.util.RxUtil.applySingleScheduler
import remix.myplayer.util.Util.hashKeyForDisk
import remix.myplayer.util.Util.installApk
import remix.myplayer.util.Util.registerLocalReceiver
import remix.myplayer.util.Util.unregisterLocalReceiver
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

/**
 *
 */
class MainActivity : MenuActivity(), View.OnClickListener {

  private val drawerAdapter by lazy {
    DrawerAdapter(R.layout.item_drawer)
  }
  private val pagerAdapter by lazy {
    MainPagerAdapter(supportFragmentManager)
  }

  private val handler by lazy {
    MsgHandler(this)
  }
  private val receiver by lazy {
    MainReceiver(this)
  }

  //当前选中的fragment
  private var currentFragment: LibraryFragment<*, *>? = null

  private var menuLayoutId = R.menu.menu_main

  /**
   * 判断安卓版本，请求安装权限或者直接安装
   *
   * @param activity
   * @param path
   */
  private var installPath: String? = null


  private var forceDialog: MaterialDialog? = null

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
  }

  override fun onResume() {
    super.onResume()
    if (hasNewIntent) {
      handler.postDelayed({ this.parseIntent() }, 500)
      handler.post {
        onMetaChanged()
      }
      hasNewIntent = false
    }
  }

  override fun onPause() {
    super.onPause()
  }

  override fun onDestroy() {
    super.onDestroy()
    unregisterLocalReceiver(receiver)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    val intentFilter = IntentFilter()
    //        intentFilter.addAction(ACTION_LOAD_FINISH);
    intentFilter.addAction(ACTION_DOWNLOAD_COMPLETE)
    intentFilter.addAction(ACTION_SHOW_DIALOG)
    intentFilter.addAction(ACTION_DISMISS_DIALOG)
    registerLocalReceiver(receiver, intentFilter)

    //初始化控件
    setUpToolbar()
    setUpPager()
    setUpTab()
    btn_add.setOnClickListener(this)
    //初始化测滑菜单
    setUpDrawerLayout()
    setUpViewColor()
    //handler
    handler.postDelayed({ this.checkUpdate() }, 500)

    //清除多选显示状态
    MultipleChoice.isActiveSomeWhere = false
  }

  override fun setStatusBarColor() {
    StatusBarUtil.setColorNoTranslucentForDrawerLayout(this,
        findViewById(R.id.drawer),
        ThemeStore.statusBarColor)
  }

  /**
   * 初始化toolbar
   */
  private fun setUpToolbar() {
    super.setUpToolbar("")
    toolbar?.setNavigationIcon(R.drawable.ic_menu_white_24dp)
    toolbar?.setNavigationOnClickListener { v -> drawer.openDrawer(navigation_view) }
  }

  /**
   * 新建播放列表
   */
  override fun onClick(v: View) {
    when (v.id) {
      R.id.btn_add -> {
        if (MultipleChoice.isActiveSomeWhere) {
          return
        }

        DatabaseRepository.getInstance()
            .getAllPlaylist()
            .compose<List<PlayList>>(applySingleScheduler<List<PlayList>>())
            .subscribe { playLists ->
              Theme.getBaseDialog(this)
                  .title(R.string.new_playlist)
                  .positiveText(R.string.create)
                  .negativeText(R.string.cancel)
                  .inputRange(1, 25)
                  .input("", getString(R.string.local_list) + playLists.size) { dialog, input ->
                    if (!TextUtils.isEmpty(input)) {
                      DatabaseRepository.getInstance()
                          .insertPlayList(input.toString())
                          .compose(applySingleScheduler())
                          .subscribe({ id ->
                            //跳转到添加歌曲界面
                            SongChooseActivity.start(this@MainActivity, id, input.toString())
                          }, { throwable ->
                            ToastUtil
                                .show(this, R.string.create_playlist_fail, throwable.toString())
                          })
                    }
                  }
                  .show()
            }
      }
      else -> {
      }
    }
  }

  //初始化ViewPager
  private fun setUpPager() {
    val libraryJson = SPUtil
        .getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LIBRARY, "")
    val libraries = if (TextUtils.isEmpty(libraryJson))
      ArrayList()
    else
      Gson().fromJson<ArrayList<Library>>(libraryJson, object : TypeToken<List<Library>>() {}.type)
    if (libraries.isEmpty()) {
      val defaultLibraries = Library.getDefaultLibrary()
      libraries.addAll(defaultLibraries)
      SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LIBRARY,
          Gson().toJson(defaultLibraries, object : TypeToken<List<Library>>() {}.type))
    }

    pagerAdapter.list = libraries
    menuLayoutId = parseMenuId(pagerAdapter.list[0].tag)
    //有且仅有一个tab
    if (libraries.size == 1) {
      if (libraries[0].isPlayList()) {
        showViewWithAnim(btn_add, true)
      }
      tabs.visibility = View.GONE
    } else {
      tabs.visibility = View.VISIBLE
    }

    view_pager.adapter = pagerAdapter
    view_pager.offscreenPageLimit = pagerAdapter.count - 1
    view_pager.currentItem = 0
    view_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
      override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

      override fun onPageSelected(position: Int) {
        val library = pagerAdapter.list[position]
        showViewWithAnim(btn_add, library.isPlayList())

        menuLayoutId = parseMenuId(pagerAdapter.list[position].tag)
        currentFragment = pagerAdapter.getFragment(position) as LibraryFragment<*, *>

        invalidateOptionsMenu()
      }


      override fun onPageScrollStateChanged(state: Int) {}
    })
    currentFragment = pagerAdapter.getFragment(0) as LibraryFragment<*, *>
  }

  fun parseMenuId(tag: Int): Int {
    return when (tag) {
      Library.TAG_SONG -> R.menu.menu_main
      Library.TAG_ALBUM -> R.menu.menu_album
      Library.TAG_ARTIST -> R.menu.menu_artist
      Library.TAG_PLAYLIST -> R.menu.menu_playlist
      Library.TAG_FOLDER -> R.menu.menu_folder
      else -> R.menu.menu_main_simple
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    super.onCreateOptionsMenu(menu)
    if (currentFragment is FolderFragment) {
      return true
    }
    var sortOrder = ""
    when (currentFragment) {
      is SongFragment -> sortOrder = SPUtil
          .getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER,
              SortOrder.SONG_A_Z)
      is AlbumFragment -> sortOrder = SPUtil
          .getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ALBUM_SORT_ORDER,
              SortOrder.ALBUM_A_Z)
      is ArtistFragment -> sortOrder = SPUtil
          .getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ARTIST_SORT_ORDER,
              SortOrder.ARTIST_A_Z)
      is PlayListFragment -> sortOrder = SPUtil
          .getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAYLIST_SORT_ORDER,
              SortOrder.PLAYLIST_DATE)
    }

    if (TextUtils.isEmpty(sortOrder)) {
      return true
    }
    setUpMenuItem(menu, sortOrder)
    return true
  }


  override fun getMenuLayoutId(): Int {
    return menuLayoutId
  }

  override fun saveSortOrder(sortOrder: String) {
    when (currentFragment) {
      is SongFragment -> SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER,
          sortOrder)
      is AlbumFragment -> SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ALBUM_SORT_ORDER,
          sortOrder)
      is ArtistFragment -> SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ARTIST_SORT_ORDER,
          sortOrder)
      is PlayListFragment -> SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAYLIST_SORT_ORDER,
          sortOrder)
    }
    currentFragment?.onMediaStoreChanged()
  }

  private fun showViewWithAnim(view: View, show: Boolean) {
    if (show) {
      if (view.visibility != View.VISIBLE) {
        view.visibility = View.VISIBLE
        SpringSystem.create().createSpring()
            .addListener(object : SimpleSpringListener() {
              override fun onSpringUpdate(spring: Spring?) {
                spring?.apply {
                  view.scaleX = currentValue.toFloat()
                  view.scaleY = currentValue.toFloat()
                }

              }
            }).endValue = 1.0
      }
    } else {
      view.visibility = View.GONE
    }

  }

  //初始化custontab
  private fun setUpTab() {
    //添加tab选项卡
    val isPrimaryColorCloseToWhite = ThemeStore.isMDColorCloseToWhite

    tabs.setBackgroundColor(ThemeStore.materialPrimaryColor)
    tabs.addTab(tabs.newTab().setText(R.string.tab_song))
    tabs.addTab(tabs.newTab().setText(R.string.tab_album))
    tabs.addTab(tabs.newTab().setText(R.string.tab_artist))
    tabs.addTab(tabs.newTab().setText(R.string.tab_playlist))
    tabs.addTab(tabs.newTab().setText(R.string.tab_folder))
    //viewpager与tablayout关联
    tabs.setupWithViewPager(view_pager)
    tabs.setSelectedTabIndicatorColor(if (isPrimaryColorCloseToWhite) Color.BLACK else Color.WHITE)
    //        tabs.setSelectedTabIndicatorColor(ColorUtil.getColor(isLightColor ? R.color.black : R.color.white));
    tabs.setSelectedTabIndicatorHeight(DensityUtil.dip2px(this, 3f))
    tabs.setTabTextColors(ColorUtil.getColor(
        if (isPrimaryColorCloseToWhite)
          R.color.dark_normal_tab_text_color
        else
          R.color.light_normal_tab_text_color),
        ColorUtil.getColor(if (isPrimaryColorCloseToWhite) R.color.black else R.color.white))

    setTabClickListener()
    tabs.post {
      for (i in 0..tabs.tabCount) {
        ((tabs.getTabAt(i)?.view?.getChildAt(1)) as TextView?)?.apply {
          if (layout != null && layout.lineCount > 1) {
            maxLines = 1
          }
        }
      }
    }
  }

  private fun setTabClickListener() {
    for (i in 0 until tabs.tabCount) {
      val tab = tabs.getTabAt(i) ?: return
      tab.view.setOnClickListener(object : DoubleClickListener() {
        override fun onDoubleClick(v: View) {
          // 只有第一个标签可能是"歌曲"
          if (currentFragment is SongFragment) {
            // 滚动到当前的歌曲
            val fragments = supportFragmentManager.fragments
            for (fragment in fragments) {
              if (fragment is SongFragment) {
                fragment.scrollToCurrent()
              }
            }
          }
        }
      })
    }
  }

  private fun setUpDrawerLayout() {
    drawerAdapter.onItemClickListener = object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        when (position) {
          //歌曲库
          0 -> drawer.closeDrawer(navigation_view)
          1 -> startActivity(Intent(this@MainActivity, HistoryActivity::class.java))
          //最近添加
          2 -> startActivity(Intent(this@MainActivity, RecentlyActivity::class.java))
          //捐赠
          3 -> startActivity(Intent(this@MainActivity, SupportActivity::class.java))
          //设置
          4 -> startActivityForResult(Intent(this@MainActivity, SettingActivity::class.java), REQUEST_SETTING)
          //退出
          5 -> {
            Timber.v("发送Exit广播")
            sendBroadcast(Intent(Constants.ACTION_EXIT)
                .setComponent(ComponentName(this@MainActivity, ExitReceiver::class.java)))
          }
        }
        drawerAdapter.setSelectIndex(position)
      }

      override fun onItemLongClick(view: View, position: Int) {}
    }
    recyclerview.adapter = drawerAdapter
    recyclerview.layoutManager = LinearLayoutManager(this)

    drawer.addDrawerListener(object : DrawerLayout.DrawerListener {
      override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

      override fun onDrawerOpened(drawerView: View) {}

      override fun onDrawerClosed(drawerView: View) {
        drawerAdapter.setSelectIndex(0)
      }

      override fun onDrawerStateChanged(newState: Int) {}
    })
  }

  /**
   * 初始化控件相关颜色
   */
  private fun setUpViewColor() {
    //正在播放文字的背景
    val bg = GradientDrawable()
    val primaryColor = ThemeStore.materialPrimaryColor

    bg.setColor(ColorUtil.darkenColor(primaryColor))
    bg.cornerRadius = DensityUtil.dip2px(this, 4f).toFloat()
    tv_header.background = bg
    tv_header.setTextColor(ThemeStore.materialPrimaryColorReverse)
    //抽屉
    header.setBackgroundColor(primaryColor)
    navigation_view.setBackgroundColor(ThemeStore.drawerDefaultColor)

    //这种图片不知道该怎么着色 暂时先这样处理
    btn_add.background = Theme.tintDrawable(R.drawable.bg_playlist_add,
        ThemeStore.accentColor)
    btn_add.setImageResource(R.drawable.icon_playlist_add)
  }

  override fun onMediaStoreChanged() {
    super.onMediaStoreChanged()
    onMetaChanged()
    //    mRefreshHandler.sendEmptyMessage(MSG_UPDATE_ADAPTER);
  }

  @SuppressLint("CheckResult")
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      REQUEST_SETTING -> {
        if (data == null) {
          return
        }
        if (data.getBooleanExtra(EXTRA_RECREATE, false)) { //设置后需要重启activity
          handler.sendEmptyMessage(MSG_RECREATE_ACTIVITY)
        } else if (data.getBooleanExtra(EXTRA_REFRESH_ADAPTER, false)) { //刷新adapter
          UriFetcher.updateAllVersion()
          UriFetcher.clearAllCache()
          GlideApp.get(this).clearMemory()
          handler.sendEmptyMessage(MSG_UPDATE_ADAPTER)
        } else if (data.getBooleanExtra(EXTRA_REFRESH_LIBRARY, false)) { //刷新Library
          val libraries = data.getSerializableExtra(EXTRA_LIBRARY) as List<Library>?
          if (libraries != null && libraries.isNotEmpty()) {
            pagerAdapter.list = libraries
            pagerAdapter.notifyDataSetChanged()
            view_pager.offscreenPageLimit = libraries.size - 1
            menuLayoutId = parseMenuId(pagerAdapter.list[view_pager.currentItem].tag)
            currentFragment = pagerAdapter.getFragment(view_pager.currentItem) as LibraryFragment<*, *>
            invalidateOptionsMenu()
            //如果只有一个Library,隐藏标签栏
            if (libraries.size == 1) {
              tabs.visibility = View.GONE
            } else {
              tabs.visibility = View.VISIBLE
            }
          }
        }
      }
      REQUEST_INSTALL_PACKAGES -> if (resultCode == Activity.RESULT_OK) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager
                .canRequestPackageInstalls()) {
          return
        }
        installApk(this, installPath)
      }

      Crop.REQUEST_CROP, Crop.REQUEST_PICK -> {
        val intent = intent

        val customCover = intent.getParcelableExtra<CustomCover>(EXTRA_COVER) ?: return
        val errorTxt = getString(
            when (customCover.type) {
              Constants.ALBUM -> R.string.set_album_cover_error
              Constants.ARTIST -> R.string.set_artist_cover_error
              else -> R.string.set_playlist_cover_error
            })
        val id = customCover.model.getKey().toLong() //专辑、艺术家、播放列表封面

        if (resultCode != Activity.RESULT_OK) {
          ToastUtil.show(this, errorTxt)
          return
        }

        if (requestCode == Crop.REQUEST_PICK) {
          //选择图片
          val cacheDir = DiskCache.getDiskCacheDir(this,
              "thumbnail/" + when (customCover.type) {
                Constants.ALBUM -> "album"
                Constants.ARTIST -> "artist"
                else -> "playlist"
              })
          if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            ToastUtil.show(this, errorTxt)
            return
          }
          val destination = Uri.fromFile(File(cacheDir, hashKeyForDisk(id.toString() + "") + ".jpg"))
          Crop.of(data?.data, destination).asSquare().start(this)
        } else {
          //图片裁剪
          //裁剪后的图片路径
          if (data == null) {
            return
          }
          if (Crop.getOutput(data) == null) {
            return
          }

          val path = Crop.getOutput(data).encodedPath
          if (TextUtils.isEmpty(path) || id == -1L) {
            ToastUtil.show(this, errorTxt)
            return
          }

          Handler(Looper.getMainLooper()).postDelayed({
            when (customCover.type) {
              Constants.ALBUM -> UriFetcher.updateAlbumVersion()
              Constants.ARTIST -> UriFetcher.updateArtistVersion()
              else -> UriFetcher.updatePlayListVersion()
            }
            UriFetcher.clearAllCache()
            GlideApp.get(this).clearMemory()
            onMediaStoreChanged()
            handler.sendEmptyMessage(MSG_UPDATE_ADAPTER)
          }, 500)
        }
      }
    }
  }

  override fun onBackPressed() {
    if (drawer.isDrawerOpen(navigation_view)) {
      drawer.closeDrawer(navigation_view)
    } else {
      var closed = false
      for (fragment in supportFragmentManager.fragments) {
        if (fragment is LibraryFragment<*, *>) {
          val choice = fragment.multiChoice
          if (choice.isActive) {
            closed = true
            choice.close()
            break
          }
        }
      }
      if (!closed) {
        super.onBackPressed()
      }
      //            Intent intent = new Intent();
      //            intent.setAction(Intent.ACTION_MAIN);
      //            intent.addCategory(Intent.CATEGORY_HOME);
      //            startActivity(intent);
    }
  }

  override fun onMetaChanged() {
    super.onMetaChanged()
    val currentSong = MusicServiceRemote.getCurrentSong()
    if (currentSong != Song.EMPTY_SONG) {
      tv_header.text = getString(R.string.play_now, currentSong.title)
      GlideApp.with(this)
          .load(currentSong)
          .centerCrop()
          .signature(ObjectKey(UriFetcher.albumVersion))
          .placeholder(Theme.resolveDrawable(this, R.attr.default_album))
          .error(Theme.resolveDrawable(this, R.attr.default_album))
          .into(iv_header)
    }
  }

  override fun onPlayStateChange() {
    super.onPlayStateChange()
    iv_header.setBackgroundResource(if (MusicServiceRemote.isPlaying() && ThemeStore.isLightTheme)
      R.drawable.drawer_bg_album_shadow
    else
      R.color.transparent)
  }

  override fun onServiceConnected(service: MusicService) {
    super.onServiceConnected(service)
    handler.postDelayed({ this.parseIntent() }, 500)
    handler.post {
      onMetaChanged()
    }
  }

  @OnHandleMessage
  fun handleInternal(msg: Message) {
    when (msg.what) {
      MSG_RECREATE_ACTIVITY -> recreate()
      MSG_RESET_MULTI -> for (temp in supportFragmentManager.fragments) {
        if (temp is LibraryFragment<*, *>) {
          temp.adapter.notifyDataSetChanged()
        }
      }
      MSG_UPDATE_ADAPTER -> //刷新适配器
        for (temp in supportFragmentManager.fragments) {
          if (temp is LibraryFragment<*, *>) {
            temp.adapter.notifyDataSetChanged()
          }
        }
    }
  }

  /**
   * 解析外部打开Intent
   */
  private fun parseIntent() {
    if (intent == null) {
      return
    }
    val intent = intent
    val uri = intent.data
    if (uri != null && uri.toString().isNotEmpty()) {
      MusicUtil.playFromUri(uri)
      setIntent(Intent())
    }
  }

  private fun checkUpdate() {
    if (!IS_GOOGLEPLAY && !alreadyCheck) {
      UpdateAgent.forceCheck = false
      UpdateAgent.listener = UpdateListener(this)
      alreadyCheck = true
      UpdateAgent.check(this)
    }
  }

  private fun checkIsAndroidO(context: Context, path: String) {
    if (!TextUtils.isEmpty(path) && path != installPath) {
      installPath = path
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val hasInstallPermission = context.packageManager.canRequestPackageInstalls()
      if (hasInstallPermission) {
        installApk(context, path)
      } else {
        //请求安装未知应用来源的权限
        ToastUtil.show(this, R.string.plz_give_install_permission)
        val packageURI = Uri.parse("package:$packageName")
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI)
        startActivityForResult(intent, REQUEST_INSTALL_PACKAGES)
      }
    } else {
      installApk(context, path)
    }
  }

  private fun dismissForceDialog() {
    if (forceDialog != null && forceDialog?.isShowing == true) {
      forceDialog?.dismiss()
      forceDialog = null
    }
  }

  private fun showForceDialog() {
    dismissForceDialog()
    forceDialog = Theme.getBaseDialog(this)
        .canceledOnTouchOutside(false)
        .cancelable(false)
        .title(R.string.updating)
        .content(R.string.please_wait)
        .progress(true, 0)
        .progressIndeterminateStyle(false).build()
    forceDialog?.show()
  }

  fun toPlayerActivity() {
    val bottomActionBarFragment = supportFragmentManager.findFragmentByTag("BottomActionBarFragment") as BottomActionBarFragment?
    bottomActionBarFragment?.startPlayerActivity()
  }

  class MainReceiver internal constructor(mainActivity: MainActivity) : BroadcastReceiver() {
    private val mRef: WeakReference<MainActivity> = WeakReference(mainActivity)

    override fun onReceive(context: Context, intent: Intent?) {
      if (intent == null) {
        return
      }
      val action = intent.action
      if (action.isNullOrEmpty()) {
        return
      }
      val mainActivity = mRef.get() ?: return
      when (action) {
        ACTION_DOWNLOAD_COMPLETE -> mainActivity.checkIsAndroidO(context, intent.getStringExtra(DownloadService.EXTRA_PATH)!!)
        ACTION_SHOW_DIALOG -> mainActivity.showForceDialog()
        ACTION_DISMISS_DIALOG -> mainActivity.dismissForceDialog()
      }

    }
  }

  companion object {
    const val EXTRA_RECREATE = "extra_needRecreate"
    const val EXTRA_REFRESH_ADAPTER = "extra_needRefreshAdapter"
    const val EXTRA_REFRESH_LIBRARY = "extra_needRefreshLibrary"
    const val EXTRA_LIBRARY = "extra_library"

    //设置界面
    private const val REQUEST_SETTING = 1

    //安装权限
    private const val REQUEST_INSTALL_PACKAGES = 2

    private val IMAGE_SIZE = DensityUtil.dip2px(App.context, 108f)

    /**
     * 检查更新
     */
    private var alreadyCheck: Boolean = false
  }
}

