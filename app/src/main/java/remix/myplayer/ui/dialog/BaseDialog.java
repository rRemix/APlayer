package remix.myplayer.ui.dialog;

import android.content.Context;
import android.support.v4.app.DialogFragment;

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
