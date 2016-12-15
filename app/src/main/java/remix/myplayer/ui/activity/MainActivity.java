package remix.myplayer.ui.activity;


import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
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
import com.soundcloud.android.crop.Crop;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.bmob.v3.update.BmobUpdateAgent;
import remix.myplayer.R;
import remix.myplayer.adapter.DrawerAdapter;
import remix.myplayer.adapter.PagerAdapter;
import remix.myplayer.asynctask.AsynLoadImage;
import remix.myplayer.fragment.AlbumFragment;
import remix.myplayer.fragment.ArtistFragment;
import remix.myplayer.fragment.BottomActionBarFragment;
import remix.myplayer.fragment.PlayListFragment;
import remix.myplayer.fragment.SongFragment;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.interfaces.OnModeChangeListener;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.DiskCache;
import remix.myplayer.util.Global;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.StatusBarUtil;
import remix.myplayer.util.ToastUtil;

/**
 *
 */
public class MainActivity extends MultiChoiceActivity implements MusicService.Callback {
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
    @BindView(R.id.recyclerview)
    RecyclerView mRecyclerView;

    private BottomActionBarFragment mBottomBar;
    private final static String TAG = "MainActivity";
    private DrawerAdapter mDrawerAdapter;
    private PagerAdapter mPagerAdapter;
    //是否正在运行
    private static boolean mIsRunning = false;
    //是否第一次启动
    private boolean mIsFirst = true;
    //是否是安装后第一次打开软件
    private boolean mIsFirstAfterInstall = true;
    //是否是第一次创建activity
    private static boolean mIsFirstCreateActivity = true;
    private static final int PERMISSIONCODE = 100;
    private static final String[] PERMISSIONS = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE};

    //设置界面
    private final int SETTING = 1;
    private Handler mRefreshHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == Constants.RECREATE_ACTIVITY) {
                recreate();
            }
            else if(msg.what == Constants.CLEAR_MULTI){
                mMultiChoice.clearSelectedViews();
            }
            else if (msg.what == Constants.UPDATE_ADAPTER || msg.what == Constants.UPDATE_ALLSONG_ADAPTER){
                boolean isAllSongOnly = msg.what == Constants.UPDATE_ALLSONG_ADAPTER;
                //刷新适配器
                for(Fragment temp : getSupportFragmentManager().getFragments()){
                    if(temp instanceof SongFragment){
                        SongFragment songFragment = (SongFragment)temp;
                        if(songFragment.getAdapter() != null){
                            songFragment.getAdapter().notifyDataSetChanged();
                        }
                        if(isAllSongOnly)
                            return;
                    }
                    if(temp instanceof AlbumFragment){
                        AlbumFragment albumFragment = (AlbumFragment)temp;
                        if(albumFragment.getAdapter() != null){
                            albumFragment.getAdapter().notifyDataSetChanged();
                        }
                    }
                    if(temp instanceof ArtistFragment){
                        ArtistFragment artistFragment = (ArtistFragment)temp;
                        if(artistFragment.getAdapter() != null){
                            artistFragment.getAdapter().notifyDataSetChanged();
                        }
                    }
                    if(temp instanceof PlayListFragment){
                        PlayListFragment playListFragment = (PlayListFragment) temp;
                        if(playListFragment.getAdapter() != null){
                            playListFragment.getAdapter().notifyDataSetChanged();
                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if(mMultiChoice.isShow()){
            mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
        }
        mIsRunning = true;
        //更新UI
        UpdateUI(MusicService.getCurrentMP3(), MusicService.getIsplay());
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        //播放的service
        MusicService.addCallback(this);
        setUpToolbar(mToolBar);
        setUpPager();
        setUpTab();
        //初始化测滑菜单
        setUpDrawerLayout();
        //初始化底部状态栏
        mBottomBar = (BottomActionBarFragment) getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);

        mIsFirstAfterInstall = SPUtil.getValue(this, "Setting", "First", true);
        SPUtil.putValue(this, "Setting", "First", false);

        if(mIsFirstCreateActivity)
            initLastSong();

        mBottomBar.UpdateBottomStatus(MusicService.getCurrentMP3(),  MusicService.getIsplay());

        BmobUpdateAgent.setUpdateOnlyWifi(false);
        BmobUpdateAgent.update(this);

    }

    /**
     * 初始化上一次退出时播放的歌曲
     * 默认为第一首歌曲
     */
    private void initLastSong() {
        //第一次打开软件，播放队列以及正在播放歌曲都为空
        mIsFirstCreateActivity = false;

        if(Global.mPlayQueue == null || Global.mPlayQueue.size() == 0)
            return;

        //读取上次退出时正在播放的歌曲的id
        int lastId = SPUtil.getValue(this,"Setting","LastSongId",0);
        //上次退出时正在播放的歌曲是否还存在
        boolean isLastSongExist = false;
        //上次退出时正在播放的歌曲的pos
        int pos = 0;
        //查找上次退出时的歌曲是否还存在
        for(int i = 0 ; i < Global.mPlayQueue.size();i++){
            if(lastId == Global.mPlayQueue.get(i)){
                isLastSongExist = true;
                pos = i;
                break;
            }
        }

        boolean isPlay = !mIsFirst && MusicService.getIsplay();
        mIsFirst = false;
        MP3Item item = null;
        //上次退出时保存的正在播放的歌曲未失效
        if(isLastSongExist && (item = MediaStoreUtil.getMP3InfoById(lastId)) != null) {
            mBottomBar.UpdateBottomStatus(item, isPlay);
            updateHeader(item,isPlay);
            MusicService.initDataSource(item,pos);
        }else {
            if(Global.mPlayQueue.size() > 0){
                //重新找到一个歌曲id
                int id =  Global.mPlayQueue.get(0);
                for(int i = 0; i < Global.mPlayQueue.size() ; i++){
                    id = Global.mPlayQueue.get(i);
                    if (id != lastId)
                        break;
                }
                item = MediaStoreUtil.getMP3InfoById(id);
                mBottomBar.UpdateBottomStatus(item,isPlay);
                updateHeader(item,isPlay);
                SPUtil.putValue(this,"Setting","LastSongId",id);
                MusicService.initDataSource(item,0);
            }
        }

    }

    @Override
    protected void setStatusBar() {
        StatusBarUtil.setColorNoTranslucentForDrawerLayout(this,
                (DrawerLayout) findViewById(R.id.drawer_layout),
                ThemeStore.getMaterialPrimaryDarkColor());
    }

    /**
     * 初始化toolbar
     * @param toolbar
     */
    protected void setUpToolbar(Toolbar toolbar) {
        super.setUpToolbar(toolbar,"");
        mToolBar.setTitle("");

        mToolBar.setNavigationIcon(R.drawable.actionbar_menu);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(mNavigationView);
            }
        });
    }


    public PagerAdapter getAdapter() {
        return mPagerAdapter;
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
                new MaterialDialog.Builder(MainActivity.this)
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
                        .input("", "本地歌单" + Global.mPlayList.size(), new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
                                if(!TextUtils.isEmpty(input)){
                                    int newPlayListId = PlayListUtil.addPlayList(input.toString());
                                    ToastUtil.show(MainActivity.this, newPlayListId > 0 ?
                                                    R.string.add_playlist_success :
                                                    newPlayListId == -1 ? R.string.add_playlist_error : R.string.playlist_alread_exist,
                                            Toast.LENGTH_SHORT);
                                    if(newPlayListId > 0){
                                        //跳转到添加歌曲界面
                                        Intent intent = new Intent(MainActivity.this,SongChooseActivity.class);
                                        intent.putExtra("PlayListID",newPlayListId);
                                        intent.putExtra("PlayListName",input.toString());
                                        startActivity(intent);
                                    }
                                }
                            }
                        })
                        .show();
                break;
            case R.id.multi_close:
                mMultiToolBar.setVisibility(View.GONE);
                mToolBar.setVisibility(View.VISIBLE);
                if(mMultiChoice.isShow()){
                    mMultiChoice.UpdateOptionMenu(false);
                    mMultiChoice.clear();
                }
                break;
        }
    }

    //初始化ViewPager
    private void setUpPager() {
        mPagerAdapter = new PagerAdapter(getSupportFragmentManager());
        mPagerAdapter.setTitles(new String[]{getResources().getString(R.string.tab_song),
                getResources().getString(R.string.tab_album),
                getResources().getString(R.string.tab_artist),
                getResources().getString(R.string.tab_playlist)});
        mPagerAdapter.AddFragment(new SongFragment());
        mPagerAdapter.AddFragment(new AlbumFragment());
        mPagerAdapter.AddFragment(new ArtistFragment());
        mPagerAdapter.AddFragment(new PlayListFragment());

        mAddButton.setImageResource(ThemeStore.isDay() ? R.drawable.icon_floatingbtn_day : R.drawable.icon_floatingbtn_night);
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setCurrentItem(0);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                mAddButton.setVisibility(position == 3 ? View.VISIBLE: View.GONE);
                if(position == 3){
                    SpringSystem.create().createSpring()
                            .addListener(new SimpleSpringListener(){
                                @Override
                                public void onSpringUpdate(Spring spring) {
                                    mAddButton.setScaleX((float) spring.getCurrentValue());
                                    mAddButton.setScaleY((float) spring.getCurrentValue());
                                }
                            })
                            .setEndValue(1);
                }
            }
            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    //初始化custontab
    private void setUpTab() {
        //添加tab选项卡
        mTablayout.addTab(mTablayout.newTab().setText(getResources().getString(R.string.tab_song)));
        mTablayout.addTab(mTablayout.newTab().setText(getResources().getString(R.string.tab_album)));
        mTablayout.addTab(mTablayout.newTab().setText(getResources().getString(R.string.tab_artist)));
        mTablayout.addTab(mTablayout.newTab().setText(getResources().getString(R.string.tab_playlist)));
        //viewpager与tablayout关联
        mTablayout.setupWithViewPager(mViewPager);
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
        mDrawerAdapter = new DrawerAdapter(this);
        mDrawerAdapter.setOnModeChangeListener(new OnModeChangeListener() {
            @Override
            public void OnModeChange(boolean isNight) {
                setNightMode(isNight);
            }
        });
        mDrawerAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                switch (position){
                    //歌曲库
                    case 0:
                        mDrawerLayout.closeDrawer(mNavigationView);
                        break;
                    //最近添加
                    case 1:
                        startActivity(new Intent(MainActivity.this,RecetenlyActivity.class));
                        break;
                    //文件夹
                    case 2:
                        startActivity(new Intent(MainActivity.this, FolderActivity.class));
                        break;
                    //夜间模式
                    case 3:
                        setNightMode(ThemeStore.isDay());
                        break;
                    //设置
                    case 4:
                        startActivityForResult(new Intent(MainActivity.this,SettingActivity.class), SETTING);
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

        //正在播放文字的背景
        TextView textView = findView(R.id.header_txt);
        if(textView != null){
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(ThemeStore.getAccentColor());
            bg.setColor(ThemeStore.isDay() ? ThemeStore.getMaterialPrimaryDarkColor() : ThemeStore.getAccentColor());
            bg.setCornerRadius(DensityUtil.dip2px(this,4));
            textView.setBackground(bg);
        }

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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data != null){
            String errorTxt = getString(
                    Global.mSetCoverType == Constants.ALBUM ? R.string.set_album_cover_error : Global.mSetCoverType == Constants.ARTIST ? R.string.set_artist_cover_error : R.string.set_playlist_cover_error);
            final int id = Global.mSetCoverID; //专辑、艺术家、播放列表封面
            switch (requestCode){
                //设置主题后重启activity或者清除缓存后刷新adapter
                case SETTING:
                    if(data.getBooleanExtra("needRecreate",false)) {
                        mRefreshHandler.sendEmptyMessage(Constants.RECREATE_ACTIVITY);
                    }else if(data.getBooleanExtra("needRefresh",false)){
                        mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
                    }
                    break;
                //图片选择
                case Crop.REQUEST_PICK:
                    if(resultCode == RESULT_OK){
                        File cacheDir = DiskCache.getDiskCacheDir(this,
                                "thumbnail/" + (Global.mSetCoverType == Constants.ALBUM ? "album" : Global.mSetCoverType == Constants.ARTIST ? "artist" : "playlist"));
                        if(!cacheDir.exists()){
                            if(!cacheDir.mkdir()){
                                ToastUtil.show(this,errorTxt);
                                return;
                            }
                        }
                        Uri destination = Uri.fromFile(new File(cacheDir, CommonUtil.hashKeyForDisk((id * 255 ) + "")));
                        Crop.of(data.getData(), destination).asSquare().start(this);
                    } else {
                        ToastUtil.show(this,errorTxt);
                    }
                    break;
                //图片裁剪
                case Crop.REQUEST_CROP:
                    //裁剪后的图片路径
                    final String path = Crop.getOutput(data).getEncodedPath();
                    if(TextUtils.isEmpty(path) || id == -1){
                        ToastUtil.show(MainActivity.this,errorTxt);
                        return;
                    }
                    //清除fresco的缓存
                    new Thread(){
                        @Override
                        public void run() {
                            ImagePipeline imagePipeline = Fresco.getImagePipeline();
                            if(Global.mSetCoverType != Constants.PLAYLIST){
                                if(new File(path).exists()){
                                    Uri fileUri = Uri.parse("file://" + path);
                                    imagePipeline.evictFromCache(fileUri);
                                } else {
                                    Uri providerUri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), id);
                                    imagePipeline.evictFromCache(providerUri);
                                }
                            } else {
                                imagePipeline.clearCaches();
                            }
                            mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
                        }
                    }.start();
                    break;
            }
        }
    }


    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
            mDrawerLayout.closeDrawer(mNavigationView);
        } else if(mMultiChoice.isShow()) {
            onBackPress();
        } else {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
            Intent intent = new Intent(Constants.NOTIFY);
            intent.putExtra("FromMainActivity", true);
            sendBroadcast(intent);
        }
    }

    //更新界面
    @Override
    public void UpdateUI(MP3Item mp3Item, boolean isplay) {
        if (!mIsRunning)
            return;
        mBottomBar.UpdateBottomStatus(mp3Item, isplay);
//        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
//        for (Fragment fragment : fragmentList) {
//            if (fragment instanceof SongFragment && ((SongFragment) fragment).getAdapter() != null) {
//                ((SongFragment) fragment).getAdapter().notifyDataSetChanged();
//            }
//        }
        mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ALLSONG_ADAPTER);
        updateHeader(mp3Item,isplay);
    }

    /**
     * 更新侧滑菜单
     * @param mp3Item
     */
    private void updateHeader(MP3Item mp3Item,boolean isPlay) {
        TextView textView = findView(R.id.header_txt);
        SimpleDraweeView shadowImgView = findView(R.id.header_img);
        if(mp3Item == null)
            return;
        if(textView != null){
            textView.setText(getString(R.string.play_now,mp3Item.getTitle()));
        }
        if(shadowImgView != null) {
//            MediaStoreUtil.setImageUrl(shadowImgView, mp3Item.getAlbumId());
            new AsynLoadImage(shadowImgView).execute(mp3Item.getAlbumId(), Constants.URL_ALBUM);
            shadowImgView.setBackgroundResource(isPlay ? R.drawable.drawer_bg_album_shadow : R.color.transparent);
        }
    }

    @Override
    public int getType() {
        return Constants.MAINACTIVITY;
    }

}

