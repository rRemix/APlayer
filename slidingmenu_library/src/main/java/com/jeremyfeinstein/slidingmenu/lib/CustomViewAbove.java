package com.jeremyfeinstein.slidingmenu.lib;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.view.KeyEventCompat;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.VelocityTrackerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewConfigurationCompat;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.FocusFinder;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnClosedListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenedListener;


public class CustomViewAbove extends ViewGroup {

	private static final String TAG = "CustomViewAbove";
	private static final boolean DEBUG = false;

	//是否使用缓存
	private static final boolean USE_CACHE = false;

	//最大持续的时间
	private static final int MAX_SETTLE_DURATION = 600; // ms
	
	//最小滑动的距离
	private static final int MIN_DISTANCE_FOR_FLING = 25; // dips

	/**
	 * 定义一个修饰动画的效果类
	 * Interpolator被用来修饰动画效果，定义动画的变化率，可以使存在的动画效果可以 accelerated(加速)，decelerated(减速),repeated(重复),bounced(弹跳)等。
	 */
	private static final Interpolator sInterpolator = new Interpolator() {
		public float getInterpolation(float t) {
			t -= 1.0f;
			return t * t * t * t * t + 1.0f;
		}
	};

	//定义内容视图
	private View mContent;

	//当前的选项
	private int mCurItem;
	
	//滚动滑轮
	private Scroller mScroller;

	//是否能够使用滑动缓存
	private boolean mScrollingCacheEnabled;

	//是否正在滑动
	private boolean mScrolling;

	//是否正在拖动
	private boolean mIsBeingDragged;
	
	//是否能够拖动
	private boolean mIsUnableToDrag;
	
	//定义触摸溢出的值
	private int mTouchSlop;
	
	//初始化触摸屏幕X轴的值
	private float mInitialMotionX;
	
	//最后移动到的X、Y的坐标
	private float mLastMotionX,mLastMotionY;
	
	/**
	 * 定义一个活动指针，在多点触摸的时候调用
	 */
	protected int mActivePointerId = INVALID_POINTER;
	
	/**
	 * 为当前的活动指针赋值
	 */
	private static final int INVALID_POINTER = -1;

	/**
	 * 触摸滚动期间的绝对速度
	 */
	protected VelocityTracker mVelocityTracker;
	
	//最小滑动速度值
	private int mMinimumVelocity;
	
	//最大滑动速度值
	protected int mMaximumVelocity;
	
	//滑动的距离
	private int mFlingDistance;

	//定义下方视图对象
	private CustomViewBehind mViewBehind;

	//是否能够使用
	private boolean mEnabled = true;

	//页面改变监听器
	private OnPageChangeListener mOnPageChangeListener;
	
	//内部页面改变监听器
	private OnPageChangeListener mInternalPageChangeListener;

	//关闭监听器
	private OnClosedListener mClosedListener;
	
	//打开监听器
	private OnOpenedListener mOpenedListener;

	//存放被忽略的视图组件列表
	private List<View> mIgnoredViews = new ArrayList<View>();

	/**
	 * 调用此接口去响应改变选中页面的状态
	 */
	public interface OnPageChangeListener {

		/**
		 * This method will be invoked when the current page is scrolled, either as part
		 * of a programmatically initiated smooth scroll or a user initiated touch scroll.
		 *
		 * @param position Position index of the first page currently being displayed.
		 *                 Page position+1 will be visible if positionOffset is nonzero.
		 * @param positionOffset Value from [0, 1) indicating the offset from the page at position.
		 * @param positionOffsetPixels Value in pixels indicating the offset from position.
		 */
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels);

