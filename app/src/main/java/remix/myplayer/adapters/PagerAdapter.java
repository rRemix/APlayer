package remix.myplayer.adapters;



import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Remix on 2015/11/29.
 */
public class PagerAdapter extends FragmentPagerAdapter {
    private List<Fragment> mFragmentList = new ArrayList<>();
    private boolean mFlag = false;
    public PagerAdapter(FragmentManager fm) {
        super(fm);
    }
    private String[] mTitles;
    public void AddFragment(Fragment fragment)
    {
        mFragmentList.add(fragment);
    }
    public void setTitles(String[] titles){
        mTitles = titles;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles[position];
    }

    @Override
    public Fragment getItem(int position) {
        return mFragmentList.get(position);
    }

    @Override
    public int getCount() {
        return mFragmentList.size();
    }
    public void SetFragment(Fragment fragment, int position)
    {
        mFragmentList.set(position,fragment);
        notifyDataSetChanged();
    }


}
