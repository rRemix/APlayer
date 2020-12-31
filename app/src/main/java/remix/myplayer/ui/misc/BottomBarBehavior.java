package remix.myplayer.ui.misc;

import android.content.Context;
import android.content.res.TypedArray;
import com.google.android.material.appbar.AppBarLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;
import remix.myplayer.App;
import remix.myplayer.util.DensityUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/1/17 09:16
 */

public class BottomBarBehavior extends CoordinatorLayout.Behavior<View> {

  public BottomBarBehavior() {
    super();
  }

  public BottomBarBehavior(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
    return dependency instanceof AppBarLayout;
  }

  @Override
  public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
    final float top = Math.abs(dependency.getTop());
    final Context context = App.getContext();
    TypedArray ta = context.obtainStyledAttributes(new int[]{android.R.attr.actionBarSize});
    int actionBarSize = ta.getDimensionPixelSize(0, 0);
    ta.recycle();
    if (actionBarSize > 0) {
      int bottomBarSize = DensityUtil.dip2px(context, 72);
      child.setTranslationY(top * bottomBarSize / actionBarSize);
      return true;
    } else {
      return false;
    }
  }
}
