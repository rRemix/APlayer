package remix.myplayer.ui.activity

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.view.Menu
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.Toolbar
import remix.myplayer.R
import remix.myplayer.theme.ThemeStore.materialPrimaryColor
import remix.myplayer.theme.ToolbarContentTintHelper
import remix.myplayer.ui.activity.base.BaseMusicActivity

/**
 * Created by taeja on 16-3-15.
 */
@SuppressLint("Registered")
open class ToolbarActivity : BaseMusicActivity() {
  protected var toolbar: Toolbar? = null

  protected fun setToolbarTitle(title: String) {
    toolbar?.title = title
  }

  protected fun setUpToolbar(title: String?, @DrawableRes iconRes: Int) {
    if (toolbar == null) {
      toolbar = findViewById(R.id.toolbar)
    }
    toolbar?.title = title
    setSupportActionBar(toolbar)
    toolbar?.setBackgroundColor(materialPrimaryColor)
    toolbar?.setNavigationIcon(iconRes)
    toolbar?.setNavigationOnClickListener { v: View? -> onClickNavigation() }
  }

  protected fun setUpToolbar(title: String?) {
    setUpToolbar(title, R.drawable.ic_arrow_back_white_24dp)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    val toolbar = toolbar
    ToolbarContentTintHelper
        .handleOnCreateOptionsMenu(this, toolbar, menu, getToolbarBackgroundColor(toolbar))
    return super.onCreateOptionsMenu(menu)
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    ToolbarContentTintHelper.handleOnPrepareOptionsMenu(this, toolbar)
    return super.onPrepareOptionsMenu(menu)
  }

  override fun setSupportActionBar(toolbar: Toolbar?) {
    this.toolbar = toolbar
    super.setSupportActionBar(toolbar)
  }

  protected open fun onClickNavigation() {
    finish()
  }

  companion object {
    fun getToolbarBackgroundColor(toolbar: Toolbar?): Int {
      return if (toolbar != null && toolbar.background is ColorDrawable) (toolbar.background as ColorDrawable).color else 0
    }
  }
}