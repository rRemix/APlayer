package remix.myplayer.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.support.annotation.ColorInt;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import remix.myplayer.R;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.DensityUtil;

/**
 * Created by taeja on 16-6-23.
 */
public class ListItemDecoration extends RecyclerView.ItemDecoration {
    private Context mContext;

    public static final int HORIZONTAL_LIST = LinearLayoutManager.HORIZONTAL;

    public static final int VERTICAL_LIST = LinearLayoutManager.VERTICAL;

    private InsetDrawable mDivider;
    private GradientDrawable mContentDrawable;
    private int mOrientation;

    public ListItemDecoration(Context context, int orientation) {
        mContext = context;
        mContentDrawable = (GradientDrawable) mContext.getResources().getDrawable(R.drawable.bg_divider_day);
        mContentDrawable.setColor(ThemeStore.getDividerColor());
        mDivider = new InsetDrawable(mContentDrawable, DensityUtil.dip2px(context, 4), 0, 0, 0);
        setOrientation(orientation);
    }

    public ListItemDecoration(Context context, int orientation, int insetLeft) {
        mContext = context;
        mContentDrawable = (GradientDrawable) mContext.getResources().getDrawable(R.drawable.bg_divider_day).mutate();
        mContentDrawable.setColor(ThemeStore.getDividerColor());
        mDivider = new InsetDrawable(mContentDrawable, insetLeft, 0, 0, 0);
        setOrientation(orientation);
    }

    public void setDividerColor(@ColorInt int color) {
        mContentDrawable.setColor(color);
    }

    public void setOrientation(int orientation) {
        if (orientation != HORIZONTAL_LIST && orientation != VERTICAL_LIST) {
            throw new IllegalArgumentException("invalid orientation");
        }
        mOrientation = orientation;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (mOrientation == VERTICAL_LIST) {
            drawVertical(c, parent);
        } else {
            drawHorizontal(c, parent);
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
        } else {
            outRect.set(0, 0, mDivider.getIntrinsicWidth(), 0);
        }
    }


}
