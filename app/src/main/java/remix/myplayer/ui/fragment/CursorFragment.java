package remix.myplayer.ui.fragment;

import android.Manifest;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import io.reactivex.functions.Consumer;
import remix.myplayer.R;
import remix.myplayer.adapter.BaseAdapter;
import remix.myplayer.helper.MusicEventHelper;
import remix.myplayer.util.ToastUtil;

/**
 * Created by Remix on 2016/12/23.
 */

public abstract class CursorFragment extends BaseFragment implements MusicEventHelper.MusicEventCallback{
    protected Cursor mCursor;
    protected BaseAdapter mAdapter;
    protected boolean mHasPermission = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MusicEventHelper.addCallback(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        new RxPermissions(getActivity())
                .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(new Consumer<Boolean>() {
                    @Override
                    public void accept(Boolean aBoolean) throws Exception {
                        if(aBoolean != mHasPermission){
                            mHasPermission = aBoolean;
                            if(aBoolean){
                                MusicEventHelper.onMediaStoreChanged();
                                mHasPermission = true;
                            }
                        }
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mAdapter != null){
            mAdapter.setCursor(null);
        }
        if(mCursor != null && !mCursor.isClosed()){
            mCursor.close();
        }
    }

    @Override
    public void onMediaStoreChanged() {

    }

}
