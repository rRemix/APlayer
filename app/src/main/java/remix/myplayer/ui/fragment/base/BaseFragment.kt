package remix.myplayer.ui.fragment.base

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import remix.myplayer.App
import remix.myplayer.ui.activity.base.BaseActivity
import remix.myplayer.util.Util

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/7/27 15:18
 */
abstract class BaseFragment : Fragment() {
  @JvmField
  protected var mContext: Context? = null
  protected var mHasPermission = false
  @JvmField
  protected var pageName = BaseFragment::class.java.simpleName

  override fun onAttach(context: Context) {
    super.onAttach(context)
    mContext = context
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mHasPermission = Util.hasPermissions(BaseActivity.EXTERNAL_STORAGE_PERMISSIONS)
  }

  override fun onDestroyView() {
    super.onDestroyView()
  }

  override fun onResume() {
    super.onResume()
  }

  override fun onPause() {
    super.onPause()
  }

  protected fun getStringSafely(@StringRes res: Int): String {
    return if (isAdded) {
      getString(res)
    } else {
      App.getContext().getString(res)
    }
  }

  protected fun getStringSafely(@StringRes res: Int, vararg args: Any?): String {
    return if (isAdded) {
      getString(res, *args)
    } else {
      App.getContext().getString(res, *args)
    }
  }
}