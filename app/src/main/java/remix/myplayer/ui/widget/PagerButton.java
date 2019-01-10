package remix.myplayer.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Button;

/**
 * Created by taeja on 16-3-18.
 */

/**
 * 自定义Button 主要用于嵌套与ViewPagrer
 */
public class PagerButton extends Button {

  public PagerButton(Context context) {
    super(context);
  }

  public PagerButton(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public PagerButton(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    //ACTION_MOVE事件继续向上传递,否则ViewPager无法滑动
    if (event.getAction() == MotionEvent.ACTION_MOVE) {
      return true;
    }
    return super.onTouchEvent(event);
  }
}
