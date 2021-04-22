package remix.myplayer.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.viewpager.widget.ViewPager

/**
 * Created by taeja on 16-1-25.
 */
class AudioViewPager : ViewPager {
  var mIntercept = false

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

  //    @Override
  //    public boolean onInterceptTouchEvent(MotionEvent ev) {
  //        if(mIntercept && ev.getAction() == MotionEvent.ACTION_MOVE)
  //            return true;
  //        return super.onInterceptTouchEvent(ev);
  //    }
  fun setIntercept(value: Boolean) {
    mIntercept = value
  }
}