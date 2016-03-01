package remix.myplayer.activities;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import remix.myplayer.R;
import remix.myplayer.adapters.SearchHisAdapter;
import remix.myplayer.adapters.SearchResAdapter;
import remix.myplayer.ui.SearchView;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.utils.XmlUtil;

/**
 * Created by taeja on 16-1-22.
 */
public class SearchActivity extends AppCompatActivity {
    public static int mIdIndex = -1;
    public static int mDisplayNameIndex = -1;
    public static int mArtistIndex = -1;
    public static int mAlbumIndex = -1;
    public static int mAlbumIdIndex = -1;
    private Cursor mCursor = null;
    private ListView mSearchHisList = null;
    private ListView mSearchResList = null;
    private SearchResAdapter mSearchResAdapter = null;
    private Button mClearHistoryBtn = null;
    private String mkey = "";
    private SearchHisAdapter mSearchHisAdapter = null;
    private SearchView mSearchView = null;
    public static SearchActivity mInstance = null;
    private static final String SDROOT = "/sdcard/";
    public static ArrayList mSearchHisKeyList = new ArrayList();
    private FrameLayout mSearchHisContainer = null;
    private TextView mSearchHisTip = null;
    private LinearLayout mSearchHisContent = null;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        mInstance = this;
        mSearchHisKeyList = XmlUtil.getSearchHisList();


        mSearchView = (SearchView)findViewById(R.id.search_view);
        mSearchView.addSearchListener(new SearchView.SearchListener() {

            @Override
            public void onSearch(String key, boolean isclick) {
                search(key,isclick);
            }

            @Override
            public void onClear() {
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



        mSearchHisContainer = (FrameLayout)findViewById(R.id.search_his_container);
        mSearchHisContent = (LinearLayout)findViewById(R.id.search_his_container_content);
        mSearchHisKeyList = XmlUtil.getSearchHisList();
        mSearchHisAdapter = new SearchHisAdapter();
        mSearchHisList = (ListView)findViewById(R.id.search_history);
        mSearchHisList.setAdapter(mSearchHisAdapter);
        mSearchHisList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView temp = (TextView)view.findViewById(R.id.search_history_item_text);
                String key = temp.getText().toString();
                search(key,false);
                mSearchView.UpdateContent(key);
            }
        });
        mSearchHisTip = (TextView)findViewById(R.id.search_history_tip);

        mClearHistoryBtn = (Button)findViewById(R.id.search_history_clear);
        mClearHistoryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                XmlUtil.removeallKey();
                mSearchHisAdapter.notifyDataSetChanged();
                UpdateUI();
            }
        });

        mSearchResList = (ListView) findViewById(R.id.search_result_native);
        mSearchResAdapter = new SearchResAdapter(getApplicationContext(), R.layout.search_reulst_item, null, new String[]{}, new int[]{}, 0);
        mSearchResList.setAdapter(mSearchResAdapter);
        mSearchResList.setOnItemClickListener(new ListViewListener());

        UpdateUI();
    }

    private void search(String key,boolean isclick) {
        mkey = key;
        if(mkey == null)
            mkey = "";
        if(isclick && !mkey.equals(""))
            XmlUtil.addKey(mkey);

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

    private void UpdateUI(){
        if(!mkey.equals("")){
            mSearchResList.setVisibility(View.VISIBLE);
            mSearchHisContainer.setVisibility(View.GONE);
        }else {
            mSearchResList.setVisibility(View.GONE);
            mSearchHisContainer.setVisibility(View.VISIBLE);
            mSearchHisTip.setVisibility(mSearchHisKeyList.size() == 0 ? View.VISIBLE : View.GONE);
            mSearchHisContent.setVisibility(mSearchHisKeyList.size() == 0 ? View.GONE : View.VISIBLE);
            mSearchHisAdapter.notifyDataSetChanged();
        }
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
                    for(int i = 0 ; i < mCursor.getCount(); i++)
                    {
                        mCursor.moveToPosition(i);
                        list.add(mCursor.getLong(mIdIndex));
                    }
                    DBUtil.setPlayingList(list);
                }
            }
        }
    }

}
