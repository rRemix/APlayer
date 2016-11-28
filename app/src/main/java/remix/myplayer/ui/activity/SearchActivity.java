package remix.myplayer.ui.activity;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.SearchResAdapter;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.ui.ListItemDecoration;
import remix.myplayer.ui.customview.SearchToolBar;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;

/**
 * Created by taeja on 16-1-22.
 */


/**
 * 搜索界面，根据关键字，搜索歌曲名，艺术家，专辑中的记录
 */
public class SearchActivity extends ToolbarActivity {
    //查询索引
    public static int mIdIndex = -1;
    public static int mTitleIndex = -1;
    public static int mArtistIndex = -1;
    public static int mAlbumIndex = -1;
    public static int mAlbumIdIndex = -1;
    private Cursor mCursor = null;
    private SearchResAdapter mSearchResAdapter = null;
    //搜索的关键字
    private String mkey = "";
    public static SearchActivity mInstance = null;

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
        MobclickAgent.onEvent(this,"Search");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);
        initToolbar(mSearchToolBar,"");
        mInstance = this;

        mSearchToolBar.addSearchListener(new SearchToolBar.SearchListener() {
            @Override
            public void onSearch(String key, boolean isclick) {
                search(key, isclick);
            }

            @Override
            public void onClear() {
                //清空搜索结果，并更新界面
                mCursor = null;
                mSearchResAdapter.setCursor(mCursor);
                mkey = "";
                UpdateUI();
            }

            @Override
            public void onBack() {
                finish();
            }
        });

        mSearchResAdapter = new SearchResAdapter(this);
        mSearchResAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (mCursor != null && mCursor.getCount() > 0 && mCursor.moveToFirst()) {
                    ArrayList<Integer> list = new ArrayList<>();
                    for(int i = 0 ; i < mCursor.getCount(); i++) {
                        mCursor.moveToPosition(i);
                        list.add(mCursor.getInt(mIdIndex));
                    }
                    Intent intent = new Intent(Constants.CTL_ACTION);
                    Bundle arg = new Bundle();
                    arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                    arg.putInt("Position", position);
                    intent.putExtras(arg);
                    Global.setPlayQueue(list,SearchActivity.this,intent);
                }
            }
            @Override
            public void onItemLongClick(View view, int position) {
            }
        });
        mSearchResRecyclerView.setAdapter(mSearchResAdapter);
        mSearchResRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mSearchResRecyclerView.addItemDecoration(new ListItemDecoration(this,ListItemDecoration.VERTICAL_LIST));
        mSearchResRecyclerView.setItemAnimator(new DefaultItemAnimator());

        UpdateUI();
    }


    /**
     * 搜索歌曲名，专辑，艺术家中包含该关键的记录
     * @param key 搜索关键字
     * @param isclick 是否点击，决定是否保存搜索历史
     */
    private void search(String key,boolean isclick) {
        mkey = key;
        if(mkey == null)
            mkey = "";

        Cursor cursor = null;
        try {
            String selection = MediaStore.Audio.Media.TITLE + " like ? " + "or " + MediaStore.Audio.Media.ARTIST + " like ? "
                    + "or " + MediaStore.Audio.Media.ALBUM + " like ? ";
            cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.ALBUM_ID},
                    selection,
                    new String[]{mkey + "%",mkey + "%",mkey + "%"}, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (cursor != null && cursor.getCount() > 0) {
            mIdIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            mTitleIndex = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            mArtistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            mAlbumIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            mAlbumIdIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            mCursor = cursor;
            mSearchResAdapter.setCursor(mCursor);
        } else {
            mCursor = null;
            mSearchResAdapter.setCursor(mCursor);
        }
        mSearchResAdapter.setCursor(mCursor);

        //更新界面
        UpdateUI();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mCursor != null)
            mCursor.close();
        mSearchResAdapter.setCursor(null);
    }

    /**
     * 更新界面
     */
    private void UpdateUI(){
        boolean flag = mCursor != null && mCursor.getCount() > 0;
        mSearchResRecyclerView.setVisibility(flag? View.VISIBLE : View.GONE);
        mSearchResBlank.setVisibility(flag? View.GONE :View.VISIBLE);
    }

    public void onResume() {
        MobclickAgent.onPageStart(SearchActivity.class.getSimpleName());
        super.onResume();
    }
    public void onPause() {
        MobclickAgent.onPageEnd(SearchActivity.class.getSimpleName());
        super.onPause();
    }

}
