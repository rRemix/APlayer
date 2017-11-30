package remix.myplayer.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.util.AttributeSet;
import android.view.View;

import remix.myplayer.APlayerApplication;
import remix.myplayer.util.DensityUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/1/17 09:16
 */

public class BottomBarBehavior extends CoordinatorLayout.Behavior<View> {
    private Context mContext;

    public BottomBarBehavior() {
        super();
        mContext = APlayerApplication.getContext();
    }

    public BottomBarBehavior(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, View child, View dependency) {
        return dependency instanceof AppBarLayout;
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, View child, View dependency) {
        float top = Math.abs(dependency.getTop());

        TypedArray ta =  mContext.obtainStyledAttributes(new int[]{android.R.attr.actionBarSize});
        int actionBarSize = ta.getDimensionPixelSize(0, 0);
        ta.recycle();
        if(actionBarSize > 0){
            int bottomBarSize = DensityUtil.dip2px(mContext,72);
            child.setTranslationY(top * bottomBarSize / actionBarSize);
            return true;
        } else {
            return false;
        }
    }
}
