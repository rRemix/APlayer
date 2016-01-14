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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import remix.myplayer.R;
import remix.myplayer.activities.ChildHolderActivity;
import remix.myplayer.adapters.ArtistAdapter;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/12/4.
 */
public class ArtistFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private ListView mListView;
    private Cursor mCursor;
    private LoaderManager mManager;
    private ArtistAdapter mAdapter;
    public static int mArtistIdIndex = -1;
    public static int mArtistIndex = -1;
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mManager = getLoaderManager();
        mManager.initLoader(1001, null, this);
        mAdapter = new ArtistAdapter(getContext(),R.layout.artist_album_item,null,new String[]{},new int[]{},0);
        mListView.setAdapter(mAdapter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.artist_album_list, null);
        mListView = (ListView) rootView.findViewById(R.id.artist_album_list);
        mListView.setOnItemClickListener(new ListViewListener());
        return rootView;

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(getContext(),MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                new String[]{BaseColumns._ID,MediaStore.Audio.ArtistColumns.ARTIST},null,null,null);
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data != null)
            mCursor = data;
        mArtistIdIndex = data.getColumnIndex(BaseColumns._ID);
        mArtistIndex = data.getColumnIndex(MediaStore.Audio.ArtistColumns.ARTIST);
        mAdapter.changeCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if(mAdapter != null)
            mAdapter.changeCursor(null);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if(mAdapter != null)
            mAdapter.changeCursor(null);
    }
    private class ListViewListener implements AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            int artistid = ((Cursor)parent.getAdapter().getItem(position)).getInt(mArtistIdIndex);
            String title = ((Cursor)parent.getAdapter().getItem(position)).getString(mArtistIndex);

            Intent intent = new Intent(getActivity(), ChildHolderActivity.class);
            intent.putExtra("Id", artistid);
            intent.putExtra("Title",title);
            intent.putExtra("Type",Utility.ARTIST_HOLDER);
            startActivity(intent);
        }
    }
}
