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
import remix.myplayer.adapters.ArtistAdapter;
import remix.myplayer.listeners.OnItemClickListener;
import remix.myplayer.utils.Constants;

/**
 * Created by Remix on 2015/12/22.
 */
public class ArtistFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    RecyclerView mRecycleView;
    Cursor mCursor = null;
    public static int mArtistIdIndex = -1;
    public static int mArtistIndex = -1;
    private LoaderManager mManager;
    private ArtistAdapter mAdapter;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mManager = getLoaderManager();
        mManager.initLoader(1001, null, this);
        mAdapter = new ArtistAdapter(mCursor,getActivity());
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if (mCursor.moveToPosition(position)) {
                    int artistid = mCursor.getInt(mArtistIdIndex);
                    String title = mCursor.getString(mArtistIndex);
                    Intent intent = new Intent(getActivity(), ChildHolderActivity.class);
                    intent.putExtra("Id", artistid);
                    intent.putExtra("Title", title);
                    intent.putExtra("Type", Constants.ARTIST_HOLDER);
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
        View rootView = inflater.inflate(R.layout.fragment_artist,null);
        mRecycleView = (RecyclerView)rootView.findViewById(R.id.artist_recycleview);
        mRecycleView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
        return rootView;
    }
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader loader = new CursorLoader(getActivity(),MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                new String[]{BaseColumns._ID,MediaStore.Audio.ArtistColumns.ARTIST},null,null,null);
        return loader;
    }
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if(mAdapter != null)
            mAdapter.setCursor(null);
    }
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data != null)
            mCursor = data;
        mArtistIdIndex = data.getColumnIndex(BaseColumns._ID);
        mArtistIndex = data.getColumnIndex(MediaStore.Audio.ArtistColumns.ARTIST);
        mAdapter.setCursor(data);
    }

    @Override
    public void onDestroy() {
        super.onDestroyView();
        if(mCursor != null)
            mCursor.close();
    }

}
