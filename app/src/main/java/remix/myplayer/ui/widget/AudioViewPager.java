package remix.myplayer.ui.widget;

import android.content.Context;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;

/**
 * Created by taeja on 16-1-25.
 */
public class AudioViewPager extends ViewPager {

  boolean mIntercept = false;

  public AudioViewPager(Context context) {
    super(context);
  }

  public AudioViewPager(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

//    @Override
//    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        if(mIntercept && ev.getAction() == MotionEvent.ACTION_MOVE)
//            return true;
//        return super.onInterceptTouchEvent(ev);
//    }

  public void setIntercept(boolean value) {
    mIntercept = value;
  }


}
