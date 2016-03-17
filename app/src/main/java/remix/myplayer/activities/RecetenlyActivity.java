package remix.myplayer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;

import remix.myplayer.R;
import remix.myplayer.adapters.RecentlyAdapter;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.services.MusicService;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;

/**
 * Created by taeja on 16-3-4.
 */
public class RecetenlyActivity extends ToolbarActivity implements MusicService.Callback{
    private RecentlyAdapter mAdapter;
    private Toolbar mToolBar;
    private ListView mListView;
    private static boolean mIsRunning = false;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recently);
        MusicService.addCallback(RecetenlyActivity.this);
        initListView();

        mToolBar = (Toolbar) findViewById(R.id.toolbar);
        initToolbar(mToolBar,"最近添加");
    }

    private void initListView() {
        mAdapter = new RecentlyAdapter(this, DBUtil.getMP3ListByIds(DBUtil.mWeekList));
        mListView = (ListView)findViewById(R.id.recently_listview);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(Constants.CTL_ACTION);
                Bundle arg = new Bundle();
                arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                arg.putInt("Position", position);
                intent.putExtras(arg);
                DBUtil.setPlayingList(DBUtil.mWeekList);
                sendBroadcast(intent);
                view.setSelected(true);

            }
        });

    }

    public void onPlayShuffle(View v){
        MusicService.setPlayModel(Constants.PLAY_SHUFFLE);
        Intent intent = new Intent(Constants.CTL_ACTION);
        intent.putExtra("Control", Constants.NEXT);
        DBUtil.setPlayingList(DBUtil.mWeekList);
        sendBroadcast(intent);
    }

//    private void initToolbar() {
//        mToolBar = (Toolbar) findViewById(R.id.toolbar);
//        mToolBar.setTitle("最近添加");
//        mToolBar.setTitleTextColor(Color.parseColor("#ffffffff"));
//        setSupportActionBar(mToolBar);
//        mToolBar.setNavigationIcon(R.drawable.common_btn_back);
//        mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                finish();
//            }
//        });
//        mToolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
//            @Override
//            public boolean onMenuItemClick(MenuItem item) {
//                switch (item.getItemId()) {
//                    case R.id.toolbar_search:
//                        startActivity(new Intent(RecetenlyActivity.this, SearchActivity.class));
//                        break;
//                    case R.id.toolbar_timer:
//                        startActivity(new Intent(RecetenlyActivity.this, TimerDialog.class));
//                        break;
//                }
//                return true;
//            }
//        });
//    }


    @Override
    public void UpdateUI(MP3Info MP3info, boolean isplay) {
//        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
//        for(Fragment fragment : fragmentList){
//            ((RecentlyFragment) fragment).getAdapter().notifyDataSetChanged();
//        }
        if(mIsRunning)
            mAdapter.notifyDataSetChanged();
    }

    @Override
    public int getType() {
        return Constants.RECENTLYACTIVITY;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsRunning = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsRunning = false;
    }
}
