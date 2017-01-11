package remix.myplayer.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.adapter.ChildHolderAdapter;
import remix.myplayer.fragment.BottomActionBarFragment;
import remix.myplayer.helper.UpdateHelper;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.SPUtil;

/**
 * Created by Remix on 2015/12/4.
 */

/**
 * 专辑、艺术家、文件夹、播放列表详情
 */
public class ChildHolderActivity extends MultiChoiceActivity implements UpdateHelper.Callback{
    public final static String TAG = ChildHolderActivity.class.getSimpleName();
    public final static String TAG_PLAYLIST_SONG = ChildHolderActivity.class.getSimpleName() + "Song";
    private boolean mIsRunning = false;
    //获得歌曲信息列表的参数
    public static int mId;
    private int mType;
    private String mArg;
    private ArrayList<MP3Item> mInfoList;

    @BindView(R.id.sort)
    TextView mSort;
    @BindView(R.id.asc_desc)
    TextView mAscDesc;
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
    private static ChildHolderActivity mInstance = null;
    private MaterialDialog mMDDialog;

    //当前是升序还是降序 0:升序 1:降序
    private int ASC_DESC = 0;
    private final int ASC = 0;
    private final int DESC = 1;
    //当前是按字母排序还是添加时间 0:字母 1:时间
    private int SORT = 0;
    private final int NAME = 0;
    private final int ADDTIME = 1;

