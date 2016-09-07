package remix.myplayer.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.SplashAdapter;
import remix.myplayer.fragment.SplashFragment;
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


        boolean isFirst = SPUtil.getValue(getApplicationContext(), "setting", "First", true);
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
    protected void onDestroy() {
        super.onDestroy();
    }

}
