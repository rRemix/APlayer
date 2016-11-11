package remix.myplayer.ui.customview;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.support.annotation.DrawableRes;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import remix.myplayer.R;
import remix.myplayer.application.Application;
import remix.myplayer.util.DensityUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/11/11 14:10
 */

public class TipPopupwindow extends PopupWindow {
    public TipPopupwindow(final Context context,final View parent, @DrawableRes int... resId){
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.HORIZONTAL);

        for(int i = 0 ; i < resId.length;i++){
            ImageView imageView = new ImageView(context);
            imageView.setImageResource(resId[i]);
            imageView.setBackgroundColor(Color.TRANSPARENT);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if(i == 1){
                lp.gravity = Gravity.CENTER_HORIZONTAL;
                lp.leftMargin = DensityUtil.dip2px(context,31);
                lp.rightMargin = DensityUtil.dip2px(context,18);
            } else if (i == 0){
                lp.leftMargin = DensityUtil.dip2px(context,60);
            } else {
                lp.leftMargin = DensityUtil.dip2px(context,240);
            }
            imageView.setLayoutParams(lp);

        }

        ImageView imageView = new ImageView(context);
        imageView.setImageResource(resId[0]);
        setContentView(LayoutInflater.from(context).inflate(R.layout.toolbar_multi_tip,null));
        setWidth(ViewGroup.LayoutParams.MATCH_PARENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
//        setBackgroundDrawable(new BitmapDrawable());
        setFocusable(false);
        setOutsideTouchable(true);


    }

    public void show(View parent){
//        showAsDropDown(parent);
        showAtLocation(parent,Gravity.TOP,0,DensityUtil.dip2px(Application.getContext(),56));
        //两秒钟后关闭
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dismiss();
            }
        }, 10000);
    }
}
