package remix.myplayer.ui.activity;

import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.adapter.ChildHolderAdapter;
import remix.myplayer.fragment.BottomActionBarFragment;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by Remix on 2015/12/4.
 */

/**
 * 专辑、艺术家、文件夹、播放列表详情
 */
public class ChildHolderActivity extends MultiChoiceActivity implements MusicService.Callback,LoaderManager.LoaderCallbacks<Cursor>{
    public final static String TAG = ChildHolderActivity.class.getSimpleName();
    public final static String TAG_PLAYLIST_SONG = ChildHolderActivity.class.getSimpleName() + "Song";
    private boolean mIsRunning = false;
    private static int LOADER_ID = 1;
    //获得歌曲信息列表的参数
    public static int mId;
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
    private BottomActionBarFragment mBottombar;
    private Cursor mCursor;

    private ChildHolderAdapter mAdapter;

    private static ChildHolderActivity mInstance = null;

    private Handler mRefreshHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case Constants.CLEAR_MULTI:
                    mMultiChoice.clearSelectedViews();
                    break;
            }
        }
    };
    private ArrayList<Integer> mIdList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_holder);
        ButterKnife.bind(this);

        mInstance = this;
        MusicService.addCallback(ChildHolderActivity.this);

        //参数，类型，标题
        mId = getIntent().getIntExtra("Id", -1);
        mType = getIntent().getIntExtra("Type", -1);
        mArg = getIntent().getStringExtra("Title");

        findView(R.id.divider).setVisibility(ThemeStore.isDay() ? View.VISIBLE : View.GONE);

        mAdapter = new ChildHolderAdapter(this,mType,mArg,mMultiChoice);
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                int songid = getSongId(mCursor,position);
                if( !mMultiChoice.itemAddorRemoveWithClick(view,position,songid,mType == Constants.PLAYLISTSONG ? TAG_PLAYLIST_SONG : TAG)){
                    //设置正在播放列表
                    Intent intent = new Intent(Constants.CTL_ACTION);
                    Bundle arg = new Bundle();
                    arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                    arg.putInt("Position", position);
                    intent.putExtras(arg);
                    Global.setPlayQueue(mIdList,ChildHolderActivity.this,intent);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                int songid = mIdList.get(position);
                mMultiChoice.itemAddorRemoveWithLongClick(view,position,songid, TAG,mType == Constants.PLAYLIST ? Constants.PLAYLISTSONG : Constants.SONG);
            }
        });
        mRecyclerView.setAdapter(mAdapter);
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
        setUpToolbar(mToolBar,Title);

        getLoaderManager().initLoader(++LOADER_ID, null, this);
        //初始化底部状态栏
        mBottombar = (BottomActionBarFragment) getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);
        if(Global.mPlayQueue == null || Global.mPlayQueue.size() == 0)
            return;

        mBottombar.UpdateBottomStatus(MusicService.getCurrentMP3(), MusicService.getIsplay());

    }

    public void updateCursor() {
        if(mIsRunning)
            getLoaderManager().restartLoader(LOADER_ID, null, this);
    }

    @Override
    public void onBackPressed() {
        if(mMultiChoice.isShow()) {
            onBackPress();
        } else {
            finish();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        StringBuilder where = new StringBuilder();
        switch (mType) {
            //专辑id
            case Constants.ALBUM:
                where.append(MediaStore.Audio.Media.ALBUM_ID).append("=").append(mId).append(" and ");
                break;
            //歌手id
            case Constants.ARTIST:
                where.append(MediaStore.Audio.Media.ARTIST_ID).append("=").append(mId).append(" and ");
                break;
            //文件夹名
            case Constants.FOLDER:
            //播放列表名
            case Constants.PLAYLIST:
                if(mType == Constants.FOLDER){
                    mIdList = Global.mFolderMap.get(mArg);
                } else {
                    mIdList = PlayListUtil.getIDList(mId);
                }
                if(mIdList == null || mIdList.size() == 0)
                    return null;
                for(int i = 0 ; i < mIdList.size() ;i++){
                    where.append(MediaStore.Audio.Media._ID).append("=").append(mIdList.get(i)).append(i != mIdList.size() - 1 ? " or " : " and ");
                }
        }
        return new android.content.CursorLoader(this,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DISPLAY_NAME,MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.ALBUM,MediaStore.Audio.Media.ALBUM_ID,MediaStore.Audio.Media.ARTIST},
                where.toString() +  MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + MediaStoreUtil.getDeleteID(),
                null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data == null)
            return;
        //查询完毕后保存结果，并设置查询索引
        mCursor = data;
        //如果是非播放列表，需要重新设置idlist,因为外部删除歌曲会导致次级目录歌曲的数量变化
        if(mType != Constants.PLAYLIST){
            mIdList = MediaStoreUtil.getSongIdListByCursor(mCursor);
        }
        mAdapter.setIDList(mIdList);
        mAdapter.setCursor(mCursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mAdapter != null) {
            mAdapter.setCursor(null);
            mAdapter.setIDList(null);
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
                /* 播放列表歌曲id列表 */
                ArrayList<Integer> mPlayListSongIDList = PlayListUtil.getIDList(mId);
                mInfoList = PlayListUtil.getMP3ListByIds(mPlayListSongIDList);
                break;
        }
        return mInfoList;
    }

    @OnClick(R.id.shuffle_container)
    public void onClick(View v){
//        MusicService.setPlayModel(Constants.PLAY_SHUFFLE);
//        Intent intent = new Intent(Constants.CTL_ACTION);
//        intent.putExtra("Control", Constants.NEXT);
//        //设置正在播放列表
//        ArrayList<Integer> ids = new ArrayList<>();
//        for (MP3Item info : mInfoList)
//            ids.add(info.getId());
//        Global.setPlayQueue(ids,this,intent);
        if(mIdList == null || mIdList.size() == 0){
            ToastUtil.show(this,R.string.no_song);
            return;
        }
        MusicService.setPlayModel(Constants.PLAY_SHUFFLE);
        Intent intent = new Intent(Constants.CTL_ACTION);
        intent.putExtra("Control", Constants.NEXT);
        Global.setPlayQueue(mIdList,this,intent);
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
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsRunning = false;
    }

    public static ChildHolderActivity getInstance() {
        return mInstance;
    }

}
