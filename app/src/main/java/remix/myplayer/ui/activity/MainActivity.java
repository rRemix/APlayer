package remix.myplayer.ui.activity;


import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.facebook.common.internal.Supplier;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.soundcloud.android.crop.Crop;
import com.umeng.analytics.MobclickAgent;
import com.umeng.update.UmengUpdateAgent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.PagerAdapter;
import remix.myplayer.fragment.AlbumFragment;
import remix.myplayer.fragment.ArtistFragment;
import remix.myplayer.fragment.BottomActionBarFragment;
import remix.myplayer.fragment.FolderFragment;
import remix.myplayer.fragment.SongFragment;
import remix.myplayer.listener.LockScreenListener;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.service.TimerService;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.dialog.TimerDialog;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DBUtil;
import remix.myplayer.util.DiskCache;
import remix.myplayer.util.ErrUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.PermissionUtil;
import remix.myplayer.util.SharedPrefsUtil;
import remix.myplayer.util.StatusBarUtil;
import remix.myplayer.util.XmlUtil;

/**
 *
 */
public class MainActivity extends BaseAppCompatActivity implements MusicService.Callback {
    public static MainActivity mInstance = null;
    @BindView(R.id.toolbar)
    Toolbar mToolBar;
    @BindView(R.id.tabs)
    TabLayout mTablayout;
    @BindView(R.id.ViewPager)
    android.support.v4.view.ViewPager mViewPager;
    @BindView(R.id.navigation_view)
    NavigationView mNavigationView;
    @BindView(R.id.drawer_layout)
    DrawerLayout mDrawerLayout;
    private BottomActionBarFragment mBottomBar;
    private final static String TAG = "MainActivity";

    private PagerAdapter mAdapter;
    //是否正在运行
    private static boolean mIsRunning = false;
    //是否第一次启动
    private static boolean mIsFirst = true;

