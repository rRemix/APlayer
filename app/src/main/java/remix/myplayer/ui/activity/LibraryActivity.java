package remix.myplayer.ui.activity;

import android.os.Bundle;

import java.util.List;

import remix.myplayer.ui.adapter.BaseAdapter;

/**
 * Created by Remix on 2017/10/20.
 */

public abstract class LibraryActivity<D, A extends BaseAdapter> extends MultiChoiceActivity implements android.app.LoaderManager.LoaderCallbacks<List<D>> {
    protected A mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mHasPermission) {
            getLoaderManager().initLoader(getLoaderId(), null, this);
        }
    }

    protected abstract int getLoaderId();


    @Override
    public void onMediaStoreChanged() {
        super.onMediaStoreChanged();
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
    public android.content.Loader<List<D>> onCreateLoader(int id, Bundle args) {
        return getLoader();
    }

    @Override
    public void onLoadFinished(android.content.Loader<List<D>> loader, List<D> data) {
        if (mAdapter != null)
            mAdapter.setData(data);
    }

    @Override
    public void onLoaderReset(android.content.Loader<List<D>> loader) {
        if (mAdapter != null)
            mAdapter.setData(null);
    }

    protected android.content.Loader<List<D>> getLoader() {
        return null;
    }

}
