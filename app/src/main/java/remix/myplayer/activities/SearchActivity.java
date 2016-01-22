package remix.myplayer.activities;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import remix.myplayer.R;

/**
 * Created by taeja on 16-1-22.
 */
public class SearchActivity extends AppCompatActivity {
    private SearchView mSearchView;
    private ArrayList<String> mResult = new ArrayList<>();
    private String mkey;
    private static final String SDROOT = "/sdcard/";
    private static ArrayList<String> mSonglist = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mSearchView = (SearchView)findViewById(R.id.search_);
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                mResult.clear();
                mkey = query;
                getAllFile();
                if(mResult.size() > 0)
                    ((TextView)findViewById(R.id.text)).setText(mResult.toString());
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        if(mSonglist.size() == 0)
        {
            Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Audio.Media.DISPLAY_NAME},
                    MediaStore.Audio.Media.SIZE + ">80000",
                    null,null);
            while (cursor != null && cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                if (name != null && !name.equals(""))
                    mSonglist.add(name);
            }
            cursor.close();
        }

    }
    private void getAllFile()
    {
        if(mSonglist.size() == 0)
            return;

        for(String name : mSonglist)
        {
            if(name.indexOf(mkey) > 0)
                mResult.add(name);
        }



//        File files[] = root.listFiles();
//        if(files != null){
//            for (File f : files){
//                if(f.isDirectory()){
//                    getAllFile(f);
//                }else{
//                    if(f.getName().indexOf(mkey) > 0)
//                        mResult.add(f.getName());
//                }
//            }
//        }
    }
    public void onBack(View v)
    {
        finish();
    }
}
