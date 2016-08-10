package remix.myplayer.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.SongAdapter;
import remix.myplayer.model.MP3Item;
import remix.myplayer.inject.ViewInject;
import remix.myplayer.listener.OnItemClickListener;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.RecyclerItemDecoration;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DBUtil;
import remix.myplayer.util.Global;

/**
 * Created by taeja on 16-3-4.
 */

/**
 * 最近添加歌曲的界面
 * 目前为最近7天添加
 */
public class RecetenlyActivity extends ToolbarActivity implements MusicService.Callback{
    private ArrayList<MP3Item> mInfoList;
    private SongAdapter mAdapter;
    @BindView(R.id.toolbar)
    Toolbar mToolBar;
    @BindView(R.id.recyclerview)
    RecyclerView mRecyclerView;

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
           mAdapter.setInfoList(mInfoList);
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recently);
        ButterKnife.bind(this);

        MusicService.addCallback(RecetenlyActivity.this);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new RecyclerItemDecoration(this,RecyclerItemDecoration.VERTICAL_LIST,getResources().getDrawable(R.drawable.divider)));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mAdapter = new SongAdapter(RecetenlyActivity.this, SongAdapter.RECENTLY);
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                Intent intent = new Intent(Constants.CTL_ACTION);
                Bundle arg = new Bundle();
                arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                arg.putInt("Position", position);
                intent.putExtras(arg);
                Global.setPlayingList(Global.mWeekList);
                sendBroadcast(intent);
            }

            @Override
            public void onItemLongClick(View view, int position) {
            }
        });
        mRecyclerView.setAdapter(mAdapter);

        initToolbar(mToolBar,getString(R.string.recently));
        new Thread(){
            @Override
            public void run() {
                mInfoList = DBUtil.getMP3ListByIds(Global.mWeekList);
                mHandler.sendEmptyMessage(0);
            }
        }.start();
    }

    //随机播放
    public void onPlayShuffle(View v){
        if(Global.mWeekList == null || Global.mWeekList.size() == 0){
            Toast.makeText(RecetenlyActivity.this,getString(R.string.no_song),Toast.LENGTH_SHORT).show();
            return;
        }
        MusicService.setPlayModel(Constants.PLAY_SHUFFLE);
        Intent intent = new Intent(Constants.CTL_ACTION);
        intent.putExtra("Control", Constants.NEXT);
        Global.setPlayingList(Global.mWeekList);
        sendBroadcast(intent);
    }


    @Override
    public void UpdateUI(MP3Item MP3Item, boolean isplay) {
//        List<Fragment> fragmentList = getSupportFragmentManager().getFragments();
//        for(Fragment fragment : fragmentList){
//            ((RecentlyFragment) fragment).getAdapter().notifyDataSetChanged();
//        }
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

}
