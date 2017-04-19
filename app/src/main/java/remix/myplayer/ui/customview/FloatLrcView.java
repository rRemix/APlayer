package remix.myplayer.ui.customview;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.WindowManager;

import remix.myplayer.R;
import remix.myplayer.util.ColorUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/3/22 15:47
 */

public class FloatLrcView extends android.support.v7.widget.AppCompatTextView {
    private WindowManager mWindowManager;
    private Context mContext;

    public FloatLrcView(Context context) {
        super(context);
        init(context);
    }

    public FloatLrcView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FloatLrcView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        setBackgroundColor(Color.TRANSPARENT);
        setTextSize(20);
        setTextColor(ColorUtil.getColor(R.color.md_blue_primary));
        setEllipsize(TextUtils.TruncateAt.END);
        setGravity(Gravity.CENTER);
        setLineSpacing(1,1.2f);
        setMaxLines(2);
        mWindowManager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        switch (event.getAction()){
//            case MotionEvent.ACTION_DOWN:
//                break;
//            case MotionEvent.ACTION_MOVE:
//                WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
//                params.y = (int) (event.getRawY() - getHeight() / 2);
//                mWindowManager.updateViewLayout(this,params);
//                break;
//            case MotionEvent.ACTION_UP:
//                params = (WindowManager.LayoutParams) getLayoutParams();
//                if(params != null)
//                    SPUtil.putValue(mContext,"Setting","FloatY",params.y);
//                break;
//        }
//        return true;
//    }
}
