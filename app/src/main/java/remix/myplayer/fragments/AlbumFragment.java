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
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import remix.myplayer.R;
import remix.myplayer.activities.ChildHolderActivity;
import remix.myplayer.adapters.AlbumAdater;
import remix.myplayer.listeners.OnItemClickListener;
import remix.myplayer.utils.Constants;

/**
 * Created by Remix on 2015/12/20.
 */
public class AlbumFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    RecyclerView mRecycleView;
    Cursor mCursor = null;
    public static int mAlbumIdIndex = -1;
    public static int mAlbumIndex = -1;
    public static int mArtistIndex = -1;
    public static int mAlbumArtIndex = -1;
    public static int mNumofSongsIndex = -1;
    private LoaderManager mManager;
    private AlbumAdater mAdapter;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mManager = getLoaderManager();
        mManager.initLoader(1001, null, this);
        mAdapter = new AlbumAdater(mCursor,getContext());
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if(mCursor != null && mCursor.moveToPosition(position))
                {
                    int albumid = mCursor.getInt(mAlbumIdIndex);
                    String title = mCursor.getString(mAlbumIndex);
                    Intent intent = new Intent(getActivity(), ChildHolderActivity.class);
                    intent.putExtra("Id", albumid);
                    intent.putExtra("Title", title);
                    intent.putExtra("Type", Constants.ALBUM_HOLDER);
                    startActivity(intent);
                }
            }
            @Override
            public void onItemLongClick(View view, int position) {
            }
        });
        mRecycleView.setAdapter(mAdapter);
    }
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.album_recycle_list,null);
        mRecycleView = (RecyclerView)rootView.findViewById(R.id.album_recycleview);
        mRecycleView.setLayoutManager(new GridLayoutManager(getContext(), 2));
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
        mAdapter.setCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mAdapter != null)
            mAdapter.setCursor(null);
    }

    @Override
    public void onDestroy() {
        super.onDestroyView();
        if(mCursor != null)
            mCursor.close();
    }
}
