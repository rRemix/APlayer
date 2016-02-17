package remix.myplayer.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import remix.myplayer.activities.AudioHolderActivity;
import remix.myplayer.services.MusicService;
import remix.myplayer.infos.LrcInfo;

/**
 * Created by Remix on 2015/12/8.
 */
public class LrcView extends TextView {

    public static LinkedList<LrcInfo> mlrcList;
    private Paint mPaint;
    private Paint mHPaint;
    private int mCurrent = 3;
    private int mInterval = 80;
    private int mX = 0;
    private int mY = 0;
    private int mMiddleY;
    public LrcView(Context context) {
        super(context);
    }

    public LrcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //mlrcMap = SearchLRC.lrcMap;
        mPaint = new Paint();
        mPaint.setColor(Color.BLACK);
        mPaint.setTextSize(30);
        mPaint.setTextAlign(Paint.Align.CENTER);
        mPaint.setAntiAlias(true);
        mPaint.setTypeface(Typeface.SERIF);
        mHPaint = new Paint();
        mHPaint.setColor(Color.WHITE);
        mHPaint.setTextSize(40);
        mHPaint.setTextAlign(Paint.Align.CENTER);
        mHPaint.setAntiAlias(true);
        mHPaint.setTypeface(Typeface.SANS_SERIF);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (AudioHolderActivity.mIsRun)
                    postInvalidate();
            }
        }, 0, 200);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if(mlrcList == null)
            return;

        mCurrent = getIndex(MusicService.getCurrentTime());
        mCurrent = SelectIndex(MusicService.getCurrentTime());
        if(mCurrent == -1)
            return;
        try
        {
            int i = 0;
            int tempY = mMiddleY;
            for(i = mCurrent - 1 ; i >= 0; i--)
            {
                tempY -= mInterval;
                if (tempY < 0) {
                    break;
                }
                canvas.drawText(mlrcList.get(i).getSentence(),mX,tempY,mPaint);
            }
            tempY = mMiddleY;
            canvas.drawText(mlrcList.get(mCurrent).getSentence(), mX, mMiddleY, mHPaint);
            for(i = mCurrent + 1; i < mlrcList.size(); i++)
            {
                tempY += mInterval;
                if(tempY > mY)
                    break;
                canvas.drawText(mlrcList.get(i).getSentence(), mX, tempY, mPaint);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }


    public int SelectIndex(int time)
    {
        int index=0;
        for(int i=0;i<mlrcList.size();i++)
        {
            LrcInfo temp = mlrcList.get(i);
            if(temp.getStartTime() < time)
                ++index;

        }
        index -= 1;
        if(index<0){
            index=0;
        }
        return index;

    }

    //根据当前播放时间获得索引
    private int getIndex(int desc)
    {
        int index = -1;
        int newDiff = Integer.MAX_VALUE;
        int oldDiff = Integer.MAX_VALUE;

//        Iterator it = mlrcMap.keySet().iterator();
//        int i = 0;
//        while(it.hasNext())
//        {
//            int cur = (int)it.next();
//            newDiff = Math.abs(desc - cur);
//            if(newDiff < oldDiff)
//                index = i;
//            i++;
//            oldDiff = newDiff;
//        }
        return index;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mX = (int)(w * 0.5);
        mY = h;
        mMiddleY = (int)(h * 0.5);
    }
}
