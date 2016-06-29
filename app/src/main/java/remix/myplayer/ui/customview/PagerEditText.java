package remix.myplayer.ui.customview;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.EditText;

/**
 * Created by taeja on 16-3-16.
 */

/**
 * 自定义EdittText 嵌套与ViewPagrer
 */
public class PagerEditText extends EditText {

    public PagerEditText(Context context) {
        super(context);
    }

    public PagerEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PagerEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //ACTION_MOVE事件继续向上传递,否则ViewPager无法滑动
        if(event.getAction() == MotionEvent.ACTION_MOVE)
            return true;
        return super.onTouchEvent(event);
    }


}
