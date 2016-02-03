package remix.myplayer.fragments;


import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;


import java.lang.ref.WeakReference;

import remix.myplayer.R;
import remix.myplayer.activities.MainActivity;
import remix.myplayer.adapters.SongListAdapter;
import remix.myplayer.listeners.ListViewListener;
import remix.myplayer.services.MusicService;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/11/30.
 */
public class AllSongFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private LoaderManager mManager;
    private SongListAdapter mAdapter;
    private MusicService.PlayerReceiver mMusicReceiver;
    private int mPrev = -1;
    private Cursor mCursor = null;
    private ListView mListView = null;
    public static int mDisPlayNameIndex = -1;
    public static int mArtistIndex = -1;
    public static int mAlbumIndex = -1;
    public static int mAlbumIdIndex = -1;
    public static int mSongId = -1;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mManager = getLoaderManager();
        mManager.initLoader(1000, null, this);
        mAdapter = new SongListAdapter(getContext(),R.layout.allsong_item,null,new String[]{},new int[]{},0);
        mListView.setAdapter(mAdapter);
    }

    class AsynLoadImage extends AsyncTask<Integer,Integer,Bitmap>
    {
        private final WeakReference mImageView;
        //        private ImageView mImageView;
        public AsynLoadImage(ImageView imageView)
        {
            mImageView = new WeakReference(imageView);
        }
        @Override
        protected Bitmap doInBackground(Integer... params) {
            return Utility.CheckBitmapByAlbumId(params[0],true);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(bitmap != null)
                ((ImageView)mImageView.get()).setImageBitmap(bitmap);
            else
                ((ImageView)mImageView.get()).setImageResource(R.drawable.default_recommend);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

//        MusicService service = new MusicService(getContext());
//        mMusicReceiver = service.new PlayerReceiver();
//        IntentFilter musicfilter = new IntentFilter(Utility.CTL_ACTION);
//        getContext().registerReceiver(mMusicReceiver, musicfilter);

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mAdapter != null)
            mAdapter.changeCursor(null);
//        getContext().unregisterReceiver(mMusicReceiver);
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater,container,savedInstanceState);
        final View rootView = inflater.inflate(R.layout.allsong_list,null);
        mListView = (ListView)rootView.findViewById(R.id.list);
        mListView.setOnItemClickListener(new ListViewListener(getContext()));
        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(getContext(),
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,MediaStore.Audio.Media.SIZE + ">80000",null,MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        return  loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data == null)
            return;
        mCursor = data;
        mDisPlayNameIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
        mArtistIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
        mAlbumIndex = data.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
        mSongId = data.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
        mAlbumIdIndex = data.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        if(mCursor != null)
        {
            mAdapter.changeCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mAdapter != null)
            mAdapter.changeCursor(null);
    }



    @Override
    public boolean onContextItemSelected(MenuItem item) {

        switch (item.getItemId())
        {
            case 1:
                Toast.makeText(getContext(),"单击了测试1",Toast.LENGTH_SHORT).show();
                break;
            case 2:
                Toast.makeText(getContext(),"单击了测试2",Toast.LENGTH_SHORT).show();
                break;
            case 3:
                Toast.makeText(getContext(),"单击了测试3",Toast.LENGTH_SHORT).show();
                break;
        }
        return super.onContextItemSelected(item);
    }
}
