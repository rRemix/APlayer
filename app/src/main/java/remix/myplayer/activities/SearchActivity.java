package remix.myplayer.activities;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import java.util.ArrayList;
import remix.myplayer.R;
import remix.myplayer.adapters.SearchAdapter;
import remix.myplayer.utils.Utility;

/**
 * Created by taeja on 16-1-22.
 */
public class SearchActivity extends AppCompatActivity {
    public static int mIdIndex;
    public static int mDisplayNameIndex;
    public static int mArtistIndex;
    public static int mAlbumIndex;
    private Cursor mCursor;
    public static boolean mUpdate = true;
    private ListView mListView;
    private SearchView mSearchView;
    private SearchAdapter mAdapter;
    private String mkey;
    private static final String SDROOT = "/sdcard/";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mSearchView = (SearchView) findViewById(R.id.search_);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mkey = query;
                search();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        mListView = (ListView) findViewById(R.id.search_result);
        mAdapter = new SearchAdapter(getApplicationContext(), R.layout.search_item, null, new String[]{}, new int[]{}, 0);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new ListViewListener());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    private void search() {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ARTIST},
                    MediaStore.Audio.Media.DISPLAY_NAME + " like ?", new String[]{mkey + "%"}, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (cursor != null && cursor.getCount() > 0) {
            mIdIndex = cursor.getColumnIndex(MediaStore.Audio.Media._ID);
            mDisplayNameIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
            mArtistIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            mAlbumIndex = cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
            mCursor = cursor;
            mAdapter.changeCursor(mCursor);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAdapter.changeCursor(null);
    }

    public void onBack(View v) {
        finish();
    }

    class ListViewListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent intent = new Intent(Utility.CTL_ACTION);
            Bundle arg = new Bundle();
            arg.putInt("Control", Utility.PLAYSELECTEDSONG);
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
//                    while (mCursor.moveToNext()) {
//                        list.add(mCursor.getLong(mIdIndex));
//                    }
                    Utility.mPlayList = list;
                    MainActivity.mInstance.getService().UpdateNextSong(position);
                }
            }
        }
    }
}
