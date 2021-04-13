package remix.myplayer.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import org.jetbrains.annotations.Nullable;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.databinding.ActivitySongChooseBinding;
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
  private ActivitySongChooseBinding binding;

  public static final String TAG = SongChooseActivity.class.getSimpleName();
  public static final String EXTRA_NAME = "PlayListName";
  public static final String EXTRA_ID = "PlayListID";

  private int mPlayListID;
  private String mPlayListName;

  public static void start(Activity activity, int playListId, String playListName) {
    Intent intent = new Intent(activity, SongChooseActivity.class);
    intent.putExtra(EXTRA_ID, playListId);
    intent.putExtra(EXTRA_NAME, playListName);
    activity.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    binding = ActivitySongChooseBinding.inflate(getLayoutInflater());
    setContentView(binding.getRoot());

    mPlayListID = getIntent().getIntExtra(EXTRA_ID, -1);
    if (mPlayListID <= 0) {
      ToastUtil.show(this, R.string.add_error, Toast.LENGTH_SHORT);
      return;
    }
    mPlayListName = getIntent().getStringExtra(EXTRA_NAME);

    mAdapter = new SongChooseAdapter(R.layout.item_song_choose, isValid -> {
      binding.confirm.setAlpha(isValid ? 1.0f : 0.6f);
      binding.confirm.setClickable(isValid);
    });

    binding.recyclerview.setLayoutManager(new LinearLayoutManager(this));
    binding.recyclerview.setAdapter(mAdapter);
    binding.recyclerview.setItemAnimator(new DefaultItemAnimator());
    binding.confirm.setAlpha(0.6f);

    binding.header.setBackgroundColor(ThemeStore.getMaterialPrimaryColor());
    for (TextView view : new TextView[]{binding.confirm, binding.cancel, binding.title}) {
      view.setTextColor(ThemeStore.getTextColorPrimaryReverse());
    }

    binding.confirm.setOnClickListener(this::onClick);
    binding.cancel.setOnClickListener(this::onClick);
  }

  public void onClick(View v) {
    int id = v.getId();
    if (id == R.id.cancel) {
      finish();
    } else if (id == R.id.confirm) {
      if (mAdapter.getCheckedSong() == null || mAdapter.getCheckedSong().size() == 0) {
        ToastUtil.show(this, R.string.choose_no_song);
        return;
      }
      DatabaseRepository.getInstance()
          .insertToPlayList(mAdapter.getCheckedSong(), mPlayListID)
          .compose(RxUtil.applySingleScheduler())
          .subscribe(num -> {
            ToastUtil.show(mContext, getString(R.string.add_song_playlist_success, num,
                mPlayListName));
            finish();
          }, throwable -> finish());
    }
  }

  @Override
  protected int getLoaderId() {
    return LoaderIds.ACTIVITY_SONGCHOOSE;
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
