package remix.myplayer.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.adapters.ChildHolderAdapter;
import remix.myplayer.fragments.BottomActionBarFragment;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.infos.PlayListItem;
import remix.myplayer.services.MusicService;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;

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
    private ArrayList<MP3Info> mInfoList;
    private ListView mListView;
    //歌曲数目与标题
    private TextView mNum;
    private TextView mTitle;
    private BottomActionBarFragment mActionbar;
    private ChildHolderAdapter mAdapter;
    public static ChildHolderActivity mInstance = null;
    //是否需要更新adapter
    private static boolean mNeedRefresh = false;
    //更新ListView
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(mInfoList == null || mInfoList.size() == 0)
                return;
//            mAdapter = new ChildHolderAdapter(mInfoList, getLayoutInflater(),ChildHolderActivity.this);
//            mAdapter.registerDataSetObserver(new DataSetObserver() {
//                @Override
//                public void onChanged() {
//                    super.onChanged();
//                }
//
//                @Override
//                public void onInvalidated() {
//                    super.onInvalidated();
//                }
//            });
//            mListView.setAdapter(mAdapter);
            mAdapter.setList(mInfoList);
            mNum.setText(mInfoList.size() + "首歌曲");

        }
    };
    //是否从文件夹打开
    public static boolean mIsFromFolder = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mInstance = this;
        setContentView(R.layout.activity_child_holder);
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

        mListView = (ListView)findViewById(R.id.child_holder_list);
        mAdapter = new ChildHolderAdapter(mInfoList, getLayoutInflater(),ChildHolderActivity.this);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mInfoList != null && mInfoList.size() == 0)
                    return;
                ArrayList<Long> ids = new ArrayList<Long>();
                for (MP3Info info : mInfoList) {
                    if(info != null && info.getId() > 0)
                        ids.add(info.getId());
                }
                //设置正在播放列表
                DBUtil.setPlayingList((ArrayList) ids.clone());

                Intent intent = new Intent(Constants.CTL_ACTION);
                Bundle arg = new Bundle();
                arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                arg.putInt("Position", position);
                intent.putExtras(arg);
                sendBroadcast(intent);
            }
        });
        //歌曲数目与标题
        mNum = (TextView)findViewById(R.id.album_holder_item_num);
        mTitle = (TextView)findViewById(R.id.artist_album_title);
        if(mType != Constants.FOLDER_HOLDER) {
            if(mArg.indexOf("unknown") > 0){
                if(mType == Constants.ARTIST_HOLDER)
                    mTitle.setText("未知艺术家");
                else if(mType == Constants.ALBUM_HOLDER){
                    mTitle.setText("未知专辑");
                }
            } else {
                mTitle.setText(mArg);
            }
        } else
            mTitle.setText(mArg.substring(mArg.lastIndexOf("/") + 1,mArg.length()));
        //初始化底部状态栏
        mActionbar = (BottomActionBarFragment) getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);
        if(DBUtil.mPlayingList == null || DBUtil.mPlayingList.size() == 0)
            return;

        mActionbar.UpdateBottomStatus(MusicService.getCurrentMP3(), MusicService.getIsplay());
    }

    public void UpdateData(){
//        if(!mIsRunning)
//            return;
        if(mType == Constants.PLAYLIST_HOLDER){
            //播放列表
            if(!PlayListActivity.getPlayList().containsKey(mArg)){
                mAdapter.setList(new ArrayList<MP3Info>());
                mNum.setText("0首歌曲");
            } else {
                new UpdateThread().start();
            }
        } else if(mType == Constants.FOLDER_HOLDER){
            //文件夹
            mArg = getIntent().getStringExtra("Title");
            if(!DBUtil.mFolderMap.containsKey(mArg)){
                mAdapter.setList(new ArrayList<MP3Info>());
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
    private ArrayList<MP3Info> getMP3List(){
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
                mIsFromFolder = true;
                mInfoList = DBUtil.getMP3ListByIds(DBUtil.mFolderMap.get(mArg));
                mArg = mArg.substring(mArg.lastIndexOf("/") + 1,mArg.length());
                break;
            //播放列表名
            case Constants.PLAYLIST_HOLDER:
                ArrayList<PlayListItem> list = PlayListActivity.getPlayList().get(mArg);
                ArrayList<Long> ids = new ArrayList<>();
                if(list == null)
                    break;
                for(PlayListItem item : list)
                    ids.add(Long.valueOf(item.getId() + ""));
                mInfoList = DBUtil.getMP3ListByIds(ids);
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
        for (MP3Info info : mInfoList)
            ids.add(info.getId());
        DBUtil.setPlayingList((ArrayList) ids.clone());
        sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //更新界面
    @Override
    public void UpdateUI(MP3Info MP3info, boolean isplay) {
        //底部状态兰
        mActionbar.UpdateBottomStatus(MP3info, isplay);
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
        if(mNeedRefresh){
            UpdateData();
            mNeedRefresh = false;
        }
        mIsRunning = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsRunning = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }



}
