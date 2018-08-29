package remix.myplayer.ui.fragment.base;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import remix.myplayer.helper.MusicEventHelper;
import remix.myplayer.ui.activity.base.BaseMusicActivity;

public class BaseMusicFragment extends BaseFragment implements MusicEventHelper.MusicEventCallback{
    private BaseMusicActivity mMusicActivity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mMusicActivity = (BaseMusicActivity) context;
        } catch (ClassCastException e) {
            throw new RuntimeException(context.getClass().getSimpleName() + " must be an instance of " + BaseMusicActivity.class.getSimpleName());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMusicActivity = null;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MusicEventHelper.addCallback(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MusicEventHelper.removeCallback(this);
    }

    @Override
    public void onMediaStoreChanged() {

    }

    @Override
    public void onPermissionChanged(boolean has) {

    }

    @Override
    public void onPlayListChanged() {

    }

    @Override
    public void onServiceConnected() {
    }

    @Override
    public void onServiceDisConnected() {
    }
}
