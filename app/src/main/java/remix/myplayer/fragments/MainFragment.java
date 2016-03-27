package remix.myplayer.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import remix.myplayer.R;
import remix.myplayer.adapters.PagerAdapter;
import remix.myplayer.listeners.TabTextListener;
import remix.myplayer.listeners.ViewPagerListener;

/**
 * Created by Remix on 2015/12/5.
 */

/**
 * 主Fragment 包含ViewpagerIndicatior，中间四个Fragment，底部控制栏
 */
public class MainFragment extends Fragment {
    public static MainFragment mInstance;
    private ImageView mTabImage = null;
    private ViewPager mViewPager;
    private LayoutInflater mInflater;
    private PagerAdapter mAdapter;
    public static TextView[] mTextViews = new TextView[4];
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mInstance = this;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInflater = inflater;
        View rootView = inflater.inflate(R.layout.homepage,null);
        initTab(rootView);
        initPager();
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
    private void initPager() {
        mAdapter = new PagerAdapter(getActivity().getSupportFragmentManager());
        mAdapter.setTitles(new String[]{"全部歌曲","专辑唱片","艺术家","文件夹"});
        mAdapter.AddFragment(new AllSongFragment());
        mAdapter.AddFragment(new AlbumFragment());
        mAdapter.AddFragment(new ArtistFragment());
        mAdapter.AddFragment(new FolderFragment());


        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(0);
        mViewPager.addOnPageChangeListener(new ViewPagerListener(getActivity(), mTabImage, 0,4));

    }

    //初始化custontab
    private void initTab(View rootView) {
        mViewPager = (ViewPager)rootView.findViewById(R.id.ViewPager);
        mTabImage = (ImageView)rootView.findViewById(R.id.tab_image);
//        mIndicator = (TabPageIndicator)rootView.findViewById(R.id.tab_indicator);
        mTextViews[0] = (TextView) rootView.findViewById(R.id.tab_song);
        mTextViews[1] = (TextView) rootView.findViewById(R.id.tab_album);
        mTextViews[2] = (TextView) rootView.findViewById(R.id.tab_artist);
        mTextViews[3] = (TextView) rootView.findViewById(R.id.tab_playlist);
        for(int i = 0 ; i < mTextViews.length ;i++){
            mTextViews[i].setOnClickListener(new TabTextListener(mViewPager,i));
        }

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
