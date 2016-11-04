package remix.myplayer.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.adapter.ChildHolderAdapter;
import remix.myplayer.fragment.BottomActionBarFragment;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.interfaces.OnUpdateOptionMenuListener;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.ListItemDecoration;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.PlayListUtil;

/**
 * Created by Remix on 2015/12/4.
 */

/**
 * 专辑、艺术家、文件夹、播放列表详情
 */
public class ChildHolderActivity extends MultiChoiceActivity implements MusicService.Callback{
    public final static String TAG = ChildHolderActivity.class.getSimpleName();
    public final static String TAG_PLAYLIST_SONG = ChildHolderActivity.class.getSimpleName() + "Song";
    private static boolean mIsRunning = false;
    //获得歌曲信息列表的参数
    public static int mId;
    private int mType;
    private String mArg;
    private ArrayList<MP3Item> mInfoList;
    /** 播放列表歌曲id列表 */
    private ArrayList<Integer> mPlayListSongIDList;

    //歌曲数目与标题
    @BindView(R.id.album_holder_item_num)
    TextView mNum;
    @BindView(R.id.child_holder_recyclerView)
    RecyclerView mRecyclerView;
    @BindView(R.id.toolbar)
    Toolbar mToolBar;

    private String Title;
    private BottomActionBarFragment mBottombar;

    private ChildHolderAdapter mAdapter;
    public static ChildHolderActivity mInstance = null;

