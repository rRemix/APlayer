package remix.myplayer.ui.adapter;

import static remix.myplayer.bean.misc.Category.TAG_SONG;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import android.view.ViewGroup;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import remix.myplayer.bean.misc.Category;
import remix.myplayer.ui.fragment.AlbumFragment;
import remix.myplayer.ui.fragment.ArtistFragment;
import remix.myplayer.ui.fragment.FolderFragment;
import remix.myplayer.ui.fragment.LibraryFragment;
import remix.myplayer.ui.fragment.PlayListFragment;
import remix.myplayer.ui.fragment.SongFragment;


/**
 * Created by Remix on 2018/1/10.
 */

public class MainPagerAdapter extends FragmentPagerAdapter {

  private static final String TAG = "MainPagerAdapter";
  private final FragmentManager mFM;
  private List<Category> mCateGory = new ArrayList<>();
  private Map<Integer, WeakReference<Fragment>> mFragmentMap = new HashMap<>();

  public MainPagerAdapter(FragmentManager fm) {
    super(fm);
    mFM = fm;
  }

  @Override
  public long getItemId(int position) {
    if (position >= mCateGory.size()) {
      return super.getItemId(position);
    }
    return mCateGory.get(position).getTag();
  }

  @Override
  public int getItemPosition(Object object) {
    for (int i = 0; i < mCateGory.size(); i++) {
      if (mCateGory.get(i).getClassName().equals(object.getClass().getName())) {
        return i;
      }
    }
    return POSITION_NONE;
  }

  @Override
  public void destroyItem(ViewGroup container, int position, Object object) {
    super.destroyItem(container, position, object);
    final WeakReference<Fragment> weakReference = mFragmentMap.get(position);
    if (weakReference != null) {
      weakReference.clear();
    }
  }

  @Override
  public Object instantiateItem(ViewGroup container, int position) {
    final Fragment fragment = (Fragment) super.instantiateItem(container, position);
    final WeakReference<Fragment> weakReference = mFragmentMap.get(position);
    if (weakReference != null) {
      weakReference.clear();
    }
    mFragmentMap.put(position, new WeakReference<>(fragment));
    return fragment;
  }

  public Fragment getFragment(final int position) {
    if (position >= mCateGory.size()) {
      return new Fragment();
    }

    final Category category = mCateGory.get(position);
    for (Fragment fragment : mFM.getFragments()) {
      if (fragment instanceof LibraryFragment && fragment.getClass().getName()
          .equals(category.getClassName())) {
        return fragment;
      }
    }

    final WeakReference<Fragment> weakReference = mFragmentMap.get(position);
    if (weakReference != null && weakReference.get() != null) {
      return weakReference.get();
    }
    return getItem(position);
  }

  @Override
  public Fragment getItem(int position) {
    if (position >= mCateGory.size()) {
      return new Fragment();
    }

    WeakReference<Fragment> weakReference = mFragmentMap.get(position);
    if (weakReference != null && weakReference.get() != null) {
      return weakReference.get();
    }

    Category category = mCateGory.get(position);
    Fragment fragment = category.getTag() == TAG_SONG ? new SongFragment() :
        category.getTag() == Category.TAG_ALBUM ? new AlbumFragment() :
            category.getTag() == Category.TAG_ARTIST ? new ArtistFragment() :
                category.getTag() == Category.TAG_PLAYLIST ? new PlayListFragment()
                    : new FolderFragment();
    WeakReference<Fragment> newWeakReference = new WeakReference<>(fragment);
    mFragmentMap.put(position, newWeakReference);
    return fragment;
  }

  public void setList(List<Category> categories) {
    mCateGory = categories;
    alignCache();
  }

  private void alignCache() {
    if (mFragmentMap.size() == 0) {
      return;
    }
    HashMap<String, WeakReference<Fragment>> mappings = new HashMap<>(mFragmentMap.size());

    for (int i = 0, size = mFragmentMap.size(); i < size; i++) {
      WeakReference<Fragment> ref = mFragmentMap.get(i);
      if (ref != null && ref.get() != null) {
        mappings.put(ref.get().getClass().getName(), ref);
      }
    }

    for (int i = 0, size = mCateGory.size(); i < size; i++) {
      WeakReference<Fragment> ref = mappings.get(mCateGory.get(i).getClassName());
      if (ref != null) {
        mFragmentMap.put(i, ref);
      } else {
        mFragmentMap.remove(i);
      }
    }
  }

  public List<Category> getList() {
    return mCateGory;
  }

  @Override
  public CharSequence getPageTitle(int position) {
    return mCateGory.get(position).getTitle();
  }

  @Override
  public int getCount() {
    return mCateGory != null ? mCateGory.size() : 0;
  }

}
