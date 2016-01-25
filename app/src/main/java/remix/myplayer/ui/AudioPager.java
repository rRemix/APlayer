package remix.myplayer.ui;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by taeja on 16-1-25.
 */
public class AudioPager extends ViewPager{
    boolean mIntercept = false;
    public AudioPager(Context context) {
        super(context);
    }

    public AudioPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }



    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(mIntercept && ev.getAction() == MotionEvent.ACTION_MOVE)
            return true;
        return super.onInterceptTouchEvent(ev);
    }

    public void setIntercept(boolean value)
    {
        mIntercept = value;
    }

}
