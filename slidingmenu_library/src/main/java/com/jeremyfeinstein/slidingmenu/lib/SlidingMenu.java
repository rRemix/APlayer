package com.jeremyfeinstein.slidingmenu.lib;

import java.lang.reflect.Method;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.jeremyfeinstein.slidingmenu.lib.CustomViewAbove.OnPageChangeListener;

public class SlidingMenu extends RelativeLayout {

	private static final String TAG = "SlidingMenu";

	public static final int SLIDING_WINDOW = 0;
	public static final int SLIDING_CONTENT = 1;
	private boolean mActionbarOverlay = false;

	/**
	 * 为setTouchModeAbove()方法设置一个常量值，允许滑动菜单通过滑动屏幕的边缘被打开 
	 */
	public static final int TOUCHMODE_MARGIN = 0;

	/** 
	 * 为setTouchModeAbove()方法设置一个常量值，允许滑动菜单通过滑动屏幕的任何地方被打开
	 */
	public static final int TOUCHMODE_FULLSCREEN = 1;

	/** 
	 * 为setTouchModeAbove()方法设置一个常量值，不允许滑动菜单通过滑动屏幕被打开
	 */
	public static final int TOUCHMODE_NONE = 2;

	/** 
	 * 为setMode()方法设置一个常量值，把滑动菜单放在左边
	 */
	public static final int LEFT = 0;

	/** 
	 * 为setMode()方法设置一个常量值，把滑动菜单放在右边
	 */
	public static final int RIGHT = 1;

	/** 
	 * 为setMode()方法设置一个常量值，把滑动菜单放在左右两边
	 */
	public static final int LEFT_RIGHT = 2;

	/**
	 * 定义上方视图对象
	 */
	private CustomViewAbove mViewAbove;

	/**
	 * 定义下方视图对象
	 */
	private CustomViewBehind mViewBehind;

	/**
	 * 定义滑动菜单打开的监听对象
	 */
	private OnOpenListener mOpenListener;

	/**
	 * 定义滑动菜单关闭的监听对象
	 */
	private OnCloseListener mCloseListener;

	/**
	 * 滑动菜单打开时的监听事件
	 */
	public interface OnOpenListener {	
		public void onOpen();
	}

	/**
	 * 监测滑动菜单是否已经打开的监听事件
	 */
	public interface OnOpenedListener {
		public void onOpened();
	}

	/**
	 * 滑动菜单关闭时的监听事件
	 */
	public interface OnCloseListener {
		public void onClose();
	}

	/**
	 * 监测滑动菜单是否已经关闭的监听事件
	 */
	public interface OnClosedListener {
		public void onClosed();
	}

	/**
	 * The Interface CanvasTransformer.
	 */
	public interface CanvasTransformer {

		/**
		 * Transform canvas.
		 *
		 * @param canvas the canvas
		 * @param percentOpen the percent open
		 */
		public void transformCanvas(Canvas canvas, float percentOpen);
	}

	/**
	 * 初始化滑动菜单
	 *
	 * @param context the associated Context
	 */
	public SlidingMenu(Context context) {
		this(context, null);
	}

	/**
	 * 初始化滑动菜单
	 *
	 * @param activity the activity to attach slidingmenu
	 * @param slideStyle the slidingmenu style
	 */
	public SlidingMenu(Activity activity, int slideStyle) {
		this(activity, null);
		this.attachToActivity(activity, slideStyle);
	}

