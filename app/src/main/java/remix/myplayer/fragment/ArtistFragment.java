package remix.myplayer.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
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
import remix.myplayer.ui.activity.ChildHolderActivity;
import remix.myplayer.adapter.ArtistAdapter;
import remix.myplayer.listener.OnItemClickListener;
import remix.myplayer.util.Constants;

/**
 * Created by Remix on 2015/12/22.
 */

/**
 * 艺术家Fragment
 */
public class ArtistFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    RecyclerView mRecycleView;
    Cursor mCursor = null;
    //艺术家与艺术家id的索引
    public static int mArtistIdIndex = -1;
    public static int mArtistIndex = -1;
    private LoaderManager mManager;
    private ArtistAdapter mAdapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mManager = getLoaderManager();
        mManager.initLoader(1001, null, this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_artist,null);
        mRecycleView = (RecyclerView)rootView.findViewById(R.id.artist_recycleview);
        mRecycleView.setLayoutManager(new GridLayoutManager(getActivity(), 2));
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
        return rootView;
    }
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
//        CursorLoader loader = new CursorLoader(getActivity(),MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
//                new String[]{BaseColumns._ID,MediaStore.Audio.ArtistColumns.ARTIST},null,null,null);
        return new CursorLoader(getActivity(),MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{"distinct " + MediaStore.Audio.Media.ARTIST_ID,MediaStore.Audio.Media.ARTIST},
                MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE + ")" + " GROUP BY (" + MediaStore.Audio.Media.ARTIST_ID,
                null,
                null);
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
        //设置查询索引
        mArtistIdIndex = data.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID);
        mArtistIndex = data.getColumnIndex(MediaStore.Audio.Media.ARTIST);
        mAdapter.setCursor(data);
    }

    @Override
    public void onDestroy() {
        super.onDestroyView();
        if(mCursor != null)
            mCursor.close();
    }

}
