package remix.myplayer.ui;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

/**
 * Created by taeja on 16-1-25.
 */
public class AudioViewPager extends ViewPager{
    boolean mIntercept = false;
    public AudioViewPager(Context context) {
        super(context);
    }

    public AudioViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }



    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        Log.d("AudioViewPager","mIntercept:" + mIntercept);
        if(mIntercept && ev.getAction() == MotionEvent.ACTION_MOVE)
            return true;
        boolean ret = super.onInterceptTouchEvent(ev);
        String event = "";
        switch (ev.getAction()){
            case MotionEvent.ACTION_DOWN:
                event = "ACTION_DOWN";
                break;
            case MotionEvent.ACTION_MOVE:
                event = "ACTION_MOVE";
                break;
            case MotionEvent.ACTION_UP:
                event = "ACTION_UP";
                break;
            case MotionEvent.ACTION_CANCEL:
                event = "ACTION_CANCEL";
                break;
        }
        Log.d("AudioViewPager","event:" + event);
        Log.d("AudioViewPager","ret:" + ret);
        return ret;
    }

    public void setIntercept(boolean value)
    {
        mIntercept = value;
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent ev) {
//        return super.onTouchEvent(ev);
//    }
}