	/**
	 * 初始化滑动菜单
	 *
	 * @param context the associated Context
	 * @param attrs the attrs
	 */
	public SlidingMenu(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	/**
	 * 初始化滑动菜单
	 *
	 * @param context the associated Context
	 * @param attrs the attrs
	 * @param defStyle the def style
	 */
	public SlidingMenu(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		LayoutParams behindParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		mViewBehind = new CustomViewBehind(context);
		addView(mViewBehind, behindParams);
		LayoutParams aboveParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		mViewAbove = new CustomViewAbove(context);
		addView(mViewAbove, aboveParams);
		// register the CustomViewBehind with the CustomViewAbove
		mViewAbove.setCustomViewBehind(mViewBehind);
		mViewBehind.setCustomViewAbove(mViewAbove);
		mViewAbove.setOnPageChangeListener(new OnPageChangeListener() {
			public static final int POSITION_OPEN = 0;
			public static final int POSITION_CLOSE = 1;

			public void onPageScrolled(int position, float positionOffset,
					int positionOffsetPixels) { }

			public void onPageSelected(int position) {
				if (position == POSITION_OPEN && mOpenListener != null) {
					mOpenListener.onOpen();
				} else if (position == POSITION_CLOSE && mCloseListener != null) {
					mCloseListener.onClose();
				}
			}
		});

		// now style everything!
		TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.SlidingMenu);
		// set the above and behind views if defined in xml
		int mode = ta.getInt(R.styleable.SlidingMenu_mode, LEFT);
		setMode(mode);
		int viewAbove = ta.getResourceId(R.styleable.SlidingMenu_viewAbove, -1);
		if (viewAbove != -1) {
			setContent(viewAbove);
		} else {
			setContent(new FrameLayout(context));
		}
		int viewBehind = ta.getResourceId(R.styleable.SlidingMenu_viewBehind, -1);
		if (viewBehind != -1) {
			setMenu(viewBehind); 
		} else {
			setMenu(new FrameLayout(context));
		}
		int touchModeAbove = ta.getInt(R.styleable.SlidingMenu_touchModeAbove, TOUCHMODE_MARGIN);
		setTouchModeAbove(touchModeAbove);
		int touchModeBehind = ta.getInt(R.styleable.SlidingMenu_touchModeBehind, TOUCHMODE_MARGIN);
		setTouchModeBehind(touchModeBehind);

		int offsetBehind = (int) ta.getDimension(R.styleable.SlidingMenu_behindOffset, -1);
		int widthBehind = (int) ta.getDimension(R.styleable.SlidingMenu_behindWidth, -1);
		if (offsetBehind != -1 && widthBehind != -1)
			throw new IllegalStateException("Cannot set both behindOffset and behindWidth for a SlidingMenu");
		else if (offsetBehind != -1)
			setBehindOffset(offsetBehind);
		else if (widthBehind != -1)
			setBehindWidth(widthBehind);
		else
			setBehindOffset(0);
		float scrollOffsetBehind = ta.getFloat(R.styleable.SlidingMenu_behindScrollScale, 0.33f);
		setBehindScrollScale(scrollOffsetBehind);
		int shadowRes = ta.getResourceId(R.styleable.SlidingMenu_shadowDrawable, -1);
		if (shadowRes != -1) {
			setShadowDrawable(shadowRes);
		}
		int shadowWidth = (int) ta.getDimension(R.styleable.SlidingMenu_shadowWidth, 0);
		setShadowWidth(shadowWidth);
		boolean fadeEnabled = ta.getBoolean(R.styleable.SlidingMenu_fadeEnabled, true);
		setFadeEnabled(fadeEnabled);
		float fadeDeg = ta.getFloat(R.styleable.SlidingMenu_fadeDegree, 0.33f);
		setFadeDegree(fadeDeg);
		boolean selectorEnabled = ta.getBoolean(R.styleable.SlidingMenu_selectorEnabled, false);
		setSelectorEnabled(selectorEnabled);
		int selectorRes = ta.getResourceId(R.styleable.SlidingMenu_selectorDrawable, -1);
		if (selectorRes != -1)
			setSelectorDrawable(selectorRes);
		ta.recycle();
	}

	/**
	 * 把滑动菜单添加进所有的Activity中
	 * 
	 * @param activity the Activity
	 * @param slideStyle either SLIDING_CONTENT or SLIDING_WINDOW
	 */
	public void attachToActivity(Activity activity, int slideStyle) {
		attachToActivity(activity, slideStyle, false);
	}

	/**
	 * 把滑动菜单添加进所有的Activity中
	 * 
	 * @param activity the Activity
	 * @param slideStyle either SLIDING_CONTENT or SLIDING_WINDOW
	 * @param actionbarOverlay whether or not the ActionBar is overlaid
	 */
	public void attachToActivity(Activity activity, int slideStyle, boolean actionbarOverlay) {
		if (slideStyle != SLIDING_WINDOW && slideStyle != SLIDING_CONTENT)
			throw new IllegalArgumentException("slideStyle must be either SLIDING_WINDOW or SLIDING_CONTENT");

		if (getParent() != null)
			throw new IllegalStateException("This SlidingMenu appears to already be attached");

		// get the window background
		TypedArray a = activity.getTheme().obtainStyledAttributes(new int[] {android.R.attr.windowBackground});
		int background = a.getResourceId(0, 0);
		a.recycle();

		switch (slideStyle) {
		case SLIDING_WINDOW:
			mActionbarOverlay = false;
			ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
			ViewGroup decorChild = (ViewGroup) decor.getChildAt(0);
			// save ActionBar themes that have transparent assets
			decorChild.setBackgroundResource(background);
			decor.removeView(decorChild);
			decor.addView(this);
			setContent(decorChild);
			break;
		case SLIDING_CONTENT:
			mActionbarOverlay = actionbarOverlay;
			// take the above view out of
			ViewGroup contentParent = (ViewGroup)activity.findViewById(android.R.id.content);
			View content = contentParent.getChildAt(0);
			contentParent.removeView(content);
			contentParent.addView(this);
			setContent(content);
			// save people from having transparent backgrounds
			if (content.getBackground() == null)
				content.setBackgroundResource(background);
			break;
		}
	}

