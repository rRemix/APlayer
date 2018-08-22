package remix.myplayer.ui.widget;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import remix.myplayer.R;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.StatusBarUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/11/11 14:10
 */

public class TipPopupwindow extends PopupWindow {
    private int mYOffset = 0;
    public TipPopupwindow(final Context context){
        if(context == null)
            return;

        setContentView(LayoutInflater.from(context).inflate(R.layout.popup_multi_tip,null));
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setBackgroundDrawable(new BitmapDrawable());
        setFocusable(false);
        setOutsideTouchable(true);

        mYOffset = StatusBarUtil.getStatusBarHeight(context) + DensityUtil.dip2px(context,48);
    }

    public void show(View parent){
        showAsDropDown(parent,0, mYOffset);
        //两秒钟后关闭
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dismiss();
            }
        }, 2000);
    }
}
