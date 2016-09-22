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
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.SongAdapter;
import remix.myplayer.listener.OnItemClickListener;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.MultiChoice;
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
    public static final String TAG = RecetenlyActivity.class.getSimpleName();
    private ArrayList<MP3Item> mInfoList;
    private SongAdapter mAdapter;
    @BindView(R.id.recently_shuffle)
    RelativeLayout mShuffle;
    @BindView(R.id.toolbar)
    Toolbar mToolBar;
    @BindView(R.id.recyclerview)
    RecyclerView mRecyclerView;
    private MaterialDialog mMDDialog;
    private final int START = 0;
    private final int END = 1;
    public static MultiChoice MultiChoice = new MultiChoice();

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case START:
                    if(mMDDialog != null && !mMDDialog.isShowing()){
                        mMDDialog.show();
                    }
                    break;
                case END:
                    if(mMDDialog != null && mMDDialog.isShowing()){
                        mAdapter.setInfoList(mInfoList);
                        mShuffle.setVisibility(mInfoList == null || mInfoList.size() == 0 ? View.GONE : View.VISIBLE);
                        mMDDialog.dismiss();
                    }
            }

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
        MultiChoice.setOnUpdateOptionMenuListener(new MultiChoice.onUpdateOptionMenuListener() {
            @Override
            public void onUpdate(boolean multiShow) {
                MultiChoice.setShowing(multiShow);
                mToolBar.setNavigationIcon(MultiChoice.isShow() ? R.drawable.actionbar_delete : R.drawable.actionbar_menu);
                mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(MultiChoice.isShow()){
                            MultiChoice.UpdateOptionMenu(false);
                        } else {
                            finish();
                        }
                    }
                });
                invalidateOptionsMenu();
            }
        });

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.addItemDecoration(new RecyclerItemDecoration(this,RecyclerItemDecoration.VERTICAL_LIST,getResources().getDrawable(R.drawable.divider)));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mAdapter = new SongAdapter(RecetenlyActivity.this, SongAdapter.RECENTLY);
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if(!MultiChoice.itemAddorRemoveWithClick(view,position,TAG)){
                    Intent intent = new Intent(Constants.CTL_ACTION);
                    Bundle arg = new Bundle();
                    arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                    arg.putInt("Position", position);
                    intent.putExtras(arg);
                    Global.setPlayingList(Global.mWeekList);
                    sendBroadcast(intent);
                }
//                if(MultiChoice.isShow()){
//                    MultiChoice.RemoveOrAddView(view);
//                } else {
//                    Intent intent = new Intent(Constants.CTL_ACTION);
//                    Bundle arg = new Bundle();
//                    arg.putInt("Control", Constants.PLAYSELECTEDSONG);
//                    arg.putInt("Position", position);
//                    intent.putExtras(arg);
//                    Global.setPlayingList(Global.mWeekList);
//                    sendBroadcast(intent);
//                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                MultiChoice.itemAddorRemoveWithLongClick(view,position,TAG);
//                if(!MultiChoice.isShow())
//                    updateOptionsMenu(true);
//                MultiChoice.RemoveOrAddView(view);
            }
        });
        mRecyclerView.setAdapter(mAdapter);

        mMDDialog = new MaterialDialog.Builder(this)
                .title("加载中")
                .content("请稍等")
                .progress(true, 0)
                .progressIndeterminateStyle(false).build();

        initToolbar(mToolBar,getString(R.string.recently));
        new Thread(){
            @Override
            public void run() {
                mHandler.sendEmptyMessage(START);
                mInfoList = DBUtil.getMP3ListByIds(Global.mWeekList);
                mHandler.sendEmptyMessage(END);
            }
        }.start();
    }


    private void cleanSelectedViews() {
        MultiChoice.clear();
    }

    @Override
    public void onBackPressed() {
        if(MultiChoice.isShow()) {
            MultiChoice.UpdateOptionMenu(false);
            cleanSelectedViews();
        } else {
            finish();
        }
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