	/**
	 * 从布局资源文件中设置上方的视图内容，这个布局会被填充添加到所有图层的最上方
	 */
	public void setContent(int res) {
		setContent(LayoutInflater.from(getContext()).inflate(res, null));
	}

	/**
	 * 通过View来设置上方的视图内容
	 */
	public void setContent(View view) {
		mViewAbove.setContent(view);
		showContent();
	}

	/**
	 * 得到上方的视图内容
	 */
	public View getContent() {
		return mViewAbove.getContent();
	}

	/**
	 * 从布局资源文件中设置下方（滑动菜单）的视图内容，这个布局会被填充添加到所有图层的最下方
	 * 
	 * @param res the new content
	 */
	public void setMenu(int res) {
		setMenu(LayoutInflater.from(getContext()).inflate(res, null));
	}

	/**
	 * 得到下方（滑动菜单）的视图内容
	 *
	 * @param view The desired content to display.
	 */
	public void setMenu(View v) {
		mViewBehind.setContent(v);
	}

	/**
	 * 得到下方（滑动菜单）的视图内容
	 */
	public View getMenu() {
		return mViewBehind.getContent();
	}

	/**
	 * 从布局资源文件中设置下方（右边滑动菜单）的视图内容，这个布局会被填充添加到所有图层的最下方
	 */
	public void setSecondaryMenu(int res) {
		setSecondaryMenu(LayoutInflater.from(getContext()).inflate(res, null));
	}

	/**
	 * 设置下方（右边滑动菜单）的视图内容
	 */
	public void setSecondaryMenu(View v) {
		mViewBehind.setSecondaryContent(v);
	}

	/**
	 * 得到下方（右边滑动菜单）的视图内容
	 */
	public View getSecondaryMenu() {
		return mViewBehind.getSecondaryContent();
	}

	/**
	 * 设置上方视图是否能够滑动
	 */
	public void setSlidingEnabled(boolean b) {
		mViewAbove.setSlidingEnabled(b);
	}

	/**
	 * 检测上方视图是否能够滑动
	 */
	public boolean isSlidingEnabled() {
		return mViewAbove.isSlidingEnabled();
	}

	/**
	 * 设置滑动菜单出现在视图中的位置
	 * 
	 * @param mode must be either SlidingMenu.LEFT or SlidingMenu.RIGHT
	 */
	public void setMode(int mode) {
		if (mode != LEFT && mode != RIGHT && mode != LEFT_RIGHT) {
			throw new IllegalStateException("SlidingMenu mode must be LEFT, RIGHT, or LEFT_RIGHT");
		}
		mViewBehind.setMode(mode);
	}

	/**
	 * 得到滑动菜单在视图中的位置
	 * 
	 * @return the current mode, either SlidingMenu.LEFT or SlidingMenu.RIGHT
	 */
	public int getMode() {
		return mViewBehind.getMode();
	}

	/**
	 * 设置滑动菜单是否是静态模式(不能够使用滑动菜单)
	 */
	public void setStatic(boolean b) {
		if (b) {
			setSlidingEnabled(false);
			mViewAbove.setCustomViewBehind(null);
			mViewAbove.setCurrentItem(1);
			//			mViewBehind.setCurrentItem(0);	
		} else {
			mViewAbove.setCurrentItem(1);
			//			mViewBehind.setCurrentItem(1);
			mViewAbove.setCustomViewBehind(mViewBehind);
			setSlidingEnabled(true);
		}
	}

	/**
	 * 打开滑动菜单并显示菜单的视图
	 */
	public void showMenu() {
		showMenu(true);
	}

	/**
	 * 是否使用动画效果打开滑动菜单并显示菜单的视图
	 */
	public void showMenu(boolean animate) {
		mViewAbove.setCurrentItem(0, animate);
	}

