package remix.myplayer.ui.activity


import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.provider.Settings
import android.support.design.widget.NavigationView
import android.support.design.widget.TabLayout
import android.support.v4.view.ViewPager
import android.support.v4.widget.DrawerLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringSystem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import remix.myplayer.App
import remix.myplayer.App.IS_GOOGLEPLAY
import remix.myplayer.R
import remix.myplayer.bean.misc.Category
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.helper.SortOrder
import remix.myplayer.misc.handler.MsgHandler
import remix.myplayer.misc.handler.OnHandleMessage
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.misc.receiver.ExitReceiver
import remix.myplayer.misc.update.DownloadService
import remix.myplayer.misc.update.DownloadService.Companion.ACTION_DISMISS_DIALOG
import remix.myplayer.misc.update.DownloadService.Companion.ACTION_DOWNLOAD_COMPLETE
import remix.myplayer.misc.update.DownloadService.Companion.ACTION_SHOW_DIALOG
import remix.myplayer.misc.update.UpdateAgent
import remix.myplayer.misc.update.UpdateListener
import remix.myplayer.request.ImageUriRequest
import remix.myplayer.request.LibraryUriRequest
import remix.myplayer.request.RequestConfig
import remix.myplayer.request.network.RxUtil.applySingleScheduler
import remix.myplayer.service.MusicService
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.theme.ThemeStore.getMaterialPrimaryColor
import remix.myplayer.theme.ThemeStore.getMaterialPrimaryColorReverse
import remix.myplayer.ui.adapter.DrawerAdapter
import remix.myplayer.ui.adapter.MainPagerAdapter
import remix.myplayer.ui.fragment.*
import remix.myplayer.ui.misc.DoubleClickListener
import remix.myplayer.ui.misc.MultipleChoice
import remix.myplayer.util.*
import remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType
import remix.myplayer.util.Util.*
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*

/**
 *
 */
open class MainActivity : MenuActivity() {
  @BindView(R.id.tabs)
  lateinit var mTabLayout: TabLayout
  @BindView(R.id.ViewPager)
  lateinit var mViewPager: ViewPager
  @BindView(R.id.navigation_view)
  lateinit var mNavigationView: NavigationView
  @BindView(R.id.drawer_layout)
  lateinit var mDrawerLayout: DrawerLayout
  @BindView(R.id.add)
  lateinit var mAddButton: ImageView
  @BindView(R.id.header_txt)
  lateinit var mHeadText: TextView
  @BindView(R.id.header_img)
  lateinit var mHeadImg: SimpleDraweeView
  @BindView(R.id.header)
  lateinit var mHeadRoot: View
  @BindView(R.id.recyclerview)
  lateinit var mRecyclerView: RecyclerView

  private val mDrawerAdapter by lazy {
    DrawerAdapter(R.layout.item_drawer)
  }
  private val mPagerAdapter by lazy {
    MainPagerAdapter(supportFragmentManager)
  }

  private val mRefreshHandler by lazy {
    MsgHandler(this)
  }
  private val mReceiver by lazy {
    MainReceiver(this)
  }

  //当前选中的fragment
  private var mCurrentFragment: LibraryFragment<*, *>? = null

  private var mMenuLayoutId = R.menu.menu_main

  /**
   * 判断安卓版本，请求安装权限或者直接安装
   *
   * @param activity
   * @param path
   */
  private var mInstallPath: String? = null


  private var mForceDialog: MaterialDialog? = null

  override fun onResume() {
    super.onResume()
  }

  override fun onPause() {
    super.onPause()
  }

