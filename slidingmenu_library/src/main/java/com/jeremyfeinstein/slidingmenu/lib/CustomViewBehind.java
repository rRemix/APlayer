package com.jeremyfeinstein.slidingmenu.lib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.CanvasTransformer;

public class CustomViewBehind extends ViewGroup {

	private static final String TAG = "CustomViewBehind";

	//边缘滑动的临界值
	private static final int MARGIN_THRESHOLD = 48; // dips
	
	//初始化触摸的模式
	private int mTouchMode = SlidingMenu.TOUCHMODE_MARGIN;

	//定义上方视图
	private CustomViewAbove mViewAbove;

	//定义内容视图
	private View mContent;
	private View mSecondaryContent;
	
	//定义滑动边缘的临界值
	private int mMarginThreshold;
	
	//宽度的偏移量
	private int mWidthOffset;
	
	private CanvasTransformer mTransformer;
	
	//是否能够使用子视图
	private boolean mChildrenEnabled;

	public CustomViewBehind(Context context) {
		this(context, null);
	}

	public CustomViewBehind(Context context, AttributeSet attrs) {
		super(context, attrs);
		mMarginThreshold = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 
				MARGIN_THRESHOLD, getResources().getDisplayMetrics());
	}

	public void setCustomViewAbove(CustomViewAbove customViewAbove) {
		mViewAbove = customViewAbove;
	}

	public void setCanvasTransformer(CanvasTransformer t) {
		mTransformer = t;
	}

	/**
	 * 设置宽度的偏移量
	 */
	public void setWidthOffset(int i) {
		mWidthOffset = i;
		requestLayout();
	}
	
	/**
	 * 设置边缘滑动的临界值
	 */
	public void setMarginThreshold(int marginThreshold) {
		mMarginThreshold = marginThreshold;
	}
	
	/**
	 * 得到边缘滑动的临界值
	 */
	public int getMarginThreshold() {
		return mMarginThreshold;
	}

	/**
	 * 得到视图的宽度
	 */
	public int getBehindWidth() {
		return mContent.getWidth();
	}

	/**
	 * 设置视图的内容
	 */
	public void setContent(View v) {
		if (mContent != null)
			removeView(mContent);
		mContent = v;
		addView(mContent);
	}

	/**
	 * 得到视图的内容
	 */
	public View getContent() {
		return mContent;
	}

	/**
	 * 设置右边滑动菜单的内容，当模式设置为LEFT_RIGHT模式时
	 */
	public void setSecondaryContent(View v) {
		if (mSecondaryContent != null)
			removeView(mSecondaryContent);
		mSecondaryContent = v;
		addView(mSecondaryContent);
	}

	/**
	 * 得到右边滑动菜单的内容
	 */
	public View getSecondaryContent() {
		return mSecondaryContent;
	}

	/**
	 * 设置是否能够使用子视图
	 */
	public void setChildrenEnabled(boolean enabled) {
		mChildrenEnabled = enabled;
	}

	@Override
	public void scrollTo(int x, int y) {
		super.scrollTo(x, y);
		if (mTransformer != null)
			invalidate();
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent e) {
		return !mChildrenEnabled;
	}

	@Override
	public boolean onTouchEvent(MotionEvent e) {
		return !mChildrenEnabled;
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		if (mTransformer != null) {
			canvas.save();
			mTransformer.transformCanvas(canvas, mViewAbove.getPercentOpen());
			super.dispatchDraw(canvas);
			canvas.restore();
		} else
			super.dispatchDraw(canvas);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final int width = r - l;
		final int height = b - t;
		mContent.layout(0, 0, width-mWidthOffset, height);
		if (mSecondaryContent != null)
			mSecondaryContent.layout(0, 0, width-mWidthOffset, height);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = getDefaultSize(0, widthMeasureSpec);
		int height = getDefaultSize(0, heightMeasureSpec);
		setMeasuredDimension(width, height);
		final int contentWidth = getChildMeasureSpec(widthMeasureSpec, 0, width-mWidthOffset);
		final int contentHeight = getChildMeasureSpec(heightMeasureSpec, 0, height);
		mContent.measure(contentWidth, contentHeight);
		if (mSecondaryContent != null)
			mSecondaryContent.measure(contentWidth, contentHeight);
	}

	//定义模式的值
	private int mMode;
	
	//是否能够使用渐入渐出效果
	private boolean mFadeEnabled;
	
	//定义渐入渐出的值
	private float mFadeDegree;
	
	//定义渐入渐出效果画笔
	private final Paint mFadePaint = new Paint();
	
	//定义滑动缩放的值
	private float mScrollScale;
	
	//定义滑动菜单的阴影
	private Drawable mShadowDrawable;
	
	//定义右边滑动菜单的阴影图片
	private Drawable mSecondaryShadowDrawable;
	
	//定义阴影的宽度
	private int mShadowWidth;
	
	/**
	 * 设置模式的值
	 */
	public void setMode(int mode) {
		if (mode == SlidingMenu.LEFT || mode == SlidingMenu.RIGHT) {
			if (mContent != null)
				mContent.setVisibility(View.VISIBLE);
			if (mSecondaryContent != null)
				mSecondaryContent.setVisibility(View.INVISIBLE);
		}
		mMode = mode;
	}

	/**
	 * 得到模式的值
	 */
	public int getMode() {
		return mMode;
	}

	/**
	 * 设置滑动缩放的值
	 */
	public void setScrollScale(float scrollScale) {
		mScrollScale = scrollScale;
	}

	/**
	 * 得到滑动缩放的值
	 */
	public float getScrollScale() {
		return mScrollScale;
	}

	/**
	 * 设置滑动菜单的阴影
	 */
	public void setShadowDrawable(Drawable shadow) {
		mShadowDrawable = shadow;
		invalidate();
	}

	/**
	 * 设置右边滑动菜单的阴影
	 */
	public void setSecondaryShadowDrawable(Drawable shadow) {
		mSecondaryShadowDrawable = shadow;
		invalidate();
	}

	/**
	 * 设置阴影的宽度
	 */
	public void setShadowWidth(int width) {
		mShadowWidth = width;
		invalidate();
	}

	/**
	 * 设置能否使用渐入渐出效果
	 */
	public void setFadeEnabled(boolean b) {
		mFadeEnabled = b;
	}

	/**
	 * 设置渐入渐出的值
	 */
	public void setFadeDegree(float degree) {
		if (degree > 1.0f || degree < 0.0f)
			throw new IllegalStateException("The BehindFadeDegree must be between 0.0f and 1.0f");
		mFadeDegree = degree;
	}

	/**
	 * 得到菜单页面
	 */
	public int getMenuPage(int page) {
		page = (page > 1) ? 2 : ((page < 1) ? 0 : page);
		if (mMode == SlidingMenu.LEFT && page > 1) {
			return 0;
		} else if (mMode == SlidingMenu.RIGHT && page < 1) {
			return 2;
		} else {
			return page;
		}
	}

	/**
	 * 滑动下方视图到达的位置
	 */
	public void scrollBehindTo(View content, int x, int y) {
		int vis = View.VISIBLE;		
		if (mMode == SlidingMenu.LEFT) {
			if (x >= content.getLeft()) vis = View.INVISIBLE;
			scrollTo((int)((x + getBehindWidth())*mScrollScale), y);
		} else if (mMode == SlidingMenu.RIGHT) {
			if (x <= content.getLeft()) vis = View.INVISIBLE;
			scrollTo((int)(getBehindWidth() - getWidth() + 
					(x-getBehindWidth())*mScrollScale), y);
		} else if (mMode == SlidingMenu.LEFT_RIGHT) {
			mContent.setVisibility(x >= content.getLeft() ? View.INVISIBLE : View.VISIBLE);
			mSecondaryContent.setVisibility(x <= content.getLeft() ? View.INVISIBLE : View.VISIBLE);
			vis = x == 0 ? View.INVISIBLE : View.VISIBLE;
			if (x <= content.getLeft()) {
				scrollTo((int)((x + getBehindWidth())*mScrollScale), y);				
			} else {
				scrollTo((int)(getBehindWidth() - getWidth() + 
						(x-getBehindWidth())*mScrollScale), y);				
			}
		}
		if (vis == View.INVISIBLE)
			Log.v(TAG, "behind INVISIBLE");
		setVisibility(vis);
	}

	/**
	 * 得到左边菜单的视图
	 */
	public int getMenuLeft(View content, int page) {
		if (mMode == SlidingMenu.LEFT) {
			switch (page) {
			case 0:
				return content.getLeft() - getBehindWidth();
			case 2:
				return content.getLeft();
			}
		} else if (mMode == SlidingMenu.RIGHT) {
			switch (page) {
			case 0:
				return content.getLeft();
			case 2:
				return content.getLeft() + getBehindWidth();	
			}
		} else if (mMode == SlidingMenu.LEFT_RIGHT) {
			switch (page) {
			case 0:
				return content.getLeft() - getBehindWidth();
			case 2:
				return content.getLeft() + getBehindWidth();
			}
		}
		return content.getLeft();
	}

	/**
	 * 得到左边框视图
	 */
	public int getAbsLeftBound(View content) {
		if (mMode == SlidingMenu.LEFT || mMode == SlidingMenu.LEFT_RIGHT) {
			return content.getLeft() - getBehindWidth();
		} else if (mMode == SlidingMenu.RIGHT) {
			return content.getLeft();
		}
		return 0;
	}

	/**
	 * 得到右边框视图
	 */
	public int getAbsRightBound(View content) {
		if (mMode == SlidingMenu.LEFT) {
			return content.getLeft();
		} else if (mMode == SlidingMenu.RIGHT || mMode == SlidingMenu.LEFT_RIGHT) {
			return content.getLeft() + getBehindWidth();
		}
		return 0;
	}

	/**
	 * 是否允许触摸屏幕的边缘
	 */
	public boolean marginTouchAllowed(View content, int x) {
		int left = content.getLeft();
		int right = content.getRight();
		if (mMode == SlidingMenu.LEFT) {
			return (x >= left && x <= mMarginThreshold + left);
		} else if (mMode == SlidingMenu.RIGHT) {
			return (x <= right && x >= right - mMarginThreshold);
		} else if (mMode == SlidingMenu.LEFT_RIGHT) {
			return (x >= left && x <= mMarginThreshold + left) || 
					(x <= right && x >= right - mMarginThreshold);
		}
		return false;
	}

	/**
	 * 设置触摸模式的值
	 */
	public void setTouchMode(int i) {
		mTouchMode = i;
	}

	/**
	 * 是否允许通过触摸打开滑动菜单
	 */
	public boolean menuOpenTouchAllowed(View content, int currPage, float x) {
		switch (mTouchMode) {
		case SlidingMenu.TOUCHMODE_FULLSCREEN:
			return true;
		case SlidingMenu.TOUCHMODE_MARGIN:
			return menuTouchInQuickReturn(content, currPage, x);
		}
		return false;
	}

	/**
	 * 滑动菜单快速返回
	 */
	public boolean menuTouchInQuickReturn(View content, int currPage, float x) {
		if (mMode == SlidingMenu.LEFT || (mMode == SlidingMenu.LEFT_RIGHT && currPage == 0)) {
			return x >= content.getLeft();
		} else if (mMode == SlidingMenu.RIGHT || (mMode == SlidingMenu.LEFT_RIGHT && currPage == 2)) {
			return x <= content.getRight();
		}
		return false;
	}

	/**
	 * 是否允许关闭滑动菜单
	 */
	public boolean menuClosedSlideAllowed(float dx) {
		if (mMode == SlidingMenu.LEFT) {
			return dx > 0;
		} else if (mMode == SlidingMenu.RIGHT) {
			return dx < 0;
		} else if (mMode == SlidingMenu.LEFT_RIGHT) {
			return true;
		}
		return false;
	}

	/**
	 * 是否允许打开滑动菜单
	 */
	public boolean menuOpenSlideAllowed(float dx) {
		if (mMode == SlidingMenu.LEFT) {
			return dx < 0;
		} else if (mMode == SlidingMenu.RIGHT) {
			return dx > 0;
		} else if (mMode == SlidingMenu.LEFT_RIGHT) {
			return true;
		}
		return false;
	}

	/**
	 * 画滑动菜单的阴影
	 */
	public void drawShadow(View content, Canvas canvas) {
		if (mShadowDrawable == null || mShadowWidth <= 0) return;
		int left = 0;
		if (mMode == SlidingMenu.LEFT) {
			left = content.getLeft() - mShadowWidth;
		} else if (mMode == SlidingMenu.RIGHT) {
			left = content.getRight();
		} else if (mMode == SlidingMenu.LEFT_RIGHT) {
			if (mSecondaryShadowDrawable != null) {
				left = content.getRight();
				mSecondaryShadowDrawable.setBounds(left, 0, left + mShadowWidth, getHeight());
				mSecondaryShadowDrawable.draw(canvas);
			}
			left = content.getLeft() - mShadowWidth;
		}
		mShadowDrawable.setBounds(left, 0, left + mShadowWidth, getHeight());
		mShadowDrawable.draw(canvas);
	}

	/**
	 * 画出渐入渐出效果
	 */
	public void drawFade(View content, Canvas canvas, float openPercent) {
		if (!mFadeEnabled) return;
		final int alpha = (int) (mFadeDegree * 255 * Math.abs(1-openPercent));
		mFadePaint.setColor(Color.argb(alpha, 0, 0, 0));
		int left = 0;
		int right = 0;
		if (mMode == SlidingMenu.LEFT) {
			left = content.getLeft() - getBehindWidth();
			right = content.getLeft();
		} else if (mMode == SlidingMenu.RIGHT) {
			left = content.getRight();
			right = content.getRight() + getBehindWidth();			
		} else if (mMode == SlidingMenu.LEFT_RIGHT) {
			left = content.getLeft() - getBehindWidth();
			right = content.getLeft();
			canvas.drawRect(left, 0, right, getHeight(), mFadePaint);
			left = content.getRight();
			right = content.getRight() + getBehindWidth();			
		}
		canvas.drawRect(left, 0, right, getHeight(), mFadePaint);
	}
	
	private boolean mSelectorEnabled = true;
	private Bitmap mSelectorDrawable;
	private View mSelectedView;
	
	public void drawSelector(View content, Canvas canvas, float openPercent) {
		if (!mSelectorEnabled) return;
		if (mSelectorDrawable != null && mSelectedView != null) {
			String tag = (String) mSelectedView.getTag(R.id.selected_view);
			if (tag.equals(TAG+"SelectedView")) {
				canvas.save();
				int left, right, offset;
				offset = (int) (mSelectorDrawable.getWidth() * openPercent);
				if (mMode == SlidingMenu.LEFT) {
					right = content.getLeft();
					left = right - offset;
					canvas.clipRect(left, 0, right, getHeight());
					canvas.drawBitmap(mSelectorDrawable, left, getSelectorTop(), null);		
				} else if (mMode == SlidingMenu.RIGHT) {
					left = content.getRight();
					right = left + offset;
					canvas.clipRect(left, 0, right, getHeight());
					canvas.drawBitmap(mSelectorDrawable, right - mSelectorDrawable.getWidth(), getSelectorTop(), null);
				}
				canvas.restore();
			}
		}
	}
	
	public void setSelectorEnabled(boolean b) {
		mSelectorEnabled = b;
	}

	public void setSelectedView(View v) {
		if (mSelectedView != null) {
			mSelectedView.setTag(R.id.selected_view, null);
			mSelectedView = null;
		}
		if (v != null && v.getParent() != null) {
			mSelectedView = v;
			mSelectedView.setTag(R.id.selected_view, TAG+"SelectedView");
			invalidate();
		}
	}

	private int getSelectorTop() {
		int y = mSelectedView.getTop();
		y += (mSelectedView.getHeight() - mSelectorDrawable.getHeight()) / 2;
		return y;
	}

	public void setSelectorBitmap(Bitmap b) {
		mSelectorDrawable = b;
		refreshDrawableState();
	}

}