	/**
	 * 打开右边的滑动菜单并显示菜单的视图 
	 */
	public void showSecondaryMenu() {
		showSecondaryMenu(true);
	}

	/**
	 * 是否使用动画效果打开右边的滑动菜单并显示菜单的视图
	 */
	public void showSecondaryMenu(boolean animate) {
		mViewAbove.setCurrentItem(2, animate);
	}

	/**
	 * 关闭菜单并显示上方的视图
	 */
	public void showContent() {
		showContent(true);
	}

	/**
	 * 是否使用动画效果关闭菜单并显示上方的视图
	 */
	public void showContent(boolean animate) {
		mViewAbove.setCurrentItem(1, animate);
	}

	/**
	 * 滑动菜单的开关
	 */
	public void toggle() {
		toggle(true);
	}

	/**
	 * 是否使用动画效果打开或关闭滑动菜单
	 */
	public void toggle(boolean animate) {
		if (isMenuShowing()) {
			showContent(animate);
		} else {
			showMenu(animate);
		}
	}

	/**
	 * 检测滑动菜单是否正在被显示
	 */
	public boolean isMenuShowing() {
		return mViewAbove.getCurrentItem() == 0 || mViewAbove.getCurrentItem() == 2;
	}
	
	/**
	 * 检测右边滑动菜单是否正在被显示
	 */
	public boolean isSecondaryMenuShowing() {
		return mViewAbove.getCurrentItem() == 2;
	}

	/**
	 * 得到下方视图的偏移量
	 */
	public int getBehindOffset() {
		return ((RelativeLayout.LayoutParams)mViewBehind.getLayoutParams()).rightMargin;
	}

	/**
	 * 根据像素的值来设置下方视图的偏移量
	 *
	 * @param i The margin, in pixels, on the right of the screen that the behind view scrolls to.
	 */
	public void setBehindOffset(int i) {		
		mViewBehind.setWidthOffset(i);
	}

	/**
	 * 根据dimension资源文件的ID来设置下方视图的偏移量
	 *
	 * @param resID The dimension resource id to be set as the behind offset.
	 * The menu, when open, will leave this width margin on the right of the screen.
	 */
	public void setBehindOffsetRes(int resID) {
		int i = (int) getContext().getResources().getDimension(resID);
		setBehindOffset(i);
	}

	/**
	 * 根据像素的值来设置上方视图的偏移量
	 *
	 * @param i the new above offset, in pixels
	 */
	public void setAboveOffset(int i) {
		mViewAbove.setAboveOffset(i);
	}

	/**
	 * 根据dimension资源文件的ID来设置上方视图的偏移量
	 *
	 * @param resID The dimension resource id to be set as the above offset.
	 */
	public void setAboveOffsetRes(int resID) {
		int i = (int) getContext().getResources().getDimension(resID);
		setAboveOffset(i);
	}

	/**
	 * 根据像素的值来设置下方视图的宽度
	 *
	 * @param i The width the Sliding Menu will open to, in pixels
	 */
	public void setBehindWidth(int i) {
		int width;
		Display display = ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		try {
			Class<?> cls = Display.class;
			Class<?>[] parameterTypes = {Point.class};
			Point parameter = new Point();
			Method method = cls.getMethod("getSize", parameterTypes);
			method.invoke(display, parameter);
			width = parameter.x;
		} catch (Exception e) {
			width = display.getWidth();
		}
		setBehindOffset(width-i);
	}

	/**
	 * 根据dimension资源文件的ID来设置下方视图的宽度
	 *
	 * @param res The dimension resource id to be set as the behind width offset.
	 * The menu, when open, will open this wide.
	 */
	public void setBehindWidthRes(int res) {
		int i = (int) getContext().getResources().getDimension(res);
		setBehindWidth(i);
	}

	/**
	 * 得到下方视图的在滚动时的缩放比例
	 *
	 * @return The scale of the parallax scroll
	 */
	public float getBehindScrollScale() {
		return mViewBehind.getScrollScale();
	}
	
	/**
	 * 设置下方视图的在滚动时的缩放比例
	 *
	 * @param f The scale of the parallax scroll (i.e. 1.0f scrolls 1 pixel for every
	 * 1 pixel that the above view scrolls and 0.0f scrolls 0 pixels)
	 */
	public void setBehindScrollScale(float f) {
		if (f < 0 && f > 1)
			throw new IllegalStateException("ScrollScale must be between 0 and 1");
		mViewBehind.setScrollScale(f);
	}
	
