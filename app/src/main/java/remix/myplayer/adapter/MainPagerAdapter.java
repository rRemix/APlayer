package remix.myplayer.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import remix.myplayer.APlayerApplication;
import remix.myplayer.R;
import remix.myplayer.bean.Category;
import remix.myplayer.ui.fragment.AlbumFragment;
import remix.myplayer.ui.fragment.ArtistFragment;
import remix.myplayer.ui.fragment.FolderFragment;
import remix.myplayer.ui.fragment.PlayListFragment;
import remix.myplayer.ui.fragment.SongFragment;
import remix.myplayer.util.LogUtil;

import static com.squareup.haha.guava.base.Joiner.checkNotNull;

/**
 * Created by Remix on 2018/1/10.
 */

public class MainPagerAdapter extends FragmentStatePagerAdapter {
    private List<Category> mCateGory = new ArrayList<>();
    private Map<String,WeakReference<Fragment>> mCacheMap = new HashMap<>();
    public MainPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        super.destroyItem(container, position, object);
    }

    @Override
    public Fragment getItem(int position) {
        Category category = mCateGory.get(position);
        WeakReference<Fragment> reference = mCacheMap.get(category.getTitle());

        if(reference != null && reference.get() != null){
            LogUtil.d("ConfigViewPager","缓存命中: " + reference.get());
            return reference.get();
        }
        Fragment fragment = category.getTitle().equals(APlayerApplication.getContext().getString(R.string.tab_song)) ? new SongFragment() :
                category.getTitle().equals(APlayerApplication.getContext().getString(R.string.tab_album)) ? new AlbumFragment() :
                category.getTitle().equals(APlayerApplication.getContext().getString(R.string.tab_artist)) ? new ArtistFragment() :
                category.getTitle().equals(APlayerApplication.getContext().getString(R.string.tab_playlist)) ? new PlayListFragment() : new FolderFragment();
        mCacheMap.put(category.getTitle(),new WeakReference<>(fragment));
        LogUtil.d("ConfigViewPager","重新创建: " + fragment);
        return fragment;
    }

    public void setList(List<Category> categories){
        checkNotNull(categories);
        mCateGory = categories;
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
