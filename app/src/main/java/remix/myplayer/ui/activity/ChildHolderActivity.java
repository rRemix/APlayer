package remix.myplayer.ui.activity;

import static remix.myplayer.helper.MusicServiceRemote.setPlayQueue;
import static remix.myplayer.service.MusicService.EXTRA_POSITION;
import static remix.myplayer.util.MusicUtil.makeCmdIntent;

import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.SingleSource;
import io.reactivex.functions.Function;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.db.room.DatabaseRepository;
import remix.myplayer.db.room.model.PlayList;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.helper.SortOrder;
import remix.myplayer.misc.asynctask.AppWrappedAsyncTaskLoader;
import remix.myplayer.misc.handler.MsgHandler;
import remix.myplayer.misc.handler.OnHandleMessage;
import remix.myplayer.misc.interfaces.LoaderIds;
import remix.myplayer.misc.interfaces.OnItemClickListener;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.adapter.ChildHolderAdapter;
import remix.myplayer.ui.fragment.BottomActionBarFragment;
import remix.myplayer.ui.misc.MultipleChoice;
import remix.myplayer.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.SPUtil.SETTING_KEY;
import remix.myplayer.util.ToastUtil;

/**
 * Created by Remix on 2015/12/4.
 */

/**
 * 专辑、艺术家、文件夹、播放列表详情
 */
public class ChildHolderActivity extends LibraryActivity<Song, ChildHolderAdapter> {

  public final static String TAG = ChildHolderActivity.class.getSimpleName();
  //获得歌曲信息列表的参数
  private long mId;
  private int mType;
  private String mArg;

  //歌曲数目与标题
  @BindView(R.id.childholder_item_num)
  TextView mNum;
  @BindView(R.id.child_holder_recyclerView)
  FastScrollRecyclerView mRecyclerView;
  @BindView(R.id.toolbar)
  Toolbar mToolBar;

  private String Title;
//    private MaterialDialog mMDDialog;

  //当前排序
  private String mSortOrder;
  private MsgHandler mRefreshHandler;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_child_holder);
    ButterKnife.bind(this);

    mRefreshHandler = new MsgHandler(this);

    //参数id，类型，标题
    mId = getIntent().getLongExtra(EXTRA_ID, -1);
    mType = getIntent().getIntExtra(EXTRA_TYPE, -1);
    mArg = getIntent().getStringExtra(EXTRA_TITLE);
    if (mId == -1 || mType == -1 || TextUtils.isEmpty(mArg)) {
      ToastUtil.show(this, R.string.illegal_arg);
      finish();
      return;
    }

    mChoice = new MultipleChoice<>(this,
        mType == Constants.PLAYLIST ? Constants.PLAYLISTSONG : Constants.SONG);

    mAdapter = new ChildHolderAdapter(R.layout.item_song_recycle, mType, mArg, mChoice,
        mRecyclerView);
    mChoice.setAdapter(mAdapter);
    mChoice.setExtra(mId);
    mAdapter.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(View view, int position) {
        final Song song = mAdapter.getDatas().get(position);

        if (MusicServiceRemote.isPlaying() && song.equals(MusicServiceRemote.getCurrentSong())) {
          final BottomActionBarFragment bottomActionBarFragment = (BottomActionBarFragment) getSupportFragmentManager()
              .findFragmentByTag("BottomActionBarFragment");
          if (bottomActionBarFragment != null) {
            bottomActionBarFragment.startPlayerActivity();
          }
        } else {
          if (!mChoice.click(position, song)) {
            final List<Song> songs = mAdapter.getDatas();
            if (songs.size() == 0) {
              return;
            }
            //设置正在播放列表
            setPlayQueue(songs, makeCmdIntent(Command.PLAYSELECTEDSONG)
                .putExtra(EXTRA_POSITION, position));
          }
        }
      }

      @Override
      public void onItemLongClick(View view, int position) {
        mChoice.longClick(position, mAdapter.getDatas().get(position));
      }
    });

    mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    mRecyclerView.setAdapter(mAdapter);
    final int accentColor = ThemeStore.getAccentColor();
    mRecyclerView.setBubbleColor(accentColor);
    mRecyclerView.setHandleColor(accentColor);
    mRecyclerView.setBubbleTextColor(ColorUtil.getColor(ColorUtil.isColorLight(accentColor) ?
        R.color.dark_text_color_primary : R.color.light_text_color_primary));

    //标题
    if (mType != Constants.FOLDER) {
      if (mArg.contains("unknown")) {
        if (mType == Constants.ARTIST) {
          Title = getString(R.string.unknown_artist);
        } else if (mType == Constants.ALBUM) {
          Title = getString(R.string.unknown_album);
        }
      } else {
        Title = mArg;
      }
    } else {
      Title = mArg.substring(mArg.lastIndexOf("/") + 1, mArg.length());
    }
    //初始化toolbar
    setUpToolbar(Title);

