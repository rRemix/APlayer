package remix.myplayer.activities;


import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;


import com.facebook.drawee.backends.pipeline.Fresco;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import remix.myplayer.adapters.SlideMenuRecycleAdpater;
import remix.myplayer.broadcastreceivers.NotifyReceiver;
import remix.myplayer.fragments.BottomActionBarFragment;
import remix.myplayer.fragments.MainFragment;
import remix.myplayer.R;
import remix.myplayer.services.MusicService;
import remix.myplayer.utils.MP3Info;
import remix.myplayer.utils.PlayListItem;
import remix.myplayer.utils.SharedPrefsUtil;
import remix.myplayer.utils.Utility;
import remix.myplayer.utils.XmlUtil;

public class MainActivity extends AppCompatActivity implements MusicService.Callback{
    public static MainActivity mInstance;
    private MusicService mService;
    private BottomActionBarFragment mActionbar;
    private LoaderManager mManager;
    private Utility mUtlity;
    private RecyclerView mMenuRecycle;
    private SlideMenuRecycleAdpater mMenuAdapter;
    private NotifyReceiver mReceiver;
    private MusicService.PlayerReceiver mMusicReceiver;
    private ServiceConnection mConnecting = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MusicService.PlayerBinder)service).getService();
            mService.addCallback(MainActivity.this);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };


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
//        unbindService(mConnecting);
        unregisterReceiver(mReceiver);

//        unregisterReceiver(mMusicReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        XmlUtil.setContext(getApplicationContext());

        Fresco.initialize(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        mInstance = this;
        setContentView(R.layout.content_main);
        mUtlity = new Utility(getApplicationContext());
        loadsongs();


        //注册控制栏监听
        mReceiver = new NotifyReceiver();
        IntentFilter filter = new IntentFilter(Utility.NOTIFY);
        registerReceiver(mReceiver,filter);
        //绑定控制播放的service
        MusicService.addCallback(MainActivity.this);
//        Intent intent = new Intent(MainActivity.this,MusicService.class);
//        bindService(intent, mConnecting, Context.BIND_AUTO_CREATE);
        startService(new Intent(this,MusicService.class));
        //加载主页fragment
        initMainFragment();
        //初始化侧滑菜单
        initSlideMenu();
        //初始化底部状态栏
        mActionbar = (BottomActionBarFragment)getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);
        if(Utility.mPlayingList == null || Utility.mPlayingList.size() == 0)
            return;
        //如果是第一次启动软件
        int mFir = SharedPrefsUtil.getValue(getApplicationContext(),"setting","mFirst",-1);
        int mPos = SharedPrefsUtil.getValue(getApplicationContext(),"setting","mPos",-1);
        SharedPrefsUtil.putValue(getApplicationContext(),"setting","mFrist",1);

        if(mFir == 0 || mPos < 0)
            mActionbar.UpdateBottomStatus(Utility.getMP3InfoById(Utility.mPlayingList.get(0)),false);
        else
            mActionbar.UpdateBottomStatus(Utility.getMP3InfoById(Utility.mPlayingList.get(mPos)), false);

//        mActionbar.UpdateBottomStatus(MusicService.getCurrentMP3() == null ?
//                Utility.getMP3InfoById(Utility.mPlayingList.get(0)) :
//                MusicService.getCurrentMP3(),
//                false);

        //注册Musicreceiver
//        MusicService service = new MusicService(getApplicationContext());
//        mMusicReceiver = service.new PlayerReceiver();
//        IntentFilter musicfilter = new IntentFilter(Utility.CTL_ACTION);
//        registerReceiver(mMusicReceiver, musicfilter);


    }

    private void initSlideMenu()
    {
//        mMenuRecycle = (RecyclerView)findViewById(R.id.slide_menu_recyclelist);
//        mMenuRecycle.setLayoutManager(new LinearLayoutManager(this));
//        mMenuAdapter = new SlideMenuRecycleAdpater(getLayoutInflater());
//        mMenuRecycle.setAdapter(mMenuAdapter);
//        mMenuRecycle.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
    }
    public RecyclerView getRecycleMenu()
    {
        return mMenuRecycle;
    }
    private void initMainFragment() {
        getSupportFragmentManager().beginTransaction().add(R.id.main_fragment_container, new MainFragment(), "MainFragment").addToBackStack(null).commit();
    }

    public FragmentManager getFM()
    {
        return getSupportFragmentManager();
    }







    //读取sd卡歌曲信息
    public static void loadsongs()
    {
        //读取所有歌曲信息
        FutureTask<ArrayList<Long>> task = new FutureTask<ArrayList<Long>>(new Callable<ArrayList<Long>>() {
            @Override
            public ArrayList<Long> call() throws Exception {
                return Utility.getAllSongsId();
            }
        });
        //开启一条线程来读取歌曲信息
        new Thread(task, "getInfo").start();
        try {
            Utility.mAllSongList = task.get();
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
        //开启一条线程来读取歌曲信息
        new Thread(task1, "getPlayingList").start();
        try {
            Utility.mPlayingList = task1.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //后退返回桌面
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
//            List<Fragment> list = getSupportFragmentManager().getFragments();
//            MainFragment fragment = null;
//            for(int i = 0; i < list.size(); i++)
//            {
//                if(list.get(i) instanceof MainFragment)
//                    fragment = (MainFragment) list.get(i);
//            }
//            if(fragment.isMenuShow())
//                fragment.toggleMenu();
//            else {
//                Intent home = new Intent(Intent.ACTION_MAIN);
//                home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                home.addCategory(Intent.CATEGORY_HOME);
//                startActivity(home);
//            }
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);

        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void UpdateUI(MP3Info MP3info, boolean isplay){
        MP3Info temp = MP3info;
        mActionbar.UpdateBottomStatus(MP3info, isplay);
    }

    @Override
    public int getType() {
        return 0;
    }

    public  MusicService getService()
    {
        return mService;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public class NotifyBroadReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_dialog)
                    .setContentTitle("Notify")
                    .setContentText("Hello World");
            Intent result = new Intent(context,AudioHolderActivity.class);
            result.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            stackBuilder.addParentStack(AudioHolderActivity.class);
//            stackBuilder.addNextIntent(new Intent(context,MainActivity.class));
            stackBuilder.addNextIntent(result);



            PendingIntent resultPendingIntent =
                    stackBuilder.getPendingIntent(
                            0,
                            PendingIntent.FLAG_CANCEL_CURRENT
                    );

            mBuilder.setContentIntent(resultPendingIntent);

            NotificationManager mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(0, mBuilder.build());
        }
    }
}

