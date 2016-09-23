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

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.adapter.ChildHolderAdapter;
import remix.myplayer.fragment.BottomActionBarFragment;
import remix.myplayer.listener.OnItemClickListener;
import remix.myplayer.model.MP3Item;
import remix.myplayer.model.PlayListItem;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.MultiChoice;
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
public class ChildHolderActivity extends ToolbarActivity implements MusicService.Callback{
    public final static String TAG = ChildHolderActivity.class.getSimpleName();

    private static boolean mIsRunning = false;
    //获得歌曲信息列表的参数
    private int mId;
    private int mType;
    private String mArg;
    private ArrayList<MP3Item> mInfoList;

    //歌曲数目与标题
    @BindView(R.id.album_holder_item_num)
    TextView mNum;
    @BindView(R.id.child_holder_recyclerView)
    RecyclerView mRecyclerView;
    @BindView(R.id.toolbar)
    Toolbar mToolBar;

    private String Title;
    private BottomActionBarFragment mActionbar;

    private ChildHolderAdapter mAdapter;
    public static ChildHolderActivity mInstance = null;
    //是否需要更新adapter
    private static boolean mNeedRefresh = false;
    //更新
    private Handler mRefreshHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.what == Constants.UPDATE_MULTI){
                MultiChoice.clearSelectedViews();
            } else if(msg.what == Constants.UPDATE_ADAPTER){
                if(mInfoList == null)
                    return;
                mAdapter.setList(mInfoList);
                mNum.setText(mInfoList.size() + "首歌曲");
            }
        }
    };

    public static MultiChoice MultiChoice = new MultiChoice();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_holder);
        ButterKnife.bind(this);

        mInstance = this;
        MusicService.addCallback(ChildHolderActivity.this);
        MultiChoice.setOnUpdateOptionMenuListener(new MultiChoice.onUpdateOptionMenuListener() {
            @Override
            public void onUpdate(final boolean multiShow) {
                MultiChoice.setShowing(multiShow);
                mToolBar.setNavigationIcon(MultiChoice.isShow() ? R.drawable.actionbar_delete : R.drawable.actionbar_menu);
                mToolBar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(multiShow){
                            MultiChoice.UpdateOptionMenu(false);
                            MultiChoice.clear();
                        } else {
                            finish();
                        }
                    }
                });
                if(!MultiChoice.isShow()){
                    MultiChoice.clear();
                }
                invalidateOptionsMenu();
            }
        });

        //参数id，类型，标题
        mId = getIntent().getIntExtra("Id", -1);
        mType = getIntent().getIntExtra("Type", -1);
        mArg = getIntent().getStringExtra("Title");

        new Thread(){
            @Override
            public void run() {
                mInfoList = getMP3List();
                mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
            }
        }.start();

        mAdapter = new ChildHolderAdapter(this,mType,mArg);
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if(!MultiChoice.itemAddorRemoveWithClick(view,position,TAG)){
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
        mRecyclerView.addItemDecoration(new RecyclerItemDecoration(this,RecyclerItemDecoration.VERTICAL_LIST,getResources().getDrawable(R.drawable.divider)));
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        //歌曲数目与标题
        if(mType != Constants.FOLDER_HOLDER) {
            if(mArg.contains("unknown")){
                if(mType == Constants.ARTIST_HOLDER)
                    Title = getString(R.string.unknow_artist);
                else if(mType == Constants.ALBUM_HOLDER){
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
        mActionbar = (BottomActionBarFragment) getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);
        if(Global.mPlayingList == null || Global.mPlayingList.size() == 0)
            return;

        mActionbar.UpdateBottomStatus(MusicService.getCurrentMP3(), MusicService.getIsplay());
    }



    @Override
    public void onBackPressed() {
        if(MultiChoice.isShow()) {
            MultiChoice.UpdateOptionMenu(false);
        } else {
            finish();
        }
    }

    public void UpdateData(){
//        if(!mIsRunning)
//            return;
        if(mType == Constants.PLAYLIST_HOLDER){
            //播放列表
            if(!Global.mPlaylist.containsKey(mArg)){
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
            mRefreshHandler.sendEmptyMessage(0);
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
                ArrayList<PlayListItem> list = Global.mPlaylist.get(mArg);
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

    @OnClick(R.id.shuffle_container)
    public void onClick(View v){
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

    @Override
    protected void onPause() {
        super.onPause();
        if(MultiChoice.isShow()){
            mRefreshHandler.sendEmptyMessageDelayed(Constants.UPDATE_MULTI,500);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsRunning = true;
        if(MultiChoice.isShow()){
            mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsRunning = false;
    }

}
