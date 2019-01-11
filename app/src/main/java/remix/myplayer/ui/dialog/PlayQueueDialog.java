package remix.myplayer.ui.dialog;

import static remix.myplayer.service.MusicService.EXTRA_POSITION;
import static remix.myplayer.util.MusicUtil.makeCmdIntent;
import static remix.myplayer.util.Util.sendLocalBroadcast;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.afollestad.materialdialogs.MaterialDialog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import remix.myplayer.Global;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.PlayList;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.misc.asynctask.WrappedAsyncTaskLoader;
import remix.myplayer.misc.interfaces.OnItemClickListener;
import remix.myplayer.service.Command;
import remix.myplayer.theme.Theme;
import remix.myplayer.ui.adapter.PlayQueueAdapter;
import remix.myplayer.ui.widget.fastcroll_recyclerview.LocationRecyclerView;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.PlayListUtil;

/**
 * Created by Remix on 2015/12/6.
 */

/**
 * 正在播放列表Dialog
 */
public class PlayQueueDialog extends BaseDialog implements
    LoaderManager.LoaderCallbacks<List<Song>> {

  public static PlayQueueDialog newInstance() {
    PlayQueueDialog playQueueDialog = new PlayQueueDialog();
    return playQueueDialog;
  }

  @BindView(R.id.bottom_actionbar_play_list)
  LocationRecyclerView mRecyclerView;
  private PlayQueueAdapter mAdapter;
  private static int LOADER_ID = 0;

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    MaterialDialog dialog = Theme.getBaseDialog(getActivity())
        .customView(R.layout.dialog_playqueue, false)
        .build();

    ButterKnife.bind(this, dialog.getCustomView());

    mAdapter = new PlayQueueAdapter(mContext, R.layout.item_playqueue);
    mAdapter.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(View view, int position) {
        sendLocalBroadcast(makeCmdIntent(Command.PLAYSELECTEDSONG)
            .putExtra(EXTRA_POSITION, position));

        mAdapter.notifyDataSetChanged();
      }

      @Override
      public void onItemLongClick(View view, int position) {
      }
    });
    mRecyclerView.setAdapter(mAdapter);
    mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));
    mRecyclerView.setItemAnimator(new DefaultItemAnimator());

    //初始化LoaderManager
    getLoaderManager().initLoader(LOADER_ID++, null, this);
    //改变播放列表高度，并置于底部
    Window window = dialog.getWindow();
    window.setWindowAnimations(R.style.DialogAnimBottom);
    Display display = getActivity().getWindowManager().getDefaultDisplay();
    DisplayMetrics metrics = new DisplayMetrics();
    display.getMetrics(metrics);
    WindowManager.LayoutParams lp = window.getAttributes();
    lp.height = DensityUtil.dip2px(mContext, 354);
    lp.width = metrics.widthPixels;
    window.setAttributes(lp);
    window.setGravity(Gravity.BOTTOM);

    return dialog;

  }

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.dialog_playqueue);
//        ButterKnife.bind(this);
//
//
//        //初始化LoaderManager
//        getSupportLoaderManager().initLoader(LOADER_ID++, null, this);
//
//
//
//
//
//
//
//
//
//
//
//    }

  public PlayQueueAdapter getAdapter() {
    return mAdapter;
  }


  @Override
  public Loader<List<Song>> onCreateLoader(int id, Bundle args) {
    return new AsyncPlayQueueSongLoader(mContext);
  }

  @Override
  public void onLoadFinished(Loader<List<Song>> loader, final List<Song> data) {
    if (data == null) {
      return;
    }
    mAdapter.setData(data);
    final int currentId = MusicServiceRemote.getCurrentSong().getId();
    if (currentId < 0) {
      return;
    }
    mRecyclerView.smoothScrollToCurrentSong(data);
  }

  @Override
  public void onLoaderReset(Loader<List<Song>> loader) {
    if (mAdapter != null) {
      mAdapter.setData(null);
    }
  }

  private static class AsyncPlayQueueSongLoader extends WrappedAsyncTaskLoader<List<Song>> {

    private AsyncPlayQueueSongLoader(Context context) {
      super(context);
    }

    @Override
    public List<Song> loadInBackground() {

      List<Integer> ids = PlayListUtil.getSongIds(Global.PlayQueueID);
      if (ids == null || ids.isEmpty()) {
        return Collections.emptyList();
      }
//      List<Song> songs = new ArrayList<>();
//      for (Integer id : ids) {
//        songs.add(MediaStoreUtil.getSongById(id));
//      }
      List<Song> songs = PlayListUtil.getMP3ListWithSort(ids,Global.PlayQueueID);
      return songs;
    }
  }

  //    @Override
//    public void onMediaStoreChanged() {
//        if (mHasPermission) {
//            getSupportLoaderManager().initLoader(LOADER_ID++, null, this);
//        } else {
//            if (mAdapter != null)
//                mAdapter.setData(null);
//        }
//    }
//
//    @Override
//    public void onPermissionChanged(boolean has) {
//        onMediaStoreChanged();
//    }
//
//    @Override
//    public void onPlayListChanged() {
//        onMediaStoreChanged();
//    }
}
