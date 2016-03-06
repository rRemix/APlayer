package remix.myplayer.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.viewpagerindicator.TabPageIndicator;

import remix.myplayer.R;
import remix.myplayer.adapters.PagerAdapter;
import remix.myplayer.listeners.TabTextListener;
import remix.myplayer.listeners.ViewPagerListener;
import remix.myplayer.services.MusicService;
import remix.myplayer.ui.MyPager;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;

/**
 * Created by Remix on 2015/12/5.
 */
public class MainFragment extends Fragment {
    public static MainFragment mInstance;
    private ImageView mTabImage = null;
    private MyPager mViewPager;
    private LayoutInflater mInflater;
    private PagerAdapter mAdapter;
    private DrawerLayout mDrawerLayout;
    private TabPageIndicator mIndicator;
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
        initTimerandSearch(rootView);
        initPager();
        initSlideMenu(rootView);
        return rootView;
    }

    private void initTimerandSearch(View v)
    {
//        mTimer = (ImageButton)v.findViewById(R.id.btn_top_timer);
//        mTimer.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startActivity(new Intent(getActivity(), TimerPopupWindow.class));
//            }
//        });
//        mSearch = (ImageButton)v.findViewById(R.id.btn_top_search);
//        mSearch.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startActivity(new Intent(getActivity(), SearchActivity.class));
//            }
//        });
    }

    private void initSlideMenu(View v)
    {
        //初始化菜单
//        mSlideMenu = new SlidingMenu(getContext());
//        mSlideMenu.setMode(SlidingMenu.LEFT);
//        mSlideMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
//        mSlideMenu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
//        mSlideMenu.setFadeDegree(0.35f);
//        mSlideMenu.attachToActivity(getActivity(), SlidingMenu.SLIDING_CONTENT);
//        mSlideMenu.setMenu(R.layout.slide_menu);
//
//        mSlideMenuList = (ListView)mSlideMenu.findViewById(R.id.slide_menu_list);
//        mSlideMenuList.setAdapter(new SlideMenuAdapter(getActivity().getLayoutInflater()));
//        mSlideMenuList.setOnItemClickListener(new SlideMenuListener(getContext()));
//
//        mSlideMenuBtn = (ImageButton)v.findViewById(R.id.btn_slide_menu);
//        mSlideMenuBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mSlideMenu.toggle();
//            }
//        });
//        mSlideMenuAbout = (ImageButton)mSlideMenu.findViewById(R.id.drawer_about);
//        mSlideMenuExit = (ImageButton)mSlideMenu.findViewById(R.id.drawer_exit);
//        mSlideMenuExit.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                getContext().sendBroadcast(new Intent(Constants.EXIT));
//            }
//        });

    }
    public PagerAdapter getAdapter()
    {
        return mAdapter;
    }
    public MyPager getViewPager(){
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
        mViewPager.addOnPageChangeListener(new ViewPagerListener(getContext(), mTabImage, 0,4));

    }
    //初始化custontab
    private void initTab(View rootView)
    {
        mViewPager = (MyPager)rootView.findViewById(R.id.ViewPager);
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

    public void onPlayShuffle(View v){
        MusicService.setPlayModel(Constants.PLAY_SHUFFLE);
        Intent intent = new Intent(Constants.CTL_ACTION);
        intent.putExtra("Control", Constants.NEXT);
        DBUtil.setPlayingList(DBUtil.mWeekList);
        getActivity().sendBroadcast(intent);
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
