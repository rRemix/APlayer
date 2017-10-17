package remix.myplayer.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.ArtistAdapter;
import remix.myplayer.helper.MusicEventHelper;
import remix.myplayer.interfaces.ModeChangeCallback;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.activity.ChildHolderActivity;
import remix.myplayer.ui.activity.MultiChoiceActivity;
import remix.myplayer.ui.customview.fastcroll_recyclerview.FastScrollRecyclerView;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.SPUtil;

/**
 * Created by Remix on 2015/12/22.
 */

/**
 * 艺术家Fragment
 */
public class ArtistFragment extends CursorFragment implements LoaderManager.LoaderCallbacks<Cursor>,MusicEventHelper.MusicEventCallback {
    @BindView(R.id.artist_recycleview)
    FastScrollRecyclerView mRecyclerView;
    private MultiChoice mMultiChoice;
    //艺术家与艺术家id的索引
    public static int mArtistIdIndex = -1;
    public static int mArtistIndex = -1;

    public static final String TAG = ArtistFragment.class.getSimpleName();
    private static int LOADER_ID = 1000;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        getLoaderManager().initLoader(++LOADER_ID, null, (LoaderManager.LoaderCallbacks) this);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageName = TAG;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater,container,savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_artist,null);
        mUnBinder = ButterKnife.bind(this,rootView);

        if(getActivity() instanceof MultiChoiceActivity){
            mMultiChoice = ((MultiChoiceActivity) getActivity()).getMultiChoice();
        }

        mAdapter = new ArtistAdapter(mCursor,getActivity(),mMultiChoice);
        ((ArtistAdapter)mAdapter).setModeChangeCallback(new ModeChangeCallback() {
            @Override
            public void OnModeChange(final int mode) {
                mRecyclerView.setLayoutManager(mode == Constants.LIST_MODEL ? new LinearLayoutManager(getActivity()) : new GridLayoutManager(getActivity(), 2));
                mRecyclerView.setAdapter(mAdapter);
            }
        });
        mAdapter.setOnItemClickLitener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                int artistId = getArtsitId(position);
                if(getUserVisibleHint() && artistId > 0 &&
                        !mMultiChoice.itemAddorRemoveWithClick(view,position,artistId,TAG)){
                    if (mCursor.moveToPosition(position)) {
                        int artistid = mCursor.getInt(mArtistIdIndex);
                        String title = mCursor.getString(mArtistIndex);
                        Intent intent = new Intent(getActivity(), ChildHolderActivity.class);
                        intent.putExtra("Id", artistid);
                        intent.putExtra("Title", title);
                        intent.putExtra("Type", Constants.ARTIST);
                        startActivity(intent);
                    }
                }
            }
            @Override
            public void onItemLongClick(View view, int position) {
                int artistId = getArtsitId(position);
                if(getUserVisibleHint() && artistId > 0)
                    mMultiChoice.itemAddorRemoveWithLongClick(view,position,artistId,TAG,Constants.ARTIST);
            }
        });

        int model = SPUtil.getValue(getActivity(),"Setting","ArtistModel",Constants.GRID_MODEL);
        mRecyclerView.setLayoutManager(model == 1 ? new LinearLayoutManager(getActivity()) : new GridLayoutManager(getActivity(), 2));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);
        return rootView;
    }


    private int getArtsitId(int position){
        int artistId = -1;
        if(mCursor != null && !mCursor.isClosed() && mCursor.moveToPosition(position)){
            artistId = mCursor.getInt(mArtistIdIndex);
        }
        return artistId;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(),MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{"distinct " + MediaStore.Audio.Media.ARTIST_ID,MediaStore.Audio.Media.ARTIST},
                MediaStore.Audio.Media.SIZE + ">" + Constants.SCAN_SIZE +  MediaStoreUtil.getBaseSelection() + ")" + " GROUP BY (" + MediaStore.Audio.Media.ARTIST_ID,
                null,
                MediaStore.Audio.Artists.DEFAULT_SORT_ORDER);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if(mAdapter != null)
            mAdapter.setCursor(null);
    }
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data == null || loader.getId() != LOADER_ID)
            return;
        mCursor = data;
        //设置查询索引
        mArtistIdIndex = mCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST_ID);
        mArtistIndex = mCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);

        mAdapter.setCursor(mCursor);
    }

    @Override
    public ArtistAdapter getAdapter(){
        return (ArtistAdapter) mAdapter;
    }

    @Override
    public void onMediaStoreChanged() {
        getLoaderManager().initLoader(++LOADER_ID, null, this);
    }
}