    //更新
    private Handler mRefreshHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == Constants.CLEAR_MULTI){
                mMultiChoice.clearSelectedViews();
            } else if(msg.what == Constants.UPDATE_ADAPTER){
                if(mInfoList == null)
                    return;
                mAdapter.setList(mInfoList);
                mNum.setText(mInfoList.size() + "首歌曲");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_holder);
        ButterKnife.bind(this);

        mInstance = this;
        MusicService.addCallback(ChildHolderActivity.this);
        mMultiChoice.setOnUpdateOptionMenuListener(new OnUpdateOptionMenuListener() {
            @Override
            public void onUpdate(final boolean multiShow) {
                mMultiChoice.setShowing(multiShow);
                mToolBar.setNavigationIcon(mMultiChoice.isShow() ? R.drawable.actionbar_delete : R.drawable.actionbar_menu);
                mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mMultiChoice.isShow()){
                            mMultiChoice.UpdateOptionMenu(false);
                            mMultiChoice.clear();
                        } else {
                            finish();
                        }
                    }
                });
                if(!mMultiChoice.isShow()){
                    mMultiChoice.clear();
                }
                invalidateOptionsMenu();
            }
        });

        //参数id，类型，标题
        mId = getIntent().getIntExtra("Id", -1);
        mType = getIntent().getIntExtra("Type", -1);
        mArg = getIntent().getStringExtra("Title");

        new UpdateThread().start();

        mAdapter = new ChildHolderAdapter(this,mType,mArg,mMultiChoice);
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                int songid = mInfoList.get(position).getId();
                if( !mMultiChoice.itemAddorRemoveWithClick(view,position,songid,mType == Constants.PLAYLISTSONG ? TAG_PLAYLIST_SONG : TAG)){
                    if (mInfoList != null && mInfoList.size() == 0)
                        return;
                    ArrayList<Integer> idList = new ArrayList<>();
                    for (MP3Item info : mInfoList) {
                        if(info != null && info.getId() > 0)
                            idList.add(info.getId());
                    }
                    //设置正在播放列表
                    Intent intent = new Intent(Constants.CTL_ACTION);
                    Bundle arg = new Bundle();
                    arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                    arg.putInt("Position", position);
                    intent.putExtras(arg);
                    Global.setPlayQueue(idList,ChildHolderActivity.this,intent);
//                    sendBroadcast(intent);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                int songid = mInfoList.get(position).getId();
//                if(songid > 0)
                    mMultiChoice.itemAddorRemoveWithLongClick(view,position,songid,
                            TAG,
                            mType == Constants.PLAYLIST ? Constants.PLAYLISTSONG : Constants.SONG);
            }


        });
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new ListItemDecoration(this, ListItemDecoration.VERTICAL_LIST,getResources().getDrawable(R.drawable.divider)));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        //歌曲数目与标题
        if(mType != Constants.FOLDER) {
            if(mArg.contains("unknown")){
                if(mType == Constants.ARTIST)
                    Title = getString(R.string.unknow_artist);
                else if(mType == Constants.ALBUM){
                    Title = getString(R.string.unknow_album);
                }
            } else {
                Title = mArg;
            }
        } else
            Title = mArg.substring(mArg.lastIndexOf("/") + 1,mArg.length());
        //初始化toolbar
        initToolbar(mToolBar,Title);
        //初始化底部状态栏
        mBottombar = (BottomActionBarFragment) getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);
        if(Global.mPlayQueue == null || Global.mPlayQueue.size() == 0)
            return;

        mBottombar.UpdateBottomStatus(MusicService.getCurrentMP3(), MusicService.getIsplay());

    }

    @Override
    public void onBackPressed() {
        if(mMultiChoice.isShow()) {
            mMultiChoice.UpdateOptionMenu(false);
        } else {
            finish();
        }
    }

    class UpdateThread extends Thread{
        @Override
        public void run() {
            mInfoList = getMP3List();
            mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
        }
    }

    /**
     * 根据参数(专辑id 歌手id 文件夹名 播放列表名)获得对应的歌曲信息列表
     * @return 对应歌曲信息列表
     */
    private ArrayList<MP3Item> getMP3List(){
        if(mId < 0)
            return  null;
        mInfoList = new ArrayList<>();
        switch (mType) {
            //专辑id
            case Constants.ALBUM:
                mInfoList = MediaStoreUtil.getMP3InfoByArg(mId, Constants.ALBUM);
                break;
            //歌手id
            case Constants.ARTIST:
                mInfoList = MediaStoreUtil.getMP3InfoByArg(mId, Constants.ARTIST);
                break;
            //文件夹名
            case Constants.FOLDER:
                mInfoList = MediaStoreUtil.getMP3ListByIds(Global.mFolderMap.get(mArg));
//                mArg = mArg.substring(mArg.lastIndexOf("/") + 1,mArg.length());
                break;
            //播放列表名
            case Constants.PLAYLIST:
                mPlayListSongIDList = PlayListUtil.getIDList(mId);
                mInfoList = PlayListUtil.getMP3ListByIds(mPlayListSongIDList);
                break;
        }
        return mInfoList;
    }

    @OnClick(R.id.shuffle_container)
    public void onClick(View v){
        MusicService.setPlayModel(Constants.PLAY_SHUFFLE);
        Intent intent = new Intent(Constants.CTL_ACTION);
        intent.putExtra("Control", Constants.NEXT);
        //设置正在播放列表
        ArrayList<Integer> ids = new ArrayList<>();
        for (MP3Item info : mInfoList)
            ids.add(info.getId());
        Global.setPlayQueue(ids,this,intent);
//        sendBroadcast(intent);
    }

    //更新界面
    @Override
    public void UpdateUI(MP3Item MP3Item, boolean isplay) {
        //底部状态兰
        mBottombar.UpdateBottomStatus(MP3Item, isplay);
        //更新高亮歌曲
        if(mAdapter != null)
            mAdapter.notifyDataSetChanged();
    }

    @Override
    public int getType() {
        return Constants.CHILDHOLDERACTIVITY;
    }

    @Override
    protected void onPause() {
        MobclickAgent.onPageEnd(ChildHolderActivity.class.getSimpleName());
        super.onPause();
        if(mMultiChoice.isShow()){
            mRefreshHandler.sendEmptyMessageDelayed(Constants.CLEAR_MULTI,500);
        }
    }

    @Override
    protected void onResume() {
        MobclickAgent.onPageStart(ChildHolderActivity.class.getSimpleName());
        super.onResume();
        mIsRunning = true;
        if(mMultiChoice.isShow()){
            mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsRunning = false;
    }

    public void UpdateList() {
        new UpdateThread().start();
    }

}
