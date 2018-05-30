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
import android.os.Bundle;
import android.os.Message;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.adapter.DrawerAdapter;
import remix.myplayer.adapter.MainPagerAdapter;
import remix.myplayer.bean.Category;
import remix.myplayer.bean.CustomThumb;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.helper.SortOrder;
import remix.myplayer.helper.UpdateHelper;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.misc.cache.DiskCache;
import remix.myplayer.misc.handler.MsgHandler;
import remix.myplayer.misc.handler.OnHandleMessage;
import remix.myplayer.misc.receiver.ExitReceiver;
import remix.myplayer.request.LibraryUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.request.SimpleUriRequest;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.fragment.AlbumFragment;
import remix.myplayer.ui.fragment.ArtistFragment;
import remix.myplayer.ui.fragment.BottomActionBarFragment;
import remix.myplayer.ui.fragment.LibraryFragment;
import remix.myplayer.ui.fragment.PlayListFragment;
import remix.myplayer.ui.fragment.SongFragment;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.StatusBarUtil;
import remix.myplayer.util.ToastUtil;
import remix.myplayer.util.Util;

import static remix.myplayer.bean.Category.DEFAULT_LIBRARY;
import static remix.myplayer.service.MusicService.ACTION_LOAD_FINISH;
import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 *
 */
public class MainActivity extends MultiChoiceActivity implements UpdateHelper.Callback {
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

    private BottomActionBarFragment mBottomBar;
    private final static String TAG = "MainActivity";
    private DrawerAdapter mDrawerAdapter;
    private MainPagerAdapter mPagerAdapter;
    //是否正在运行
    private static boolean mIsRunning = false;

    private MsgHandler mRefreshHandler;
    //设置界面
    private final int REQUEST_SETTING = 1;
    private BroadcastReceiver mLoadReceiver;

