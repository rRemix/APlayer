package remix.myplayer.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import remix.myplayer.R;
import remix.myplayer.adapters.PagerAdapter;
import remix.myplayer.fragments.DayWeekFragment;
import remix.myplayer.listeners.TabTextListener;
import remix.myplayer.listeners.ViewPagerListener;
import remix.myplayer.utils.Constants;

/**
 * Created by taeja on 16-3-4.
 */
public class RecetenlyActivity extends AppCompatActivity{
    private ViewPager mViewPager;
    private PagerAdapter mAdapter;
    private ImageView mTabImage;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recently);

        mAdapter = new PagerAdapter(getSupportFragmentManager());
        mAdapter.setTitles(new String[]{"今天添加","最近一周"});
        DayWeekFragment todayfragment = new DayWeekFragment();
        Bundle arg = new Bundle();
        arg.putInt("Type", Constants.DAY);
        todayfragment.setArguments(arg);
        mAdapter.AddFragment(todayfragment);

        DayWeekFragment weekfragment = new DayWeekFragment();
        arg.putInt("Type",Constants.WEEK);
        weekfragment.setArguments(arg);
        mAdapter.AddFragment(weekfragment);

        mTabImage = (ImageView)findViewById(R.id.tab_image);
        mViewPager = (ViewPager)findViewById(R.id.ViewPager);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(0);
        mViewPager.addOnPageChangeListener(new ViewPagerListener(this, mTabImage, 0,2));

        TextView view1 = (TextView)findViewById(R.id.tab_today);
        TextView view2 = (TextView)findViewById(R.id.tab_week);
        view1.setOnClickListener(new TabTextListener(mViewPager, 0));
        view2.setOnClickListener(new TabTextListener(mViewPager, 1));
    }
}