//        mMDDialog = new MaterialDialog.Builder(this)
//                .title(R.string.loading)
//                .titleColorAttr(R.attr.text_color_primary)
//                .content(R.string.please_wait)
//                .contentColorAttr(R.attr.text_color_primary)
//                .progress(true, 0)
//                .backgroundColorAttr(R.attr.background_color_3)
//                .progressIndeterminateStyle(false).build();
  }

  @Override
  public boolean onCreateOptionsMenu(@NonNull Menu menu) {
    super.onCreateOptionsMenu(menu);
    if (mType == Constants.PLAYLIST) {
      mSortOrder = SPUtil
          .getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.CHILD_PLAYLIST_SONG_SORT_ORDER,
              SortOrder.PlayListSongSortOrder.SONG_A_Z);
    } else if (mType == Constants.ALBUM) {
      mSortOrder = SPUtil
          .getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.CHILD_ALBUM_SONG_SORT_ORDER,
              SortOrder.ChildHolderSongSortOrder.SONG_TRACK_NUMBER);
    } else if (mType == Constants.ARTIST) {
      mSortOrder = SPUtil
          .getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.CHILD_ARTIST_SONG_SORT_ORDER,
              SortOrder.ChildHolderSongSortOrder.SONG_A_Z);
    } else {
      mSortOrder = SPUtil
          .getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.CHILD_FOLDER_SONG_SORT_ORDER,
              SortOrder.ChildHolderSongSortOrder.SONG_A_Z);
    }
    if (TextUtils.isEmpty(mSortOrder)) {
      return true;
    }
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
          CustomSortActivity.start(mContext, mId, mArg, new ArrayList<>(mAdapter.getDatas()));
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
      SPUtil.putValue(mContext, SETTING_KEY.NAME, SETTING_KEY.CHILD_PLAYLIST_SONG_SORT_ORDER,
          sortOrder);
    } else if (mType == Constants.ALBUM) {
      SPUtil
          .putValue(mContext, SETTING_KEY.NAME, SETTING_KEY.CHILD_ALBUM_SONG_SORT_ORDER, sortOrder);
    } else if (mType == Constants.ARTIST) {
      SPUtil.putValue(mContext, SETTING_KEY.NAME, SETTING_KEY.CHILD_ARTIST_SONG_SORT_ORDER,
          sortOrder);
    } else {
      SPUtil.putValue(mContext, SETTING_KEY.NAME, SETTING_KEY.CHILD_FOLDER_SONG_SORT_ORDER,
          sortOrder);
    }
    mSortOrder = sortOrder;
    if (update) {
      onMediaStoreChanged();
    }

  }


  @Override
  public int getMenuLayoutId() {
    return mType == Constants.PLAYLIST ? R.menu.menu_child_for_playlist :
        mType == Constants.ALBUM ? R.menu.menu_child_for_album :
            mType == Constants.ARTIST ? R.menu.menu_child_for_artist : R.menu.menu_child_for_folder;
  }

  @Override
  protected int getLoaderId() {
    return LoaderIds.CHILDHOLDER_ACTIVITY;
  }


  @Override
  public void onServiceConnected(@NotNull MusicService service) {
    super.onServiceConnected(service);
  }

  @Override
  public void onPlayListChanged(String name) {
    super.onPlayListChanged(name);
    if (name.equals(PlayList.TABLE_NAME)) {
      onMediaStoreChanged();
    }
  }

  @Override
  public void onMediaStoreChanged() {
    super.onMediaStoreChanged();
  }

  @Override
  public void onTagChanged(@NotNull Song oldSong, @NotNull Song newSong) {
    super.onTagChanged(oldSong, newSong);
//    if (mType == Constants.ARTIST || mType == Constants.ALBUM) {
//      mId = mType == Constants.ARTIST ? newSong.getArtistId() : newSong.getAlbumId();
//      Title = mType == Constants.ARTIST ? newSong.getArtist() : newSong.getAlbum();
//      mToolBar.setTitle(Title);
//    }
  }

  /**
   * 根据参数(专辑id 歌手id 文件夹名 播放列表名)获得对应的歌曲信息列表
   *
   * @return 对应歌曲信息列表
   */
  @WorkerThread
  private List<Song> getSongs() {
    if (mId < 0) {
      return null;
    }
    switch (mType) {
      //专辑id
      case Constants.ALBUM:
        return MediaStoreUtil.getSongsByArtistIdOrAlbumId(mId, Constants.ALBUM);
      //歌手id
      case Constants.ARTIST:
        return MediaStoreUtil.getSongsByArtistIdOrAlbumId(mId, Constants.ARTIST);
      //文件夹名
      case Constants.FOLDER:
        return MediaStoreUtil.getSongsByParentId(mId);
      //播放列表名
      case Constants.PLAYLIST:
        /* 播放列表歌曲id列表 */
        return DatabaseRepository.getInstance()
            .getPlayList(mId)
            .flatMap((Function<PlayList, SingleSource<List<Song>>>) playList ->
                DatabaseRepository.getInstance()
                    .getPlayListSongs(mContext, playList, false))
            .blockingGet();
    }
    return new ArrayList<>();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    mRefreshHandler.remove();
  }

  @OnHandleMessage
  public void handleInternal(Message msg) {
    switch (msg.what) {
      case MSG_RESET_MULTI:
        mAdapter.notifyDataSetChanged();
        break;
//            case START:
//                if (mMDDialog != null && !mMDDialog.isShowing()) {
//                    mMDDialog.show();
//                }
//                break;
//            case END:
//                if (mMDDialog != null && mMDDialog.isShowing()) {
//                    mMDDialog.dismiss();
//                }
//                break;
    }
  }

  @Override
  public void onLoadFinished(Loader<List<Song>> loader, List<Song> data) {
    super.onLoadFinished(loader, data);
    mNum.setText(getString(R.string.song_count, data != null ? data.size() : 0));
  }

  @Override
  protected Loader<List<Song>> getLoader() {
    return new AsyncChildSongLoader(this);
  }

  @Override
  public void onMetaChanged() {
    super.onMetaChanged();
    if (mAdapter != null) {
      mAdapter.updatePlayingSong();
    }
  }

  private static final String EXTRA_ID = "id";
  private static final String EXTRA_TYPE = "type";
  private static final String EXTRA_TITLE = "title";

  public static void start(Context context, int type, long id, String title) {
    context.startActivity(new Intent(context, ChildHolderActivity.class)
        .putExtra(EXTRA_ID, id)
        .putExtra(EXTRA_TYPE, type)
        .putExtra(EXTRA_TITLE, title));
  }

  private static class AsyncChildSongLoader extends AppWrappedAsyncTaskLoader<List<Song>> {

    private final WeakReference<ChildHolderActivity> mRef;

    private AsyncChildSongLoader(ChildHolderActivity childHolderActivity) {
      super(childHolderActivity);
      mRef = new WeakReference<>(childHolderActivity);
    }

    @Override
    public List<Song> loadInBackground() {
      return getChildSongs();
    }

    @NonNull
    private List<Song> getChildSongs() {
      final ChildHolderActivity activity = mRef.get();
      List<Song> songs = new ArrayList<>();
      if (activity != null) {
        songs = activity.getSongs();
      }
      return songs != null ? songs : new ArrayList<>();
    }
  }

}