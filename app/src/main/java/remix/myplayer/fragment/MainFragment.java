package remix.myplayer.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import remix.myplayer.R;
import remix.myplayer.adapter.PagerAdapter;

/**
 * Created by Remix on 2015/12/5.
 */

/**
 * 主Fragment 包含中间四个Fragment，底部控制栏
 */
public class MainFragment extends Fragment {
    public static MainFragment mInstance;
    private ImageView mTabImage = null;
    private ViewPager mViewPager;
    private LayoutInflater mInflater;
    private PagerAdapter mAdapter;
    private TabLayout mTablayout;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater;
        View rootView = inflater.inflate(R.layout.fragment_main,null);
        initPager(rootView);
        initTab(rootView);
        return rootView;
    }


    public PagerAdapter getAdapter()
    {
        return mAdapter;
    }
    public ViewPager getViewPager(){
        return mViewPager;
    }
    //初始化ViewPager
    private void initPager(View rootView) {
        mAdapter = new PagerAdapter(getActivity().getSupportFragmentManager());
        mAdapter.setTitles(new String[]{getActivity().getResources().getString(R.string.tab_song),
                getActivity().getResources().getString(R.string.tab_album),
                getActivity().getResources().getString(R.string.tab_artist),
                getActivity().getResources().getString(R.string.tab_folder)});
        mAdapter.AddFragment(new AllSongFragment());
        mAdapter.AddFragment(new AlbumFragment());
        mAdapter.AddFragment(new ArtistFragment());
        mAdapter.AddFragment(new FolderFragment());

        mViewPager = (ViewPager)rootView.findViewById(R.id.ViewPager);
        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(0);

    }

    //初始化custontab
    private void initTab(View rootView) {
        mTablayout = (TabLayout)rootView.findViewById(R.id.tabs);
        //添加tab选项卡

        mTablayout.addTab(mTablayout.newTab().setText(getActivity().getResources().getString(R.string.tab_song)));
        mTablayout.addTab(mTablayout.newTab().setText(getActivity().getResources().getString(R.string.tab_album)));
        mTablayout.addTab(mTablayout.newTab().setText(getActivity().getResources().getString(R.string.tab_artist)));
        mTablayout.addTab(mTablayout.newTab().setText(getActivity().getResources().getString(R.string.tab_folder)));
        //给Tabs设置适配器
        mTablayout.setTabsFromPagerAdapter(mAdapter);
        //viewpager与tablayout关联
        mTablayout.setupWithViewPager(mViewPager);
        //设置tab模式，当前为系统默认模式
        mTablayout.setTabMode(TabLayout.MODE_FIXED);

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}
