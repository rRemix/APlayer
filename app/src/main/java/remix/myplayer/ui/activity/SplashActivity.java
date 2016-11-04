package remix.myplayer.ui.activity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.view.ViewPager;
import android.telephony.gsm.GsmCellLocation;
import android.view.View;

import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.SplashAdapter;
import remix.myplayer.fragment.SplashFragment;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.StatusBarUtil;

/**
 * Created by taeja on 16-6-8.
 */
public class SplashActivity extends BaseActivity {
    @BindView(R.id.viewPager)
    ViewPager mViewPager;

    private SplashAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);

        final boolean isFirst = SPUtil.getValue(getApplicationContext(), "Setting", "First", true);

        new Thread(){
            @Override
            public void run() {
                //第一次启动软件
                if(isFirst){
                    //保存默认主题设置
                    SPUtil.putValue(SplashActivity.this,"Setting","ThemeMode", ThemeStore.DAY);
                    SPUtil.putValue(SplashActivity.this,"Setting","ThemeColor",ThemeStore.THEME_RED);
                    //添加我的收藏列表
                    Global.mPlayQueueID = PlayListUtil.addPlayList(Constants.PLAY_QUEUE);
                    SPUtil.putValue(SplashActivity.this,"Setting","PlayQueueID",Global.mPlayQueueID);
                    Global.mMyLoveID = PlayListUtil.addPlayList(getString(R.string.my_favorite));
                    SPUtil.putValue(SplashActivity.this,"Setting","MyLoveID",Global.mMyLoveID);
                }else {
                    Global.mPlayQueueID = SPUtil.getValue(SplashActivity.this,"Setting","PlayQueueID",-1);
                    Global.mMyLoveID = SPUtil.getValue(SplashActivity.this,"Setting","MyLoveID",-1);
                    Global.mPlayQueue = PlayListUtil.getIDList(Global.mPlayQueueID);
                    Global.mPlayList = PlayListUtil.getAllPlayListInfo();
                    Global.mRecentlyID = SPUtil.getValue(SplashActivity.this,"Setting","RecentlyID",-1);
                }
//                /** 更新最近添加列表 */
//                //不是第一次打开软件，先删除原来的数据
//                if(!isFirst){
//                    long start = System.currentTimeMillis();
//                    PlayListUtil.deleteMultiSongs(PlayListUtil.getIDList(Global.mRecentlyID),Global.mRecentlyID);
//                    PlayListUtil.deletePlayList(SPUtil.getValue(SplashActivity.this,"Setting","RecentlyID",-1));
//                    long time = System.currentTimeMillis() - start;
//                    LogUtil.d("DELETE","time:" + time);
//                }
//                //新建最近添加列表
//                Global.mRecentlyID = PlayListUtil.addPlayList(Constants.RECENTLY);
//                SPUtil.putValue(SplashActivity.this,"Setting","RecentlyID", Global.mRecentlyID);
//                //获得今天日期
//                Calendar today = Calendar.getInstance();
//                today.setTime(new Date());
//                //查询最近七天添加的歌曲
//                Cursor cursor = null;
//                try {
//                    cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                            new String[]{MediaStore.Audio.Media._ID},
//                            MediaStore.Audio.Media.DATE_ADDED + ">=" + (today.getTimeInMillis() / 1000 - (3600 * 24 * 7)),
//                            null,
//                            null);
//                    if(cursor != null && cursor.getCount() > 0){
//                        ArrayList<Integer> IDList = new ArrayList<>();
//                        while (cursor.moveToNext()){
//                            IDList.add(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
//                        }
//                        PlayListUtil.addMultiSongs(IDList,Constants.RECENTLY,Global.mRecentlyID);
//                    }
//                }catch (Exception e){
//                    e.printStackTrace();
//                } finally {
//                    if(cursor != null && !cursor.isClosed())
//                        cursor.close();
//                }
            }
        }.start();

        if(!isFirst){
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        getWindow().getDecorView().setSystemUiVisibility(View.INVISIBLE);

        mAdapter = new SplashAdapter(getSupportFragmentManager());

        for(int i = 0 ; i < 4 ;i++){
            SplashFragment fragment = new SplashFragment();
            Bundle args = new Bundle();
            args.putInt("Index",i);
            fragment.setArguments(args);
            mAdapter.AddFragment(fragment);
        }

        mViewPager.setAdapter(mAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }


    @Override
    protected void setStatusBar() {
        StatusBarUtil.setTransparent(this);
    }

    public void onResume() {
        MobclickAgent.onPageStart(SplashActivity.class.getSimpleName());
        super.onResume();
    }
    public void onPause() {
        MobclickAgent.onPageEnd(SplashActivity.class.getSimpleName());
        super.onPause();
    }

}
