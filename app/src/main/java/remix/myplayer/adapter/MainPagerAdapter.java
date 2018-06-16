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

import remix.myplayer.bean.Category;
import remix.myplayer.ui.fragment.AlbumFragment;
import remix.myplayer.ui.fragment.ArtistFragment;
import remix.myplayer.ui.fragment.FolderFragment;
import remix.myplayer.ui.fragment.PlayListFragment;
import remix.myplayer.ui.fragment.SongFragment;
import remix.myplayer.util.LogUtil;


/**
 * Created by Remix on 2018/1/10.
 */

public class MainPagerAdapter extends FragmentStatePagerAdapter {
    private List<Category> mCateGory = new ArrayList<>();
    private Map<Integer,WeakReference<Fragment>> mCacheMap = new HashMap<>();
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
        WeakReference<Fragment> reference = mCacheMap.get(category.getTag());

        if(reference != null && reference.get() != null){
            LogUtil.d("ConfigViewPager","缓存命中: " + reference.get());
            return reference.get();
        }
        Fragment fragment = category.getTag() == Category.TAG_SONG ? new SongFragment() :
                category.getTag() == Category.TAG_ALBUM ? new AlbumFragment() :
                category.getTag() == Category.TAG_ARTIST ? new ArtistFragment() :
                category.getTag() == Category.TAG_PLAYLIST ? new PlayListFragment() : new FolderFragment();
        mCacheMap.put(category.getTag(),new WeakReference<>(fragment));
        LogUtil.d("ConfigViewPager","重新创建: " + fragment);
        return fragment;
    }

    public void setList(List<Category> categories){
        mCateGory = categories;
    }

    public List<Category> getList(){
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
