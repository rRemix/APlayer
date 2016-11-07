package remix.myplayer.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.ColorInt;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * @ClassName GridItemDecoration
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/8 14:33
 */
public class GridItemDecoration extends RecyclerView.ItemDecoration{
    private Paint mPaint;
    private Context mContext;
    public GridItemDecoration(Context context, int width, @ColorInt int color){
        mContext = context;
        mPaint = new Paint();
        mPaint.setColor(color);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(width);
        mPaint.setStyle(Paint.Style.STROKE);
    }
    public GridItemDecoration(){

    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
//        super.getItemOffsets(outRect, view, parent, state);
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
//        for(int i = 0 ; i < parent.getChildCount();i++){
//            final View child = parent.getChildAt(i);
//            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
//                    .getLayoutParams();
//            c.drawRect(child.getLeft(),child.getTop(),child.getRight(),child.getBottom(),mPaint);
//        }
    }
}