	/**
	 * 得到边缘触摸的临界值
	 */
	public int getTouchmodeMarginThreshold() {
		return mViewBehind.getMarginThreshold();
	}
	
	/**
	 * 当触摸的的模式为边缘触摸时，设置边缘触摸的临界值
	 */
	public void setTouchmodeMarginThreshold(int touchmodeMarginThreshold) {
		mViewBehind.setMarginThreshold(touchmodeMarginThreshold);
	}

	/**
	 * Sets the behind canvas transformer.
	 *
	 * @param t the new behind canvas transformer
	 */
	public void setBehindCanvasTransformer(CanvasTransformer t) {
		mViewBehind.setCanvasTransformer(t);
	}

	/**
	 * 得到上方视图的触摸模式的值
	 */
	public int getTouchModeAbove() {
		return mViewAbove.getTouchMode();
	}


	/**
	 * 设置上方视图的触摸模式的值
	 */
	public void setTouchModeAbove(int i) {
		if (i != TOUCHMODE_FULLSCREEN && i != TOUCHMODE_MARGIN
				&& i != TOUCHMODE_NONE) {
			throw new IllegalStateException("TouchMode must be set to either" +
					"TOUCHMODE_FULLSCREEN or TOUCHMODE_MARGIN or TOUCHMODE_NONE.");
		}
		mViewAbove.setTouchMode(i);
	}

	/**
	 * 设置下方视图的触摸模式的值
	 */
	public void setTouchModeBehind(int i) {
		if (i != TOUCHMODE_FULLSCREEN && i != TOUCHMODE_MARGIN
				&& i != TOUCHMODE_NONE) {
			throw new IllegalStateException("TouchMode must be set to either" +
					"TOUCHMODE_FULLSCREEN or TOUCHMODE_MARGIN or TOUCHMODE_NONE.");
		}
		mViewBehind.setTouchMode(i);
	}

	/**
	 * 根据资源文件ID来设置滑动菜单的阴影效果
	 *
	 * @param resId the resource ID of the new shadow drawable
	 */
	public void setShadowDrawable(int resId) {
		setShadowDrawable(getContext().getResources().getDrawable(resId));
	}

	/**
	 * 根据Drawable来设置滑动菜单的阴影效果
	 *
	 * @param d the new shadow drawable
	 */
	public void setShadowDrawable(Drawable d) {
		mViewBehind.setShadowDrawable(d);
	}

	/**
	 * 根据资源文件ID来设置右边滑动菜单的阴影效果
	 *
	 * @param resId the resource ID of the new shadow drawable
	 */
	public void setSecondaryShadowDrawable(int resId) {
		setSecondaryShadowDrawable(getContext().getResources().getDrawable(resId));
	}

	/**
	 * 根据Drawable来设置滑动菜单的阴影效果
	 *
	 * @param d the new shadow drawable
	 */
	public void setSecondaryShadowDrawable(Drawable d) {
		mViewBehind.setSecondaryShadowDrawable(d);
	}

	/**
	 * 根据dimension资源文件的ID来设置阴影的宽度
	 *
	 * @param resId The dimension resource id to be set as the shadow width.
	 */
	public void setShadowWidthRes(int resId) {
		setShadowWidth((int)getResources().getDimension(resId));
	}

	/**
	 * 根据像素的值来设置阴影的宽度
	 *
	 * @param pixels the new shadow width, in pixels
	 */
	public void setShadowWidth(int pixels) {
		mViewBehind.setShadowWidth(pixels);
	}

	/**
	 * 设置是否能够使用滑动菜单渐入渐出的效果
	 **/
	public void setFadeEnabled(boolean b) {
		mViewBehind.setFadeEnabled(b);
	}

	/**
	 * 设置渐入渐出效果的值
	 *
	 * @param f the new fade degree, between 0.0f and 1.0f
	 */
	public void setFadeDegree(float f) {
		mViewBehind.setFadeDegree(f);
	}

	/**
	 * Enables or disables whether the selector is drawn
	 *
	 * @param b true to draw the selector, false to not draw the selector
	 */
	public void setSelectorEnabled(boolean b) {
		mViewBehind.setSelectorEnabled(true);
	}

	/**
	 * Sets the selected view. The selector will be drawn here
	 *
	 * @param v the new selected view
	 */
	public void setSelectedView(View v) {
		mViewBehind.setSelectedView(v);
	}

