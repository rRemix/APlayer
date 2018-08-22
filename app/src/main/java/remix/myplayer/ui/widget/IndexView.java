package remix.myplayer.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

import remix.myplayer.util.DensityUtil;

/**
 * Created by taeja on 16-6-29.
 */
public class IndexView extends View {
    private static final String TAG = IndexView.class.getSimpleName();
    private int mViewHeight;
    private int mViewWidth;
    private Paint mIndexPaint;
    private Paint mBgPaint;
    private int mTextSize;
    private ArrayList<Integer> mPosList = new ArrayList<>();
    private char mLetter;
    private OnLetterChangedListener mListener;

    public IndexView(Context context) {
        super(context);
        init(null);
    }

    public IndexView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public IndexView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        mViewHeight = getMeasuredHeight();
        mViewWidth = getMeasuredWidth();

        //所有字母
        for(int i = 'A' ; i < 'Z' + 1 ;i++) {
            int y = (i - '@') * (mTextSize + DensityUtil.dip2px(getContext(),2));
            canvas.drawText(String.valueOf((char)i),mViewWidth / 2,y,mIndexPaint);
            mPosList.add(y);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        int temp = Integer.MAX_VALUE;
        for(int i = 0 ; i < mPosList.size() ;i++){
            if(Math.abs(mPosList.get(i) - event.getY()) < temp){
                mLetter = (char)(i + 'A');
                temp = Math.abs((int)(mPosList.get(i) - event.getY()));
            }
        }
        seekTo();
        return true;
    }

//    @Override
//    public boolean onTouchEvent(MotionEvent event) {
//        int temp = Integer.MAX_VALUE;
//        for(int i = 0 ; i < mPosList.size() ;i++){
//            if(Math.abs(mPosList.get(i) - event.getY()) < temp){
//                mLetter = (char)(i + 'A');
//                temp = Math.abs((int)(mPosList.get(i) - event.getY()));
//            }
//        }
//        seekTo();
//
//        return true;
//    }

    public void seekTo(){
        if(mListener != null){
            mListener.onLetterChanged(mLetter);
        }
    }

    public void setPositionChangedListener(OnLetterChangedListener listener){
        mListener = listener;
    }

    private void init(AttributeSet attributeSet){
        mTextSize = DensityUtil.dip2px(getContext(),12);
        mIndexPaint = new Paint();
        mIndexPaint.setColor(Color.GRAY);
        mIndexPaint.setAntiAlias(true);
        mIndexPaint.setStyle(Paint.Style.STROKE);
        mIndexPaint.setTextSize(mTextSize);
        mIndexPaint.setTextAlign(Paint.Align.CENTER);

        mBgPaint = new Paint();
        mBgPaint.setColor(Color.TRANSPARENT);
    }

    public interface OnLetterChangedListener {
        void onLetterChanged(char c);
    }

}
