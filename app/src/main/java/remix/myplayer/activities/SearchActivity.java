package remix.myplayer.activities;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.adapters.SearchResAdapter;
import remix.myplayer.ui.customviews.SearchView;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;

/**
 * Created by taeja on 16-1-22.
 */


/**
 * 搜索界面，根据关键字，搜索歌曲名，艺术家，专辑中的记录
 */
public class SearchActivity extends BaseAppCompatActivity {
    //查询索引
    public static int mIdIndex = -1;
    public static int mDisplayNameIndex = -1;
    public static int mArtistIndex = -1;
    public static int mAlbumIndex = -1;
    public static int mAlbumIdIndex = -1;
    private Cursor mCursor = null;
    //搜索结果的listview
    private ListView mSearchResList = null;
    private SearchResAdapter mSearchResAdapter = null;
    //搜索的关键字
    private String mkey = "";
    private SearchView mSearchView = null;
    public static SearchActivity mInstance = null;
    private static final String SDROOT = "/sdcard/";
    //背景
    private ImageView mSearchLogo;
    //无搜索结果
    private TextView mSearchResBlank;
    private FrameLayout mSearchResContainer;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        mInstance = this;

        mSearchLogo = (ImageView)findViewById(R.id.search_logo);
        mSearchView = (SearchView)findViewById(R.id.search_view);
        mSearchView.addSearchListener(new SearchView.SearchListener() {
            @Override
            public void onSearch(String key, boolean isclick) {
                search(key, isclick);
            }

            @Override
            public void onClear() {
                //清空搜索结果，并更新界面
                mCursor = null;
                mSearchResAdapter.changeCursor(mCursor);
                mkey = "";
                UpdateUI();
            }

            @Override
            public void onBack() {
                finish();
            }
        });

        mSearchResContainer = (FrameLayout)findViewById(R.id.search_result_container);
        mSearchResBlank = (TextView)findViewById(R.id.search_result_blank);
        mSearchResList = (ListView) findViewById(R.id.search_result_native);
        mSearchResAdapter = new SearchResAdapter(getApplicationContext(), R.layout.search_reulst_item, null, new String[]{}, new int[]{}, 0);
        mSearchResList.setAdapter(mSearchResAdapter);
        mSearchResList.setOnItemClickListener(new ListViewListener());

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
        if(isclick && !mkey.equals("")){
            //            XmlUtil.addKey(mkey);
        }

        Cursor cursor = null;
        try {
            String selection = MediaStore.Audio.Media.DISPLAY_NAME + " like ? " + "or " + MediaStore.Audio.Media.ARTIST + " like ? "
                    + "or " + MediaStore.Audio.Media.ALBUM + " like ? ";
            cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.ALBUM_ID},
                    selection,
                    new String[]{mkey + "%",mkey + "%",mkey + "%"}, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (cursor != null && cursor.getCount() > 0) {
            mIdIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            mDisplayNameIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
            mArtistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            mAlbumIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            mAlbumIdIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
            mCursor = cursor;
            mSearchResAdapter.changeCursor(mCursor);
        } else {
            mCursor = null;
            mSearchResAdapter.changeCursor(mCursor);
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
        mSearchResAdapter.changeCursor(null);
    }

    /**
     * 更新界面
     * 如果搜素关键字为空，显示搜索历史或者无搜索历史
     * 如果关键字不为空，显示搜索结果或者无搜索结果
     */
    private void UpdateUI(){
        if(!mkey.equals("")){
            mSearchResContainer.setVisibility(View.VISIBLE);
            mSearchLogo.setVisibility(View.GONE);
            boolean flag = mCursor != null && mCursor.getCount() > 0;
            mSearchResList.setVisibility(flag == true ? View.VISIBLE : View.GONE);
            mSearchResBlank.setVisibility(flag == true ? View.GONE :View.VISIBLE);
        }else {
            mSearchResContainer.setVisibility(View.GONE);
            mSearchLogo.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    class ListViewListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent intent = new Intent(Constants.CTL_ACTION);
            Bundle arg = new Bundle();
            arg.putInt("Control", Constants.PLAYSELECTEDSONG);
            arg.putInt("Position", position);
            intent.putExtras(arg);
            getApplicationContext().sendBroadcast(intent);
            if (mCursor != null && mCursor.getCount() > 0 && mCursor.moveToFirst()) {
                {
                    ArrayList<Long> list = new ArrayList<>();
                    for(int i = 0 ; i < mCursor.getCount(); i++) {
                        mCursor.moveToPosition(i);
                        list.add(mCursor.getLong(mIdIndex));
                    }
                    DBUtil.setPlayingList(list);
                }
            }
        }
    }

}
