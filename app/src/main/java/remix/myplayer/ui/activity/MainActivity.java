package remix.myplayer.ui.activity;


import static remix.myplayer.App.IS_GOOGLEPLAY;
import static remix.myplayer.misc.update.DownloadService.ACTION_DISMISS_DIALOG;
import static remix.myplayer.misc.update.DownloadService.ACTION_DOWNLOAD_COMPLETE;
import static remix.myplayer.misc.update.DownloadService.ACTION_SHOW_DIALOG;
import static remix.myplayer.request.network.RxUtil.applySingleScheduler;
import static remix.myplayer.theme.ThemeStore.getMaterialPrimaryColor;
import static remix.myplayer.theme.ThemeStore.getMaterialPrimaryColorReverse;
import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;
import static remix.myplayer.util.Util.installApk;
import static remix.myplayer.util.Util.registerLocalReceiver;
import static remix.myplayer.util.Util.unregisterLocalReceiver;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.provider.Settings;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.bean.misc.Category;
import remix.myplayer.db.room.DatabaseRepository;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.helper.SortOrder;
import remix.myplayer.misc.handler.MsgHandler;
import remix.myplayer.misc.handler.OnHandleMessage;
import remix.myplayer.misc.interfaces.OnItemClickListener;
import remix.myplayer.misc.receiver.ExitReceiver;
import remix.myplayer.misc.update.DownloadService;
import remix.myplayer.misc.update.UpdateAgent;
import remix.myplayer.misc.update.UpdateListener;
import remix.myplayer.request.LibraryUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.adapter.DrawerAdapter;
import remix.myplayer.ui.adapter.HeaderAdapter;
import remix.myplayer.ui.adapter.MainPagerAdapter;
import remix.myplayer.ui.fragment.AlbumFragment;
import remix.myplayer.ui.fragment.ArtistFragment;
import remix.myplayer.ui.fragment.FolderFragment;
import remix.myplayer.ui.fragment.LibraryFragment;
import remix.myplayer.ui.fragment.PlayListFragment;
import remix.myplayer.ui.fragment.SongFragment;
import remix.myplayer.ui.misc.DoubleClickListener;
import remix.myplayer.ui.misc.MultipleChoice;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.MusicUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.StatusBarUtil;
import remix.myplayer.util.ToastUtil;

/**
 *
 */
public class MainActivity extends MenuActivity {

  public static final String EXTRA_RECREATE = "needRecreate";
  public static final String EXTRA_REFRESH_ADAPTER = "needRefreshAdapter";
  public static final String EXTRA_REFRESH_LIBRARY = "needRefreshLibrary";
  public static final String EXTRA_CATEGORY = "Category";


  @BindView(R.id.tabs)
  TabLayout mTablayout;
  @BindView(R.id.ViewPager)
  ViewPager mViewPager;
  @BindView(R.id.navigation_view)
  NavigationView mNavigationView;
  @BindView(R.id.drawer_layout)
  DrawerLayout mDrawerLayout;
  @BindView(R.id.add)
  ImageView mAddButton;
  @BindView(R.id.header_txt)
  TextView mHeadText;
  @BindView(R.id.header_img)
  SimpleDraweeView mHeadImg;
  @BindView(R.id.header)
  View mHeadRoot;
  @BindView(R.id.recyclerview)
  RecyclerView mRecyclerView;

  private DrawerAdapter mDrawerAdapter;
  private MainPagerAdapter mPagerAdapter;

  private MsgHandler mRefreshHandler;
  //设置界面
  private static final int REQUEST_SETTING = 1;
  //安装权限
  private static final int REQUEST_INSTALL_PACKAGES = 2;
  private BroadcastReceiver mReceiver;

  //当前选中的fragment
  private LibraryFragment mCurrentFragment;

