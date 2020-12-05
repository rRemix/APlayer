package remix.myplayer.ui.dialog;

import static remix.myplayer.request.network.RxUtil.applySingleScheduler;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.MaterialDialog;
import io.reactivex.SingleSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import java.util.ArrayList;
import java.util.List;
import remix.myplayer.R;
import remix.myplayer.db.room.DatabaseRepository;
import remix.myplayer.db.room.model.PlayList;
import remix.myplayer.misc.interfaces.OnItemClickListener;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.theme.Theme;
import remix.myplayer.ui.adapter.AddtoPlayListAdapter;
import remix.myplayer.ui.dialog.base.BaseMusicDialog;
import remix.myplayer.ui.fragment.PlayListFragment;
import remix.myplayer.util.ToastUtil;

/**
 * Created by taeja on 16-2-1.
 */

/**
 * 将歌曲添加到播放列表的对话框
 */
public class AddtoPlayListDialog extends BaseMusicDialog implements
    LoaderManager.LoaderCallbacks<List<PlayList>> {

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
      ToastUtil.show(getContext(), R.string.add_song_playlist_error);
      dismiss();
    }

    mAdapter = new AddtoPlayListAdapter(R.layout.item_playlist_addto);
    mAdapter.setOnItemClickListener(new OnItemClickListener() {
      @SuppressLint("CheckResult")
      @Override
      public void onItemClick(View view, int position) {
        PlayList playList = mAdapter.getDatas().get(position);
        DatabaseRepository.getInstance()
            .insertToPlayList(mList, playList.getId())
            .compose(RxUtil.applySingleScheduler())
            .doFinally(() -> dismiss())
            .subscribe(num -> ToastUtil.show(getContext(), R.string.add_song_playlist_success, num, playList.getName()),
                throwable -> ToastUtil.show(getContext(), R.string.add_song_playlist_error));
      }

      @Override
      public void onItemLongClick(View view, int position) {

      }
    });

    mRecyclerView.setAdapter(mAdapter);
    mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

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


  @SuppressLint("CheckResult")
  @OnClick({R.id.playlist_addto_new})
  public void onClick(View v) {
    DatabaseRepository.getInstance()
        .getAllPlaylist()
        .compose(applySingleScheduler())
        .subscribe(new Consumer<List<PlayList>>() {
          @Override
          public void accept(List<PlayList> playLists) throws Exception {
            Theme.getBaseDialog(getContext())
                .title(R.string.new_playlist)
                .positiveText(R.string.create)
                .negativeText(R.string.cancel)
                .inputRange(1, 15)
                .input("", getString(R.string.local_list) + playLists.size(), (dialog, input) -> {
                  if (TextUtils.isEmpty(input)) {
                    ToastUtil.show(getContext(), R.string.add_error);
                    return;
                  }

                  DatabaseRepository.getInstance()
                      .insertPlayList(input.toString())
                      .flatMap(new Function<Integer, SingleSource<Integer>>() {
                        @Override
                        public SingleSource<Integer> apply(Integer newId) throws Exception {
                          return DatabaseRepository.getInstance().insertToPlayList(mList, newId);
                        }
                      })
                      .compose(applySingleScheduler())
                      .subscribe(num -> {
                        ToastUtil.show(getContext(), R.string.add_playlist_success);
                        ToastUtil
                            .show(getContext(), getString(R.string.add_song_playlist_success, num, input.toString()));
                      }, throwable -> ToastUtil.show(getContext(), R.string.add_error));
                })
                .dismissListener(dialog -> dismiss())
                .show();
          }
        });


  }


  @NonNull
  @Override
  public Loader<List<PlayList>> onCreateLoader(int id, @Nullable Bundle args) {
    return new PlayListFragment.AsyncPlayListLoader(getContext());
  }

  @Override
  public void onLoadFinished(@NonNull Loader<List<PlayList>> loader, List<PlayList> data) {
    if (data == null) {
      return;
    }
    if (mAdapter != null) {
      mAdapter.setData(data);
    }
  }

  @Override
  public void onLoaderReset(Loader<List<PlayList>> loader) {
    if (mAdapter != null) {
      mAdapter.setData(null);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (mAdapter != null) {
      mAdapter.setData(null);
    }
  }

}
