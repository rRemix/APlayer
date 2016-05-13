package remix.myplayer.activities;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.facebook.common.internal.Supplier;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import remix.myplayer.R;
import remix.myplayer.adapters.SlideMenuAdapter;
import remix.myplayer.fragments.AllSongFragment;
import remix.myplayer.fragments.BottomActionBarFragment;
import remix.myplayer.fragments.MainFragment;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.listeners.LockScreenListener;
import remix.myplayer.services.MusicService;
import remix.myplayer.services.TimerService;
import remix.myplayer.ui.dialog.TimerDialog;
import remix.myplayer.utils.CommonUtil;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.utils.ErrUtil;
import remix.myplayer.utils.Global;
import remix.myplayer.utils.SharedPrefsUtil;
import remix.myplayer.utils.XmlUtil;


/**
 *
 */
public class MainActivity extends BaseAppCompatActivity implements MusicService.Callback {
    public static MainActivity mInstance = null;
    private BottomActionBarFragment mBottomBar;
    private final static String TAG = "MainActivity";
    //测滑的Listview
    private ListView mSlideMenuList;
    private Toolbar mToolBar;
    //测滑
    private DrawerLayout mDrawerLayout;
    private LinearLayout mDrawerMenu;
    private ImageButton mSlideMenuExit;
    private ActionBarDrawerToggle mDrawerToggle;
    //是否正在运行
    private static boolean mIsRunning = false;
    //是否第一次启动
    private static boolean mIsFirst = true;
    @Override
    protected void onResume() {
        super.onResume();
        mIsRunning = true;
        UpdateUI(MusicService.getCurrentMP3(), MusicService.getIsplay());
    }

    @Override
    protected void onPause() {
        super.onPause();
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
    protected void onCreate(Bundle savedInstanceState) {
        //检查更新
//        UmengUpdateAgent.update(this);
//        MobclickAgent.setDebugMode(true);
        MobclickAgent.setCatchUncaughtExceptions(true);
        initUtil();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);
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
        //加载主页fragment
        initMainFragment();
        //初始化toolbar
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        initToolbar();
        //初始化测滑菜单
        initDrawerLayout();
        //初始化底部状态栏
        mBottomBar = (BottomActionBarFragment) getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);
        //初始化toolbar

        if (Global.mPlayingList == null || Global.mPlayingList.size() == 0){
            SharedPrefsUtil.putValue(getApplicationContext(), "setting", "Pos", -1);
            return;
        }

        boolean isFirst = SharedPrefsUtil.getValue(getApplicationContext(), "setting", "First", true);
        int position = SharedPrefsUtil.getValue(getApplicationContext(), "setting", "Pos", -1);
        SharedPrefsUtil.putValue(getApplicationContext(), "setting", "First", false);