	/**
	 * Sets the selector drawable.
	 *
	 * @param res a resource ID for the selector drawable
	 */
	public void setSelectorDrawable(int res) {
		mViewBehind.setSelectorBitmap(BitmapFactory.decodeResource(getResources(), res));
	}

	/**
	 * Sets the selector drawable.
	 *
	 * @param b the new selector bitmap
	 */
	public void setSelectorBitmap(Bitmap b) {
		mViewBehind.setSelectorBitmap(b);
	}

	/**
	 * 添加被忽略的视图
	 */
	public void addIgnoredView(View v) {
		mViewAbove.addIgnoredView(v);
	}

	/**
	 * 移除被忽略的视图
	 */
	public void removeIgnoredView(View v) {
		mViewAbove.removeIgnoredView(v);
	}

	/**
	 * 当模式为Fullscreen模式时，触摸屏幕清除所有被忽略的视图
	 */
	public void clearIgnoredViews() {
		mViewAbove.clearIgnoredViews();
	}

	/**
	 * 设置打开监听事件，当滑动菜单被打开时调用
	 */
	public void setOnOpenListener(OnOpenListener listener) {
		mOpenListener = listener;
	}

	/**
	 * 设置关闭监听事件，当滑动菜单被关闭时调用
	 */
	public void setOnCloseListener(OnCloseListener listener) {
		//mViewAbove.setOnCloseListener(listener);
		mCloseListener = listener;
	}

	/**
	 * 设置打开监听事件，当滑动菜单被打开过之后调用
	 */
	public void setOnOpenedListener(OnOpenedListener listener) {
		mViewAbove.setOnOpenedListener(listener);
	}

	/**
	 * 设置关闭监听事件，当滑动菜单被关闭过之后调用
	 */
	public void setOnClosedListener(OnClosedListener listener) {
		mViewAbove.setOnClosedListener(listener);
	}

	/**
	 * 功能描述：保存状态的类，继承自BaseSavedState
	 */
	public static class SavedState extends BaseSavedState {
		private final int mItem;

		public SavedState(Parcelable superState, int item) {
			super(superState);
			mItem = item;
		}

		private SavedState(Parcel in) {
			super(in);
			mItem = in.readInt();
		}

		public int getItem() {
			return mItem;
		}

		/* (non-Javadoc)
		 * @see android.view.AbsSavedState#writeToParcel(android.os.Parcel, int)
		 */
		public void writeToParcel(Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeInt(mItem);
		}

		public static final Parcelable.Creator<SavedState> CREATOR =
				new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

	/* (non-Javadoc)
	 * @see android.view.View#onSaveInstanceState()
	 */
	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState ss = new SavedState(superState, mViewAbove.getCurrentItem());
		return ss;
	}

	/* (non-Javadoc)
	 * @see android.view.View#onRestoreInstanceState(android.os.Parcelable)
	 */
	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		SavedState ss = (SavedState)state;
		super.onRestoreInstanceState(ss.getSuperState());
		mViewAbove.setCurrentItem(ss.getItem());
	}

	/* (non-Javadoc)
	 * @see android.view.ViewGroup#fitSystemWindows(android.graphics.Rect)
	 */
	@SuppressLint("NewApi")
	@Override
	protected boolean fitSystemWindows(Rect insets) {
		int leftPadding = insets.left;
		int rightPadding = insets.right;
		int topPadding = insets.top;
		int bottomPadding = insets.bottom;
		if (!mActionbarOverlay) {
			Log.v(TAG, "setting padding!");
			setPadding(leftPadding, topPadding, rightPadding, bottomPadding);
		}
		return true;
	}
	
	private Handler mHandler = new Handler();

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public void manageLayers(float percentOpen) {
		if (Build.VERSION.SDK_INT < 11) return;

		boolean layer = percentOpen > 0.0f && percentOpen < 1.0f;
		final int layerType = layer ? View.LAYER_TYPE_HARDWARE : View.LAYER_TYPE_NONE;

		if (layerType != getContent().getLayerType()) {
			mHandler.post(new Runnable() {
				public void run() {
					Log.v(TAG, "changing layerType. hardware? " + (layerType == View.LAYER_TYPE_HARDWARE));
					getContent().setLayerType(layerType, null);
					getMenu().setLayerType(layerType, null);
					if (getSecondaryMenu() != null) {
						getSecondaryMenu().setLayerType(layerType, null);
					}
				}
			});
		}
	}

}