  override fun onDestroy() {
    super.onDestroy()
    unregisterLocalReceiver(mReceiver)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    ButterKnife.bind(this)

    val intentFilter = IntentFilter()
    //        intentFilter.addAction(ACTION_LOAD_FINISH);
    intentFilter.addAction(ACTION_DOWNLOAD_COMPLETE)
    intentFilter.addAction(ACTION_SHOW_DIALOG)
    intentFilter.addAction(ACTION_DISMISS_DIALOG)
    registerLocalReceiver(mReceiver, intentFilter)

    //初始化控件
    setUpToolbar()
    setUpPager()
    setUpTab()
    //初始化测滑菜单
    setUpDrawerLayout()
    setUpViewColor()
    //handler
    mRefreshHandler.postDelayed({ this.checkUpdate() }, 500)

    //清除多选显示状态
    MultipleChoice.isActiveSomeWhere = false
  }

  override fun setStatusBarColor() {
    StatusBarUtil.setColorNoTranslucentForDrawerLayout(this,
        findViewById(R.id.drawer_layout),
        ThemeStore.getStatusBarColor())
  }

  /**
   * 初始化toolbar
   */
  private fun setUpToolbar() {
    super.setUpToolbar("")
    toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp)
    toolbar.setNavigationOnClickListener { v -> mDrawerLayout.openDrawer(mNavigationView) }
  }

  /**
   * 新建播放列表
   */
  @OnClick(R.id.add)
  fun onClick(v: View) {
    when (v.id) {
      R.id.add -> {
        if (MultipleChoice.isActiveSomeWhere) {
          return
        }

        DatabaseRepository.getInstance()
            .getAllPlaylist()
            .compose<List<PlayList>>(applySingleScheduler<List<PlayList>>())
            .subscribe { playLists ->
              Theme.getBaseDialog(mContext)
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
                                .show(mContext, R.string.create_playlist_fail, throwable.toString())
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
    val categoryJson = SPUtil
        .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LIBRARY_CATEGORY, "")
    val categories = if (TextUtils.isEmpty(categoryJson))
      ArrayList()
    else
      Gson().fromJson<ArrayList<Category>>(categoryJson, object : TypeToken<List<Category>>() {}.type)
    if (categories.isEmpty()) {
      val defaultCategories = Category.getDefaultLibrary(this)
      categories.addAll(defaultCategories)
      SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LIBRARY_CATEGORY,
          Gson().toJson(defaultCategories, object : TypeToken<List<Category>>() {

          }.type))
    }

    mPagerAdapter.list = categories
    mMenuLayoutId = parseMenuId(mPagerAdapter.list[0].tag)
    //有且仅有一个tab
    if (categories.size == 1) {
      if (categories[0].isPlayList) {
        showViewWithAnim(mAddButton, true)
      }
      mTabLayout.visibility = View.GONE
    } else {
      mTabLayout.visibility = View.VISIBLE
    }

    mViewPager.adapter = mPagerAdapter
    mViewPager.offscreenPageLimit = mPagerAdapter.count - 1
    mViewPager.currentItem = 0
    mViewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
      override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

      override fun onPageSelected(position: Int) {
        val category = mPagerAdapter.list[position]
        showViewWithAnim(mAddButton, category.isPlayList)

        mMenuLayoutId = parseMenuId(mPagerAdapter.list[position].tag)
        mCurrentFragment = mPagerAdapter.getFragment(position) as LibraryFragment<*, *>

        invalidateOptionsMenu()
      }


      override fun onPageScrollStateChanged(state: Int) {}
    })
    mCurrentFragment = mPagerAdapter.getFragment(0) as LibraryFragment<*, *>
  }

  fun parseMenuId(tag: Int): Int {
    return when (tag) {
      Category.TAG_SONG -> R.menu.menu_main
      Category.TAG_ALBUM -> R.menu.menu_album
      Category.TAG_ARTIST -> R.menu.menu_artist
      Category.TAG_PLAYLIST -> R.menu.menu_playlist
      Category.TAG_FOLDER -> R.menu.menu_folder
      else -> R.menu.menu_main_simple
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    super.onCreateOptionsMenu(menu)
    if (mCurrentFragment is FolderFragment) {
      return true
    }
    var sortOrder = ""
    when (mCurrentFragment) {
      is SongFragment -> sortOrder = SPUtil
          .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER,
              SortOrder.SongSortOrder.SONG_A_Z)
      is AlbumFragment -> sortOrder = SPUtil
          .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ALBUM_SORT_ORDER,
              SortOrder.AlbumSortOrder.ALBUM_A_Z)
      is ArtistFragment -> sortOrder = SPUtil
          .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ARTIST_SORT_ORDER,
              SortOrder.ArtistSortOrder.ARTIST_A_Z)
      is PlayListFragment -> sortOrder = SPUtil
          .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAYLIST_SORT_ORDER,
              SortOrder.PlayListSortOrder.PLAYLIST_DATE)
    }

    if (TextUtils.isEmpty(sortOrder)) {
      return true
    }
    setUpMenuItem(menu, sortOrder)
    return true
  }


  override fun getMenuLayoutId(): Int {
    return mMenuLayoutId
  }

  override fun saveSortOrder(sortOrder: String?) {
    when (mCurrentFragment) {
      is SongFragment -> SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER,
          sortOrder)
      is AlbumFragment -> SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ALBUM_SORT_ORDER,
          sortOrder)
      is ArtistFragment -> SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ARTIST_SORT_ORDER,
          sortOrder)
      is PlayListFragment -> SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAYLIST_SORT_ORDER,
          sortOrder)
    }
    mCurrentFragment?.onMediaStoreChanged()
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
    val isPrimaryColorCloseToWhite = ThemeStore.isMDColorCloseToWhite()
    //        mTabLayout = new TabLayout(new ContextThemeWrapper(this, !ColorUtil.isColorLight(ThemeStore.getMaterialPrimaryColor()) ? R.style.CustomTabLayout_Light : R.style.CustomTabLayout_Dark));
    //        mTabLayout = new TabLayout(new ContextThemeWrapper(this,R.style.CustomTabLayout_Light));
    //        mTabLayout.setLayoutParams(new AppBarLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,DensityUtil.dip2px(this,48)));
    //        mTabLayout = new TabLayout(this);
    mTabLayout.setBackgroundColor(getMaterialPrimaryColor())
    mTabLayout.addTab(mTabLayout.newTab().setText(R.string.tab_song))
    mTabLayout.addTab(mTabLayout.newTab().setText(R.string.tab_album))
    mTabLayout.addTab(mTabLayout.newTab().setText(R.string.tab_artist))
    mTabLayout.addTab(mTabLayout.newTab().setText(R.string.tab_playlist))
    mTabLayout.addTab(mTabLayout.newTab().setText(R.string.tab_folder))
    //viewpager与tablayout关联
    mTabLayout.setupWithViewPager(mViewPager)
    mTabLayout.setSelectedTabIndicatorColor(if (isPrimaryColorCloseToWhite) Color.BLACK else Color.WHITE)
    //        mTabLayout.setSelectedTabIndicatorColor(ColorUtil.getColor(isLightColor ? R.color.black : R.color.white));
    mTabLayout.setSelectedTabIndicatorHeight(DensityUtil.dip2px(this, 3f))
    mTabLayout.setTabTextColors(ColorUtil.getColor(
        if (isPrimaryColorCloseToWhite)
          R.color.dark_normal_tab_text_color
        else
          R.color.light_normal_tab_text_color),
        ColorUtil.getColor(if (isPrimaryColorCloseToWhite) R.color.black else R.color.white))

    setTabClickListener()
  }

  private fun setTabClickListener() {
    for (i in 0 until mTabLayout.tabCount) {
      val tab = mTabLayout.getTabAt(i) ?: return
      val c = tab.javaClass
      try {
        val field = c.getDeclaredField("view")
        field.isAccessible = true
        val view = field.get(tab) as View
        view.setOnClickListener(object : DoubleClickListener() {
          override fun onDoubleClick(v: View) {
            // 只有第一个标签可能是"歌曲"
            if (mCurrentFragment is SongFragment) {
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
      } catch (e: Exception) {
        Timber.w(e)
      }
    }
  }


  private fun setUpDrawerLayout() {
    mDrawerAdapter.setOnItemClickListener(object : OnItemClickListener {
      override fun onItemClick(view: View, position: Int) {
        when (position) {
          //歌曲库
          0 -> mDrawerLayout.closeDrawer(mNavigationView)
          //最近添加
          1 -> startActivity(Intent(mContext, RecentlyActivity::class.java))
          //捐赠
          2 -> startActivity(Intent(mContext, SupportDevelopActivity::class.java))
          //设置
          3 -> startActivityForResult(Intent(mContext, SettingActivity::class.java), REQUEST_SETTING)
          //退出
          4 -> {
            Timber.v("发送Exit广播")
            sendBroadcast(Intent(Constants.ACTION_EXIT)
                .setComponent(ComponentName(mContext, ExitReceiver::class.java)))
          }
        }
        mDrawerAdapter.setSelectIndex(position)
      }

      override fun onItemLongClick(view: View, position: Int) {}
    })
    mRecyclerView.adapter = mDrawerAdapter
    mRecyclerView.layoutManager = LinearLayoutManager(this)

    mDrawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
      override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

      override fun onDrawerOpened(drawerView: View) {}

      override fun onDrawerClosed(drawerView: View) {
        mDrawerAdapter.setSelectIndex(0)
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
    val primaryColor = getMaterialPrimaryColor()

    bg.setColor(ColorUtil.darkenColor(primaryColor))
    bg.cornerRadius = DensityUtil.dip2px(this, 4f).toFloat()
    mHeadText.background = bg
    mHeadText.setTextColor(getMaterialPrimaryColorReverse())
    //抽屉
    mHeadRoot.setBackgroundColor(primaryColor)
    mNavigationView.setBackgroundColor(ThemeStore.getDrawerDefaultColor())

    //这种图片不知道该怎么着色 暂时先这样处理
    mAddButton.background = Theme.tintDrawable(R.drawable.bg_playlist_add,
        ThemeStore.getAccentColor())
    mAddButton.setImageResource(R.drawable.icon_playlist_add)
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
          mRefreshHandler.sendEmptyMessage(MSG_RECREATE_ACTIVITY)
        } else if (data.getBooleanExtra(EXTRA_REFRESH_ADAPTER, false)) { //刷新adapter
          ImageUriRequest.clearUriCache()
          mRefreshHandler.sendEmptyMessage(MSG_UPDATE_ADAPTER)
        } else if (data.getBooleanExtra(EXTRA_REFRESH_LIBRARY, false)) { //刷新Library
          val categories = data.getSerializableExtra(EXTRA_CATEGORY) as List<Category>?
          if (categories != null && categories.isNotEmpty()) {
            mPagerAdapter.list = categories
            mPagerAdapter.notifyDataSetChanged()
            mViewPager.offscreenPageLimit = categories.size - 1
            mMenuLayoutId = parseMenuId(mPagerAdapter.list[mViewPager.currentItem].tag)
            mCurrentFragment = mPagerAdapter.getFragment(mViewPager.currentItem) as LibraryFragment<*, *>
            invalidateOptionsMenu()
            //如果只有一个Library,隐藏标签栏
            if (categories.size == 1) {
              mTabLayout.visibility = View.GONE
            } else {
              mTabLayout.visibility = View.VISIBLE
            }
          }
        }
      }
      REQUEST_INSTALL_PACKAGES -> if (resultCode == Activity.RESULT_OK) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager
                .canRequestPackageInstalls()) {
          return
        }
        installApk(mContext, mInstallPath)
      }
    }
  }

  override fun onBackPressed() {
    if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
      mDrawerLayout.closeDrawer(mNavigationView)
    } else {
      var closed = false
      for (fragment in supportFragmentManager.fragments) {
        if (fragment is LibraryFragment<*, *>) {
          val choice = fragment.choice
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
    mHeadText.text = getString(R.string.play_now, MusicServiceRemote.getCurrentSong().title)
    LibraryUriRequest(mHeadImg,
        getSearchRequestWithAlbumType(MusicServiceRemote.getCurrentSong()),
        RequestConfig.Builder(IMAGE_SIZE, IMAGE_SIZE).build()).load()
  }

  override fun onPlayStateChange() {
    super.onPlayStateChange()
    mHeadImg.setBackgroundResource(if (MusicServiceRemote.isPlaying() && ThemeStore.isLightTheme())
      R.drawable.drawer_bg_album_shadow
    else
      R.color.transparent)
  }

  override fun onServiceConnected(service: MusicService) {
    super.onServiceConnected(service)
    mRefreshHandler.postDelayed({ this.parseIntent() }, 500)
  }

  @OnHandleMessage
  fun handleInternal(msg: Message) {
    when {
      msg.what == MSG_RECREATE_ACTIVITY -> recreate()
      msg.what == MSG_RESET_MULTI -> for (temp in supportFragmentManager.fragments) {
        if (temp is LibraryFragment<*, *>) {
          temp.adapter?.notifyDataSetChanged()
        }
      }
      msg.what == MSG_UPDATE_ADAPTER -> //刷新适配器
        for (temp in supportFragmentManager.fragments) {
          if (temp is LibraryFragment<*, *>) {
            temp.adapter?.notifyDataSetChanged()
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
    if (!IS_GOOGLEPLAY && !mAlreadyCheck) {
      UpdateAgent.forceCheck = false
      UpdateAgent.listener = UpdateListener(mContext)
      mAlreadyCheck = true
      UpdateAgent.check(this)
    }
  }

  private fun checkIsAndroidO(context: Context, path: String) {
    if (!TextUtils.isEmpty(path) && path != mInstallPath) {
      mInstallPath = path
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val hasInstallPermission = context.packageManager.canRequestPackageInstalls()
      if (hasInstallPermission) {
        installApk(context, path)
      } else {
        //请求安装未知应用来源的权限
        ToastUtil.show(mContext, R.string.plz_give_install_permission)
        val packageURI = Uri.parse("package:$packageName")
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI)
        startActivityForResult(intent, REQUEST_INSTALL_PACKAGES)
      }
    } else {
      installApk(context, path)
    }
  }

  private fun dismissForceDialog() {
    if (mForceDialog != null && mForceDialog?.isShowing == true) {
      mForceDialog?.dismiss()
      mForceDialog = null
    }
  }

  private fun showForceDialog() {
    dismissForceDialog()
    mForceDialog = Theme.getBaseDialog(mContext)
        .canceledOnTouchOutside(false)
        .cancelable(false)
        .title(R.string.updating)
        .content(R.string.please_wait)
        .progress(true, 0)
        .progressIndeterminateStyle(false).build()
    mForceDialog?.show()
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
        ACTION_DOWNLOAD_COMPLETE -> mainActivity.checkIsAndroidO(context, intent.getStringExtra(DownloadService.EXTRA_PATH))
        ACTION_SHOW_DIALOG -> mainActivity.showForceDialog()
        ACTION_DISMISS_DIALOG -> mainActivity.dismissForceDialog()
      }

    }
  }

  companion object {
    const val EXTRA_RECREATE = "needRecreate"
    const val EXTRA_REFRESH_ADAPTER = "needRefreshAdapter"
    const val EXTRA_REFRESH_LIBRARY = "needRefreshLibrary"
    const val EXTRA_CATEGORY = "Category"
    //设置界面
    private const val REQUEST_SETTING = 1
    //安装权限
    private const val REQUEST_INSTALL_PACKAGES = 2

    private val IMAGE_SIZE = DensityUtil.dip2px(App.getContext(), 108f)

    /**
     * 检查更新
     */
    private var mAlreadyCheck: Boolean = false
  }
}