        //第一次启动添加我的收藏列表
        if (isFirst) {
            XmlUtil.addPlaylist(getString(R.string.my_favorite));
        }
        //如果是第一次启动软件,将第一首歌曲设置为正在播放
        try {
            if (isFirst || position < 0) {
                mBottomBar.UpdateBottomStatus(DBUtil.getMP3InfoById(Global.mPlayingList.get(0)), MusicService.getIsplay());
                SharedPrefsUtil.putValue(getApplicationContext(), "setting", "Pos", 0);
            } else {
                if(position >= Global.mPlayingList.size()){
                    position = Global.mPlayingList.size() - 1;
                    if(position >= 0)
                        SharedPrefsUtil.putValue(getApplicationContext(), "setting", "Pos", position);
                }
                mBottomBar.UpdateBottomStatus(DBUtil.getMP3InfoById(Global.mPlayingList.get(position)), MusicService.getIsplay());
            }
        } catch (Exception e){
            e.printStackTrace();
        }


    }

    private void initToolbar() {
        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        mToolBar.setTitle("");

        setSupportActionBar(mToolBar);
        mToolBar.setNavigationIcon(R.drawable.actionbar_menu);
        mToolBar.setLogo(R.drawable.allsong_icon_musicbox);
        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(mDrawerMenu);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }


    private void initUtil() {
        //初始化工具类
        XmlUtil.setContext(getApplicationContext());
        DBUtil.setContext(getApplicationContext());
        CommonUtil.setContext(getApplicationContext());
        ErrUtil.setContext(getApplicationContext());

        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
                .setBitmapMemoryCacheParamsSupplier(new Supplier<MemoryCacheParams>() {
                    @Override
                    public MemoryCacheParams get() {
                        //10M内存缓存
                        return new MemoryCacheParams(10 * 1024 * 1024, 10, 2048, 5, 1024);
                    }
                }).build();
        Fresco.initialize(this, config);


        DisplayImageOptions option = new DisplayImageOptions.Builder()
                .showImageForEmptyUri(R.drawable.song_artist_empty_bg)
                .showImageOnFail(R.drawable.song_artist_empty_bg)
                .cacheOnDisk(true)
                .resetViewBeforeLoading(false)
                .cacheInMemory(true)
                .build();

        ImageLoaderConfiguration config1 = new ImageLoaderConfiguration.Builder(this)
                .memoryCacheSize(20 * 1024 * 1024)
                .defaultDisplayImageOptions(option)
                .build();
        ImageLoader.getInstance().init(config1);
    }

    private void initMainFragment() {
        getSupportFragmentManager().beginTransaction().add(R.id.main_fragment_container, new MainFragment(), "MainFragment").addToBackStack(null).commit();
    }

    private void initDrawerLayout() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerMenu = (LinearLayout) findViewById(R.id.slide_menu);

        mSlideMenuList = (ListView) mDrawerMenu.findViewById(R.id.slide_menu_list);
        mSlideMenuList.setAdapter(new SlideMenuAdapter(getLayoutInflater()));
        mSlideMenuList.setOnItemClickListener(new SlideMenuListener(this));

        mSlideMenuExit = (ImageButton) findViewById(R.id.drawer_exit);
        mSlideMenuExit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendBroadcast(new Intent(Constants.EXIT));
            }
        });

    }


    class SlideMenuListener implements AdapterView.OnItemClickListener {
        private Context mContext;

        public SlideMenuListener(Context mContext) {
            this.mContext = mContext;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (view.getId()) {
                case 0:
                    //最近添加
                    startActivity(new Intent(MainActivity.this, RecetenlyActivity.class));
                    break;
                case 1:
                    startActivity(new Intent(MainActivity.this, PlayListActivity.class));
                    break;
                case 2:
                    mDrawerLayout.closeDrawer(mDrawerMenu);
                    MainFragment.mInstance.getViewPager().setCurrentItem(0);
                    break;
                case 3:
                    //设置
                    startActivity(new Intent(MainActivity.this, SettingActivity.class));
                    break;
                default:
                    break;
            }
        }
    }

    //读取sd卡歌曲信息
    public static void loadsongs() {
        //读取所有歌曲信息
        FutureTask<ArrayList<Long>> task = new FutureTask<ArrayList<Long>>(new Callable<ArrayList<Long>>() {
            @Override
            public ArrayList<Long> call() throws Exception {
                return DBUtil.getAllSongsId();
            }
        });
        new Thread(task, "getInfo").start();
        try {
            Global.mAllSongList = task.get();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //读取正在播放列表信息
        FutureTask<ArrayList<Long>> task1 = new FutureTask<ArrayList<Long>>(new Callable<ArrayList<Long>>() {
            @Override
            public ArrayList<Long> call() throws Exception {
                return XmlUtil.getPlayingList();
            }
        });
        new Thread(task1, "getPlayingList").start();
        try {
            Global.mPlayingList = task1.get();
            if (Global.mPlayingList == null || Global.mPlayingList.size() == 0)
                Global.mPlayingList = (ArrayList<Long>) task.get().clone();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //隐藏侧滑菜单
    public void HideDrawer() {
        if (mDrawerLayout.isDrawerOpen(mDrawerMenu))
            mDrawerLayout.closeDrawer(mDrawerMenu);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(mDrawerMenu))
            mDrawerLayout.closeDrawer(mDrawerMenu);
        else {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
            Intent intent = new Intent(Constants.NOTIFY);
            intent.putExtra("FromMainActivity",true);
            sendBroadcast(intent);
        }
    }

    //更新界面
    @Override
    public void UpdateUI(MP3Info MP3info, boolean isplay) {
        if(!mIsRunning)
            return;
        mBottomBar.UpdateBottomStatus(MP3info, isplay);
        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        for (Fragment fragment : fragmentList) {
            if (fragment instanceof AllSongFragment && ((AllSongFragment) fragment).getAdapter() != null) {
                ((AllSongFragment) fragment).getAdapter().notifyDataSetChanged();
            }
        }
    }

    @Override
    public int getType() {
        return Constants.MAINACTIVITY;
    }

}

