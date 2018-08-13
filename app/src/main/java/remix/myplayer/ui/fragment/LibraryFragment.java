package remix.myplayer.ui.fragment;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.List;

import butterknife.ButterKnife;
import remix.myplayer.helper.MusicEventHelper;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.activity.MultiChoiceActivity;
import remix.myplayer.ui.adapter.BaseAdapter;
import remix.myplayer.util.Util;

/**
 * Created by Remix on 2016/12/23.
 */

public abstract class LibraryFragment<D,A extends BaseAdapter> extends BaseFragment implements MusicEventHelper.MusicEventCallback,LoaderManager.LoaderCallbacks<List<D>>{
    protected A mAdapter;
    protected boolean mHasPermission = false;
    protected MultiChoice mMultiChoice;
    private final String[] mPermissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE};

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(mHasPermission = Util.hasPermissions(mPermissions)){
            getLoaderManager().initLoader(getLoaderId(), null, this);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MusicEventHelper.addCallback(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View rootView = inflater.inflate(getLayoutID(),container,false);
        mUnBinder = ButterKnife.bind(this,rootView);

        if(mContext instanceof MultiChoiceActivity){
            mMultiChoice = ((MultiChoiceActivity)mContext).getMultiChoice();
        }
        initAdapter();
        initView();
        if(mMultiChoice != null && mMultiChoice.getAdapter() == null){
            mMultiChoice.setAdapter(mAdapter);
        }

        return rootView;
    }


    protected abstract int getLayoutID();
    protected abstract void initAdapter();
    protected abstract void initView();

    @Override
    public void onResume() {
        super.onResume();
        new RxPermissions(getActivity())
                .request(Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe(aBoolean -> {
                    if(aBoolean != mHasPermission){
                        Intent intent = new Intent(MusicService.ACTION_PERMISSION_CHANGE);
                        intent.putExtra("permission",aBoolean);
                        mContext.sendBroadcast(intent);
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MusicEventHelper.removeCallback(this);
        if(mAdapter != null){
            mAdapter.setData(null);
        }
    }

    @Override
    public void onMediaStoreChanged() {
        if(mHasPermission)
            getLoaderManager().restartLoader(getLoaderId(), null, this);
        else{
            if(mAdapter != null)
                mAdapter.setData(null);
        }
    }

    @Override
    public void onPermissionChanged(boolean has) {
        if(has != mHasPermission){
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
        if(mAdapter != null)
            mAdapter.setData(null);
    }

    protected abstract Loader<List<D>> getLoader();
    protected abstract int getLoaderId();

}
