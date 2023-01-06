package remix.myplayer.ui.fragment.base

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import remix.myplayer.App
import remix.myplayer.util.PermissionUtil

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/7/27 15:18
 */
abstract class BaseFragment : Fragment() {
  protected var hasPermission = false
  @JvmField
  protected var pageName = BaseFragment::class.java.simpleName

  override fun onAttach(context: Context) {
    super.onAttach(context)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    hasPermission = PermissionUtil.hasNecessaryPermission()
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
      App.context.getString(res)
    }
  }

  protected fun getStringSafely(@StringRes res: Int, vararg args: Any?): String {
    return if (isAdded) {
      getString(res, *args)
    } else {
      App.context.getString(res, *args)
    }
  }
}