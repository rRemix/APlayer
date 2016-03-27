package remix.myplayer.ui.customviews;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

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


    private int mActionCount = 0 ;
    private int mOriginX1 = 0;
    private int mOriginX2 = 0;
//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
////        Log.d("AudioViewPager","mIntercept:" + mIntercept);
//
//        String event = "";
//
//        mActionCount++;
//        switch (ev.getAction()){
//            case MotionEvent.ACTION_DOWN:
//                mActionCount = 1;
//                event = "ACTION_DOWN";
//                break;
//            case MotionEvent.ACTION_MOVE:
//                event = "ACTION_MOVE";
//                if(mActionCount == 2) {
//                    mActionCount = 0;
////                    return true;
//                }
//                break;
//            case MotionEvent.ACTION_UP:
//                event = "ACTION_UP";
//                if(mActionCount == 2){
////                    return false;
//                }
//                break;
//            case MotionEvent.ACTION_CANCEL:
//                event = "ACTION_CANCEL";
//                break;
//        }
//
//        boolean ret = super.onInterceptTouchEvent(ev);
//        Log.d("AudioViewPager","event: " + event);
//        Log.d("AudioViewPager","ret: " + ret);
//        return ret;
//
////        if(mIntercept && ev.getAction() == MotionEvent.ACTION_MOVE)
////            return true;
////        return super.onInterceptTouchEvent(ev);
//    }

    public void setIntercept(boolean value)
    {
        mIntercept = value;
    }


}
