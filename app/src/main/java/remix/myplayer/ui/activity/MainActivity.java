package remix.myplayer.ui.activity;


import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.soundcloud.android.crop.Crop;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import remix.myplayer.App;
import remix.myplayer.Global;
import remix.myplayer.R;
import remix.myplayer.bean.misc.Category;
import remix.myplayer.bean.misc.CustomThumb;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.helper.SortOrder;
import remix.myplayer.misc.cache.DiskCache;
import remix.myplayer.misc.handler.MsgHandler;
import remix.myplayer.misc.handler.OnHandleMessage;
import remix.myplayer.misc.interfaces.OnItemClickListener;
import remix.myplayer.misc.receiver.ExitReceiver;
import remix.myplayer.misc.update.DownloadService;
import remix.myplayer.misc.update.UpdateAgent;
import remix.myplayer.misc.update.UpdateListener;
import remix.myplayer.request.LibraryUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.request.SimpleUriRequest;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultipleChoice;
import remix.myplayer.ui.adapter.DrawerAdapter;
import remix.myplayer.ui.adapter.MainPagerAdapter;
import remix.myplayer.ui.fragment.AlbumFragment;
import remix.myplayer.ui.fragment.ArtistFragment;
import remix.myplayer.ui.fragment.FolderFragment;
import remix.myplayer.ui.fragment.LibraryFragment;
import remix.myplayer.ui.fragment.PlayListFragment;
import remix.myplayer.ui.fragment.SongFragment;
import remix.myplayer.ui.widget.fastcroll_recyclerview.LocationRecyclerView;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.MusicUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.StatusBarUtil;
import remix.myplayer.util.ToastUtil;
import remix.myplayer.util.Util;

import static remix.myplayer.App.IS_GOOGLEPLAY;
import static remix.myplayer.bean.misc.Category.DEFAULT_LIBRARY;
import static remix.myplayer.misc.update.DownloadService.ACTION_DISMISS_DIALOG;
import static remix.myplayer.misc.update.DownloadService.ACTION_DOWNLOAD_COMPLETE;
import static remix.myplayer.misc.update.DownloadService.ACTION_SHOW_DIALOG;
import static remix.myplayer.ui.activity.SongChooseActivity.EXTRA_ID;
import static remix.myplayer.ui.activity.SongChooseActivity.EXTRA_NAME;
import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;
import static remix.myplayer.util.Util.installApk;
import static remix.myplayer.util.Util.registerLocalReceiver;
import static remix.myplayer.util.Util.unregisterLocalReceiver;

/**
 *
 */
public class MainActivity extends MenuActivity {
    private final static String TAG = "MainActivity";
    public static final String EXTRA_RECREATE = "needRecreate";
    public static final String EXTRA_REFRESH_ADAPTER = "needRefreshAdapter";
    public static final String EXTRA_REFRESH_LIBRARY = "needRefreshLibrary";
    public static final String EXTRA_CATEGORY = "Category";

