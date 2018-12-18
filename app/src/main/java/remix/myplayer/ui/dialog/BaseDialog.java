package remix.myplayer.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import remix.myplayer.R;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.base.BaseMusicActivity;

/**
 * Created by Remix on 2016/3/16.
 */


public abstract class BaseDialog extends DialogFragment {

    protected Context mContext;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }
}
