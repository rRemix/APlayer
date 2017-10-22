package remix.myplayer.ui.activity;

import android.content.Context;
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
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.SongAdapter;
import remix.myplayer.asynctask.AppWrappedAsyncTaskLoader;
import remix.myplayer.helper.MusicEventHelper;
import remix.myplayer.helper.UpdateHelper;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.model.mp3.Song;
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
public class RecetenlyActivity extends PermissActivity<Song,SongAdapter> implements UpdateHelper.Callback{
    public static final String TAG = RecetenlyActivity.class.getSimpleName();
    private static int LOADER_ID = 0;

    @BindView(R.id.recyclerview)
    FastScrollRecyclerView mRecyclerView;
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

        mAdapter = new SongAdapter(this, R.layout.item_song_recycle,mMultiChoice,SongAdapter.RECENTLY);
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
                    Global.setPlayQueue(mIdList,mContext,intent);
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

        setUpToolbar(mToolBar,getString(R.string.recently));
    }

    /**
     * 获得歌曲id
     * @param position
     * @return
     */
    private int getSongId(int position){
        int id = -1;
        if(mAdapter.getDatas() != null && mAdapter.getDatas().size() > position - 1){
            id = mAdapter.getDatas().get(position).getId();
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
    public void UpdateUI(Song Song, boolean isplay) {
//        mAdapter.notifyDataSetChanged();
    }


    @Override
    public void onLoadFinished(android.content.Loader<List<Song>> loader, List<Song> data) {
        super.onLoadFinished(loader, data);
        if(data != null){
            mIdList = new ArrayList<>();
            for(Song song : data){
                mIdList.add(song.getId());
            }
        }

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
    public void onMediaStoreChanged() {
        if(mHasPermission)
            getLoaderManager().initLoader(++LOADER_ID, null, this);
        else{
            if(mAdapter != null)
                mAdapter.setDatas(null);
        }
    }

    @Override
    protected android.content.Loader<List<Song>> getLoader() {
        return new AsyncRecentlySongLoader(this);
    }

    private static class AsyncRecentlySongLoader extends AppWrappedAsyncTaskLoader<List<Song>> {
        private AsyncRecentlySongLoader(Context context) {
            super(context);
        }

        @Override
        public List<Song> loadInBackground() {
            Cursor cursor = null;
            List<Song> songs = new ArrayList<>();
            try {
                Calendar today = Calendar.getInstance();
                today.setTime(new Date());
                cursor = getContext().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        null,
                        MediaStore.Audio.Media.DATE_ADDED + " >= " + (today.getTimeInMillis() / 1000 - (3600 * 24 * 7)) +
                                " and " + MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + MediaStoreUtil.getBaseSelection(),
                        null,
                        MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
                if(cursor != null) {
                    while (cursor.moveToNext()) {
                        songs.add(MediaStoreUtil.getMP3Info(cursor));
                    }
                }
            }finally {
                if(cursor != null && !cursor.isClosed())
                    cursor.close();
            }
            return songs;
        }
    }
}
