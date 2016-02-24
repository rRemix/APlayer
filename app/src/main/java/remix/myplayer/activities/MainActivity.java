package remix.myplayer.activities;


import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Window;
import android.widget.LinearLayout;

import com.facebook.common.internal.Supplier;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import remix.myplayer.R;
import remix.myplayer.fragments.AllSongFragment;
import remix.myplayer.fragments.BottomActionBarFragment;
import remix.myplayer.fragments.MainFragment;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.receivers.LineCtlReceiver;
import remix.myplayer.services.MusicService;
import remix.myplayer.services.NotifyService;
import remix.myplayer.utils.CommonUtil;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.utils.SharedPrefsUtil;
import remix.myplayer.utils.XmlUtil;

public class MainActivity extends AppCompatActivity implements MusicService.Callback{
    public static MainActivity mInstance = null;
    private MusicService mService;
    private BottomActionBarFragment mBottomBar;
    private final static String TAG = "MainActivity";
    private boolean mFromNotify = false;
//    private ServiceConnection mConnecting = new ServiceConnection() {
//        @Override
//        public void onServiceConnected(ComponentName name, IBinder service) {
//            mService = ((MusicService.PlayerBinder)service).getService();
//            mService.addCallback(MainActivity.this);
//        }
//        @Override
//        public void onServiceDisconnected(ComponentName name) {
//            mService = null;
//        }
//    };
    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initUtil();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.content_main);
        mInstance = this;

        mFromNotify = getIntent().getBooleanExtra("Notify",false);
        if(!mFromNotify) {
            loadsongs();
            //播放的service
            MusicService.addCallback(MainActivity.this);
            startService(new Intent(this,MusicService.class));
            //NofityService
            startService(new Intent(this, NotifyService.class));


        }
        //加载主页fragment
        initMainFragment();
        //初始化底部状态栏
        mBottomBar = (BottomActionBarFragment)getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);
        if(DBUtil.mPlayingList == null || DBUtil.mPlayingList.size() == 0)
            return;
        //如果是第一次启动软件
        boolean mFir = SharedPrefsUtil.getValue(getApplicationContext(),"setting","mFirst",true);
        int mPos = SharedPrefsUtil.getValue(getApplicationContext(),"setting","mPos",-1);
        SharedPrefsUtil.putValue(getApplicationContext(),"setting","mFirst",false);

        if(mFir || mPos < 0)
            mBottomBar.UpdateBottomStatus(DBUtil.getMP3InfoById(DBUtil.mPlayingList.get(0)),mFromNotify);
        else
            mBottomBar.UpdateBottomStatus(DBUtil.getMP3InfoById(DBUtil.mPlayingList.get(mPos)), mFromNotify);
    }

    private void initUtil() {
        //初始化库和工具类
        XmlUtil.setContext(getApplicationContext());
        DBUtil.setContext(getApplicationContext());
        CommonUtil.setContext(getApplicationContext());

        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
                .setBitmapMemoryCacheParamsSupplier(new Supplier<MemoryCacheParams>() {
                    @Override
                    public MemoryCacheParams get() {
                        //50M内存缓存
                        return new MemoryCacheParams(50 * 1024 * 1024,10,2048,5,1024);
                    }
                }).build();
        Fresco.initialize(this,config);

        DisplayImageOptions option = new DisplayImageOptions.Builder()
                .showImageForEmptyUri(R.drawable.default_recommend)
                .showImageOnFail(R.drawable.default_recommend                                                                                                                                                                                                                                                                                         )
                .resetViewBeforeLoading(false)
                .cacheOnDisk(true)
                .cacheInMemory(true)
                .build();

        ImageLoaderConfiguration config1 = new ImageLoaderConfiguration.Builder(this)
                .diskCacheSize(50 * 1024 * 1024) // 50 Mb sd卡(本地)缓存的最大值
                .diskCacheFileCount(50)
                .defaultDisplayImageOptions(option)
                .build();
        ImageLoader.getInstance().init(config1);
    }

    private void initMainFragment() {

        getSupportFragmentManager().beginTransaction().add(R.id.main_fragment_container, new MainFragment(), "MainFragment").addToBackStack(null).commit();
    }


    //读取sd卡歌曲信息
    public static void loadsongs()
    {
        //读取所有歌曲信息
        FutureTask<ArrayList<Long>> task = new FutureTask<ArrayList<Long>>(new Callable<ArrayList<Long>>() {
            @Override
            public ArrayList<Long> call() throws Exception {
                return DBUtil.getAllSongsId();
            }
        });
        new Thread(task, "getInfo").start();
        try {
            DBUtil.mAllSongList = task.get();
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
            DBUtil.mPlayingList = task1.get();
            if(DBUtil.mPlayingList == null || DBUtil.mPlayingList.size()  == 0)
                DBUtil.mPlayingList = (ArrayList<Long>)task.get().clone();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //后退返回桌面
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK) {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
            sendBroadcast(new Intent(Constants.NOTIFY));
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void UpdateUI(MP3Info MP3info, boolean isplay){
        MP3Info temp = MP3info;
        mBottomBar.UpdateBottomStatus(MP3info, isplay);
        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
        for(Fragment fragment : fragmentList){
            if(fragment instanceof AllSongFragment){
                ((AllSongFragment) fragment).getAdapter().notifyDataSetChanged();
            }
        }
    }

    @Override
    public int getType() {
        return 0;
    }

}

