package remix.myplayer.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.interfaces.LoaderIds;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.misc.asynctask.AppWrappedAsyncTaskLoader;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.adapter.SearchAdapter;
import remix.myplayer.ui.widget.SearchToolBar;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by taeja on 16-1-22.
 */


/**
 * 搜索界面，根据关键字，搜索歌曲名，艺术家，专辑中的记录
 */
public class SearchActivity extends LibraryActivity<Song,SearchAdapter> {
    //搜索的关键字
    private String mkey;
    //搜索结果的listview
    @BindView(R.id.search_result_native)
    RecyclerView mSearchResRecyclerView;
    @BindView(R.id.search_view)
    SearchToolBar mSearchToolBar;
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
        setUpToolbar(mSearchToolBar,"");

        mSearchToolBar.addSearchListener(new SearchToolBar.SearchListener() {
            @Override
            public void onSearch(String key, boolean isclick) {
                if(!key.equals(mkey))
                    search(key);
            }

            @Override
            public void onClear() {
                //清空搜索结果，并更新界面
                mAdapter.setData(null);
                mkey = "";
                UpdateUI();
            }

            @Override
            public void onBack() {
                finish();
            }
        });

        mAdapter = new SearchAdapter(this,R.layout.item_search_reulst);
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if(mAdapter != null && mAdapter.getDatas() != null){
                    Intent intent = new Intent(MusicService.ACTION_CMD);
                    intent.putExtra("Control", Command.PLAY_TEMP);
                    intent.putExtra("Song",  mAdapter.getDatas().get(position));
                    sendBroadcast(intent);
                }else {
                    ToastUtil.show(mContext,R.string.illegal_arg);
                }
            }
            @Override
            public void onItemLongClick(View view, int position) {
            }
        });
        mSearchResRecyclerView.setAdapter(mAdapter);
        mSearchResRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mSearchResRecyclerView.setItemAnimator(new DefaultItemAnimator());

        UpdateUI();

    }

    @Override
    public void onLoadFinished(Loader<List<Song>> loader, List<Song> data) {
        super.onLoadFinished(loader,data);
        //更新界面
        UpdateUI();
    }

    @Override
    protected Loader<List<Song>> getLoader() {
        return new AsyncSearchLoader(mContext,mkey);
    }

    @Override
    protected int getLoaderId() {
        return LoaderIds.SEARCH_ACTIVITY;
    }

    @Override
    protected void setUpMultiChoice() {
    }

    @Override
    protected void setUpClick() {
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    /**
     * 搜索歌曲名，专辑，艺术家中包含该关键的记录
     * @param key 搜索关键字
     */
    private void search(String key) {
        mkey = key;
        getLoaderManager().restartLoader(LoaderIds.SEARCH_ACTIVITY, null, this);
    }


    private static class AsyncSearchLoader extends AppWrappedAsyncTaskLoader<List<Song>> {
        private String mkey;
        private AsyncSearchLoader(Context context,String key) {
            super(context);
            mkey = key;
        }

        @Override
        public List<Song> loadInBackground() {
            if(TextUtils.isEmpty(mkey))
                return new ArrayList<>();
            Cursor cursor = null;
            List<Song> songs = new ArrayList<>();
            try {
                String selection = MediaStore.Audio.Media.TITLE + " like ? " + "or " + MediaStore.Audio.Media.ARTIST + " like ? "
                        + "or " + MediaStore.Audio.Media.ALBUM + " like ? and " +  MediaStoreUtil.getBaseSelection();
                cursor = getContext().getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        null,
                        selection,
                        new String[]{"%" + mkey + "%","%" + mkey + "%","%" + mkey + "%"}, null);

                if (cursor != null && cursor.getCount() > 0) {
                    Set<String> blackList = SPUtil.getStringSet(App.getContext(),SPUtil.SETTING_KEY.NAME,SPUtil.SETTING_KEY.BLACKLIST_SONG);
                    while (cursor.moveToNext()){
                        if(!blackList.contains(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media._ID))))
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

    /**
     * 更新界面
     */
    private void UpdateUI(){
        boolean flag = mAdapter.getDatas() != null && mAdapter.getDatas().size() > 0;
        mSearchResRecyclerView.setVisibility(flag? View.VISIBLE : View.GONE);
        mSearchResBlank.setVisibility(flag? View.GONE :View.VISIBLE);
    }

}
