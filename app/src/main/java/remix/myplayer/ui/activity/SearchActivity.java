package remix.myplayer.ui.activity;

import static remix.myplayer.service.MusicService.EXTRA_SONG;
import static remix.myplayer.util.MusicUtil.makeCmdIntent;
import static remix.myplayer.util.Util.sendLocalBroadcast;

import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.misc.asynctask.AppWrappedAsyncTaskLoader;
import remix.myplayer.misc.interfaces.LoaderIds;
import remix.myplayer.misc.interfaces.OnItemClickListener;
import remix.myplayer.service.Command;
import remix.myplayer.ui.adapter.SearchAdapter;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by taeja on 16-1-22.
 */


/**
 * 搜索界面，根据关键字，搜索歌曲名，艺术家，专辑中的记录
 */
public class SearchActivity extends LibraryActivity<Song, SearchAdapter> implements
    SearchView.OnQueryTextListener {

  //搜索的关键字
  private String mkey;
  //搜索结果的listview
  @BindView(R.id.search_result_native)
  RecyclerView mSearchResRecyclerView;
  //无搜索结果
  @BindView(R.id.search_result_blank)
  TextView mSearchResBlank;
  @BindView(R.id.search_result_container)
  FrameLayout mSearchResContainer;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_search);
    ButterKnife.bind(this);
    setUpToolbar("");

    mAdapter = new SearchAdapter(R.layout.item_search_reulst);
    mAdapter.setOnItemClickListener(new OnItemClickListener() {
      @Override
      public void onItemClick(View view, int position) {
        if (mAdapter != null && mAdapter.getDatas() != null && position >= 0 && position < mAdapter.getDatas().size()) {
          sendLocalBroadcast(makeCmdIntent(Command.PLAY_TEMP)
              .putExtra(EXTRA_SONG, mAdapter.getDatas().get(position)));
        } else {
          ToastUtil.show(mContext, R.string.illegal_arg);
        }
      }

      @Override
      public void onItemLongClick(View view, int position) {
      }
    });
    mSearchResRecyclerView.setAdapter(mAdapter);
    mSearchResRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    mSearchResRecyclerView.setItemAnimator(new DefaultItemAnimator());

    updateUI();

  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);

    final MenuItem searchItem = menu.findItem(R.id.search);
    searchItem.expandActionView();

    SearchView searchView = (SearchView) searchItem.getActionView();
    if (searchView == null) {
      ToastUtil.show(this, R.string.init_failed);
      finish();
      return true;
    }
    searchView.setQueryHint(getString(R.string.search_hint));
    searchView.setMaxWidth(Integer.MAX_VALUE);

    //去掉搜索图标
    try {
      Field mDrawable = SearchView.class.getDeclaredField("mSearchHintIcon");
      mDrawable.setAccessible(true);
      Drawable drawable = (Drawable) mDrawable.get(searchView);
      drawable.setBounds(0, 0, 0, 0);
    } catch (Exception e) {
      e.printStackTrace();
    }

    searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
      @Override
      public boolean onMenuItemActionExpand(MenuItem item) {
        return true;
      }

      @Override
      public boolean onMenuItemActionCollapse(MenuItem item) {
        onBackPressed();
        return false;
      }
    });

    searchView.setQuery(mkey, false);
    searchView.post(() -> searchView.setOnQueryTextListener(SearchActivity.this));

    return true;
  }


  @Override
  public int getMenuLayoutId() {
    return R.menu.menu_search;
  }


  @Override
  public void onLoadFinished(Loader<List<Song>> loader, List<Song> data) {
    super.onLoadFinished(loader, data);
    //更新界面
    updateUI();
  }

  @Override
  protected Loader<List<Song>> getLoader() {
    return new AsyncSearchLoader(mContext, mkey);
  }

  @Override
  protected int getLoaderId() {
    return LoaderIds.SEARCH_ACTIVITY;
  }


  /**
   * 搜索歌曲名，专辑，艺术家中包含该关键的记录
   *
   * @param key 搜索关键字
   */
  private void search(String key) {
    mkey = key;
    getLoaderManager().restartLoader(LoaderIds.SEARCH_ACTIVITY, null, this);
  }

  @Override
  public boolean onQueryTextSubmit(String key) {
    if (!key.equals(mkey)) {
      search(key);
      return true;
    }
    return false;
  }

  @Override
  public boolean onQueryTextChange(String key) {
    if (!key.equals(mkey)) {
      search(key);
      return true;
    }
    return false;
  }


  private static class AsyncSearchLoader extends AppWrappedAsyncTaskLoader<List<Song>> {

    private String mkey;

    private AsyncSearchLoader(Context context, String key) {
      super(context);
      mkey = key;
    }

    @Override
    public List<Song> loadInBackground() {
      if (TextUtils.isEmpty(mkey)) {
        return new ArrayList<>();
      }
      Cursor cursor = null;
      List<Song> songs = new ArrayList<>();
      try {
        String selection =
            MediaStore.Audio.Media.TITLE + " like ? " + "or " + MediaStore.Audio.Media.ARTIST
                + " like ? "
                + "or " + MediaStore.Audio.Media.ALBUM + " like ? and " + MediaStoreUtil
                .getBaseSelection();
        cursor = getContext().getContentResolver()
            .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                selection,
                new String[]{"%" + mkey + "%", "%" + mkey + "%", "%" + mkey + "%"}, null);

        if (cursor != null && cursor.getCount() > 0) {
          Set<String> blackList = SPUtil.getStringSet(App.getContext(), SPUtil.SETTING_KEY.NAME,
              SPUtil.SETTING_KEY.BLACKLIST_SONG);
          while (cursor.moveToNext()) {
            if (!blackList
                .contains(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID)))) {
              songs.add(MediaStoreUtil.getSongInfo(cursor));
            }
          }
        }
      } finally {
        if (cursor != null && !cursor.isClosed()) {
          cursor.close();
        }
      }
      return songs;
    }
  }

  /**
   * 更新界面
   */
  private void updateUI() {
    boolean flag = mAdapter.getDatas() != null && mAdapter.getDatas().size() > 0;
    mSearchResRecyclerView.setVisibility(flag ? View.VISIBLE : View.GONE);
    mSearchResBlank.setVisibility(flag ? View.GONE : View.VISIBLE);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
  }
}
