package remix.myplayer.ui.customview;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

import remix.myplayer.R;
import remix.myplayer.util.LogUtil;

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

        for(int i = 0 ; i < getChildCount();i++){
            View child = getChildAt(i);
            if(child.getId() == R.id.item_container){
                mContainer = (RelativeLayout) child;
                mMoreButton = (ImageButton) mContainer.findViewById(R.id.item_button);
            }
//            if(child.getID() == R.id.recycleview_card && child instanceof Button){
//                mCardBg = (Button) child;
//            }
        }

        if(mContainer == null || mMoreButton == null || mCardBg == null){
            return super.dispatchTouchEvent(ev);
        }

        //判断点击在哪个按钮上
        if(ev.getAction() == KeyEvent.ACTION_DOWN){
            //判断是否点击在更多按钮上
            Rect rect = new Rect();
            mMoreButton.getHitRect(rect);
            rect.top += getPaddingTop();
            rect.bottom += getPaddingTop();
            rect.left += getPaddingLeft();
            rect.right += getPaddingLeft();
            mTargetView = null;
            mTargetView = rect.contains((int) ev.getX(), (int) ev.getY()) ? mMoreButton : mContainer;
        }

        if(mTargetView == null)
            return super.dispatchTouchEvent(ev);

        LogUtil.d("CardFrameLayout","left:" + mTargetView.getLeft() + " top:" + mTargetView.getTop());
        if(mTargetView instanceof RelativeLayout) {
            mCardBg.dispatchTouchEvent(ev);
            ev.setLocation(ev.getX() - mTargetView.getLeft(),ev.getY() - mTargetView.getTop());
            return mTargetView.dispatchTouchEvent(ev);
        } else {
            final float targetX = ev.getX() - mTargetView.getLeft();
            final float targetY = ev.getY() - mTargetView.getTop();
            ev.setLocation(targetX,targetY);
            return mTargetView.dispatchTouchEvent(ev);
        }

    }
}
