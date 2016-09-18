package remix.myplayer.ui.customview;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import remix.myplayer.R;
import remix.myplayer.application.Application;
import remix.myplayer.util.DensityUtil;

/**
 * Created by Remix on 2016/9/18.
 */
public class CardFrameLayout extends FrameLayout {

    public CardFrameLayout(Context context) {
        super(context);
    }

    public CardFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CardFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean ret = super.onInterceptTouchEvent(ev);
        return ret;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean ret = super.onTouchEvent(event);
        return ret;
    }

    private static boolean test = false;
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        ImageButton imageButton = null;
        Button cardButton = null;
        RelativeLayout container = null;
        for(int i = 0 ; i < getChildCount();i++){
            View child = getChildAt(i);
            if(child.getId() == R.id.item_container){
                container = (RelativeLayout) child;
                imageButton = (ImageButton) container.findViewById(R.id.recycleview_button);
            }
            if(child.getId() == R.id.recycleview_card && child instanceof Button){
                cardButton = (Button) child;
            }
        }

        //判断是否点击在更多按钮上
        if(imageButton != null) {
            Rect rect = new Rect();
            imageButton.getHitRect(rect);
            int dy = DensityUtil.dip2px(Application.getContext(),164.0f);
            rect.top = dy;
            rect.bottom += dy;
            if (rect.contains((int) ev.getX(), (int) ev.getY())) {
                return imageButton.dispatchTouchEvent(ev);
            }
        }
        //正常处理
        if(cardButton != null){
            cardButton.dispatchTouchEvent(ev);
        }
        if(container != null){
            container.dispatchTouchEvent(ev);
        }
        return true;
//        return super.dispatchTouchEvent(ev);
    }
}