  @Override
  protected void onResume() {
    super.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    unregisterLocalReceiver(mReceiver);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    ButterKnife.bind(this);
    //receiver
    mReceiver = new MainReceiver(this);
    IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(ACTION_LOAD_FINISH);
    intentFilter.addAction(ACTION_DOWNLOAD_COMPLETE);
    intentFilter.addAction(ACTION_SHOW_DIALOG);
    intentFilter.addAction(ACTION_DISMISS_DIALOG);
    registerLocalReceiver(mReceiver, intentFilter);

    //初始化控件
    setUpToolbar();
    setUpPager();
    setUpTab();
    //初始化测滑菜单
    setUpDrawerLayout();
    setUpViewColor();
    //handler
    mRefreshHandler = new MsgHandler(this);
    mRefreshHandler.postDelayed(this::checkUpdate, 500);
    mRefreshHandler.postDelayed(this::parseIntent, 500);

    //清除多选显示状态
    MultipleChoice.setActiveSomeWhere(false);
  }

  @Override
  protected void setStatusBarColor() {
    StatusBarUtil.setColorNoTranslucentForDrawerLayout(this,
        findViewById(R.id.drawer_layout),
        ThemeStore.getStatusBarColor());
  }

  /**
   * 初始化toolbar
   */
  protected void setUpToolbar() {
    super.setUpToolbar("");
    toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
    toolbar.setNavigationOnClickListener(v -> mDrawerLayout.openDrawer(mNavigationView));
  }

  /**
   * 新建播放列表
   */
  @OnClick({R.id.add})
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.add:
        if (MultipleChoice.isActiveSomeWhere()) {
          return;
        }

