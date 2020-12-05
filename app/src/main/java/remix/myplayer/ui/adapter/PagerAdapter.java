package remix.myplayer.ui.adapter;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Remix on 2015/11/29.
 */

/**
 * ViewPager的适配器
 */

public class PagerAdapter extends FragmentPagerAdapter {

  private List<Fragment> mFragmentList = new ArrayList<>();

  public PagerAdapter(FragmentManager fm) {
    super(fm);
  }

  private List<String> mTitles = new ArrayList<>();

  public void addFragment(Fragment fragment) {
    mFragmentList.add(fragment);
  }

  public void setTitles(List<String> titles) {
    mTitles = titles;
  }

  public void addTitle(String title) {
    mTitles.add(title);
  }

  @Override
  public CharSequence getPageTitle(int position) {
    return mTitles.get(position);
  }

  @Override
  public Fragment getItem(int position) {
    return mFragmentList.get(position);
  }

  @Override
  public int getCount() {
    return mFragmentList.size();
  }

}