    //当前选中的fragment
    private LibraryFragment mCurrentFragment;
    @Override
    protected void onResume() {
        super.onResume();
        if(mMultiChoice.isShow()){
            mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
        }
        mIsRunning = true;
        UpdateUI(MusicService.getCurrentMP3(), MusicService.isPlay());
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsRunning = false;
        if(mMultiChoice.isShow()){
            mRefreshHandler.sendEmptyMessageDelayed(Constants.CLEAR_MULTI,500);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Util.unregisterReceiver(mContext,mLoadReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        //初始化底部状态栏
        mBottomBar = (BottomActionBarFragment) getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);
        //receiver
        mLoadReceiver = new LoadFinishReceiver();
        registerReceiver(mLoadReceiver,new IntentFilter(MusicService.ACTION_LOAD_FINISH));
        //初始化控件
        setUpToolbar(mToolBar);
        setUpPager();
        setUpTab();
        //初始化测滑菜单
        setUpDrawerLayout();
        setUpViewColor();
        //handler
        mRefreshHandler = new MsgHandler(this);

        parseIntent();

//        new MaterialDialog.Builder(this)
//                .title("暂停应用内更新")
//                .titleColorAttr(R.attr.text_color_primary)
//                .content("后续版本将不再提供应用内更新的功能,获取最新版本可前往360、小米、酷安等应用市场")
//                .contentColorAttr(R.attr.text_color_primary)
//                .positiveText(R.string.choose)
//                .positiveColorAttr(R.attr.text_color_primary)
//                .buttonRippleColorAttr(R.attr.ripple_color)
//                .backgroundColorAttr(R.attr.background_color_3)
//                .itemsColorAttr(R.attr.text_color_primary)
//                .theme(ThemeStore.getMDDialogTheme())
//                .show();
    }

    /**
     * 初始化底部显示控件
     */
    private void setUpBottomBar() {
        //初始化底部状态栏
        mBottomBar = (BottomActionBarFragment) getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);
        int lastId = SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.LAST_SONG_ID,-1);
        Song item;
        if(lastId > 0 && (item = MediaStoreUtil.getMP3InfoById(lastId)) != null) {
            mBottomBar.updateBottomStatus(item,  MusicService.isPlay());
        } else {
            if(Global.PlayQueue == null || Global.PlayQueue.size() == 0)
                return ;
            int id =  Global.PlayQueue.get(0);
            for(int i = 0; i < Global.PlayQueue.size() ; i++){
                id = Global.PlayQueue.get(i);
                if (id != lastId)
                    break;
            }
            item = MediaStoreUtil.getMP3InfoById(id);
            if(item != null){
                mBottomBar.updateBottomStatus(item,  MusicService.isPlay());
            }
        }
    }

    @Override
    protected void setStatusBar() {
        StatusBarUtil.setColorNoTranslucentForDrawerLayout(this,
                findViewById(R.id.drawer_layout),
                ThemeStore.getStatusBarColor());
    }

    /**
     * 初始化toolbar
     * @param toolbar
     */
    protected void setUpToolbar(Toolbar toolbar) {
        super.setUpToolbar(toolbar,"");
        if(mToolBar != null){
            mToolBar.setTitle("");
            int themeColor = ColorUtil.getColor(ThemeStore.isLightTheme() ? R.color.black : R.color.white);
            toolbar.setNavigationIcon(Theme.TintDrawable(R.drawable.actionbar_menu,themeColor));
            mToolBar.setNavigationOnClickListener(v -> mDrawerLayout.openDrawer(mNavigationView));
        }
    }

    /**
     * 新建播放列表
     * @param v
     */
    @OnClick({R.id.add,R.id.multi_close})
    public void onClick(View v){
        switch (v.getId()){
            case R.id.add:
                if(mMultiChoice.isShow())
                    return;
                new MaterialDialog.Builder(mContext)
                        .title(R.string.new_playlist)
                        .titleColorAttr(R.attr.text_color_primary)
                        .positiveText(R.string.create)
                        .positiveColorAttr(R.attr.text_color_primary)
                        .negativeText(R.string.cancel)
                        .negativeColorAttr(R.attr.text_color_primary)
                        .buttonRippleColorAttr(R.attr.ripple_color)
                        .backgroundColorAttr(R.attr.background_color_3)
                        .contentColorAttr(R.attr.text_color_primary)
                        .inputRange(1,15)
                        .input("", getString(R.string.local_list) + Global.PlayList.size(), (dialog,input) ->{
                            int newPlayListId;
                            try {
                                if(!TextUtils.isEmpty(input)){
                                    newPlayListId = PlayListUtil.addPlayList(input.toString());
                                    ToastUtil.show(mContext, newPlayListId > 0 ?
                                                    R.string.add_playlist_success :
                                                    newPlayListId == -1 ? R.string.add_playlist_error : R.string.playlist_already_exist,
                                            Toast.LENGTH_SHORT);
                                    if(newPlayListId > 0){
                                        //跳转到添加歌曲界面
                                        Intent intent = new Intent(mContext,SongChooseActivity.class);
                                        intent.putExtra("PlayListID",newPlayListId);
                                        intent.putExtra("PlayListName",input.toString());
                                        startActivity(intent);
                                    }
                                }
                            } catch (Exception e){
                                ToastUtil.show(mContext,"创建播放列表错误:" + e.toString());
                            }
                        })
                        .show();
                break;
            case R.id.multi_close:
                mMultiToolBar.setVisibility(View.GONE);
                mToolBar.setVisibility(View.VISIBLE);
                if(mMultiChoice.isShow()){
                    mMultiChoice.updateOptionMenu(false);
                    mMultiChoice.clear();
                }
                break;
        }
    }

    //初始化ViewPager
    private void setUpPager() {
        String categoryJson = SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME, SPUtil.SETTING_KEY.LIBRARY_CATEGORY,"");
        List<Category> categories = TextUtils.isEmpty(categoryJson) ? new ArrayList<>() : new Gson().fromJson(categoryJson,new TypeToken<List<Category>>(){}.getType());
        if(categories.size() == 0){
            categories.addAll(DEFAULT_LIBRARY);
            SPUtil.putValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME, SPUtil.SETTING_KEY.LIBRARY_CATEGORY,new Gson().toJson(DEFAULT_LIBRARY,new TypeToken<List<Category>>(){}.getType()));
        }
        mPagerAdapter = new MainPagerAdapter(getSupportFragmentManager());
        mPagerAdapter.setList(categories);
        mMenuLayoutId = parseMenuId(mPagerAdapter.getList().get(0).getTag());
        //有且仅有播放列表一个tab
        if(categories.size() == 1 && categories.get(0).getTag() == R.string.tab_playlist){
            showAddPlayListButton(true);
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
                showAddPlayListButton(mPagerAdapter.getList().get(position).getTitle().equals(getString(R.string.tab_playlist)));
                mMenuLayoutId = parseMenuId(mPagerAdapter.getList().get(position).getTag());
                mCurrentFragment = (LibraryFragment) mPagerAdapter.getItem(position);
                invalidateOptionsMenu();
            }
            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        mCurrentFragment = (LibraryFragment) mPagerAdapter.getItem(0);
    }

    private int mMenuLayoutId = R.menu.menu_main;
    public int parseMenuId(int tag) {
        return tag == Category.TAG_SONG ? R.menu.menu_main :
                tag == Category.TAG_ALBUM ? R.menu.menu_album :
                tag == Category.TAG_ARTIST ? R.menu.menu_artist :
                tag == Category.TAG_PLAYLIST ? R.menu.menu_playlist :
                tag ==  Category.TAG_FOLDER ? R.menu.menu_folder : R.menu.menu_main_simple;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        String sortOrder = null;

        if(mCurrentFragment instanceof SongFragment){
            sortOrder = SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.SONG_SORT_ORDER, SortOrder.SongSortOrder.SONG_A_Z);
        } else if(mCurrentFragment instanceof AlbumFragment){
            sortOrder = SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.ALBUM_SORT_ORDER,SortOrder.AlbumSortOrder.ALBUM_A_Z);
        } else if(mCurrentFragment instanceof ArtistFragment){
            sortOrder = SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.ARTIST_SORT_ORDER,SortOrder.ArtistSortOrder.ARTIST_A_Z);
        } else if(mCurrentFragment instanceof PlayListFragment){
            sortOrder = SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.PLAYLIST_SORT_ORDER,SortOrder.PlayListSortOrder.PLAYLIST_DATE);
        }

        if(TextUtils.isEmpty(sortOrder)){
            return true;
        }
        setUpMenuItem(menu, sortOrder);
        return true;
    }


    @Override
    protected int getMenuLayoutId() {
        return mMenuLayoutId;
    }

    @Override
    protected void saveSortOrder(String sortOrder) {
        if(mCurrentFragment instanceof SongFragment){
            SPUtil.putValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.SONG_SORT_ORDER,sortOrder);
        } else if(mCurrentFragment instanceof AlbumFragment){
            SPUtil.putValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.ALBUM_SORT_ORDER,sortOrder);
        } else if(mCurrentFragment instanceof ArtistFragment){
            SPUtil.putValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.ARTIST_SORT_ORDER,sortOrder);
        } else if(mCurrentFragment instanceof PlayListFragment ){
            SPUtil.putValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.PLAYLIST_SORT_ORDER,sortOrder);
        }
        mCurrentFragment.onMediaStoreChanged();
    }

    private void showAddPlayListButton(boolean show) {
        if(show){
            mAddButton.setVisibility(View.VISIBLE);
            SpringSystem.create().createSpring()
                    .addListener(new SimpleSpringListener(){
                        @Override
                        public void onSpringUpdate(Spring spring) {
                            mAddButton.setScaleX((float) spring.getCurrentValue());
                            mAddButton.setScaleY((float) spring.getCurrentValue());
                        }
                    })
                    .setEndValue(1);
        }else {
            mAddButton.setVisibility(View.GONE);
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
        mTablayout.setSelectedTabIndicatorHeight(DensityUtil.dip2px(this,3));
        mTablayout.setTabTextColors(ColorUtil.getColor(isLightColor ? R.color.dark_normal_tab_text_color : R.color.light_normal_tab_text_color),
                ColorUtil.getColor(isLightColor ? R.color.black : R.color.white));

//        AppBarLayout appBarLayout = findView(R.id.appbar);
//        appBarLayout.addView(mTablayout);
    }

    /**
     * 设置夜间模式
     * @param isNight
     */
    private void setNightMode(boolean isNight){
        ThemeStore.THEME_MODE = isNight ? ThemeStore.NIGHT : ThemeStore.DAY;
        ThemeStore.THEME_COLOR = ThemeStore.loadThemeColor();
        ThemeStore.MATERIAL_COLOR_PRIMARY = ThemeStore.getMaterialPrimaryColorRes();
        ThemeStore.MATERIAL_COLOR_PRIMARY_DARK = ThemeStore.getMaterialPrimaryDarkColorRes();
        ThemeStore.saveThemeMode(ThemeStore.THEME_MODE);
        mRefreshHandler.sendEmptyMessage(Constants.RECREATE_ACTIVITY);
    }


    private void setUpDrawerLayout() {
        mDrawerAdapter = new DrawerAdapter(this,R.layout.item_drawer);
        mDrawerAdapter.setOnModeChangeListener(this::setNightMode);
        mDrawerAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                switch (position){
                    //歌曲库
                    case 0:
                        mDrawerLayout.closeDrawer(mNavigationView);
                        break;
                    //最近添加
                    case 1:
                        startActivity(new Intent(mContext,RecentlyActivity.class));
                        break;
                    //夜间模式
                    case 2:
                        setNightMode(ThemeStore.isDay());
                        break;
                    //捐赠
                    case 3:
                        startActivity(new Intent(mContext,SupportDevelopActivity.class));
                        break;
                    //设置
                    case 4:
                        startActivityForResult(new Intent(mContext,SettingActivity.class), REQUEST_SETTING);
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
                if(mDrawerAdapter != null)
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
        bg.setCornerRadius(DensityUtil.dip2px(this,4));
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
        if(data != null){
            switch (requestCode){
                case REQUEST_SETTING:
                    if(data.getBooleanExtra("needRecreate",false)) { //设置后需要重启activity
                        mRefreshHandler.sendEmptyMessage(Constants.RECREATE_ACTIVITY);
                    } else if(data.getBooleanExtra("needRefreshAdapter",false)){ //清除缓存后刷新adapter
                        mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
                    } else if(data.getBooleanExtra("needRefreshLibrary",false)){ //刷新Library
                        List<Category> categories = (List<Category>) data.getSerializableExtra("Category");
                        if(categories != null && categories.size() > 0){
                            mViewPager.setOffscreenPageLimit(categories.size() - 1);
                            mPagerAdapter.setList(categories);
                            mPagerAdapter.notifyDataSetChanged();
                            mMenuLayoutId = parseMenuId(mPagerAdapter.getList().get(mViewPager.getCurrentItem()).getTag());
                            mCurrentFragment = (LibraryFragment) mPagerAdapter.getItem(mViewPager.getCurrentItem());
                            invalidateOptionsMenu();
                        }
                    }
                    break;
                //图片选择
                case Crop.REQUEST_CROP:
                case Crop.REQUEST_PICK:
                    Intent intent = getIntent();
                    final CustomThumb thumbBean = intent.getParcelableExtra("thumb");
                    if(thumbBean == null)
                        break;
                    String errorTxt = getString(
                            thumbBean.getType() == Constants.ALBUM ? R.string.set_album_cover_error : thumbBean.getType() == Constants.ARTIST ? R.string.set_artist_cover_error : R.string.set_playlist_cover_error);
                    final int id = thumbBean.getId(); //专辑、艺术家、播放列表封面

                    if(resultCode != Activity.RESULT_OK) {
                        ToastUtil.show(this,errorTxt);
                        break;
                    }
                    if(requestCode == Crop.REQUEST_PICK){
                        //选择图片
                        File cacheDir = DiskCache.getDiskCacheDir(this,
                                "thumbnail/" + (thumbBean.getType() == Constants.ALBUM ? "album" : thumbBean.getType() == Constants.ARTIST ? "artist" : "playlist"));
                        if(!cacheDir.exists() && !cacheDir.mkdirs()){
                            ToastUtil.show(this,errorTxt);
                            return;
                        }
                        Uri destination = Uri.fromFile(new File(cacheDir, Util.hashKeyForDisk((id * 255 ) + "")));
                        Crop.of(data.getData(), destination).asSquare().start(this);
                    } else {
                        //图片裁剪
                        //裁剪后的图片路径
                        if(Crop.getOutput(data) == null)
                            return;

                        final String path = Crop.getOutput(data).getEncodedPath();
                        if(TextUtils.isEmpty(path) || id == -1){
                            ToastUtil.show(mContext,errorTxt);
                            return;
                        }
                        //清除fresco的缓存
                        //如果设置的是专辑封面 修改内嵌封面
                        Observable.create((ObservableOnSubscribe<Uri>) emitter -> {
                            if(thumbBean.getType() == Constants.ALBUM){
                                saveArtwork(thumbBean.getId(),new File(path));
                            }
                            if(thumbBean.getType() == Constants.ALBUM){
                                new SimpleUriRequest(getSearchRequestWithAlbumType(MediaStoreUtil.getMP3InfoByAlbumId(thumbBean.getId()))){
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
                            } else{
                                emitter.onNext(Uri.parse("file://" + path));
                                emitter.onComplete();
                            }
                        }).compose(RxUtil.applyScheduler())
                        .doFinally(() -> mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER))
                        .subscribe(uri -> {
                            ImagePipeline imagePipeline = Fresco.getImagePipeline();
                            imagePipeline.evictFromCache(uri);
                            imagePipeline.evictFromDiskCache(uri);
                        }, throwable -> ToastUtil.show(mContext,R.string.tag_save_error,throwable.toString()));
                    }
                    break;
            }
        }
    }

    private void saveArtwork(int albumId, File artFile) throws TagException, ReadOnlyFileException, CannotReadException, InvalidAudioFrameException, IOException, CannotWriteException {
        Song song = MediaStoreUtil.getMP3InfoByAlbumId(albumId);
        if(song == null)
            return;
        AudioFile audioFile = AudioFileIO.read(new File(song.getUrl()));
        Tag tag = audioFile.getTagOrCreateAndSetDefault();
        Artwork artwork = ArtworkFactory.createArtworkFromFile(artFile);
        tag.deleteArtworkField();
        tag.setField(artwork);
        audioFile.commit();
        MediaStoreUtil.insertAlbumArt(this,albumId,artFile.getAbsolutePath());
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
            mDrawerLayout.closeDrawer(mNavigationView);
        } else if(mMultiChoice.isShow()) {
            onMultiBackPress();
        } else {
            super.onBackPressed();
        }
    }

    //更新界面
    @Override
    public void UpdateUI(Song song, boolean isplay) {
        if (!mIsRunning)
            return;

        mBottomBar.updateBottomStatus(song, isplay);
//        for(Fragment temp : getSupportFragmentManager().getFragments()) {
//            if (temp instanceof SongFragment) {
//                SongFragment songFragment = (SongFragment) temp;
//                if(songFragment.getAdapter() != null){
//                    songFragment.getAdapter().onUpdateHighLight();
//                }
//            }
//        }
        updateHeader(song,isplay);
    }

    /**
     * 更新侧滑菜单
     * @param song
     */
    private static final int IMAGE_SIZE = DensityUtil.dip2px(App.getContext(),108);
    private void updateHeader(Song song, boolean isPlay) {
        if(song == null)
            return;
        mHeadText.setText(getString(R.string.play_now, song.getTitle()));
        new LibraryUriRequest(mHeadImg,
                getSearchRequestWithAlbumType(song),
                new RequestConfig.Builder(IMAGE_SIZE,IMAGE_SIZE).build()).load();
        mHeadImg.setBackgroundResource(isPlay && ThemeStore.isDay() ? R.drawable.drawer_bg_album_shadow : R.color.transparent);
    }

    @OnHandleMessage
    public void handleInternal(Message msg){
        if(msg.what == Constants.RECREATE_ACTIVITY) {
            recreate();
        } else if(msg.what == Constants.CLEAR_MULTI){
            mMultiChoice.clearSelectedViews();
        } else if (msg.what == Constants.UPDATE_ADAPTER){
            //刷新适配器
            for(Fragment temp : getSupportFragmentManager().getFragments()){
                if(temp instanceof LibraryFragment){
                    ((LibraryFragment) temp).getAdapter().notifyDataSetChanged();
                }
            }
        }
    }

    private static boolean mLoadComplete = false;
    private class LoadFinishReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent receive) {
            LogUtil.d("StartAPlayer","receiveBroadcast");
            if(ACTION_LOAD_FINISH.equals(receive != null ? receive.getAction() : "") && !mLoadComplete){
                setUpBottomBar();
                mLoadComplete = true;
            }
            if(mLoadComplete){
                parseIntent();
            }

        }
    }

    private void parseIntent() {
        final Intent param = getIntent();
        if(param != null && param.getData() != null && mLoadComplete){
            int id = MediaStoreUtil.getSongIdByUrl(Uri.decode(param.getData().getPath()));
            if(id < 0)
                return;
            Intent intent = new Intent(MusicService.ACTION_CMD);
            Bundle arg = new Bundle();
            arg.putInt("Control", Constants.PLAYSELECTEDSONG);
            arg.putInt("Position", 0);
            intent.putExtras(arg);
            ArrayList<Integer> list = new ArrayList<>();
            list.add(id);
            Global.setPlayQueue(list,mContext,intent);
            setIntent(new Intent());
        }
    }
}

