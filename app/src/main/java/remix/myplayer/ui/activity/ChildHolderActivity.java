package remix.myplayer.ui.activity;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.drawee.backends.pipeline.Fresco;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.helper.SortOrder;
import remix.myplayer.misc.handler.MsgHandler;
import remix.myplayer.misc.handler.OnHandleMessage;
import remix.myplayer.misc.interfaces.LoaderIds;
import remix.myplayer.misc.interfaces.OnItemClickListener;
import remix.myplayer.misc.interfaces.OnTagEditListener;
import remix.myplayer.misc.tageditor.TagReceiver;
import remix.myplayer.request.UriRequest;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.adapter.ChildHolderAdapter;
import remix.myplayer.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.ImageUriUtil;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.SPUtil;

import static remix.myplayer.util.Constants.TAG_EDIT;
import static remix.myplayer.util.Util.registerLocalReceiver;
import static remix.myplayer.util.Util.unregisterLocalReceiver;

/**
 * Created by Remix on 2015/12/4.
 */

/**
 * 专辑、艺术家、文件夹、播放列表详情
 */
public class ChildHolderActivity extends LibraryActivity<Song, ChildHolderAdapter>
        implements OnTagEditListener {
    public final static String TAG = ChildHolderActivity.class.getSimpleName();
    public final static String TAG_PLAYLIST_SONG = ChildHolderActivity.class.getSimpleName() + "Song";
    //获得歌曲信息列表的参数
    private int mId;
    private int mType;
    private String mArg;
    private List<Song> mInfoList;
    private TagReceiver mTagEditReceiver;

    //歌曲数目与标题
    @BindView(R.id.childholder_item_num)
    TextView mNum;
    @BindView(R.id.child_holder_recyclerView)
    FastScrollRecyclerView mRecyclerView;
    @BindView(R.id.toolbar)
    Toolbar mToolBar;

    private String Title;
    private MaterialDialog mMDDialog;

    //当前排序
    private String mSortOrder;
    //更新
    private static final int START = 0;
    private static final int END = 1;
    private MsgHandler mRefreshHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_holder);
        ButterKnife.bind(this);

        mRefreshHandler = new MsgHandler(this);
        mTagEditReceiver = new TagReceiver(this);
        registerLocalReceiver(mTagEditReceiver, new IntentFilter(TAG_EDIT));

        //参数id，类型，标题
        mId = getIntent().getIntExtra("Id", -1);
        mType = getIntent().getIntExtra("Type", -1);
        mArg = getIntent().getStringExtra("Title");

        mAdapter = new ChildHolderAdapter(this, R.layout.item_child_holder, mType, mArg, mMultiChoice, mRecyclerView);
        mMultiChoice.setAdapter(mAdapter);
        mMultiChoice.setExtra(mId);
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (position < 0 || mInfoList == null || position >= mInfoList.size())
                    return;
                int songId = mInfoList.get(position).getId();
                if (!mMultiChoice.itemClick(position, songId, mType == Constants.PLAYLISTSONG ? TAG_PLAYLIST_SONG : TAG)) {
                    if (mInfoList != null && mInfoList.size() == 0)
                        return;
                    ArrayList<Integer> idList = new ArrayList<>();
                    for (Song info : mInfoList) {
                        if (info != null && info.getId() > 0)
                            idList.add(info.getId());
                    }
                    //设置正在播放列表
                    Intent intent = new Intent(MusicService.ACTION_CMD);
                    Bundle arg = new Bundle();
                    arg.putInt("Control", Command.PLAYSELECTEDSONG);
                    arg.putInt("Position", position);
                    intent.putExtras(arg);
                    MusicServiceRemote.setPlayQueue(idList, intent);

//                    startActivity(new Intent(mContext,CustomSortActivity.class)
//                            .putExtra("list",new ArrayList<>(mInfoList))
//                            .putExtra("name",mArg)
//                            .putExtra("id",mId);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                mMultiChoice.itemLongClick(position, mInfoList.get(position).getId(), TAG, mType == Constants.PLAYLIST ? Constants.PLAYLISTSONG : Constants.SONG);
            }
        });

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setBubbleTextColor(ThemeStore.isLightTheme()
                ? ColorUtil.getColor(R.color.white)
                : ThemeStore.getTextColorPrimary());

        //标题
        if (mType != Constants.FOLDER) {
            if (mArg.contains("unknown")) {
                if (mType == Constants.ARTIST)
                    Title = getString(R.string.unknown_artist);
                else if (mType == Constants.ALBUM) {
                    Title = getString(R.string.unknown_album);
                }
            } else {
                Title = mArg;
            }
        } else
            Title = mArg.substring(mArg.lastIndexOf("/") + 1, mArg.length());
        //初始化toolbar
        setUpToolbar(mToolBar, Title);

        //加载歌曲列表

        mMDDialog = new MaterialDialog.Builder(this)
                .title(R.string.loading)
                .titleColorAttr(R.attr.text_color_primary)
                .content(R.string.please_wait)
                .contentColorAttr(R.attr.text_color_primary)
                .progress(true, 0)
                .backgroundColorAttr(R.attr.background_color_3)
                .progressIndeterminateStyle(false).build();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (mType == Constants.PLAYLIST) {
            mSortOrder = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.CHILD_PLAYLIST_SONG_SORT_ORDER, SortOrder.PlayListSongSortOrder.SONG_A_Z);
        } else if (mType == Constants.ALBUM) {
            mSortOrder = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.CHILD_ALBUM_SONG_SORT_ORDER, SortOrder.ChildHolderSongSortOrder.SONG_TRACK_NUMBER);
        } else if (mType == Constants.ARTIST) {
            mSortOrder = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.CHILD_ARTIST_SONG_SORT_ORDER, SortOrder.ChildHolderSongSortOrder.SONG_A_Z);
        } else {
            mSortOrder = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.CHILD_FOLDER_SONG_SORT_ORDER, SortOrder.ChildHolderSongSortOrder.SONG_A_Z);
        }
        if (TextUtils.isEmpty(mSortOrder))
            return true;
        setUpMenuItem(menu, mSortOrder);
        return true;
    }

    @Override
    protected void saveSortOrder(String sortOrder) {
        boolean update = false;
        if (mType == Constants.PLAYLIST) {
            //手动排序或者排序发生变化
            if (sortOrder.equalsIgnoreCase(SortOrder.PlayListSongSortOrder.PLAYLIST_SONG_CUSTOM) ||
                    !mSortOrder.equalsIgnoreCase(sortOrder)) {
                //选择的是手动排序
                if (sortOrder.equalsIgnoreCase(SortOrder.PlayListSongSortOrder.PLAYLIST_SONG_CUSTOM)) {
                    startActivity(new Intent(mContext, CustomSortActivity.class)
                            .putExtra("list", new ArrayList<>(mInfoList))
                            .putExtra("id", mId)
                            .putExtra("name", mArg));
                } else {
                    update = true;
                }
            }
        } else {
            //排序发生变化
            if (!mSortOrder.equalsIgnoreCase(sortOrder)) {
                update = true;
            }
        }
        if (mType == Constants.PLAYLIST) {
            SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.CHILD_PLAYLIST_SONG_SORT_ORDER, sortOrder);
        } else if (mType == Constants.ALBUM) {
            SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.CHILD_ALBUM_SONG_SORT_ORDER, sortOrder);
        } else if (mType == Constants.ARTIST) {
            SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.CHILD_ARTIST_SONG_SORT_ORDER, sortOrder);
        } else {
            SPUtil.putValue(mContext, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.CHILD_FOLDER_SONG_SORT_ORDER, sortOrder);
        }
        mSortOrder = sortOrder;
        if (update)
            updateList(true);

    }

    @Override
    protected int getMenuLayoutId() {
        return mType == Constants.PLAYLIST ? R.menu.menu_child_for_playlist :
                mType == Constants.ALBUM ? R.menu.menu_child_for_album :
                        mType == Constants.ARTIST ? R.menu.menu_child_for_artist : R.menu.menu_child_for_folder;
    }

    @Override
    protected int getLoaderId() {
        return LoaderIds.CHILDHOLDER_ACTIVITY;
    }

    @Override
    public void onBackPressed() {
        if (mMultiChoice.isShow()) {
            onMultiBackPress();
        } else {
            finish();
        }
    }

    @Override
    public void onMediaStoreChanged() {
        updateList(true);
    }

    @Override
    public void onServiceConnected(@NotNull MusicService service) {
        super.onServiceConnected(service);
    }

    @Override
    public void onPlayListChanged() {
        updateList(true);
    }

    public void onTagEdit(Song newSong) {
        if (newSong == null)
            return;
        Fresco.getImagePipeline().clearCaches();
        final UriRequest request = ImageUriUtil.getSearchRequestWithAlbumType(newSong);
        SPUtil.deleteValue(mContext, SPUtil.COVER_KEY.NAME, request.getLastFMKey());
        SPUtil.deleteValue(mContext, SPUtil.COVER_KEY.NAME, request.getNeteaseCacheKey());
        if (mType == Constants.ARTIST || mType == Constants.ALBUM) {
            mId = mType == Constants.ARTIST ? newSong.getArtistId() : newSong.getAlbumId();
            Title = mType == Constants.ARTIST ? newSong.getArtist() : newSong.getAlbum();
            mToolBar.setTitle(Title);
            if (mIsForeground)
                updateList(true);
        }
    }

    /**
     * 根据参数(专辑id 歌手id 文件夹名 播放列表名)获得对应的歌曲信息列表
     *
     * @return 对应歌曲信息列表
     */
    private List<Song> getMP3List() {
        if (mId < 0)
            return null;
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
                mInfoList = MediaStoreUtil.getMP3ListByFolderName(mArg);
                break;
            //播放列表名
            case Constants.PLAYLIST:
                /* 播放列表歌曲id列表 */
                List<Integer> playListSongIDList = PlayListUtil.getIDList(mId);
                if (playListSongIDList == null)
                    return mInfoList;
                mInfoList = PlayListUtil.getMP3ListByIds(playListSongIDList, mId);
                break;
        }
        return mInfoList;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mMultiChoice.isShow()) {
            mRefreshHandler.sendEmptyMessageDelayed(Constants.CLEAR_MULTI, 500);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateList(true);
    }

    private void updateList(boolean reset) {
        if (mIsForeground) {
            if (mHasPermission) {
                new GetSongThread(reset).start();
            } else {
                mInfoList = null;
                mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRefreshHandler.remove();
        unregisterLocalReceiver(mTagEditReceiver);
    }

    @OnHandleMessage
    public void handleInternal(Message msg) {
        switch (msg.what) {
            case Constants.CLEAR_MULTI:
                mAdapter.notifyDataSetChanged();
                break;
            case Constants.UPDATE_ADAPTER:
                mAdapter.setData(mInfoList);
                mNum.setText(getString(R.string.song_count, mInfoList.size()));
                break;
            case START:
                if (mMDDialog != null && !mMDDialog.isShowing()) {
                    mMDDialog.show();
                }
                break;
            case END:
                if (mMDDialog != null && mMDDialog.isShowing()) {
                    mMDDialog.dismiss();
                }
                break;
        }
    }

    private class GetSongThread extends Thread {
        //是否需要重新查询歌曲列表
        private boolean mNeedReset;

        GetSongThread(boolean needReset) {
            this.mNeedReset = needReset;
        }

        @Override
        public void run() {
            mRefreshHandler.sendEmptyMessage(START);
            if (mNeedReset)
                mInfoList = getMP3List();
            mRefreshHandler.sendEmptyMessage(END);
            mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
        }
    }

}