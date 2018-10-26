package remix.myplayer.ui.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import butterknife.ButterKnife;
import remix.myplayer.helper.MusicEventCallback;
import remix.myplayer.ui.MultipleChoice;
import remix.myplayer.ui.adapter.BaseAdapter;
import remix.myplayer.ui.fragment.base.BaseMusicFragment;
import remix.myplayer.util.Constants;

/**
 * Created by Remix on 2016/12/23.
 */

public abstract class LibraryFragment<D, A extends BaseAdapter> extends BaseMusicFragment implements MusicEventCallback, LoaderManager.LoaderCallbacks<List<D>> {
    protected A mAdapter;
    protected MultipleChoice<D> mChoice;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mHasPermission) {
            getLoaderManager().initLoader(getLoaderId(), null, this);
        }
    }

    public MultipleChoice getChoice() {
        return mChoice;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View rootView = inflater.inflate(getLayoutID(), container, false);
        mUnBinder = ButterKnife.bind(this, rootView);

        int type = this instanceof SongFragment ? Constants.SONG :
                this instanceof AlbumFragment ? Constants.ALBUM :
                        this instanceof ArtistFragment ? Constants.ARTIST :
                                this instanceof PlayListFragment ? Constants.PLAYLIST :
                                        Constants.FOLDER;
        mChoice = new MultipleChoice<>(getActivity(), type);
        initAdapter();
        initView();

        mChoice.setAdapter(mAdapter);
        return rootView;
    }


    protected abstract int getLayoutID();

    protected abstract void initAdapter();

    protected abstract void initView();

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdapter != null) {
            mAdapter.setData(null);
        }
    }

    @Override
    public void onMediaStoreChanged() {
        if (mHasPermission)
            getLoaderManager().restartLoader(getLoaderId(), null, this);
        else {
            if (mAdapter != null)
                mAdapter.setData(null);
        }
    }

    @Override
    public void onPermissionChanged(boolean has) {
        if (has != mHasPermission) {
            mHasPermission = has;
            onMediaStoreChanged();
        }
    }

    @Override
    public void onPlayListChanged() {

    }

    @Override
    public Loader<List<D>> onCreateLoader(int id, Bundle args) {
        return getLoader();
    }

    @Override
    public void onLoadFinished(Loader<List<D>> loader, List<D> data) {
        mAdapter.setData(data);
    }

    @Override
    public void onLoaderReset(Loader<List<D>> loader) {
        if (mAdapter != null)
            mAdapter.setData(null);
    }

    protected abstract Loader<List<D>> getLoader();

    protected abstract int getLoaderId();

}
