package remix.myplayer.ui.customviews;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.EditText;

/**
 * Created by taeja on 16-3-16.
 */
public class EditTest extends EditText {

    public EditTest(Context context) {
        super(context);
    }

    public EditTest(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTest(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        if(event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_UP){
//            return true;
//        }
//        else
//            return false;
        boolean ret = super.onTouchEvent(event);
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return super.dispatchTouchEvent(event);
    }
}