		/**
		 * This method will be invoked when a new page becomes selected. Animation is not
		 * necessarily complete.
		 *
		 * @param position Position index of the new selected page.
		 */
		public void onPageSelected(int position);

	}

	/**
	 * Simple implementation of the {@link OnPageChangeListener} interface with stub
	 * implementations of each method. Extend this if you do not intend to override
	 * every method of {@link OnPageChangeListener}.
	 */
	public static class SimpleOnPageChangeListener implements OnPageChangeListener {

		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			// This space for rent
		}

		public void onPageSelected(int position) {
			// This space for rent
		}

		public void onPageScrollStateChanged(int state) {
			// This space for rent
		}

	}

	public CustomViewAbove(Context context) {
		this(context, null);
	}

	public CustomViewAbove(Context context, AttributeSet attrs) {
		super(context, attrs);
		initCustomViewAbove();
	}

	/**
	 * 初始化最上方视图
	 */
	void initCustomViewAbove() {
		//设置是否能够调用自定义的布局，false是可以
		setWillNotDraw(false);
		//优先其子类控件而获取到焦点
		setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
		//设置是否能够获取焦点
		setFocusable(true);
		
		//得到上下文
		final Context context = getContext();
		
		//实例化滚动器
		mScroller = new Scroller(context, sInterpolator);
		
		final ViewConfiguration configuration = ViewConfiguration.get(context);
		
		//获得能够进行手势滑动的距离，表示滑动的时候，手的移动要大于这个距离才开始移动控件
		mTouchSlop = ViewConfigurationCompat.getScaledPagingTouchSlop(configuration);
		
		//获得允许执行一个fling手势动作的最小速度值
		mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
		
		//获得允许执行一个fling手势动作的最大速度值
		mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();
		
		setInternalPageChangeListener(new SimpleOnPageChangeListener() {
			public void onPageSelected(int position) {
				if (mViewBehind != null) {
					switch (position) {
					case 0:
					case 2:
						mViewBehind.setChildrenEnabled(true);
						break;
					case 1:
						mViewBehind.setChildrenEnabled(false);
						break;
					}
				}
			}
		});
		//获得该手机设备的屏幕密度值
		final float density = context.getResources().getDisplayMetrics().density;
		//滑动的距离
		mFlingDistance = (int) (MIN_DISTANCE_FOR_FLING * density);
	}

	/**
	 * 设置当前选中的项
	 */
	public void setCurrentItem(int item) {
		setCurrentItemInternal(item, true, false);
	}

	/**
	 * 设置当前选中的项，是否平滑的过渡到选中项的页面
	 */
	public void setCurrentItem(int item, boolean smoothScroll) {
		setCurrentItemInternal(item, smoothScroll, false);
	}

	/**
	 * 得到当前选中的项
	 */
	public int getCurrentItem() {
		return mCurItem;
	}

	/**
	 * 设置当前内部选中的项
	 */
	void setCurrentItemInternal(int item, boolean smoothScroll, boolean always) {
		setCurrentItemInternal(item, smoothScroll, always, 0);
	}

	/**
	 * 设置当前内部选中的项
	 */
	void setCurrentItemInternal(int item, boolean smoothScroll, boolean always, int velocity) {
		if (!always && mCurItem == item) {
			setScrollingCacheEnabled(false);
			return;
		}

		item = mViewBehind.getMenuPage(item);

		final boolean dispatchSelected = mCurItem != item;
		mCurItem = item;
		final int destX = getDestScrollX(mCurItem);
		if (dispatchSelected && mOnPageChangeListener != null) {
			mOnPageChangeListener.onPageSelected(item);
		}
		if (dispatchSelected && mInternalPageChangeListener != null) {
			mInternalPageChangeListener.onPageSelected(item);
		}
		if (smoothScroll) {
			smoothScrollTo(destX, 0, velocity);
		} else {
			completeScroll();
			scrollTo(destX, 0);
		}
	}

	/**
	 * 设置一个监听事件当页面改变或者加速滚动的时候调用
	 */
	public void setOnPageChangeListener(OnPageChangeListener listener) {
		mOnPageChangeListener = listener;
	}

	/**
	 * 设置打开监听事件
	 */
	public void setOnOpenedListener(OnOpenedListener l) {
		mOpenedListener = l;
	}

	/**
	 * 设置关闭监听事件
	 */
	public void setOnClosedListener(OnClosedListener l) {
		mClosedListener = l;
	}

	/**
	 * Set a separate OnPageChangeListener for internal use by the support library.
	 *
	 * @param listener Listener to set
	 * @return The old listener that was set, if any.
	 */
	OnPageChangeListener setInternalPageChangeListener(OnPageChangeListener listener) {
		OnPageChangeListener oldListener = mInternalPageChangeListener;
		mInternalPageChangeListener = listener;
		return oldListener;
	}

	/**
	 * 添加被忽略的组件
	 */
	public void addIgnoredView(View v) {
		if (!mIgnoredViews.contains(v)) {
			mIgnoredViews.add(v);
		}
	}

	/**
	 * 移除被忽略的组件
	 */
	public void removeIgnoredView(View v) {
		mIgnoredViews.remove(v);
	}

	/**
	 * 清空被忽略的组件
	 */
	public void clearIgnoredViews() {
		mIgnoredViews.clear();
	}

	// We want the duration of the page snap animation to be influenced by the distance that
	// the screen has to travel, however, we don't want this duration to be effected in a
	// purely linear fashion. Instead, we use this method to moderate the effect that the distance
	// of travel has on the overall snap duration.
	float distanceInfluenceForSnapDuration(float f) {
		f -= 0.5f; // center the values about 0.
		f *= 0.3f * Math.PI / 2.0f;
		return (float) FloatMath.sin(f);
	}

	/**
	 * 得到滑动到的X轴的坐标
	 */
	public int getDestScrollX(int page) {
		switch (page) {
		case 0:
		case 2:
			return mViewBehind.getMenuLeft(mContent, page);
		case 1:
			return mContent.getLeft();
		}
		return 0;
	}

	/**
	 * 得到左边框
	 */
	private int getLeftBound() {
		return mViewBehind.getAbsLeftBound(mContent);
	}

	/**
	 * 得到右边框
	 */
	private int getRightBound() {
		return mViewBehind.getAbsRightBound(mContent);
	}

	public int getContentLeft() {
		return mContent.getLeft() + mContent.getPaddingLeft();
	}

	/**
	 * 得到滑动菜单是否打开
	 */
	public boolean isMenuOpen() {
		return mCurItem == 0 || mCurItem == 2;
	}

	/**
	 * 是否忽略视图
	 */
	private boolean isInIgnoredView(MotionEvent ev) {
		Rect rect = new Rect();
		for (View v : mIgnoredViews) {
			v.getHitRect(rect);
			if (rect.contains((int)ev.getX(), (int)ev.getY())) return true;
		}
		return false;
	}

	/**
	 * 得到下方视图的宽度
	 */
	public int getBehindWidth() {
		if (mViewBehind == null) {
			return 0;
		} else {
			return mViewBehind.getBehindWidth();
		}
	}

	/**
	 * 得到子控件的宽度
	 */
	public int getChildWidth(int i) {
		switch (i) {
		case 0:
			return getBehindWidth();
		case 1:
			return mContent.getWidth();
		default:
			return 0;
		}
	}

	/**
	 * 得到是否能够滑动
	 */
	public boolean isSlidingEnabled() {
		return mEnabled;
	}

	/**
	 * 设置是否能够滑动
	 */
	public void setSlidingEnabled(boolean b) {
		mEnabled = b;
	}

	/**
	 * 平滑的滑动到指定的位置
	 */
	void smoothScrollTo(int x, int y) {
		smoothScrollTo(x, y, 0);
	}

	/**
	 * 通过设置速度来平滑的滑动到指定的位置
	 */
	void smoothScrollTo(int x, int y, int velocity) {
		if (getChildCount() == 0) {
			// Nothing to do.
			setScrollingCacheEnabled(false);
			return;
		}
		//获得当前View显示部分的左边到第一个View的左边的距离
		int sx = getScrollX();
		int sy = getScrollY();
		
		int dx = x - sx;
		int dy = y - sy;
		
		//如果都等于0，说明正好是滑动了一个屏幕的距离
		if (dx == 0 && dy == 0) {
			completeScroll();
			if (isMenuOpen()) {
				if (mOpenedListener != null)
					mOpenedListener.onOpened();
			} else {
				if (mClosedListener != null)
					mClosedListener.onClosed();
			}
			return;
		}

		setScrollingCacheEnabled(true);
		mScrolling = true;

		//获得下方视图的宽度
		final int width = getBehindWidth();
		
		final int halfWidth = width / 2;
		
		//取两数中最小的值赋给滑动距离与下方视图宽度的比值
		final float distanceRatio = Math.min(1f, 1.0f * Math.abs(dx) / width);
		
		//获得当前滑动的距离
		final float distance = halfWidth + halfWidth * distanceInfluenceForSnapDuration(distanceRatio);

		//初始化持续的时间
		int duration = 0;
		
		//获得速度的绝对值
		velocity = Math.abs(velocity);
		
		if (velocity > 0) {
			//Math.round()四舍五入
			duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
		} else {
			final float pageDelta = (float) Math.abs(dx) / width;
			duration = (int) ((pageDelta + 1) * 100);
			duration = MAX_SETTLE_DURATION;
		}
		//取两数中最小的一个值赋给持续的时间
		duration = Math.min(duration, MAX_SETTLE_DURATION);

		//开始滑动
		mScroller.startScroll(sx, sy, dx, dy, duration);
		
		//刷新界面
		invalidate();
	}

	/**
	 * 设置内容视图
	 */
	public void setContent(View v) {
		if (mContent != null) 
			this.removeView(mContent);
		mContent = v;
		addView(mContent);
	}

	/**
	 * 得到内容视图
	 */
	public View getContent() {
		return mContent;
	}

	/**
	 * 设置下方视图
	 */
	public void setCustomViewBehind(CustomViewBehind cvb) {
		mViewBehind = cvb;
	}

	/**
	 * 在父元素正要放置该控件时调用。它会问一个问题，“你想要用多大地方啊？”，然后传入两个参数——widthMeasureSpec和heightMeasureSpec。
	 */
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = getDefaultSize(0, widthMeasureSpec);
		int height = getDefaultSize(0, heightMeasureSpec);
		setMeasuredDimension(width, height);

		final int contentWidth = getChildMeasureSpec(widthMeasureSpec, 0, width);
		final int contentHeight = getChildMeasureSpec(heightMeasureSpec, 0, height);
		mContent.measure(contentWidth, contentHeight);
	}

	/**
	 * 当视图尺寸改变的时候调用
	 */
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		// Make sure scroll position is set correctly.
		if (w != oldw) {
			// [ChrisJ] - This fixes the onConfiguration change for orientation issue..
			// maybe worth having a look why the recomputeScroll pos is screwing
			// up?
			completeScroll();
			scrollTo(getDestScrollX(mCurItem), getScrollY());
		}
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		final int width = r - l;
		final int height = b - t;
		mContent.layout(0, 0, width, height);
	}

	/**
	 * 设置上方视图的偏移量
	 */
	public void setAboveOffset(int i) {		
		mContent.setPadding(i, mContent.getPaddingTop(), mContent.getPaddingRight(), mContent.getPaddingBottom());
	}


	@Override
	public void computeScroll() {
		if (!mScroller.isFinished()) {
			if (mScroller.computeScrollOffset()) {
				int oldX = getScrollX();
				int oldY = getScrollY();
				int x = mScroller.getCurrX();
				int y = mScroller.getCurrY();

				if (oldX != x || oldY != y) {
					scrollTo(x, y);
					pageScrolled(x);
				}

				// Keep on drawing until the animation has finished.
				invalidate();
				return;
			}
		}

		//完成滑动，清除状态
		completeScroll();
	}

	/**
	 * 页面滚动
	 */
	private void pageScrolled(int xpos) {
		final int widthWithMargin = getWidth();
		final int position = xpos / widthWithMargin;
		final int offsetPixels = xpos % widthWithMargin;
		final float offset = (float) offsetPixels / widthWithMargin;

		onPageScrolled(position, offset, offsetPixels);
	}

	/**
	 * 页面滚动
	 *
	 * @param position Position index of the first page currently being displayed.
	 *                 Page position+1 will be visible if positionOffset is nonzero.
	 * @param offset Value from [0, 1) indicating the offset from the page at position.
	 * @param offsetPixels Value in pixels indicating the offset from position.
	 */
	protected void onPageScrolled(int position, float offset, int offsetPixels) {
		if (mOnPageChangeListener != null) {
			mOnPageChangeListener.onPageScrolled(position, offset, offsetPixels);
		}
		if (mInternalPageChangeListener != null) {
			mInternalPageChangeListener.onPageScrolled(position, offset, offsetPixels);
		}
	}

	/**
	 * 完成滑动
	 */
	private void completeScroll() {
		//是否需要移动
		boolean needPopulate = mScrolling;
		
		if (needPopulate) {
			// Done with scroll, no longer want to cache view drawing.
			setScrollingCacheEnabled(false);
			//终止动画效果
			mScroller.abortAnimation();
			
			//获得滚动条初始的坐标
			int oldX = getScrollX();
			int oldY = getScrollY();
			
			//获得滚动条当前的坐标
			int x = mScroller.getCurrX();
			int y = mScroller.getCurrY();
			
			//如果滚动条初始的坐标和当前的坐标不等则滑动
			if (oldX != x || oldY != y) {
				scrollTo(x, y);
			}
			if (isMenuOpen()) {
				if (mOpenedListener != null)
					mOpenedListener.onOpened();
			} else {
				if (mClosedListener != null)
					mClosedListener.onClosed();
			}
		}
		//将滑动的状态设置为false
		mScrolling = false;
	}

	//获得触摸模式的值
	protected int mTouchMode = SlidingMenu.TOUCHMODE_MARGIN;

	/**
	 * 设置触摸的模式
	 */
	public void setTouchMode(int i) {
		mTouchMode = i;
	}

	/**
	 * 得到触摸的模式
	 */
	public int getTouchMode() {
		return mTouchMode;
	}

	/**
	 * 判断是否允许触摸打开滑动菜单
	 */
	private boolean thisTouchAllowed(MotionEvent ev) {
		int x = (int) (ev.getX() + mScrollX);
		if (isMenuOpen()) {
			return mViewBehind.menuOpenTouchAllowed(mContent, mCurItem, x);
		} else {
			switch (mTouchMode) {
			case SlidingMenu.TOUCHMODE_FULLSCREEN:
				return !isInIgnoredView(ev);
			case SlidingMenu.TOUCHMODE_NONE:
				return false;
			case SlidingMenu.TOUCHMODE_MARGIN:
				return mViewBehind.marginTouchAllowed(mContent, x);
			}
		}
		return false;
	}

	/**
	 * 判断是否允许滑动
	 */
	private boolean thisSlideAllowed(float dx) {
		boolean allowed = false;
		if (isMenuOpen()) {
			allowed = mViewBehind.menuOpenSlideAllowed(dx);
		} else {
			allowed = mViewBehind.menuClosedSlideAllowed(dx);
		}
		if (DEBUG)
			Log.v(TAG, "this slide allowed " + allowed + " dx: " + dx);
		return allowed;
	}

	/**
	 * 得到指针的索引值
	 */
	private int getPointerIndex(MotionEvent ev, int id) {
		int activePointerIndex = MotionEventCompat.findPointerIndex(ev, id);
		if (activePointerIndex == -1)
			mActivePointerId = INVALID_POINTER;
		return activePointerIndex;
	}

	private boolean mQuickReturn = false;

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {

		if (!mEnabled)
			return false;

		final int action = ev.getAction() & MotionEventCompat.ACTION_MASK;

		if (DEBUG)
			if (action == MotionEvent.ACTION_DOWN)
				Log.v(TAG, "Received ACTION_DOWN");

		if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP
				|| (action != MotionEvent.ACTION_DOWN && mIsUnableToDrag)) {
			endDrag();
			return false;
		}

		switch (action) {
		case MotionEvent.ACTION_MOVE:
			determineDrag(ev);
			break;
		case MotionEvent.ACTION_DOWN:
			int index = MotionEventCompat.getActionIndex(ev);
			mActivePointerId = MotionEventCompat.getPointerId(ev, index);
			if (mActivePointerId == INVALID_POINTER)
				break;
			mLastMotionX = mInitialMotionX = MotionEventCompat.getX(ev, index);
			mLastMotionY = MotionEventCompat.getY(ev, index);
			if (thisTouchAllowed(ev)) {
				mIsBeingDragged = false;
				mIsUnableToDrag = false;
				if (isMenuOpen() && mViewBehind.menuTouchInQuickReturn(mContent, mCurItem, ev.getX() + mScrollX)) {
					mQuickReturn = true;
				}
			} else {
				mIsUnableToDrag = true;
			}
			break;
		case MotionEventCompat.ACTION_POINTER_UP:
			onSecondaryPointerUp(ev);
			break;
		}

		if (!mIsBeingDragged) {
			if (mVelocityTracker == null) {
				mVelocityTracker = VelocityTracker.obtain();
			}
			mVelocityTracker.addMovement(ev);
		}
		return mIsBeingDragged || mQuickReturn;
	}


	@Override
	public boolean onTouchEvent(MotionEvent ev) {

		if (!mEnabled)
			return false;

		if (!mIsBeingDragged && !thisTouchAllowed(ev))
			return false;

		//		if (!mIsBeingDragged && !mQuickReturn)
		//			return false;

		final int action = ev.getAction();

		if (mVelocityTracker == null) {
			mVelocityTracker = VelocityTracker.obtain();
		}
		mVelocityTracker.addMovement(ev);

		switch (action & MotionEventCompat.ACTION_MASK) {
		case MotionEvent.ACTION_DOWN:
			/*
			 * If being flinged and user touches, stop the fling. isFinished
			 * will be false if being flinged.
			 */
			completeScroll();

			// Remember where the motion event started
			int index = MotionEventCompat.getActionIndex(ev);
			mActivePointerId = MotionEventCompat.getPointerId(ev, index);
			mLastMotionX = mInitialMotionX = ev.getX();
			break;
		case MotionEvent.ACTION_MOVE:
			if (!mIsBeingDragged) {	
				determineDrag(ev);
				if (mIsUnableToDrag)
					return false;
			}
			if (mIsBeingDragged) {
				// Scroll to follow the motion event
				final int activePointerIndex = getPointerIndex(ev, mActivePointerId);
				if (mActivePointerId == INVALID_POINTER)
					break;
				final float x = MotionEventCompat.getX(ev, activePointerIndex);
				final float deltaX = mLastMotionX - x;
				mLastMotionX = x;
				float oldScrollX = getScrollX();
				float scrollX = oldScrollX + deltaX;
				final float leftBound = getLeftBound();
				final float rightBound = getRightBound();
				if (scrollX < leftBound) {
					scrollX = leftBound;
				} else if (scrollX > rightBound) {
					scrollX = rightBound;
				}
				// Don't lose the rounded component
				mLastMotionX += scrollX - (int) scrollX;
				scrollTo((int) scrollX, getScrollY());
				pageScrolled((int) scrollX);
			}
			break;
		case MotionEvent.ACTION_UP:
			if (mIsBeingDragged) {
				final VelocityTracker velocityTracker = mVelocityTracker;
				velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
				int initialVelocity = (int) VelocityTrackerCompat.getXVelocity(
						velocityTracker, mActivePointerId);
				final int scrollX = getScrollX();
				//				final int widthWithMargin = getWidth();
				//				final float pageOffset = (float) (scrollX % widthWithMargin) / widthWithMargin;
				// TODO test this. should get better flinging behavior
				final float pageOffset = (float) (scrollX - getDestScrollX(mCurItem)) / getBehindWidth();
				final int activePointerIndex = getPointerIndex(ev, mActivePointerId);
				if (mActivePointerId != INVALID_POINTER) {
					final float x = MotionEventCompat.getX(ev, activePointerIndex);
					final int totalDelta = (int) (x - mInitialMotionX);
					int nextPage = determineTargetPage(pageOffset, initialVelocity, totalDelta);
					setCurrentItemInternal(nextPage, true, true, initialVelocity);
				} else {	
					setCurrentItemInternal(mCurItem, true, true, initialVelocity);
				}
				mActivePointerId = INVALID_POINTER;
				endDrag();
			} else if (mQuickReturn && mViewBehind.menuTouchInQuickReturn(mContent, mCurItem, ev.getX() + mScrollX)) {
				// close the menu
				setCurrentItem(1);
				endDrag();
			}
			break;
		case MotionEvent.ACTION_CANCEL:
			if (mIsBeingDragged) {
				setCurrentItemInternal(mCurItem, true, true);
				mActivePointerId = INVALID_POINTER;
				endDrag();
			}
			break;
		case MotionEventCompat.ACTION_POINTER_DOWN: {
			final int indexx = MotionEventCompat.getActionIndex(ev);
			mLastMotionX = MotionEventCompat.getX(ev, indexx);
			mActivePointerId = MotionEventCompat.getPointerId(ev, indexx);
			break;
		}
		case MotionEventCompat.ACTION_POINTER_UP:
			onSecondaryPointerUp(ev);
			int pointerIndex = getPointerIndex(ev, mActivePointerId);
			if (mActivePointerId == INVALID_POINTER)
				break;
			mLastMotionX = MotionEventCompat.getX(ev, pointerIndex);
			break;
		}
		return true;
	}
	
	private void determineDrag(MotionEvent ev) {
		final int activePointerId = mActivePointerId;
		final int pointerIndex = getPointerIndex(ev, activePointerId);
		if (activePointerId == INVALID_POINTER)
			return;
		final float x = MotionEventCompat.getX(ev, pointerIndex);
		final float dx = x - mLastMotionX;
		final float xDiff = Math.abs(dx);
		final float y = MotionEventCompat.getY(ev, pointerIndex);
		final float dy = y - mLastMotionY;
		final float yDiff = Math.abs(dy);
		if (xDiff > (isMenuOpen()?mTouchSlop/2:mTouchSlop) && xDiff > yDiff && thisSlideAllowed(dx)) {		
			startDrag();
			mLastMotionX = x;
			mLastMotionY = y;
			setScrollingCacheEnabled(true);
			// TODO add back in touch slop check
		} else if (xDiff > mTouchSlop) {
			mIsUnableToDrag = true;
		}
	}

	@Override
	public void scrollTo(int x, int y) {
		super.scrollTo(x, y);
		mScrollX = x;
		mViewBehind.scrollBehindTo(mContent, x, y);	
		((SlidingMenu)getParent()).manageLayers(getPercentOpen());
	}

	private int determineTargetPage(float pageOffset, int velocity, int deltaX) {
		int targetPage = mCurItem;
		if (Math.abs(deltaX) > mFlingDistance && Math.abs(velocity) > mMinimumVelocity) {
			if (velocity > 0 && deltaX > 0) {
				targetPage -= 1;
			} else if (velocity < 0 && deltaX < 0){
				targetPage += 1;
			}
		} else {
			targetPage = (int) Math.round(mCurItem + pageOffset);
		}
		return targetPage;
	}

	protected float getPercentOpen() {
		return Math.abs(mScrollX-mContent.getLeft()) / getBehindWidth();
	}

	@Override
	protected void dispatchDraw(Canvas canvas) {
		super.dispatchDraw(canvas);
		// Draw the margin drawable if needed.
		mViewBehind.drawShadow(mContent, canvas);
		mViewBehind.drawFade(mContent, canvas, getPercentOpen());
		mViewBehind.drawSelector(mContent, canvas, getPercentOpen());
	}

	// variables for drawing
	private float mScrollX = 0.0f;

	private void onSecondaryPointerUp(MotionEvent ev) {
		if (DEBUG) Log.v(TAG, "onSecondaryPointerUp called");
		final int pointerIndex = MotionEventCompat.getActionIndex(ev);
		final int pointerId = MotionEventCompat.getPointerId(ev, pointerIndex);
		if (pointerId == mActivePointerId) {
			// This was our active pointer going up. Choose a new
			// active pointer and adjust accordingly.
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			mLastMotionX = MotionEventCompat.getX(ev, newPointerIndex);
			mActivePointerId = MotionEventCompat.getPointerId(ev, newPointerIndex);
			if (mVelocityTracker != null) {
				mVelocityTracker.clear();
			}
		}
	}

	/**
	 * 开始拖动
	 */
	private void startDrag() {
		mIsBeingDragged = true;
		mQuickReturn = false;
	}

	/**
	 * 结束拖动
	 */
	private void endDrag() {
		mQuickReturn = false;
		mIsBeingDragged = false;
		mIsUnableToDrag = false;
		mActivePointerId = INVALID_POINTER;

		if (mVelocityTracker != null) {
			mVelocityTracker.recycle();
			mVelocityTracker = null;
		}
	}

	/**
	 * 设置能否使用滑动缓存
	 */
	private void setScrollingCacheEnabled(boolean enabled) {
		if (mScrollingCacheEnabled != enabled) {
			mScrollingCacheEnabled = enabled;
			if (USE_CACHE) {
				final int size = getChildCount();
				for (int i = 0; i < size; ++i) {
					final View child = getChildAt(i);
					if (child.getVisibility() != GONE) {
						child.setDrawingCacheEnabled(enabled);
					}
				}
			}
		}
	}

	/**
	 * Tests scrollability within child views of v given a delta of dx.
	 *
	 * @param v View to test for horizontal scrollability
	 * @param checkV Whether the view v passed should itself be checked for scrollability (true),
	 *               or just its children (false).
	 * @param dx Delta scrolled in pixels
	 * @param x X coordinate of the active touch point
	 * @param y Y coordinate of the active touch point
	 * @return true if child views of v can be scrolled by delta of dx.
	 */
	protected boolean canScroll(View v, boolean checkV, int dx, int x, int y) {
		if (v instanceof ViewGroup) {
			final ViewGroup group = (ViewGroup) v;
			final int scrollX = v.getScrollX();
			final int scrollY = v.getScrollY();
			final int count = group.getChildCount();
			// Count backwards - let topmost views consume scroll distance first.
			for (int i = count - 1; i >= 0; i--) {
				final View child = group.getChildAt(i);
				if (x + scrollX >= child.getLeft() && x + scrollX < child.getRight() &&
						y + scrollY >= child.getTop() && y + scrollY < child.getBottom() &&
						canScroll(child, true, dx, x + scrollX - child.getLeft(),
								y + scrollY - child.getTop())) {
					return true;
				}
			}
		}

		return checkV && ViewCompat.canScrollHorizontally(v, -dx);
	}


	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		// Let the focused view and/or our descendants get the key first
		return super.dispatchKeyEvent(event) || executeKeyEvent(event);
	}

	/**
	 * 执行按键响应事件
	 */
	public boolean executeKeyEvent(KeyEvent event) {
		boolean handled = false;
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_DPAD_LEFT:
				handled = arrowScroll(FOCUS_LEFT);
				break;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				handled = arrowScroll(FOCUS_RIGHT);
				break;
			case KeyEvent.KEYCODE_TAB:
				if (Build.VERSION.SDK_INT >= 11) {
					// The focus finder had a bug handling FOCUS_FORWARD and FOCUS_BACKWARD
					// before Android 3.0. Ignore the tab key on those devices.
					if (KeyEventCompat.hasNoModifiers(event)) {
						handled = arrowScroll(FOCUS_FORWARD);
					} else if (KeyEventCompat.hasModifiers(event, KeyEvent.META_SHIFT_ON)) {
						handled = arrowScroll(FOCUS_BACKWARD);
					}
				}
				break;
			}
		}
		return handled;
	}

	/**
	 * 获得滑动的方向
	 */
	public boolean arrowScroll(int direction) {
		View currentFocused = findFocus();
		if (currentFocused == this) currentFocused = null;

		boolean handled = false;

		View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused,
				direction);
		if (nextFocused != null && nextFocused != currentFocused) {
			if (direction == View.FOCUS_LEFT) {
				handled = nextFocused.requestFocus();
			} else if (direction == View.FOCUS_RIGHT) {
				// If there is nothing to the right, or this is causing us to
				// jump to the left, then what we really want to do is page right.
				if (currentFocused != null && nextFocused.getLeft() <= currentFocused.getLeft()) {
					handled = pageRight();
				} else {
					handled = nextFocused.requestFocus();
				}
			}
		} else if (direction == FOCUS_LEFT || direction == FOCUS_BACKWARD) {
			// Trying to move left and nothing there; try to page.
			handled = pageLeft();
		} else if (direction == FOCUS_RIGHT || direction == FOCUS_FORWARD) {
			// Trying to move right and nothing there; try to page.
			handled = pageRight();
		}
		if (handled) {
			playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
		}
		return handled;
	}

	/**
	 * 页面是否向左移动
	 */
	boolean pageLeft() {
		if (mCurItem > 0) {
			setCurrentItem(mCurItem-1, true);
			return true;
		}
		return false;
	}

	/**
	 * 页面是否向右移动
	 */
	boolean pageRight() {
		if (mCurItem < 1) {
			setCurrentItem(mCurItem+1, true);
			return true;
		}
		return false;
	}

}
