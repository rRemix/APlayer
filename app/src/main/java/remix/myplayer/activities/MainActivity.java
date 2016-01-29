package remix.myplayer.activities;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Xml;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;


import com.facebook.drawee.backends.pipeline.Fresco;
import com.tencent.open.t.Weibo;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import remix.myplayer.adapters.SlideMenuRecycleAdpater;
import remix.myplayer.fragments.BottomActionBarFragment;
import remix.myplayer.fragments.MainFragment;
import remix.myplayer.R;
import remix.myplayer.services.MusicService;
import remix.myplayer.utils.MP3Info;
import remix.myplayer.utils.Utility;
import remix.myplayer.utils.XmlUtil;

public class MainActivity extends AppCompatActivity implements MusicService.Callback, LoaderManager.LoaderCallbacks<Cursor>{
    public static MainActivity mInstance;
    private MusicService mService;
    private BottomActionBarFragment mActionbar;
    private LoaderManager mManager;
    private Utility mUtlity;
    private RecyclerView mMenuRecycle;
    private SlideMenuRecycleAdpater mMenuAdapter;
    private ServiceConnection mConnecting = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MusicService.PlayerBinder)service).getService();
            mService.addCallback(MainActivity.this,0);
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        XmlUtil.setContext(getApplicationContext());

        Fresco.initialize(this);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        mInstance = this;
        setContentView(R.layout.content_main);
        mUtlity = new Utility(this);
        loadsongs();
        mManager = getSupportLoaderManager();
        mManager.initLoader(1003,null,this);


        //绑定控制播放的service
        Intent intent = new Intent(MainActivity.this,MusicService.class);
        bindService(intent, mConnecting, Context.BIND_AUTO_CREATE);
        //加载主页fragment
        initMainFragment();
        //初始化侧滑菜单
        initSlideMenu();
        //初始化底部状态栏
        mActionbar = (BottomActionBarFragment)getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);
        if(Utility.mPlayList == null || Utility.mPlayList.size() == 0)
            return;
        mActionbar.UpdateBottomStatus(Utility.getMP3InfoById(Utility.mPlayList.get(0)), false);

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
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mConnecting);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(0, 1, 0, "测试1");
        menu.add(0, 2, 0, "测试2");
        menu.add(0, 3, 0, "测试3");
        menu.setHeaderTitle("菜单测试");
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case 1:
                Toast.makeText(this, "单击了测试1", Toast.LENGTH_SHORT).show();
                break;
            case 2:
                Toast.makeText(this,"单击了测试1",Toast.LENGTH_SHORT).show();
                break;
            case 3:
                Toast.makeText(this,"单击了测试1",Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(this,  MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DATA},
                MediaStore.Audio.Media.SIZE + ">80000", null, MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
//        while (cursor.moveToNext()) {
//            long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
//            if (id > 0)
//                Utility.mAllSongList.add(Long.valueOf(id));
//            String fullpath = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
//            Utility.SortWithFolder(id,fullpath);
//        }
//        System.out.println(Utility.mFolderMap);
//        long start = System.currentTimeMillis();
//        for(int i = 0 ; i < Utility.mAllSongList.size() ; i++)
//        {
//            Utility.SortWithFolder(Utility.mAllSongList.get(i));
//        }
//        long end = System.currentTimeMillis();
//        System.out.println("耗时: " + (end - start));
//        cursor.close();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

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
            Utility.mPlayList = task.get();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //后退返回桌面
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK)
        {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void getCurrentInfo(MP3Info MP3info,boolean isplay){
        MP3Info temp = MP3info;
        mActionbar.UpdateBottomStatus(MP3info, isplay);
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
}

