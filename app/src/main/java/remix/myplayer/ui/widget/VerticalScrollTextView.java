package remix.myplayer.ui.widget;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;

/**
 * 滚动换行
 */
public class VerticalScrollTextView extends AppCompatTextView {
    public static final int VERTICAL = 0;
    public static final int HORIZONTAL = 1;

    private static final int DURATION = 2000;

    private int mOrientation = VERTICAL;


    public VerticalScrollTextView(Context context) {
        super(context);
    }

    public VerticalScrollTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public VerticalScrollTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL && orientation != VERTICAL) {
            throw new IllegalArgumentException("Invalid orientation");
        }
        mOrientation = orientation;
    }

    public void setTextWithAnimation(@StringRes int textRes) {
        setTextWithAnimation(getResources().getString(textRes));
    }

    public void setTextWithAnimation(CharSequence text) {
        setText(text);
    }
}
