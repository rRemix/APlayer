package remix.myplayer.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;

import remix.myplayer.R;
import remix.myplayer.activities.PlayListActivity;
import remix.myplayer.activities.SearchActivity;
import remix.myplayer.adapters.PagerAdapter;
import remix.myplayer.adapters.SlideMenuAdapter;
import remix.myplayer.adapters.SlideMenuRecycleAdpater;
import remix.myplayer.listeners.SlideMenuListener;
import remix.myplayer.listeners.TabTextListener;
import remix.myplayer.listeners.ViewPagerListener;
import remix.myplayer.ui.MyPager;
import remix.myplayer.ui.TimerPopupWindow;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/12/5.
 */
public class MainFragment extends Fragment {
    public static MainFragment mInstance;
    private ImageView mTabImage = null;
    private ImageButton mSlideMenuBtn;
    private ImageButton mTimer;
    private ImageButton mSearch;
    private ListView mSlideMenuList;
    private SlidingMenu mSlideMenu;
    private MyPager mViewPager;
    private LayoutInflater mInflater;
    private PagerAdapter mAdapter;

    private RecyclerView mMenuRecycle;
    private SlideMenuRecycleAdpater mMenuAdapter;
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
        mTimer = (ImageButton)v.findViewById(R.id.btn_top_timer);
        mTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), TimerPopupWindow.class));
            }
        });
        mSearch = (ImageButton)v.findViewById(R.id.btn_top_search);
        mSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getActivity(), SearchActivity.class));
            }
        });
    }

    private void initSlideMenu(View v)
    {
        //初始化菜单
        mSlideMenu = new SlidingMenu(getContext());
        mSlideMenu.setMode(SlidingMenu.LEFT);
        mSlideMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
        mSlideMenu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        mSlideMenu.setFadeDegree(0.35f);
        mSlideMenu.attachToActivity(getActivity(), SlidingMenu.SLIDING_CONTENT);
        mSlideMenu.setMenu(R.layout.slide_menu);
        mSlideMenuList = (ListView)mSlideMenu.findViewById(R.id.slide_menu_list);
        mSlideMenuList.setAdapter(new SlideMenuAdapter(getActivity().getLayoutInflater()));
        mSlideMenuList.setOnItemClickListener(new SlideMenuListener(getContext()));

        mSlideMenuBtn = (ImageButton)v.findViewById(R.id.btn_slide_menu);
        mSlideMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSlideMenu.toggle();
            }
        });
//        mSlideMenuBtn = (Button)v.findViewById(R.id.btn_slide_menu);
//        mSlideMenuBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                mMenuRecycle = ((MainActivity)getActivity()).getRecycleMenu();
//                if(mMenuRecycle == null)
//                    return;
//                if(mMenuRecycle.isShown())
//                    mMenuRecycle.setVisibility(View.INVISIBLE);
//                else
//                    mMenuRecycle.setVisibility(View.VISIBLE);
//            }
//        });
    }
    public PagerAdapter getAdapter()
    {
        return mAdapter;
    }
    //初始化ViewPager
    private void initPager() {
        mAdapter = new PagerAdapter(getActivity().getSupportFragmentManager());
        mAdapter.AddFragment(new AllSongFragment());
        mAdapter.AddFragment(new AlbumRecyleFragment());
        mAdapter.AddFragment(new ArtistRecycleFragment());
        mAdapter.AddFragment(new FolderFragment());
        mViewPager.setAdapter(mAdapter);
        mViewPager.setCurrentItem(0);
        mViewPager.addOnPageChangeListener(new ViewPagerListener(getContext(), mTabImage, 0));
    }
    //初始化custontab
    private void initTab(View rootView)
    {
        mViewPager = (MyPager)rootView.findViewById(R.id.ViewPager);
        mTabImage = (ImageView)rootView.findViewById(R.id.tab_image);
        TextView view1 = (TextView) rootView.findViewById(R.id.tab_song);
        TextView view2 = (TextView)rootView.findViewById(R.id.tab_album);
        TextView view3 = (TextView)rootView.findViewById(R.id.tab_artist);
        TextView view4 = (TextView)rootView.findViewById(R.id.tab_playlist);
        view1.setOnClickListener(new TabTextListener(mViewPager, 0));
        view2.setOnClickListener(new TabTextListener(mViewPager, 1));
        view3.setOnClickListener(new TabTextListener(mViewPager, 2));
        view4.setOnClickListener(new TabTextListener(mViewPager, 3));
    }

    public boolean isMenuShow()
    {
        return mSlideMenu.isMenuShowing();
    }
    public void toggleMenu()
    {
        mSlideMenu.toggle();
    }

    class SlideMenuListener implements AdapterView.OnItemClickListener {
        private Context mContext;

        public SlideMenuListener(Context mContext) {
            this.mContext = mContext;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            switch (view.getId()) {
                case 0:
                    mSlideMenu.toggle();
                    mViewPager.setCurrentItem(0);
                    break;
                case 1:
                    startActivity(new Intent(getActivity(), PlayListActivity.class));
                    break;
                case 2:
                    Intent intent = new Intent(Utility.CTL_ACTION);
                    intent.putExtra("Control", Utility.PREV);
                    mContext.sendBroadcast(intent);
                    break;
                default:break;
            }
        }
    }


}
