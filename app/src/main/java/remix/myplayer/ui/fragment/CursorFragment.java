package remix.myplayer.ui.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import remix.myplayer.adapter.BaseAdapter;
import remix.myplayer.helper.MusicEventHelper;

/**
 * Created by Remix on 2016/12/23.
 */

public class CursorFragment extends BaseFragment{
    protected Cursor mCursor;
    protected BaseAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(this instanceof MusicEventHelper.MusicEventCallback)
            MusicEventHelper.addCallback((MusicEventHelper.MusicEventCallback) this);

        return super.onCreateView(inflater,container,savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if(this instanceof MusicEventHelper.MusicEventCallback)
            MusicEventHelper.removeCallback((MusicEventHelper.MusicEventCallback) this);
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
}
