package remix.myplayer.ui.dialog;

import android.app.Dialog;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.MaterialDialog;
import java.util.ArrayList;
import java.util.List;
import remix.myplayer.Global;
import remix.myplayer.R;
import remix.myplayer.db.PlayLists;
import remix.myplayer.misc.interfaces.OnItemClickListener;
import remix.myplayer.theme.Theme;
import remix.myplayer.ui.adapter.AddtoPlayListAdapter;
import remix.myplayer.util.Constants;
import remix.myplayer.util.LogUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by taeja on 16-2-1.
 */

/**
 * 将歌曲添加到播放列表的对话框
 */
public class AddtoPlayListDialog extends BaseDialog implements
    LoaderManager.LoaderCallbacks<Cursor> {

  public static final String EXTRA_SONG_LIST = "list";

  public static AddtoPlayListDialog newInstance(List<Integer> ids) {
    AddtoPlayListDialog dialog = new AddtoPlayListDialog();
    Bundle arg = new Bundle();
    arg.putSerializable(EXTRA_SONG_LIST, new ArrayList(ids));
    dialog.setArguments(arg);
    return dialog;
  }


  @BindView(R.id.playlist_addto_list)
  RecyclerView mRecyclerView;
  @BindView(R.id.playlist_addto_new)
  TextView mNew;

  private AddtoPlayListAdapter mAdapter;
  private Cursor mCursor;
  public static int mPlayListNameIndex;
  public static int mPlayListIdIndex;

  private boolean mAddAfterCreate = true;
  private static int LOADER_ID = 0;

  private List<Integer> mList;

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    MaterialDialog dialog = Theme.getBaseDialog(getActivity())
        .customView(R.layout.dialog_addto_playlist, false)
        .build();

    ButterKnife.bind(this, dialog.getCustomView());

    mList = (List<Integer>) getArguments().getSerializable(EXTRA_SONG_LIST);
    if (mList == null) {
      ToastUtil.show(mContext, R.string.add_song_playlist_error);
      dismiss();
    }

    mAdapter = new AddtoPlayListAdapter(mContext);
    mAdapter.setOnItemClickLitener(new OnItemClickListener() {
      @Override
      public void onItemClick(View view, int position) {
        if (view != null) {
          ToastUtil.show(mContext, R.string.add_song_playlist_success,
              PlayListUtil.addMultiSongs(mList, getPlayListName(position)),
              getPlayListName(position));
        } else {
          ToastUtil.show(mContext, R.string.add_song_playlist_error, Toast.LENGTH_SHORT);
        }
        dismiss();
      }

      @Override
      public void onItemLongClick(View view, int position) {
      }
    });
    mRecyclerView.setAdapter(mAdapter);
    mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    mRecyclerView.setLayoutManager(new LinearLayoutManager(mContext));

    getLoaderManager().initLoader(LOADER_ID++, null, this);

    //改变高度，并置于底部
    Window window = dialog.getWindow();
    window.setWindowAnimations(R.style.DialogAnimBottom);

    Display display = getActivity().getWindowManager().getDefaultDisplay();
    DisplayMetrics metrics = new DisplayMetrics();
    display.getMetrics(metrics);
    WindowManager.LayoutParams lp = window.getAttributes();
    lp.width = metrics.widthPixels;
    window.setAttributes(lp);
    window.setGravity(Gravity.BOTTOM);

    return dialog;
  }


  private String getPlayListName(int position) {
    String playlistName = "";
    if (mCursor != null && !mCursor.isClosed() && mCursor.moveToPosition(position)) {
      playlistName = mCursor.getString(mCursor.getColumnIndex(PlayLists.PlayListColumns.NAME));
    }
    return playlistName;
  }

  @OnClick({R.id.playlist_addto_new})
  public void onClick(View v) {
    if (v.getId() == R.id.playlist_addto_new) {
      Theme.getBaseDialog(mContext)
          .title(R.string.new_playlist)
          .positiveText(R.string.create)
          .negativeText(R.string.cancel)
          .inputRange(1, 15)
          .input("", "本地歌单" + Global.PlayList.size(), (dialog, input) -> {
            if (!TextUtils.isEmpty(input)) {
              int newPlayListId = -1;
              try {
                newPlayListId = PlayListUtil.addPlayList(input.toString());
                ToastUtil.show(mContext, newPlayListId > 0 ?
                        R.string.add_playlist_success :
                        newPlayListId == -1 ? R.string.add_playlist_error
                            : R.string.playlist_already_exist,
                    Toast.LENGTH_SHORT);
                if (newPlayListId < 0) {
                  return;
                }
                if (mAddAfterCreate) {
                  ToastUtil.show(mContext, R.string.add_song_playlist_success, input.toString(),
                      PlayListUtil.addMultiSongs(mList, input.toString(), newPlayListId));
                }
              } catch (Exception e) {
                LogUtil.d("AddtoPlayList", e.toString());
              }
            }
          })
          .dismissListener(dialog -> dismiss())
          .show();

    }
  }


  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    return new CursorLoader(mContext, PlayLists.CONTENT_URI, null,
        PlayLists.PlayListColumns.NAME + "!= ?", new String[]{Constants.PLAY_QUEUE}, null);
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
      if (data == null) {
          return;
      }
    mCursor = data;
    mPlayListIdIndex = mCursor.getColumnIndex(PlayLists.PlayListColumns._ID);
    mPlayListNameIndex = mCursor.getColumnIndex(PlayLists.PlayListColumns.NAME);
      if (mAdapter != null) {
          mAdapter.setCursor(mCursor);
      }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
      if (mAdapter != null) {
          mAdapter.setCursor(null);
      }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
      if (mCursor != null) {
          mCursor.close();
      }
  }
}
