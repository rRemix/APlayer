package remix.myplayer.ui.fragment.base;

import static remix.myplayer.ui.activity.base.BaseActivity.EXTERNAL_STORAGE_PERMISSIONS;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.Unbinder;
import remix.myplayer.App;
import remix.myplayer.util.Util;


/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/7/27 15:18
 */
public abstract class BaseFragment extends Fragment {

  protected Unbinder mUnBinder;
  protected Context mContext;
  protected boolean mHasPermission = false;
  protected String mPageName = BaseFragment.class.getSimpleName();

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    mContext = context;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mHasPermission = Util.hasPermissions(EXTERNAL_STORAGE_PERMISSIONS);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    if (mUnBinder != null) {
      mUnBinder.unbind();
    }
  }

  public RecyclerView.Adapter getAdapter() {
    return null;
  }

  public void onResume() {
    super.onResume();
  }

  public void onPause() {
    super.onPause();
  }

  protected String getStringSafely(@StringRes int res) {
    if (isAdded()) {
      return getString(res);
    } else {
      return App.getContext().getString(res);
    }
  }

  protected String getStringSafely(@StringRes int res, Object... args) {
    if (isAdded()) {
      return getString(res, args);
    } else {
      return App.getContext().getString(res, args);
    }
  }

}