    //更新
    private static final int START = 0;
    private static final int END = 1;
    private Handler mRefreshHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case Constants.CLEAR_MULTI:
                    mMultiChoice.clearSelectedViews();
                    break;
                case Constants.UPDATE_ADAPTER:
                    if(mInfoList == null)
                        return;
                    mAdapter.setList(mInfoList);
                    mNum.setText(mInfoList.size() + "首歌曲");
                    break;
                case START:
                    if(mMDDialog != null && !mMDDialog.isShowing()){
                        mMDDialog.show();
                    }
                    break;
                case END:
                    if(mMDDialog != null && mMDDialog.isShowing()){
                        findViewById(R.id.shuffle_container).setVisibility(mInfoList != null && mInfoList.size() > 0 ? View.VISIBLE : View.GONE);
                        mMDDialog.dismiss();
                    }
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_holder);
        ButterKnife.bind(this);

        mInstance = this;

        //参数id，类型，标题
        mId = getIntent().getIntExtra("Id", -1);
        mType = getIntent().getIntExtra("Type", -1);
        mArg = getIntent().getStringExtra("Title");

        mAdapter = new ChildHolderAdapter(this,mType,mArg,mMultiChoice);
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if(position < 0 || mInfoList == null || position >= mInfoList.size())
                    return;
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
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                int songid = mInfoList.get(position).getId();
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

        //加载歌曲列表
        mMDDialog = new MaterialDialog.Builder(this)
                .title("加载中")
                .titleColorAttr(R.attr.text_color_primary)
                .content("请稍等")
                .contentColorAttr(R.attr.text_color_primary)
                .progress(true, 0)
                .backgroundColorAttr(R.attr.background_color_3)
                .progressIndeterminateStyle(false).build();
        //读取之前的排序方式
        SORT = SPUtil.getValue(this,"Setting","SubDirSort",NAME);
        ASC_DESC = SPUtil.getValue(this,"Setting","SubDirAscDesc",ASC);
        mAscDesc.setText(ASC_DESC == ASC ? "升序" : "降序");
        mSort.setText(SORT == NAME ?  "按字母" : "按添加时间");
        //读取歌曲列表
        new GetSongList().start();
        //初始化底部状态栏
        mBottombar = (BottomActionBarFragment) getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);
        if(Global.PlayQueue == null || Global.PlayQueue.size() == 0)
            return;
        mBottombar.UpdateBottomStatus(MusicService.getCurrentMP3(), MusicService.getIsplay());

    }

    @Override
    public void onBackPressed() {
        if(mMultiChoice.isShow()) {
            onBackPress();
        } else {
            finish();
        }
    }

    class GetSongList extends Thread{
        //是否需要重新查询歌曲列表
        private boolean mNeedReset = true;
        GetSongList(boolean needReset) {
            this.mNeedReset = needReset;
        }
        GetSongList(){}

        @Override
        public void run() {
            mRefreshHandler.sendEmptyMessage(START);
            if(mNeedReset)
                mInfoList = getMP3List();
            sortList();
            mRefreshHandler.sendEmptyMessage(END);
            mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
        }
    }

    /**
     * 根据条件排序
     */
    private void sortList(){
        Collections.sort(mInfoList, new Comparator<MP3Item>() {
            @Override
            public int compare(MP3Item o1, MP3Item o2) {
                if(o1 == null || o2 == null)
                    return 0;
                //当前是按名字排序
                if(SORT == NAME){
                    if(TextUtils.isEmpty(o1.getTitleKey()) || TextUtils.isEmpty(o2.getTitleKey()))
                        return 0;
                    if(ASC_DESC == ASC){
                        return o1.getTitleKey().compareTo(o2.getTitleKey());
                    } else {
                        return o2.getTitleKey().compareTo(o1.getTitleKey());
                    }
                } else if(SORT == ADDTIME){
                    //当前是按添加时间排序
                    if(o1.getAddTime() == o2.getAddTime()){
                        return 0;
                    }
                    if(ASC_DESC == ASC){
                        return  o1.getAddTime() > o2.getAddTime() ? 1 : -1;
                    } else {
                        return  o2.getAddTime() > o1.getAddTime() ? 1 : -1;
                    }
                } else {
                    return 0;
                }
            }
        });
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
                mInfoList = MediaStoreUtil.getMP3ListByIds(Global.FolderMap.get(mArg));
                break;
            //播放列表名
            case Constants.PLAYLIST:
                /* 播放列表歌曲id列表 */
                ArrayList<Integer> playListSongIDList = PlayListUtil.getIDList(mId);
                if(playListSongIDList == null)
                    return mInfoList;
                mInfoList = PlayListUtil.getMP3ListByIds(playListSongIDList);
                break;
        }
        return mInfoList;
    }

    @OnClick({R.id.play_shuffle,R.id.sort,R.id.asc_desc})
    public void onClick(View v){
        switch (v.getId()){
            case R.id.play_shuffle:
                MusicService.setPlayModel(Constants.PLAY_SHUFFLE);
                Intent intent = new Intent(Constants.CTL_ACTION);
                intent.putExtra("Control", Constants.NEXT);
                intent.putExtra("shuffle",true);
                //设置正在播放列表
                ArrayList<Integer> ids = new ArrayList<>();
                for (MP3Item info : mInfoList)
                    ids.add(info.getId());
                Global.setPlayQueue(ids,this,intent);
                break;
            case R.id.sort:
                if(SORT == NAME){
                    SORT = ADDTIME;
                } else if(SORT == ADDTIME){
                    SORT = NAME;
                }
                SPUtil.putValue(this,"Setting","SubDirSort",SORT);
                mSort.setText(SORT == NAME ?  "按字母" : "按添加时间");
                new GetSongList(false).start();
                break;
            case R.id.asc_desc:
                if(ASC_DESC == ASC){
                    ASC_DESC = DESC;
                } else if(ASC_DESC == DESC){
                    ASC_DESC = ASC;
                }
                SPUtil.putValue(this,"Setting","SubDirAscDesc",ASC_DESC);
                mAscDesc.setText(ASC_DESC == ASC ? "升序" : "降序");
                new GetSongList(false).start();
                break;
        }

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

    public void updateList() {
        if(mIsRunning)
            new GetSongList().start();
    }

    public static ChildHolderActivity getInstance(){
        return mInstance;
    }

}