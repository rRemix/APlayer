package remix.myplayer.ui.customview;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
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


    private View mTargetView = null;
    private RelativeLayout mContainer;
    private ImageButton mMoreButton;
    private Button mCardBg;
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int paddingTop = getPaddingTop();
        int paddingLeft = getPaddingLeft();

        for(int i = 0 ; i < getChildCount();i++){
            View child = getChildAt(i);
            if(child.getId() == R.id.item_container){
                mContainer = (RelativeLayout) child;
                mMoreButton = (ImageButton) mContainer.findViewById(R.id.recycleview_button);
            }
            if(child.getId() == R.id.recycleview_card && child instanceof Button){
                mCardBg = (Button) child;
            }
        }

        if(mContainer == null || mMoreButton == null || mCardBg == null){
            return super.dispatchTouchEvent(ev);
        }

        //判断点击在哪个按钮上
        if(ev.getAction() == KeyEvent.ACTION_DOWN){
            //判断是否点击在更多按钮上
            Rect rect = new Rect();
            mMoreButton.getHitRect(rect);
            rect.top += paddingTop;
            rect.bottom += paddingTop;
            rect.left += paddingLeft;
            rect.right += paddingLeft;
            mTargetView = null;
            mTargetView = rect.contains((int) ev.getX(), (int) ev.getY()) ? mMoreButton : mContainer;
        }

        if(mTargetView == null)
            return super.dispatchTouchEvent(ev);

        if(mTargetView instanceof RelativeLayout) {
            mCardBg.dispatchTouchEvent(ev);
            ev.setLocation(ev.getX() - mTargetView.getLeft(),ev.getY() - mTargetView.getTop());
            return mTargetView.dispatchTouchEvent(ev);
        } else {
            final float targetX = ev.getX() - paddingLeft - mTargetView.getLeft();
            final float targetY = ev.getY() - paddingTop - mTargetView.getTop();
            ev.setLocation(targetX,targetY);
            return mTargetView.dispatchTouchEvent(ev);
        }

    }
}
