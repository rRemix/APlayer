package remix.myplayer.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;

import com.umeng.analytics.MobclickAgent;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.SplashAdapter;
import remix.myplayer.fragment.SplashFragment;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.SPUtil;

/**
 * Created by taeja on 16-6-8.
 */
public class SplashActivity extends BaseAppCompatActivity {
    @BindView(R.id.viewPager)
    ViewPager mViewPager;

    private SplashAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        ButterKnife.bind(this);


        boolean isFirst = SPUtil.getValue(getApplicationContext(), "Setting", "First", true);
        //第一次启动软件
        if(isFirst){
            //保存默认主题设置
            SPUtil.putValue(this,"Setting","ThemeMode", ThemeStore.DAY);
            SPUtil.putValue(this,"Setting","ThemeColor",ThemeStore.THEME_PINK);
            //添加我的收藏列表
//            XmlUtil.addPlaylist(this,"我的收藏");
            Global.mPlayQueueId = PlayListUtil.addPlayList(Constants.PLAY_QUEUE);
            SPUtil.putValue(this,"Setting","PlayQueueID",Global.mPlayQueueId);
            Global.mMyLoveId = PlayListUtil.addPlayList(getString(R.string.my_favorite));
            SPUtil.putValue(this,"Setting","MyLoveID",Global.mMyLoveId);
        }else {
            new Thread(){
                @Override
                public void run() {
                    Global.mPlayQueueId = SPUtil.getValue(SplashActivity.this,"Setting","PlayQueueID",-1);
                    Global.mMyLoveId = SPUtil.getValue(SplashActivity.this,"Setting","MyLoveID",-1);
                    Global.mPlayQueue = PlayListUtil.getIDList(Global.mPlayQueueId);
                    Global.mPlayList = PlayListUtil.getAllPlayListInfo();
                }
            }.start();
        }
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

    public void onResume() {
        MobclickAgent.onPageStart(SplashActivity.class.getSimpleName());
        super.onResume();
    }
    public void onPause() {
        MobclickAgent.onPageEnd(SplashActivity.class.getSimpleName());
        super.onPause();
    }

}
