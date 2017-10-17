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
import android.view.View;

import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.SongAdapter;
import remix.myplayer.helper.MusicEventHelper;
import remix.myplayer.helper.UpdateHelper;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.model.mp3.MP3Item;
import remix.myplayer.ui.customview.fastcroll_recyclerview.FastScrollRecyclerView;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;
import remix.myplayer.util.MediaStoreUtil;

/**
 * Created by taeja on 16-3-4.
 */

/**
 * 最近添加歌曲的界面
 * 目前为最近7天添加
 */
public class RecetenlyActivity extends MultiChoiceActivity implements UpdateHelper.Callback,
        MusicEventHelper.MusicEventCallback,
        LoaderManager.LoaderCallbacks<Cursor>{
    public static final String TAG = RecetenlyActivity.class.getSimpleName();
    private static int LOADER_ID = 0;

    private SongAdapter mAdapter;
    @BindView(R.id.recyclerview)
    FastScrollRecyclerView mRecyclerView;
    private Cursor mCursor;
    private ArrayList<Integer> mIdList = new ArrayList<>();

    private Handler mRefreshHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
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

        mAdapter = new SongAdapter(this, mCursor,mMultiChoice,SongAdapter.RECENTLY);
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
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                int id = getSongId(position);
                if(id > 0)
                    mMultiChoice.itemAddorRemoveWithLongClick(view,position,id,TAG,Constants.SONG);
            }
        });

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);

        MusicEventHelper.addCallback(this);
        getLoaderManager().initLoader(++LOADER_ID, null, this);
        setUpToolbar(mToolBar,getString(R.string.recently));

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

    @Override
    public void UpdateUI(MP3Item MP3Item, boolean isplay) {
//        mAdapter.notifyDataSetChanged();
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
                MediaStore.Audio.Media.DATE_ADDED + " >= " + (today.getTimeInMillis() / 1000 - (3600 * 24 * 7)) +
                        " and " + MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + MediaStoreUtil.getBaseSelection(),
                null,
                MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoadFinished(android.content.Loader<Cursor> loader, Cursor data) {
        if(loader.getId() != ++LOADER_ID || data == null)
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
        MusicEventHelper.removeCallback(this);
    }

    @Override
    public void onMediaStoreChanged() {
        getLoaderManager().initLoader(++LOADER_ID, null, this);
    }
}
