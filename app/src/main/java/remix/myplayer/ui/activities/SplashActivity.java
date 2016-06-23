package remix.myplayer.ui.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.View;

import remix.myplayer.R;
import remix.myplayer.adapters.SplashAdapter;
import remix.myplayer.fragments.SplashFragment;
import remix.myplayer.inject.ViewInject;
import remix.myplayer.utils.SharedPrefsUtil;

/**
 * Created by taeja on 16-6-8.
 */
public class SplashActivity extends BaseAppCompatActivity {
    @ViewInject(R.id.viewPager)
    private ViewPager mViewPager;
    private SplashAdapter mAdapter;

    @Override
    public int getLayoutId() {
        return R.layout.activity_splash;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean isFirst = SharedPrefsUtil.getValue(getApplicationContext(), "setting", "First", true);
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

    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
    }
}
