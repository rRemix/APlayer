package remix.myplayer.ui.activity;

import android.app.LoaderManager;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RelativeLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.SongAdapter;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by taeja on 16-3-4.
 */

/**
 * 最近添加歌曲的界面
 * 目前为最近7天添加
 */
public class RecetenlyActivity extends MultiChoiceActivity implements MusicService.Callback,LoaderManager.LoaderCallbacks<Cursor>{
    public static final String TAG = RecetenlyActivity.class.getSimpleName();
    private static int LOADER_ID = 1;

    private ArrayList<MP3Item> mInfoList;
    private SongAdapter mAdapter;
    @BindView(R.id.recently_shuffle)
    RelativeLayout mShuffle;
    @BindView(R.id.recyclerview)
    RecyclerView mRecyclerView;
    private Cursor mCursor;
    private ArrayList<Integer> mIdList = new ArrayList<>();

    private MaterialDialog mMDDialog;

    private static final int START = 0;
    private static final int END = 1;
    private Handler mRefreshHandler = new Handler(){
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
                    break;
                case Constants.CLEAR_MULTI:
                    mMultiChoice.clearSelectedViews();
                    break;
                case Constants.UPDATE_ADAPTER:
                    if(mAdapter != null)
                        mAdapter.notifyDataSetChanged();
            }

        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        MobclickAgent.onEvent(this,"RecentlyAdd");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recently);
        ButterKnife.bind(this);
        MusicService.addCallback(RecetenlyActivity.this);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        mAdapter = new SongAdapter(RecetenlyActivity.this, mMultiChoice,SongAdapter.RECENTLY);
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                int id = getSongId(position);
                if(id > 0 && !mMultiChoice.itemAddorRemoveWithClick(view,position,id,TAG)){
                    Intent intent = new Intent(Constants.CTL_ACTION);
                    Bundle arg = new Bundle();
                    arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                    arg.putInt("Position", position);
                    intent.putExtras(arg);
                    Global.setPlayQueue(mIdList,RecetenlyActivity.this,intent);
//                    sendBroadcast(intent);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                int id = getSongId(position);
                if(id > 0)
                    mMultiChoice.itemAddorRemoveWithLongClick(view,position,id,TAG,Constants.SONG);
            }
        });
        mRecyclerView.setAdapter(mAdapter);
        getLoaderManager().initLoader(LOADER_ID++, null, this);

        mMDDialog = new MaterialDialog.Builder(this)
                .title("加载中")
                .titleColorAttr(R.attr.text_color_primary)
                .content("请稍等")
                .contentColorAttr(R.attr.text_color_primary)
                .progress(true, 0)
                .backgroundColorAttr(R.attr.background_color_3)
                .progressIndeterminateStyle(false).build();

        initToolbar(mToolBar,getString(R.string.recently));

    }

    /**
     * 获得歌曲id
     * @param position
     * @return
     */
    private int getSongId(int position){
        int id = -1;
        if(mCursor != null && !mCursor.isClosed() && mCursor.moveToPosition(position)){
            id = mCursor.getInt(mCursor.getColumnIndex(MediaStore.Audio.Media._ID));
        }
        return id;
    }


    @Override
    public void onBackPressed() {
        if(mMultiChoice.isShow()) {
            onBackPress();
        } else {
            finish();
        }
    }

    //随机播放
    public void onPlayShuffle(View v){
        if(mIdList == null || mIdList.size() == 0){
            ToastUtil.show(RecetenlyActivity.this,R.string.no_song);
            return;
        }
        MusicService.setPlayModel(Constants.PLAY_SHUFFLE);
        Intent intent = new Intent(Constants.CTL_ACTION);
        intent.putExtra("Control", Constants.NEXT);
        Global.setPlayQueue(mIdList,this,intent);
    }


    @Override
    public void UpdateUI(MP3Item MP3Item, boolean isplay) {
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public int getType() {
        return Constants.RECENTLYACTIVITY;
    }


    @Override
    public android.content.Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //获得今天日期
        Calendar today = Calendar.getInstance();
        today.setTime(new Date());
        //最近七天

        return new android.content.CursorLoader(this,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID,MediaStore.Audio.Media.DISPLAY_NAME,MediaStore.Audio.Media.TITLE,
                                MediaStore.Audio.Media.ALBUM,MediaStore.Audio.Media.ALBUM_ID,MediaStore.Audio.Media.ARTIST},
                MediaStore.Audio.Media.DATE_ADDED + " >= " + (today.getTimeInMillis() / 1000 - (3600 * 24 * 7)) +  " and " + Constants.MEDIASTORE_WHERE_SIZE,
                null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(android.content.Loader<Cursor> loader, Cursor data) {
        if(data == null)
            return;
        //查询完毕后保存结果，并设置查询索引
        mCursor = data;
        mIdList = MediaStoreUtil.getSongIdListByCursor(mCursor);
        mAdapter.setCursor(mCursor);
    }

    @Override
    public void onLoaderReset(android.content.Loader<Cursor> loader) {
        if (mAdapter != null)
            mAdapter.setCursor(null);
    }

    @Override
    protected void onResume() {
        MobclickAgent.onPageStart(RecetenlyActivity.class.getSimpleName());
        super.onResume();
        if(mMultiChoice.isShow()){
            mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
        }
    }

    @Override
    protected void onPause() {
        MobclickAgent.onPageEnd(RecetenlyActivity.class.getSimpleName());
        super.onPause();
        if(mMultiChoice.isShow()){
            mRefreshHandler.sendEmptyMessageDelayed(Constants.CLEAR_MULTI,500);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mCursor != null) {
            mCursor.close();
            mCursor = null;
        }
    }

}