    public static final long DELAY_HIDE_LOCATION = TimeUnit.SECONDS.toMillis(4);

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
    @BindView(R.id.location)
    ImageView mLocation;
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
    private RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (Math.abs(dy) > 1) {
                showViewWithAnim(mLocation, true);
                mLocation.removeCallbacks(mLocationRunnable);
                mLocation.postDelayed(mLocationRunnable,DELAY_HIDE_LOCATION);
            }
        }
    };
    private Runnable mLocationRunnable = new Runnable() {
        @Override
        public void run() {
            mLocation.setVisibility(View.GONE);
        }
    };

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
        setUpToolbar(findViewById(R.id.toolbar));
        setUpPager();
        setUpTab();
        //初始化测滑菜单
        setUpDrawerLayout();
        setUpViewColor();
        //handler
        mRefreshHandler = new MsgHandler(this);
        mRefreshHandler.postDelayed(this::checkUpdate, 500);
        mRefreshHandler.postDelayed(this::parseIntent, 500);
    }

    @Override
    protected void setStatusBarColor() {
        StatusBarUtil.setColorNoTranslucentForDrawerLayout(this,
                findViewById(R.id.drawer_layout),
                ThemeStore.getStatusBarColor());
    }

    /**
     * 初始化toolbar
     *
     * @param toolbar
     */
    protected void setUpToolbar(Toolbar toolbar) {
        super.setUpToolbar(toolbar, "");
        if (toolbar != null) {
            toolbar.setTitle("");
            int themeColor = ColorUtil.getColor(ThemeStore.isLightTheme() ? R.color.black : R.color.white);
            toolbar.setNavigationIcon(Theme.TintDrawable(R.drawable.actionbar_menu, themeColor));
            toolbar.setNavigationOnClickListener(v -> mDrawerLayout.openDrawer(mNavigationView));
        }
    }

    /**
     * 新建播放列表
     *
     * @param v
     */
    @OnClick({R.id.add, R.id.location})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.add:
                if (MultipleChoice.isActiveSomeWhere()) {
                    return;
                }
                Theme.getBaseDialog(mContext)
                        .title(R.string.new_playlist)
                        .positiveText(R.string.create)
                        .negativeText(R.string.cancel)
                        .inputRange(1, 15)
                        .input("", getString(R.string.local_list) + Global.PlayList.size(), (dialog, input) -> {
                            int newPlayListId;
                            try {
                                if (!TextUtils.isEmpty(input)) {
                                    newPlayListId = PlayListUtil.addPlayList(input.toString());
                                    ToastUtil.show(mContext, newPlayListId > 0 ?
                                                    R.string.add_playlist_success :
                                                    newPlayListId == -1 ? R.string.add_playlist_error : R.string.playlist_already_exist,
                                            Toast.LENGTH_SHORT);
                                    if (newPlayListId > 0) {
                                        //跳转到添加歌曲界面
                                        Intent intent = new Intent(mContext, SongChooseActivity.class);
                                        intent.putExtra(EXTRA_ID, newPlayListId);
                                        intent.putExtra(EXTRA_NAME, input.toString());
                                        startActivity(intent);
                                    }
                                }
                            } catch (Exception e) {
                                ToastUtil.show(mContext, R.string.create_playlist_fail, e.toString());
                            }
                        })
                        .show();
                break;
            case R.id.location:
                List<Fragment> fragments = getSupportFragmentManager().getFragments();
                for (Fragment fragment : fragments) {
                    if (fragment instanceof SongFragment) {
                        ((SongFragment) fragment).scrollToCurrent();
                    }
                }
                break;
        }
    }

    //初始化ViewPager
    private void setUpPager() {
        String categoryJson = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LIBRARY_CATEGORY, "");
        List<Category> categories = TextUtils.isEmpty(categoryJson) ? new ArrayList<>() : new Gson().fromJson(categoryJson, new TypeToken<List<Category>>() {
        }.getType());
        if (categories.size() == 0) {
            categories.addAll(DEFAULT_LIBRARY);
            SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LIBRARY_CATEGORY, new Gson().toJson(DEFAULT_LIBRARY, new TypeToken<List<Category>>() {
            }.getType()));
        }
        mPagerAdapter = new MainPagerAdapter(getSupportFragmentManager());
        mPagerAdapter.setList(categories);
        mMenuLayoutId = parseMenuId(mPagerAdapter.getList().get(0).getTag());
        //有且仅有一个tab
        if (categories.size() == 1) {
            if (categories.get(0).isPlayList())
                showViewWithAnim(mAddButton, true);
        }

        mAddButton.setImageResource(ThemeStore.isDay() ? R.drawable.icon_floatingbtn_day : R.drawable.icon_floatingbtn_night);
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
                if(!category.isSongList()){ //滑动到其他tab时隐藏歌曲列表的定位按钮
                    showViewWithAnim(mLocation, false);
                }

                mMenuLayoutId = parseMenuId(mPagerAdapter.getList().get(position).getTag());
                mCurrentFragment = (LibraryFragment) mPagerAdapter.getFragment(position);

                invalidateOptionsMenu();

                addScrollListener();
            }


            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        mCurrentFragment = (LibraryFragment) mPagerAdapter.getFragment(0);

        mLocation.setImageDrawable(Theme.TintVectorDrawable(this,R.drawable.ic_my_location_black_24dp, ThemeStore.getAccentColor()));
        mLocation.postDelayed(this::addScrollListener,500);
    }

    private int mMenuLayoutId = R.menu.menu_main;
    public int parseMenuId(int tag) {
        return tag == Category.TAG_SONG ? R.menu.menu_main :
                tag == Category.TAG_ALBUM ? R.menu.menu_album :
                        tag == Category.TAG_ARTIST ? R.menu.menu_artist :
                                tag == Category.TAG_PLAYLIST ? R.menu.menu_playlist :
                                        tag == Category.TAG_FOLDER ? R.menu.menu_folder : R.menu.menu_main_simple;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (mCurrentFragment instanceof FolderFragment)
            return true;
        String sortOrder = null;
        if (mCurrentFragment instanceof SongFragment) {
            sortOrder = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_A_Z);
        } else if (mCurrentFragment instanceof AlbumFragment) {
            sortOrder = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ALBUM_SORT_ORDER, SortOrder.AlbumSortOrder.ALBUM_A_Z);
        } else if (mCurrentFragment instanceof ArtistFragment) {
            sortOrder = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ARTIST_SORT_ORDER, SortOrder.ArtistSortOrder.ARTIST_A_Z);
        } else if (mCurrentFragment instanceof PlayListFragment) {
            sortOrder = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAYLIST_SORT_ORDER, SortOrder.PlayListSortOrder.PLAYLIST_DATE);
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
            SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.SONG_SORT_ORDER, sortOrder);
        } else if (mCurrentFragment instanceof AlbumFragment) {
            SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ALBUM_SORT_ORDER, sortOrder);
        } else if (mCurrentFragment instanceof ArtistFragment) {
            SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.ARTIST_SORT_ORDER, sortOrder);
        } else if (mCurrentFragment instanceof PlayListFragment) {
            SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAYLIST_SORT_ORDER, sortOrder);
        }
        mCurrentFragment.onMediaStoreChanged();
    }

    private void addScrollListener() {
        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        if(recyclerView instanceof LocationRecyclerView){
            LocationRecyclerView locationRecyclerView = (LocationRecyclerView) recyclerView;
            locationRecyclerView.removeOnScrollListener(mScrollListener);
            locationRecyclerView.addOnScrollListener(mScrollListener);
        }
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
        boolean isLightColor = ThemeStore.isLightTheme();
//        mTablayout = new TabLayout(new ContextThemeWrapper(this, !ColorUtil.isColorLight(ThemeStore.getMaterialPrimaryColor()) ? R.style.CustomTabLayout_Light : R.style.CustomTabLayout_Dark));
//        mTablayout = new TabLayout(new ContextThemeWrapper(this,R.style.CustomTabLayout_Light));
//        mTablayout.setLayoutParams(new AppBarLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,DensityUtil.dip2px(this,48)));
//        mTablayout = new TabLayout(this);
        mTablayout.addTab(mTablayout.newTab().setText(R.string.tab_song));
        mTablayout.addTab(mTablayout.newTab().setText(R.string.tab_album));
        mTablayout.addTab(mTablayout.newTab().setText(R.string.tab_artist));
        mTablayout.addTab(mTablayout.newTab().setText(R.string.tab_playlist));
        mTablayout.addTab(mTablayout.newTab().setText(R.string.tab_folder));
        //viewpager与tablayout关联
        mTablayout.setupWithViewPager(mViewPager);
        mTablayout.setSelectedTabIndicatorColor(ColorUtil.getColor(isLightColor ? R.color.black : R.color.white));
        mTablayout.setSelectedTabIndicatorHeight(DensityUtil.dip2px(this, 3));
        mTablayout.setTabTextColors(ColorUtil.getColor(isLightColor ? R.color.dark_normal_tab_text_color : R.color.light_normal_tab_text_color),
                ColorUtil.getColor(isLightColor ? R.color.black : R.color.white));

//        AppBarLayout appBarLayout = findView(R.id.appbar);
//        appBarLayout.addView(mTablayout);
    }

    /**
     * 设置夜间模式
     *
     * @param isNight
     */
    private void setNightMode(boolean isNight) {
        ThemeStore.THEME_MODE = isNight ? ThemeStore.NIGHT : ThemeStore.DAY;
        ThemeStore.THEME_COLOR = ThemeStore.loadThemeColor();
        ThemeStore.MATERIAL_COLOR_PRIMARY = ThemeStore.getMaterialPrimaryColorRes();
        ThemeStore.MATERIAL_COLOR_PRIMARY_DARK = ThemeStore.getMaterialPrimaryDarkColorRes();
        ThemeStore.saveThemeMode(ThemeStore.THEME_MODE);
        mRefreshHandler.sendEmptyMessage(Constants.RECREATE_ACTIVITY);
    }


    private void setUpDrawerLayout() {
        mDrawerAdapter = new DrawerAdapter(this, R.layout.item_drawer);
        mDrawerAdapter.setOnModeChangeListener(this::setNightMode);
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
                    //夜间模式
                    case 2:
                        setNightMode(ThemeStore.isDay());
                        break;
                    //捐赠
                    case 3:
                        startActivity(new Intent(mContext, SupportDevelopActivity.class));
                        break;
                    //设置
                    case 4:
                        startActivityForResult(new Intent(mContext, SettingActivity.class), REQUEST_SETTING);
                        break;
                    //退出
                    case 5:
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
                if (mDrawerAdapter != null)
                    mDrawerAdapter.setSelectIndex(0);
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
        bg.setColor(ThemeStore.isDay() ?
                ThemeStore.isLightTheme() ? Color.TRANSPARENT : ThemeStore.getMaterialPrimaryDarkColor() :
                ColorUtil.getColor(R.color.gray_343438));
        bg.setCornerRadius(DensityUtil.dip2px(this, 4));
        mHeadText.setBackground(bg);
        mHeadText.setTextColor(ColorUtil.getColor(ThemeStore.isDay() ?
                ThemeStore.isLightTheme() ? R.color.black : R.color.white :
                R.color.white_e5e5e5));
        //抽屉
        mHeadRoot.setBackgroundColor(ThemeStore.isDay() ? ThemeStore.getMaterialPrimaryColor() : ColorUtil.getColor(R.color.night_background_color_main));
        mNavigationView.setBackgroundColor(ColorUtil.getColor(ThemeStore.isDay() ? R.color.white : R.color.gray_343438));

    }

    @SuppressLint("CheckResult")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_SETTING:
                if (data == null)
                    return;
                if (data.getBooleanExtra(EXTRA_RECREATE, false)) { //设置后需要重启activity
                    mRefreshHandler.sendEmptyMessage(Constants.RECREATE_ACTIVITY);
                } else if (data.getBooleanExtra(EXTRA_REFRESH_ADAPTER, false)) { //刷新adapter
                    mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
                } else if (data.getBooleanExtra(EXTRA_REFRESH_LIBRARY, false)) { //刷新Library
                    List<Category> categories = (List<Category>) data.getSerializableExtra(EXTRA_CATEGORY);
                    LogUtil.d("MainPagerAdapter", "更新过后: " + categories);
                    if (categories != null && categories.size() > 0) {
                        mPagerAdapter.setList(categories);
                        mPagerAdapter.notifyDataSetChanged();
                        mViewPager.setOffscreenPageLimit(categories.size() - 1);
                        mMenuLayoutId = parseMenuId(mPagerAdapter.getList().get(mViewPager.getCurrentItem()).getTag());
                        mCurrentFragment = (LibraryFragment) mPagerAdapter.getFragment(mViewPager.getCurrentItem());
                        invalidateOptionsMenu();
                    }
                }
                break;
            //图片选择
            case Crop.REQUEST_CROP:
            case Crop.REQUEST_PICK:
                Intent intent = getIntent();
                final CustomThumb thumbBean = intent.getParcelableExtra("thumb");
                if (thumbBean == null)
                    break;
                String errorTxt = getString(
                        thumbBean.getType() == Constants.ALBUM ? R.string.set_album_cover_error : thumbBean.getType() == Constants.ARTIST ? R.string.set_artist_cover_error : R.string.set_playlist_cover_error);
                final int id = thumbBean.getId(); //专辑、艺术家、播放列表封面

                if (resultCode != Activity.RESULT_OK) {
                    ToastUtil.show(this, errorTxt);
                    break;
                }
                if (requestCode == Crop.REQUEST_PICK) {
                    //选择图片
                    File cacheDir = DiskCache.getDiskCacheDir(this,
                            "thumbnail/" + (thumbBean.getType() == Constants.ALBUM ? "album" : thumbBean.getType() == Constants.ARTIST ? "artist" : "playlist"));
                    if (!cacheDir.exists() && !cacheDir.mkdirs()) {
                        ToastUtil.show(this, errorTxt);
                        return;
                    }
                    Uri destination = Uri.fromFile(new File(cacheDir, Util.hashKeyForDisk((id * 255) + "")));
                    Crop.of(data.getData(), destination).asSquare().start(this);
                } else {
                    //图片裁剪
                    //裁剪后的图片路径
                    if (data == null)
                        return;
                    if (Crop.getOutput(data) == null)
                        return;

                    final String path = Crop.getOutput(data).getEncodedPath();
                    if (TextUtils.isEmpty(path) || id == -1) {
                        ToastUtil.show(mContext, errorTxt);
                        return;
                    }
                    //清除fresco的缓存
                    //如果设置的是专辑封面 修改内嵌封面
                    Observable.create((ObservableOnSubscribe<Uri>) emitter -> {
                        if (thumbBean.getType() == Constants.ALBUM) {
                            saveArtwork(thumbBean.getId(), new File(path));
                        }
                        if (thumbBean.getType() == Constants.ALBUM) {
                            new SimpleUriRequest(getSearchRequestWithAlbumType(MediaStoreUtil.getSongByAlbumId(thumbBean.getId()))) {
                                @Override
                                public void onError(String errMsg) {
                                    emitter.onError(new Throwable(errMsg));
                                }

                                @Override
                                public void onSuccess(Uri result) {
                                    emitter.onNext(result);
                                    emitter.onComplete();
                                }
                            }.load();
                        } else {
                            emitter.onNext(Uri.parse("file://" + path));
                            emitter.onComplete();
                        }
                    }).compose(RxUtil.applyScheduler())
                            .doFinally(() -> mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER))
                            .subscribe(uri -> {
                                ImagePipeline imagePipeline = Fresco.getImagePipeline();
                                imagePipeline.evictFromCache(uri);
                                imagePipeline.evictFromDiskCache(uri);
                            }, throwable -> ToastUtil.show(mContext, R.string.save_error, throwable.toString()));
                }
                break;
            case REQUEST_INSTALL_PACKAGES:
                if (resultCode == RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls())
                        return;
                    installApk(mContext, mInstallPath);
                }
                break;
        }
    }

    private void saveArtwork(int albumId, File artFile) throws TagException, ReadOnlyFileException, CannotReadException, InvalidAudioFrameException, IOException, CannotWriteException {
        Song song = MediaStoreUtil.getSongByAlbumId(albumId);
        if (song == null)
            return;
        AudioFile audioFile = AudioFileIO.read(new File(song.getUrl()));
        Tag tag = audioFile.getTagOrCreateAndSetDefault();
        Artwork artwork = ArtworkFactory.createArtworkFromFile(artFile);
        tag.deleteArtworkField();
        tag.setField(artwork);
        audioFile.commit();
        MediaStoreUtil.insertAlbumArt(this, albumId, artFile.getAbsolutePath());
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
        mHeadImg.setBackgroundResource(MusicServiceRemote.isPlaying() && ThemeStore.isDay() ? R.drawable.drawer_bg_album_shadow : R.color.transparent);
    }

    @Override
    public void onServiceConnected(@NotNull MusicService service) {
        super.onServiceConnected(service);
        onMetaChanged();
        onPlayStateChange();
    }

    @OnHandleMessage
    public void handleInternal(Message msg) {
        if (msg.what == Constants.RECREATE_ACTIVITY) {
            recreate();
        } else if (msg.what == Constants.CLEAR_MULTI) {
            for (Fragment temp : getSupportFragmentManager().getFragments()) {
                if (temp instanceof LibraryFragment) {
                    ((LibraryFragment) temp).getAdapter().notifyDataSetChanged();
                }
            }
        } else if (msg.what == Constants.UPDATE_ADAPTER) {
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
        if (!TextUtils.isEmpty(path) && !path.equals(mInstallPath))
            mInstallPath = path;
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
            if (intent == null)
                return;
            final String action = intent.getAction();
            if (TextUtils.isEmpty(action) || mRef.get() == null)
                return;
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

