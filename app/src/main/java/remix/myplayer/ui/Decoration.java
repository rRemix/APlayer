package remix.myplayer.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import remix.myplayer.R;
import remix.myplayer.theme.ThemeUtil;
import remix.myplayer.util.DensityUtil;

/**
 * Created by taeja on 16-6-23.
 */
public class Decoration extends RecyclerView.ItemDecoration {
    private Context mContext;

    public static final int VERTICAL_LIST = 1;
    public static final int GRID_LIST = 2;

    private Drawable mDivider;
    private int mOrientation;

    public Decoration(Context context) {
        mContext = context;
        mDivider = context.getResources().getDrawable(R.drawable.bg_divider_grid);
        setOrientation(GRID_LIST);
    }

    public Decoration(Context context, float insetLeftInDp) {
        mContext = context;
        GradientDrawable contentDrawable = (GradientDrawable) mContext.getResources().getDrawable(R.drawable.bg_divider_list).mutate();
        contentDrawable.setColor(ThemeUtil.resolveColor(context, R.attr.divider_color));
        mDivider = new InsetDrawable(contentDrawable, DensityUtil.dip2px(insetLeftInDp), 0, 0, 0);
        setOrientation(VERTICAL_LIST);
    }


    public void setOrientation(int orientation) {
        if (orientation != VERTICAL_LIST && orientation != GRID_LIST) {
            throw new IllegalArgumentException("invalid orientation");
        }
        mOrientation = orientation;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (mOrientation == VERTICAL_LIST) {
            drawVertical(c, parent);
        } else if (mOrientation == GRID_LIST) {
            drawHorizontal(c, parent);
            drawVertical(c, parent);
        }
    }

    public void drawVertical(Canvas c, RecyclerView parent) {
        final int left = parent.getPaddingLeft();
        final int right = parent.getWidth() - parent.getPaddingRight();

        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                    .getLayoutParams();
            final int top = child.getBottom() + params.bottomMargin;
            final int bottom = top + mDivider.getIntrinsicHeight();
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }
    }

    public void drawHorizontal(Canvas c, RecyclerView parent) {
        final int top = parent.getPaddingTop();
        final int bottom = parent.getHeight() - parent.getPaddingBottom();

        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child
                    .getLayoutParams();
            final int left = child.getRight() + params.rightMargin;
            final int right = left + mDivider.getIntrinsicHeight();
            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (mOrientation == VERTICAL_LIST) {
            outRect.set(0, 0, 0, mDivider.getIntrinsicHeight());
        } else if (mOrientation == GRID_LIST) {
            outRect.set(0, 0, mDivider.getIntrinsicWidth(), mDivider.getIntrinsicHeight());
        }
    }


}
