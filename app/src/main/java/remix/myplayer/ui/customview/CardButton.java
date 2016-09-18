package remix.myplayer.ui.customview;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Button;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/9/18 17:16
 */
public class CardButton extends Button {
    public CardButton(Context context) {
        super(context);
    }

    public CardButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CardButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CardButton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

}
