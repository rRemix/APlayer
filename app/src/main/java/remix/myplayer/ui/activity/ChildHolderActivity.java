package remix.myplayer.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.ChildHolderAdapter;
import remix.myplayer.fragment.BottomActionBarFragment;
import remix.myplayer.model.MP3Item;
import remix.myplayer.model.PlayListItem;
import remix.myplayer.inject.ViewInject;
import remix.myplayer.listener.OnItemClickListener;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.RecyclerItemDecoration;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DBUtil;
import remix.myplayer.util.Global;

/**
 * Created by Remix on 2015/12/4.
 */

/**
 * 专辑、艺术家、文件夹、播放列表详情
 */
public class ChildHolderActivity extends BaseAppCompatActivity implements MusicService.Callback{
    private final static String TAG = "ChildHolderActivity";
    private static boolean mIsRunning = false;
    private ImageView mBack;
    //获得歌曲信息列表的参数
    private int mId;
    private int mType;
    private String mArg;
    private ArrayList<MP3Item> mInfoList;

    //歌曲数目与标题
    @BindView(R.id.album_holder_item_num)
    TextView mNum;
    @BindView(R.id.artist_album_title)
    TextView mTitle;
    @BindView(R.id.child_holder_recyclerView)
    RecyclerView mRecyclerView;

    private BottomActionBarFragment mActionbar;

    private ChildHolderAdapter mAdapter;
    public static ChildHolderActivity mInstance = null;
    //是否需要更新adapter
    private static boolean mNeedRefresh = false;
    //更新ListView
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(mInfoList == null)
                return;
            mAdapter.setList(mInfoList);
            mNum.setText(mInfoList.size() + "首歌曲");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_holder);
        ButterKnife.bind(this);

        mInstance = this;
        MusicService.addCallback(ChildHolderActivity.this);

        //参数id，类型，标题
        mId = getIntent().getIntExtra("Id", -1);
        mType = getIntent().getIntExtra("Type", -1);
        mArg = getIntent().getStringExtra("Title");

        new Thread(){
            @Override
            public void run() {
                mInfoList = getMP3List();
                mHandler.sendEmptyMessage(0);
            }
        }.start();

        mAdapter = new ChildHolderAdapter(this,mType,mArg);
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (mInfoList != null && mInfoList.size() == 0)
                    return;
                ArrayList<Long> ids = new ArrayList<Long>();
                for (MP3Item info : mInfoList) {
                    if(info != null && info.getId() > 0)
                        ids.add(info.getId());
                }
                //设置正在播放列表
                Global.setPlayingList((ArrayList) ids.clone());

                Intent intent = new Intent(Constants.CTL_ACTION);
                Bundle arg = new Bundle();
                arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                arg.putInt("Position", position);
                intent.putExtras(arg);
                sendBroadcast(intent);
            }

            @Override
            public void onItemLongClick(View view, int position) {

            }
        });
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.addItemDecoration(new RecyclerItemDecoration(this,RecyclerItemDecoration.VERTICAL_LIST,getResources().getDrawable(R.drawable.divider)));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        //歌曲数目与标题
        if(mType != Constants.FOLDER_HOLDER) {
            if(mArg.contains("unknown")){
                if(mType == Constants.ARTIST_HOLDER)
                    mTitle.setText(getString(R.string.unknow_artist));
                else if(mType == Constants.ALBUM_HOLDER){
                    mTitle.setText(getString(R.string.unknow_album));
                }
            } else {
                mTitle.setText(mArg);
            }
        } else
            mTitle.setText(mArg.substring(mArg.lastIndexOf("/") + 1,mArg.length()));
        //初始化底部状态栏
        mActionbar = (BottomActionBarFragment) getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);
        if(Global.mPlayingList == null || Global.mPlayingList.size() == 0)
            return;

        mActionbar.UpdateBottomStatus(MusicService.getCurrentMP3(), MusicService.getIsplay());
    }

    public void UpdateData(){
//        if(!mIsRunning)
//            return;
        if(mType == Constants.PLAYLIST_HOLDER){
            //播放列表
            if(!PlayListActivity.getPlayList().containsKey(mArg)){
                mAdapter.setList(new ArrayList<MP3Item>());
                mNum.setText("0首歌曲");
            } else {
                new UpdateThread().start();
            }
        } else if(mType == Constants.FOLDER_HOLDER){
            //文件夹
            mArg = getIntent().getStringExtra("Title");
            if(!Global.mFolderMap.containsKey(mArg)){
                mAdapter.setList(new ArrayList<MP3Item>());
                mNum.setText("0首歌曲");
            } else {
                new UpdateThread().start();
            }
        } else {
            //艺术家或者专辑
            new UpdateThread().start();
        }
    }

    class UpdateThread extends Thread{
        @Override
        public void run() {
            mInfoList = getMP3List();
            mHandler.sendEmptyMessage(0);
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
            case Constants.ALBUM_HOLDER:
                mInfoList = DBUtil.getMP3InfoByArtistIdOrAlbumId(mId, Constants.ALBUM_HOLDER);
                break;
            //歌手id
            case Constants.ARTIST_HOLDER:
                mInfoList = DBUtil.getMP3InfoByArtistIdOrAlbumId(mId, Constants.ARTIST_HOLDER);
                break;
            //文件夹名
            case Constants.FOLDER_HOLDER:
                mInfoList = DBUtil.getMP3ListByIds(Global.mFolderMap.get(mArg));
                mArg = mArg.substring(mArg.lastIndexOf("/") + 1,mArg.length());
                break;
            //播放列表名
            case Constants.PLAYLIST_HOLDER:
                ArrayList<PlayListItem> list = PlayListActivity.getPlayList().get(mArg);
                ArrayList<Long> ids = new ArrayList<>();
                if(list == null)
                    break;
                for(PlayListItem item : list) {
                    MP3Item temp = new MP3Item(item.getId(),item.getSongame(),item.getSongame(),"",item.getAlbumId(),
                            item.getArtist(),0,"","",0,"");
                    mInfoList.add(temp);
//                    MP3Item temp = DBUtil.getMP3InfoById(item.getId());
//                    //该歌曲已经失效
//                    if(temp == null){
//                        DBUtil.deleteSongInPlayList(mArg,item.getId());
//                    } else {
//                        mInfoList.add(temp);
//                    }
                }
                break;
        }
        return mInfoList;
    }

    //退出按钮
    public void onBack(View v){
        finish();
    }

    //随机播放按钮
    public void onPlayShuffle(View v){
        MusicService.setPlayModel(Constants.PLAY_SHUFFLE);
        Intent intent = new Intent(Constants.CTL_ACTION);
        intent.putExtra("Control", Constants.NEXT);
        //设置正在播放列表
        ArrayList<Long> ids = new ArrayList<Long>();
        for (MP3Item info : mInfoList)
            ids.add(info.getId());
        Global.setPlayingList((ArrayList) ids.clone());
        sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //更新界面
    @Override
    public void UpdateUI(MP3Item MP3Item, boolean isplay) {
        //底部状态兰
        mActionbar.UpdateBottomStatus(MP3Item, isplay);
        //更新高亮歌曲
        if(mAdapter != null)
            mAdapter.notifyDataSetChanged();
    }

    @Override
    public int getType() {
        return Constants.CHILDHOLDERACTIVITY;
    }

    public static void setFresh(boolean needfresh){
        mNeedRefresh = needfresh;
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if(mNeedRefresh){
//            UpdateData();
//            mNeedRefresh = false;
//        }
        mIsRunning = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsRunning = false;
    }

}
