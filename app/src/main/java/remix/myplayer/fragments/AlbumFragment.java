package remix.myplayer.fragments;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;

import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import remix.myplayer.R;
import remix.myplayer.activities.ChildHolderActivity;
import remix.myplayer.adapters.AlbumAdapter;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/12/4.
 */
public class AlbumFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    private AlbumAdapter mAdapter;
    public static int mAlbumIdIndex = -1;
    public static int mAlbumIndex = -1;
    public static int mArtistIndex = -1;
    public static int mAlbumArtIndex = -1;
    public static int mNumofSongsIndex = -1;
    private Cursor mCursor;
    private LoaderManager mManager;
    private ListView mListView;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mManager = getLoaderManager();
        mManager.initLoader(1001, null, this);
        mAdapter = new AlbumAdapter(getContext(),R.layout.artist_album_item,null,new String[]{},new int[]{},0);
        mListView.setAdapter(mAdapter);
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.artist_album_list,null);
        mListView = (ListView)rootView.findViewById(R.id.artist_album_list);

        mListView.setOnItemClickListener(new ListViewListener());
        return rootView;

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(getContext(), MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                new String[]{BaseColumns._ID,MediaStore.Audio.AlbumColumns.ALBUM,
                        MediaStore.Audio.AlbumColumns.ARTIST,
                        MediaStore.Audio.AlbumColumns.ALBUM_ART,
                        MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS}, null,null,null);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data == null)
            return;
        mCursor = data;
        mAlbumIdIndex = data.getColumnIndex(BaseColumns._ID);
        mAlbumIndex = data.getColumnIndex(MediaStore.Audio.AlbumColumns.ALBUM);
        mArtistIndex = data.getColumnIndex(MediaStore.Audio.AlbumColumns.ARTIST);
        mAlbumArtIndex = data.getColumnIndex(MediaStore.Audio.AlbumColumns.ALBUM_ART);
        mNumofSongsIndex = data.getColumnIndex(MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS);
        mAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mAdapter != null)
            mAdapter.changeCursor(null);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(mAdapter != null)
            mAdapter.changeCursor(null);
    }

    private class ListViewListener implements AdapterView.OnItemClickListener
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            int albumid = ((Cursor)parent.getAdapter().getItem(position)).getInt(mAlbumIdIndex);
            String title = ((Cursor)parent.getAdapter().getItem(position)).getString(mAlbumIndex);

            Intent intent = new Intent(getActivity(), ChildHolderActivity.class);
            intent.putExtra("Id",albumid);
            intent.putExtra("Title",title);
            intent.putExtra("Type",Utility.ALBUM_HOLDER);
            startActivity(intent);

//            AlbumHolderFragment fragment = new AlbumHolderFragment();
//            Bundle bundle = new Bundle();
//            bundle.putInt("Id", Utility.mAlbumList.get(position).getAlbumId());
//            fragment.setArguments(bundle);
//
//            FragmentManager fm = MainActivity.mInstance.getSupportFragmentManager();
//
//            List<Fragment> fragList = fm.getFragments();
//            for(Fragment fragment1 : fragList)
//            {
//                fm.beginTransaction().hide(fragment1);
//            }
//            fm.beginTransaction().replace(R.id.main_fragment_container, fragment)
//                    .addToBackStack(null).commit();
//
//            int count = MainActivity.mInstance.getSupportFragmentManager().getBackStackEntryCount();
//            System.out.println(count);
        }
    }

}