    private static final int PERMISSIONCODE = 100;
    private static final String[] PERMISSIONS = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE};

    private int mAlpha = ThemeStore.STATUS_BAR_ALPHA / 2;
    private final int RECREATE = 0;
    private final int UPDATECOVER = 1;
    private Handler mRefreshHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == RECREATE) {
                recreate();
            }
            if(msg.what == UPDATECOVER){
                //刷新
                for(Fragment temp : getSupportFragmentManager().getFragments()){
                    if(temp instanceof SongFragment){
                       SongFragment songFragment = (SongFragment)temp;
                        if(songFragment.getAdapter() != null)
                            songFragment.getAdapter().notifyDataSetChanged();
                    }
                    if(temp instanceof AlbumFragment){
                        AlbumFragment albumFragment = (AlbumFragment)temp;
                        if(albumFragment.getAdapter() != null)
                            albumFragment.getAdapter().notifyDataSetChanged();
                    }
                    if(temp instanceof ArtistFragment){
                        ArtistFragment albumFragment = (ArtistFragment)temp;
                        if(albumFragment.getAdapter() != null)
                            albumFragment.getAdapter().notifyDataSetChanged();
                    }
                }
            }
        }
    };
    //更新主题
    private final int UPDATE_THEME = 1;

    @Override
    protected void onResume() {
        super.onResume();
        mIsRunning = true;
        //更新UI
        UpdateUI(MusicService.getCurrentMP3(), MusicService.getIsplay());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsRunning = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //检查更新
        UmengUpdateAgent.update(this);
//        MobclickAgent.setDebugMode(true);
        MobclickAgent.setCatchUncaughtExceptions(true);
        initUtil();
        initTheme();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);
        mInstance = this;

        if (mIsFirst) {
            mIsFirst = false;
            //读取歌曲
            loadsongs();
            startService(new Intent(this, MusicService.class));
            //定时
            startService(new Intent(this, TimerService.class));
            //监听锁屏
            new LockScreenListener(getApplicationContext()).beginListen();

        }
        //播放的service
        MusicService.addCallback(MainActivity.this);

        //初始化toolbar
        initToolbar();
        initPager();
        initTab();
        //初始化测滑菜单
        initDrawerLayout();
        //根据主题设置颜色
        initColor();
        //初始化底部状态栏
        mBottomBar = (BottomActionBarFragment) getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);

        boolean isFirst = SharedPrefsUtil.getValue(this, "Setting", "First", true);
        int position = SharedPrefsUtil.getValue(this, "Setting", "Pos", 0);
        SharedPrefsUtil.putValue(this, "Setting", "First", false);

        if (Global.mPlayingList == null || Global.mPlayingList.size() == 0) {
            SharedPrefsUtil.putValue(getApplicationContext(), "Setting", "Pos", 0);
            return;
        }


        if (isFirst) {
            //第一次启动添加我的收藏列表
            XmlUtil.addPlaylist(getString(R.string.my_favorite));
            //保存默认主题设置
            SharedPrefsUtil.putValue(this,"Setting","ThemeMode",ThemeStore.DAY);
            SharedPrefsUtil.putValue(this,"Setting","ThemeColor",ThemeStore.THEME_PINK);
        }
        //如果是第一次启动软件,将第一首歌曲设置为正在播放
        if (isFirst) {
            mBottomBar.UpdateBottomStatus(DBUtil.getMP3InfoById(Global.mPlayingList.get(0)), MusicService.getIsplay());
            SharedPrefsUtil.putValue(getApplicationContext(), "Setting", "Pos", 0);
        } else {
            if (position >= Global.mPlayingList.size()) {
                position = Global.mPlayingList.size() - 1;
                if (position >= 0){
                    SharedPrefsUtil.putValue(getApplicationContext(), "Setting", "Pos", position);
                    mBottomBar.UpdateBottomStatus(DBUtil.getMP3InfoById(Global.mPlayingList.get(position)), MusicService.getIsplay());
                }
            }

        }

    }

    private void initTheme() {
        ThemeStore.THEME_MODE = ThemeStore.loadThemeMode();
        ThemeStore.THEME_COLOR = ThemeStore.loadThemeColor();

        ThemeStore.MATERIAL_COLOR_PRIMARY = ThemeStore.getMaterialPrimaryColor();
        ThemeStore.MATERIAL_COLOR_PRIMARY_DARK = ThemeStore.getMaterialPrimaryDarkColor();
        Log.d(TAG,"primary:" + ThemeStore.MATERIAL_COLOR_PRIMARY + "\r\nprimary dark:" + ThemeStore.MATERIAL_COLOR_PRIMARY_DARK);
//        int color = ThemeStore.getMaterialPrimaryColor(this);
//        ThemeStore.MATERIAL_COLOR_PRIMARY_DARK = ThemeStore.getMaterialPrimaryColor(ThemeStore.THEME_COLOR);
    }

    private void initColor() {
        mToolBar.setBackgroundColor(ColorUtil.getColor(ThemeStore.MATERIAL_COLOR_PRIMARY));
        mTablayout.setBackgroundColor(ColorUtil.getColor(ThemeStore.MATERIAL_COLOR_PRIMARY));
    }


    @Override
    protected void setStatusBar() {
        StatusBarUtil.setColorNoTranslucentForDrawerLayout(this,
                (DrawerLayout) findViewById(R.id.drawer_layout),
                ColorUtil.getColor(ThemeStore.MATERIAL_COLOR_PRIMARY_DARK));
    }

    private void initToolbar() {
        mToolBar.setTitle("");

        setSupportActionBar(mToolBar);
        mToolBar.setNavigationIcon(R.drawable.actionbar_menu);
        mToolBar.setBackgroundColor(getResources().getColor(ThemeStore.MATERIAL_COLOR_PRIMARY_DARK));
//        mToolBar.setLogo(R.drawable.allsong_icon_musicbox);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                mDrawerLayout.openDrawer(mDrawerMenu);
                mDrawerLayout.openDrawer(mNavigationView);
            }
        });
        mToolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.toolbar_search:
                        startActivity(new Intent(MainActivity.this, SearchActivity.class));
                        break;
                    case R.id.toolbar_timer:
                        startActivity(new Intent(MainActivity.this, TimerDialog.class));
                        break;
                }
                return true;
            }
        });

    }


    public PagerAdapter getAdapter() {
        return mAdapter;
    }

    public ViewPager getViewPager() {
        return mViewPager;
    }

    //初始化ViewPager
    private void initPager() {
        mAdapter = new PagerAdapter(getSupportFragmentManager());
        mAdapter.setTitles(new String[]{getResources().getString(R.string.tab_song),
                getResources().getString(R.string.tab_album),
                getResources().getString(R.string.tab_artist),
                getResources().getString(R.string.tab_folder)});
        mAdapter.AddFragment(new SongFragment());
        mAdapter.AddFragment(new AlbumFragment());
        mAdapter.AddFragment(new ArtistFragment());
        mAdapter.AddFragment(new FolderFragment());

        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(0);
    }

    //初始化custontab
    private void initTab() {
        //添加tab选项卡
        mTablayout.addTab(mTablayout.newTab().setText(getResources().getString(R.string.tab_song)));
        mTablayout.addTab(mTablayout.newTab().setText(getResources().getString(R.string.tab_album)));
        mTablayout.addTab(mTablayout.newTab().setText(getResources().getString(R.string.tab_artist)));
        mTablayout.addTab(mTablayout.newTab().setText(getResources().getString(R.string.tab_folder)));
        //viewpager与tablayout关联
        mTablayout.setupWithViewPager(mViewPager);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    private void initUtil() {
        //初始化工具类
        PermissionUtil.setContext(getApplicationContext());
        XmlUtil.setContext(getApplicationContext());
        DBUtil.setContext(getApplicationContext());
        CommonUtil.setContext(getApplicationContext());
        ErrUtil.setContext(getApplicationContext());
        DiskCache.init(getApplicationContext());
        ColorUtil.setContext(getApplicationContext());
        final int MAX_HEAP_SIZE = (int) Runtime.getRuntime().maxMemory();//分配的可用内存
        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
                .setBitmapMemoryCacheParamsSupplier(new Supplier<MemoryCacheParams>() {
                    @Override
                    public MemoryCacheParams get() {
                        //20M内存缓存
                        return new MemoryCacheParams(MAX_HEAP_SIZE / 8, Integer.MAX_VALUE, MAX_HEAP_SIZE / 8, Integer.MAX_VALUE, Integer.MAX_VALUE);
                    }
                })
                .build();
        Fresco.initialize(this,config);
    }

    private void initDrawerLayout() {
//        mNavigationView.setItemTextAppearance(R.style.Drawer_text_style);
        ColorStateList colorStateList = new ColorStateList(new int[][]{{android.R.attr.state_pressed},{android.R.attr.state_checked} ,{}},
                new int[]{ColorUtil.getColor(ThemeStore.MATERIAL_COLOR_PRIMARY), ColorUtil.getColor(ThemeStore.MATERIAL_COLOR_PRIMARY),ColorUtil.getColor(R.color.black_737373)});
        mNavigationView.setItemIconTintList(colorStateList);
        mNavigationView.setItemTextColor(colorStateList);
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                item.setChecked(true);
                switch (item.getItemId()) {
                    case R.id.item_recently:
                        //最近添加
                        startActivity(new Intent(MainActivity.this, RecetenlyActivity.class));
                        break;
                    case R.id.item_playlist:
                        startActivity(new Intent(MainActivity.this, PlayListActivity.class));
                        break;
                    case R.id.item_allsong:
                        mDrawerLayout.closeDrawer(mNavigationView);
                        break;
                    case R.id.item_setting:
                        //设置
                        startActivityForResult(new Intent(MainActivity.this,SettingActivity.class),UPDATE_THEME);
//                        startActivityForResult(new Intent(MainActivity.this,ThemeActivity.class),UPDATE_THEME);
                        break;
                    case R.id.item_exit:
                        sendBroadcast(new Intent(Constants.EXIT));
                        break;
                    default:
                        break;
                }
                return true;
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data != null){
            boolean isAlbum = Global.mAlbunOrArtist == Constants.ALBUM_HOLDER;
            String errorTxt = isAlbum ? "设置专辑封面失败" : "设置艺术家封面失败";
            int id = Global.mAlbumArtistID; //专辑或艺术家封面
            String name = Global.mAlbumArtistName;
            switch (requestCode){
                //重启activity
                case UPDATE_THEME:
                    if(data.getBooleanExtra("needRefresh",false))
                        mRefreshHandler.sendEmptyMessage(RECREATE);
                    break;
                //图片选择
                case Crop.REQUEST_PICK:
                    if(resultCode == RESULT_OK){
                        File cacheDir = DiskCache.getDiskCacheDir(this,"thumbnail/" + (isAlbum ? "album" : "artist"));
                        if(!cacheDir.exists()){
                            if(!cacheDir.mkdir()){
                                Toast.makeText(this,errorTxt,Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        Uri destination = Uri.fromFile(new File(cacheDir, CommonUtil.hashKeyForDisk((id * 255 ) + "")));
                        Crop.of(data.getData(), destination).asSquare().start(this);
                    } else {
                        Toast.makeText(this,errorTxt,Toast.LENGTH_SHORT).show();
                    }
                    break;
                //图片裁剪
                case Crop.REQUEST_CROP:
                    //裁剪后的图片路径
                    String path = Crop.getOutput(data).getEncodedPath();
                    if(TextUtils.isEmpty(path) || id == -1){
                        Toast.makeText(MainActivity.this, errorTxt, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    new ModifyCoverThread(id,path).start();
                    break;
            }
        }
    }

    //读取sd卡歌曲信息
    public static void loadsongs() {
        new Thread() {
            @Override
            public void run() {
                //读取sd卡歌曲id
                Global.mAllSongList = DBUtil.getAllSongsId();
                //读取正在播放列表
                Global.mPlayingList = XmlUtil.getPlayingList();
                if (Global.mPlayingList == null || Global.mPlayingList.size() == 0)
                    Global.mPlayingList = (ArrayList<Long>) Global.mAllSongList.clone();
            }
        }.start();

    }

    //隐藏侧滑菜单
    public void HideDrawer() {
        if (mDrawerLayout.isDrawerOpen(mNavigationView))
            mDrawerLayout.closeDrawer(mNavigationView);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mNavigationView))
            mDrawerLayout.closeDrawer(mNavigationView);
        else {
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
    public void UpdateUI(MP3Item mP3Item, boolean isplay) {
        if (!mIsRunning)
            return;
        mBottomBar.UpdateBottomStatus(mP3Item, isplay);
        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragmentList) {
            if (fragment instanceof SongFragment && ((SongFragment) fragment).getAdapter() != null) {
                ((SongFragment) fragment).getAdapter().notifyDataSetChanged();
            }
        }
    }

    @Override
    public int getType() {
        return Constants.MAINACTIVITY;
    }


    /**
     * 将本专辑封面的缓存替换为剪切后的图片
     */
    class ModifyCoverThread extends Thread{
        private int mId;
        private String mNewPath;
        public ModifyCoverThread(int id,String path){
            mId = id;
            mNewPath = path;
        }
        @Override
        public void run() {
            if(Global.mAlbunOrArtist == Constants.ARTIST_HOLDER){
                mRefreshHandler.sendEmptyMessage(UPDATECOVER);
                return;
            }
            String oriPath = DBUtil.getImageUrl(mId + "",Constants.URL_ALBUM);
            if(TextUtils.isEmpty(oriPath)){
                return;
            }
            //清除fresco的缓存
            ImagePipeline imagePipeline = Fresco.getImagePipeline();
            Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mId);
            imagePipeline.evictFromCache(uri);

            FileOutputStream fos = null;
            FileInputStream fin = null;
            try {
                fos = new FileOutputStream(oriPath,false);
                fin = new FileInputStream(mNewPath);
                byte[] bytes = new byte[1000];
                int length = -1;
                while ((length = fin.read(bytes)) != -1){
                    fos.write(bytes);
                }
                fos.flush();
                mRefreshHandler.sendEmptyMessage(UPDATECOVER);
            }
            catch (IOException e) {
                e.printStackTrace();
            } finally {
                if(fos != null){
                    try {
                        fos.close();
                        fos = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if(fin != null){
                    try {
                        fin.close();
                        fin = null;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        switch (requestCode) {
//            case PERMISSIONCODE: {
//                boolean haspermission = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
//                break;
//            }
//
//            default:break;
//        }
//    }
}