        DatabaseRepository.getInstance()
            .getAllPlaylist()
            .compose(applySingleScheduler())
            .subscribe(playLists -> Theme.getBaseDialog(mContext)
                .title(R.string.new_playlist)
                .positiveText(R.string.create)
                .negativeText(R.string.cancel)
                .inputRange(1, 15)
                .input("", getString(R.string.local_list) + playLists.size(), (dialog, input) -> {
                  if (!TextUtils.isEmpty(input)) {
                    DatabaseRepository.getInstance()
                        .insertPlayList(input.toString())
                        .compose(applySingleScheduler())
                        .subscribe(id -> {
                          //跳转到添加歌曲界面
                          SongChooseActivity.start(MainActivity.this, id, input.toString());
                        }, throwable -> ToastUtil
                            .show(mContext, R.string.create_playlist_fail, throwable.toString()));
                  }
                })
                .show());
        break;
      default:
        break;
    }
  }

  //初始化ViewPager
  private void setUpPager() {
    String categoryJson = SPUtil
        .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LIBRARY_CATEGORY, "");
    List<Category> categories = TextUtils.isEmpty(categoryJson) ? new ArrayList<>()
        : new Gson().fromJson(categoryJson, new TypeToken<List<Category>>() {
        }.getType());
    if (categories.size() == 0) {
      final List<Category> defaultCategories = Category.getDefaultLibrary(this);
      categories.addAll(defaultCategories);
      SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LIBRARY_CATEGORY,
          new Gson().toJson(defaultCategories, new TypeToken<List<Category>>() {
          }.getType()));
    }
    mPagerAdapter = new MainPagerAdapter(getSupportFragmentManager());
    mPagerAdapter.setList(categories);
    mMenuLayoutId = parseMenuId(mPagerAdapter.getList().get(0).getTag());
    //有且仅有一个tab
    if (categories.size() == 1) {
      if (categories.get(0).isPlayList()) {
        showViewWithAnim(mAddButton, true);
      }
      mTablayout.setVisibility(View.GONE);
    } else {
      mTablayout.setVisibility(View.VISIBLE);
    }

    mViewPager.setAdapter(mPagerAdapter);
    mViewPager.setOffscreenPageLimit(mPagerAdapter.getCount() - 1);
    mViewPager.setCurrentItem(0);
    mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
      }

      @Override
      public void onPageSelected(int position) {
        final Category category = mPagerAdapter.getList().get(position);
        showViewWithAnim(mAddButton, category.isPlayList());

        mMenuLayoutId = parseMenuId(mPagerAdapter.getList().get(position).getTag());
        mCurrentFragment = (LibraryFragment) mPagerAdapter.getFragment(position);

        invalidateOptionsMenu();
      }


      @Override
      public void onPageScrollStateChanged(int state) {
      }
    });
    mCurrentFragment = (LibraryFragment) mPagerAdapter.getFragment(0);
  }

  private int mMenuLayoutId = R.menu.menu_main;

  public int parseMenuId(int tag) {
    switch (tag) {
      case Category.TAG_SONG:
        return R.menu.menu_main;
      case Category.TAG_ALBUM:
        return R.menu.menu_album;
      case Category.TAG_ARTIST:
        return R.menu.menu_artist;
      case Category.TAG_PLAYLIST:
        return R.menu.menu_playlist;
      case Category.TAG_FOLDER:
        return R.menu.menu_folder;
      default:
        return R.menu.menu_main_simple;
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    if (mCurrentFragment instanceof FolderFragment) {
      return true;
    }
    String sortOrder = null;
    if (mCurrentFragment instanceof SongFragment) {
      sortOrder = SPUtil
          .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER,
              SortOrder.SongSortOrder.SONG_A_Z);
    } else if (mCurrentFragment instanceof AlbumFragment) {
      sortOrder = SPUtil
          .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ALBUM_SORT_ORDER,
              SortOrder.AlbumSortOrder.ALBUM_A_Z);
    } else if (mCurrentFragment instanceof ArtistFragment) {
      sortOrder = SPUtil
          .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ARTIST_SORT_ORDER,
              SortOrder.ArtistSortOrder.ARTIST_A_Z);
    } else if (mCurrentFragment instanceof PlayListFragment) {
      sortOrder = SPUtil
          .getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAYLIST_SORT_ORDER,
              SortOrder.PlayListSortOrder.PLAYLIST_DATE);
    }

    if (TextUtils.isEmpty(sortOrder)) {
      return true;
    }
    setUpMenuItem(menu, sortOrder);
    return true;
  }


  @Override
  public int getMenuLayoutId() {
    return mMenuLayoutId;
  }

  @Override
  protected void saveSortOrder(String sortOrder) {
    if (mCurrentFragment instanceof SongFragment) {
      SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER,
          sortOrder);
    } else if (mCurrentFragment instanceof AlbumFragment) {
      SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ALBUM_SORT_ORDER,
          sortOrder);
    } else if (mCurrentFragment instanceof ArtistFragment) {
      SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ARTIST_SORT_ORDER,
          sortOrder);
    } else if (mCurrentFragment instanceof PlayListFragment) {
      SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAYLIST_SORT_ORDER,
          sortOrder);
    }
    clearUriCache();
    mCurrentFragment.onMediaStoreChanged();
  }

  private void showViewWithAnim(View view, boolean show) {
    if (show) {
      if (view.getVisibility() != View.VISIBLE) {
        view.setVisibility(View.VISIBLE);
        SpringSystem.create().createSpring()
            .addListener(new SimpleSpringListener() {
              @Override
              public void onSpringUpdate(Spring spring) {
                view.setScaleX((float) spring.getCurrentValue());
                view.setScaleY((float) spring.getCurrentValue());
              }
            })
            .setEndValue(1);
      }
    } else {
      view.setVisibility(View.GONE);
    }

  }

  //初始化custontab
  private void setUpTab() {
    //添加tab选项卡
    boolean isPrimaryColorCloseToWhite = ThemeStore.isMDColorCloseToWhite();
//        mTablayout = new TabLayout(new ContextThemeWrapper(this, !ColorUtil.isColorLight(ThemeStore.getMaterialPrimaryColor()) ? R.style.CustomTabLayout_Light : R.style.CustomTabLayout_Dark));
//        mTablayout = new TabLayout(new ContextThemeWrapper(this,R.style.CustomTabLayout_Light));
//        mTablayout.setLayoutParams(new AppBarLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,DensityUtil.dip2px(this,48)));
//        mTablayout = new TabLayout(this);
    mTablayout.setBackgroundColor(getMaterialPrimaryColor());
    mTablayout.addTab(mTablayout.newTab().setText(R.string.tab_song));
    mTablayout.addTab(mTablayout.newTab().setText(R.string.tab_album));
    mTablayout.addTab(mTablayout.newTab().setText(R.string.tab_artist));
    mTablayout.addTab(mTablayout.newTab().setText(R.string.tab_playlist));
    mTablayout.addTab(mTablayout.newTab().setText(R.string.tab_folder));
    //viewpager与tablayout关联
    mTablayout.setupWithViewPager(mViewPager);
    mTablayout.setSelectedTabIndicatorColor(isPrimaryColorCloseToWhite ? Color.BLACK : Color.WHITE);
//        mTablayout.setSelectedTabIndicatorColor(ColorUtil.getColor(isLightColor ? R.color.black : R.color.white));
    mTablayout.setSelectedTabIndicatorHeight(DensityUtil.dip2px(this, 3));
    mTablayout.setTabTextColors(ColorUtil.getColor(
        isPrimaryColorCloseToWhite ? R.color.dark_normal_tab_text_color
            : R.color.light_normal_tab_text_color),
        ColorUtil.getColor(isPrimaryColorCloseToWhite ? R.color.black : R.color.white));

    setTabClickListener();
  }

  private void setTabClickListener() {

    for (int i = 0; i < mTablayout.getTabCount(); i++) {
      TabLayout.Tab tab = mTablayout.getTabAt(i);
      if (tab == null) {
        return;
      }
      Class c = tab.getClass();
      try {
        Field field = c.getDeclaredField("mView");
        field.setAccessible(true);
        final View view = (View) field.get(tab);
        if (view == null) {
          return;
        }
        view.setOnClickListener(new DoubleClickListener() {
          @Override
          public void onDoubleClick(@NotNull View v) {
            // 只有第一个标签可能是"歌曲"
            if (mCurrentFragment instanceof SongFragment) {
              // 滚动到当前的歌曲
              List<Fragment> fragments = getSupportFragmentManager().getFragments();
              for (Fragment fragment : fragments) {
                if (fragment instanceof SongFragment) {
                  ((SongFragment) fragment).scrollToCurrent();
                }
              }
            }
          }
        });
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }


  private void setUpDrawerLayout() {
    mDrawerAdapter = new DrawerAdapter(R.layout.item_drawer);
    mDrawerAdapter.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(View view, int position) {
        switch (position) {
          //歌曲库
          case 0:
            mDrawerLayout.closeDrawer(mNavigationView);
            break;
          //最近添加
          case 1:
            startActivity(new Intent(mContext, RecentlyActivity.class));
            break;
          //捐赠
          case 2:
            startActivity(new Intent(mContext, SupportDevelopActivity.class));
            break;
          //设置
          case 3:
            startActivityForResult(new Intent(mContext, SettingActivity.class), REQUEST_SETTING);
            break;
          //退出
          case 4:
            sendBroadcast(new Intent(Constants.EXIT)
                .setComponent(new ComponentName(mContext, ExitReceiver.class)));
            break;
        }
        mDrawerAdapter.setSelectIndex(position);
      }

      @Override
      public void onItemLongClick(View view, int position) {
      }
    });
    mRecyclerView.setAdapter(mDrawerAdapter);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

    mDrawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
      @Override
      public void onDrawerSlide(View drawerView, float slideOffset) {
      }

      @Override
      public void onDrawerOpened(View drawerView) {
      }

      @Override
      public void onDrawerClosed(View drawerView) {
        if (mDrawerAdapter != null) {
          mDrawerAdapter.setSelectIndex(0);
        }
      }

      @Override
      public void onDrawerStateChanged(int newState) {
      }
    });
  }

  /**
   * 初始化控件相关颜色
   */
  private void setUpViewColor() {
    //正在播放文字的背景
    GradientDrawable bg = new GradientDrawable();
    final int primaryColor = ThemeStore.getMaterialPrimaryColor();

    bg.setColor(ColorUtil.darkenColor(primaryColor));
    bg.setCornerRadius(DensityUtil.dip2px(this, 4));
    mHeadText.setBackground(bg);
    mHeadText.setTextColor(getMaterialPrimaryColorReverse());
    //抽屉
    mHeadRoot.setBackgroundColor(primaryColor);
    mNavigationView.setBackgroundColor(ThemeStore.getDrawerDefaultColor());

    //这种图片不知道该怎么着色 暂时先这样处理
    mAddButton.setBackground(Theme.tintDrawable(R.drawable.bg_playlist_add,
        ThemeStore.getAccentColor()));
    mAddButton.setImageResource(R.drawable.icon_playlist_add);
  }

  @Override
  public void onMediaStoreChanged() {
    clearUriCache();
    super.onMediaStoreChanged();
    onMetaChanged();
//    mRefreshHandler.sendEmptyMessage(MSG_UPDATE_ADAPTER);
  }

  @SuppressLint("CheckResult")
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    switch (requestCode) {
      case REQUEST_SETTING:
        if (data == null) {
          return;
        }
        if (data.getBooleanExtra(EXTRA_RECREATE, false)) { //设置后需要重启activity
          mRefreshHandler.sendEmptyMessage(MSG_RECREATE_ACTIVITY);
        } else if (data.getBooleanExtra(EXTRA_REFRESH_ADAPTER, false)) { //刷新adapter
          clearUriCache();
          mRefreshHandler.sendEmptyMessage(MSG_UPDATE_ADAPTER);
        } else if (data.getBooleanExtra(EXTRA_REFRESH_LIBRARY, false)) { //刷新Library
          List<Category> categories = (List<Category>) data.getSerializableExtra(EXTRA_CATEGORY);
          if (categories != null && categories.size() > 0) {
            mPagerAdapter.setList(categories);
            mPagerAdapter.notifyDataSetChanged();
            mViewPager.setOffscreenPageLimit(categories.size() - 1);
            mMenuLayoutId = parseMenuId(
                mPagerAdapter.getList().get(mViewPager.getCurrentItem()).getTag());
            mCurrentFragment = (LibraryFragment) mPagerAdapter
                .getFragment(mViewPager.getCurrentItem());
            invalidateOptionsMenu();
            //如果只有一个Library,隐藏标签栏
            if (categories.size() == 1) {
              mTablayout.setVisibility(View.GONE);
            } else {
              mTablayout.setVisibility(View.VISIBLE);
            }
          }
        }
        break;
      case REQUEST_INSTALL_PACKAGES:
        if (resultCode == RESULT_OK) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager()
              .canRequestPackageInstalls()) {
            return;
          }
          installApk(mContext, mInstallPath);
        }
        break;
    }
  }

  private void clearUriCache() {
    // 清除uriCache
    for (Fragment fragment : getSupportFragmentManager().getFragments()) {
      if (fragment instanceof LibraryFragment) {
        final LibraryFragment libraryFragment = (LibraryFragment) fragment;
        final Adapter adapter = libraryFragment.getAdapter();
        if (adapter instanceof HeaderAdapter) {
          ((HeaderAdapter) adapter).clearUriCache();
        }
      }
    }
  }


  @Override
  public void onBackPressed() {
    if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
      mDrawerLayout.closeDrawer(mNavigationView);
    } else {
      boolean closed = false;
      for (Fragment fragment : getSupportFragmentManager().getFragments()) {
        if (fragment instanceof LibraryFragment) {
          MultipleChoice choice = ((LibraryFragment) fragment).getChoice();
          if (choice.isActive()) {
            closed = true;
            choice.close();
            break;
          }
        }
      }
      if (!closed) {
        super.onBackPressed();
      }
//            Intent intent = new Intent();
//            intent.setAction(Intent.ACTION_MAIN);
//            intent.addCategory(Intent.CATEGORY_HOME);
//            startActivity(intent);
    }
  }

  private static final int IMAGE_SIZE = DensityUtil.dip2px(App.getContext(), 108);

  @Override
  public void onMetaChanged() {
    super.onMetaChanged();
    mHeadText.setText(getString(R.string.play_now, MusicServiceRemote.getCurrentSong().getTitle()));
    new LibraryUriRequest(mHeadImg,
        getSearchRequestWithAlbumType(MusicServiceRemote.getCurrentSong()),
        new RequestConfig.Builder(IMAGE_SIZE, IMAGE_SIZE).build()).load();
  }

  @Override
  public void onPlayStateChange() {
    super.onPlayStateChange();
    mHeadImg.setBackgroundResource(MusicServiceRemote.isPlaying() && ThemeStore.isLightTheme()
        ? R.drawable.drawer_bg_album_shadow : R.color.transparent);
  }

  @Override
  public void onServiceConnected(@NotNull MusicService service) {
    super.onServiceConnected(service);
  }

  @OnHandleMessage
  public void handleInternal(Message msg) {
    if (msg.what == MSG_RECREATE_ACTIVITY) {
      recreate();
    } else if (msg.what == MSG_RESET_MULTI) {
      for (Fragment temp : getSupportFragmentManager().getFragments()) {
        if (temp instanceof LibraryFragment) {
          ((LibraryFragment) temp).getAdapter().notifyDataSetChanged();
        }
      }
    } else if (msg.what == MSG_UPDATE_ADAPTER) {
      //刷新适配器
      for (Fragment temp : getSupportFragmentManager().getFragments()) {
        if (temp instanceof LibraryFragment) {
          ((LibraryFragment) temp).getAdapter().notifyDataSetChanged();
        }
      }
    }
  }

  /**
   * 解析外部打开Intent
   */
  private void parseIntent() {
    if (getIntent() == null) {
      return;
    }
    final Intent intent = getIntent();
    final Uri uri = intent.getData();
    if (uri != null && uri.toString().length() > 0) {
      MusicUtil.playFromUri(uri);
      setIntent(new Intent());
    }
  }

  /**
   * 检查更新
   */
  private static boolean mAlreadyCheck;

  private void checkUpdate() {
    if (!IS_GOOGLEPLAY && !mAlreadyCheck) {
      UpdateAgent.setForceCheck(false);
      UpdateAgent.setListener(new UpdateListener(mContext));
      mAlreadyCheck = true;
      UpdateAgent.check(this);
    }
  }

  /**
   * 判断安卓版本，请求安装权限或者直接安装
   *
   * @param activity
   * @param path
   */
  private String mInstallPath;

  private void checkIsAndroidO(Context context, String path) {
    if (!TextUtils.isEmpty(path) && !path.equals(mInstallPath)) {
      mInstallPath = path;
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      boolean hasInstallPermission = context.getPackageManager().canRequestPackageInstalls();
      if (hasInstallPermission) {
        installApk(context, path);
      } else {
        //请求安装未知应用来源的权限
        ToastUtil.show(mContext, R.string.plz_give_install_permission);
        Uri packageURI = Uri.parse("package:" + getPackageName());
        Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, packageURI);
        startActivityForResult(intent, REQUEST_INSTALL_PACKAGES);
      }
    } else {
      installApk(context, path);
    }
  }

  private void dismissForceDialog() {
    if (mForceDialog != null && mForceDialog.isShowing()) {
      mForceDialog.dismiss();
      mForceDialog = null;
    }
  }

  private void showForceDialog() {
    dismissForceDialog();
    mForceDialog = Theme.getBaseDialog(mContext)
        .canceledOnTouchOutside(false)
        .cancelable(false)
        .title(R.string.updating)
        .content(R.string.please_wait)
        .progress(true, 0)
        .progressIndeterminateStyle(false).build();
    mForceDialog.show();
  }


  private MaterialDialog mForceDialog;

  public static class MainReceiver extends BroadcastReceiver {

    private final WeakReference<MainActivity> mRef;

    MainReceiver(MainActivity mainActivity) {
      mRef = new WeakReference<>(mainActivity);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent == null) {
        return;
      }
      final String action = intent.getAction();
      if (TextUtils.isEmpty(action) || mRef.get() == null) {
        return;
      }
      MainActivity mainActivity = mRef.get();
      switch (action) {
        case ACTION_DOWNLOAD_COMPLETE:
          mainActivity.checkIsAndroidO(context, intent.getStringExtra(DownloadService.EXTRA_PATH));
          break;
        case ACTION_SHOW_DIALOG:
          mainActivity.showForceDialog();
          break;
        case ACTION_DISMISS_DIALOG:
          mainActivity.dismissForceDialog();
          break;
      }

    }
  }
}

