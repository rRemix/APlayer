package remix.myplayer.ui.activity;

import static remix.myplayer.theme.ThemeStore.getTextColorPrimary;
import static remix.myplayer.theme.ThemeStore.getTextColorSecondary;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.db.room.DatabaseRepository;
import remix.myplayer.misc.asynctask.AppWrappedAsyncTaskLoader;
import remix.myplayer.misc.interfaces.LoaderIds;
import remix.myplayer.request.network.RxUtil;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.adapter.SongChooseAdapter;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.ToastUtil;

/**
 * @ClassName SongChooseActivity
 * @Description 新建列表后添加歌曲
 * @Author Xiaoborui
 * @Date 2016/10/21 09:34
 */

public class SongChooseActivity extends LibraryActivity<Song, SongChooseAdapter> {

  public static final String TAG = SongChooseActivity.class.getSimpleName();
  public static final String EXTRA_NAME = "PlayListName";
  public static final String EXTRA_ID = "PlayListID";

  private int mPlayListID;
  private String mPlayListName;
  @BindView(R.id.confirm)
  TextView mConfirm;
  @BindView(R.id.cancel)
  TextView mCancel;
  @BindView(R.id.title)
  TextView mTitle;
  @BindView(R.id.recyclerview)
  RecyclerView mRecyclerView;

  public static void start(Activity activity, int playListId, String playListName) {
    Intent intent = new Intent(activity, SongChooseActivity.class);
    intent.putExtra(EXTRA_ID, playListId);
    intent.putExtra(EXTRA_NAME, playListName);
    activity.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_song_choose);
    ButterKnife.bind(this);

    mPlayListID = getIntent().getIntExtra(EXTRA_ID, -1);
    if (mPlayListID <= 0) {
      ToastUtil.show(this, R.string.add_error, Toast.LENGTH_SHORT);
      return;
    }
    mPlayListName = getIntent().getStringExtra(EXTRA_NAME);

    TextView cancel = findViewById(R.id.cancel);
    cancel.setTextColor(getTextColorPrimary());
    mConfirm.setTextColor(getTextColorSecondary());
    mAdapter = new SongChooseAdapter(R.layout.item_song_choose, isValid -> {
      mConfirm.setAlpha(isValid ? 1.0f : 0.6f);
      mConfirm.setClickable(isValid);
    });

    mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    mRecyclerView.setAdapter(mAdapter);
    mRecyclerView.setItemAnimator(new DefaultItemAnimator());
    mConfirm.setAlpha(0.6f);

    findViewById(R.id.header).setBackgroundColor(ThemeStore.getMaterialPrimaryColor());
    ButterKnife
        .apply(new TextView[]{mConfirm, mCancel, mTitle}, new ButterKnife.Action<TextView>() {
          @Override
          public void apply(@NonNull TextView view, int index) {
            view.setTextColor(ThemeStore.getTextColorPrimaryReverse());
          }
        });
  }

  @OnClick({R.id.confirm, R.id.cancel})
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.cancel:
        finish();
        break;
      case R.id.confirm:
        if (mAdapter.getCheckedSong() == null || mAdapter.getCheckedSong().size() == 0) {
          ToastUtil.show(this, R.string.choose_no_song);
          return;
        }
        DatabaseRepository.getInstance()
            .insertToPlayList(mAdapter.getCheckedSong(), mPlayListID)
            .compose(RxUtil.applySingleScheduler())
            .subscribe(num -> {
              ToastUtil.show(mContext, getString(R.string.add_song_playlist_success, num, mPlayListName));
              finish();
            }, throwable -> finish());
    }
  }

  @Override
  protected int getLoaderId() {
    return LoaderIds.SONGCHOOSE_ACTIVITY;
  }


  @Override
  protected Loader<List<Song>> getLoader() {
    return new AsyncSongLoader(mContext);
  }

  @Override
  public void saveSortOrder(@Nullable String sortOrder) {

  }

  private static class AsyncSongLoader extends AppWrappedAsyncTaskLoader<List<Song>> {

    private AsyncSongLoader(Context context) {
      super(context);
    }

    @Override
    public List<Song> loadInBackground() {
      return MediaStoreUtil.getAllSong();
    }
  }

}
