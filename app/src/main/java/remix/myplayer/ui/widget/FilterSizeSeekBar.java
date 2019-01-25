package remix.myplayer.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import remix.myplayer.R;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.DensityUtil;

/**
 * Created by Remix on 2016/3/7.
 */

/**
 * 设置扫描文件大小的seekbar
 */
public class FilterSizeSeekBar extends View {

  private OnSeekBarChangeListener mOnSeekBarChangeListener;
  private static final String TAG = "CustomSeekBar";
  private Context mContext;
  /**
   * 控件高度与宽度
   */
  private int mViewWidth;
  private int mViewHeight;

  /**
   * 提示字体
   */
  private Paint mTextPaint;

  /**
   * 轨道垂直中心点
   */
  private int mTrackCenterY;

  /**
   * 整个轨道的颜色
   */
  private int mTrackColor;

  /**
   * 整个轨道画笔
   */
  private Paint mTrackPaint;

  /**
   * 已完成轨道的颜色
   */
  private int mProgressColor;

  /**
   * 已完成轨道的画笔
   */
  private Paint mProgressPaint;

  /**
   * 文字颜色
   */
  private int mTextColor;

  /**
   * 小圆点的画笔
   */
  private Paint mDotPaint;

  /**
   * 小圆点颜色
   */
  private int mDotColor;

  /**
   * 轨道的高度与长度
   */
  private int mTrackHeigh;
  private int mTrackWidth;

  /**
   * 总共几个小圆点
   */
  private int mDotNum;

  /**
   * 小圆点宽度
   */
  private int mDotWidth;

  /**
   * 两个小圆点之间的距离
   */
  private int mDotBetween;

  /**
   * 所有小圆点的坐标
   */
  private ArrayList<Integer> mDotPosition = new ArrayList<>();

  /**
   * Thumb的高度与宽度
   */
  private int mThumbWidth = 0;
  private int mThumbHeight = 0;

  /**
   * ThumbDrawable 以及两个状态
   */
  private StateListDrawable mThumbDrawable = null;
  private int[] mThumbNormal = null;
  private int[] mThumbPressed = null;

  /**
   * Thumb所在位置的中心点
   */
  private int mThumbCenterX;
  private int mThumbCenterY;

  /**
   * 当前索引
   */
  private int mPositon;
  /**
   * 是否初始化完成
   */
  private boolean mInit = false;

  /**
   * 扫描大小设置常量
   */
  private String[] mTexts = new String[]{"0", "300k", "500K", "800k", "1MB", "2MB"};

  public FilterSizeSeekBar(Context context) {
    super(context);
    mContext = context;
    init(null);
  }

  public FilterSizeSeekBar(Context context, AttributeSet attrs) {
    super(context, attrs);
    mContext = context;
    init(attrs);
  }

  public FilterSizeSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    mContext = context;
    init(attrs);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    //整个轨道
    canvas.drawLine(mThumbWidth, mTrackCenterY, mTrackWidth + mThumbWidth, mTrackCenterY,
        mTrackPaint);
    //已完成轨道
    canvas.drawLine(mThumbWidth, mTrackCenterY, mThumbCenterX, mTrackCenterY, mProgressPaint);
    //小圆点与底部文字
    for (int i = 0; i < mDotNum; i++) {
      canvas.drawCircle(mDotPosition.get(i), mTrackCenterY, mDotWidth, mDotPaint);
      canvas.drawText(mTexts[i], mDotPosition.get(i), mThumbHeight * 2, mTextPaint);
    }
    //thumb
    mThumbDrawable.setBounds(mThumbCenterX - mThumbWidth / 2, 0, mThumbCenterX + mThumbWidth / 2,
        mThumbHeight);
    mThumbDrawable.draw(canvas);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int eventX = (int) event.getX();
    boolean isUp = event.getAction() == MotionEvent.ACTION_UP;

    //设置thumb状态
    mThumbDrawable.setState(isUp ? mThumbNormal : mThumbPressed);

    if (eventX > mDotPosition.get(mDotPosition.size() - 1) || eventX < mThumbWidth) {
      invalidate();
      return true;
    }

    if (isUp) {
      //寻找与当前触摸点最近的值
      int temp = Integer.MAX_VALUE;
      for (int i = 0; i < mDotPosition.size(); i++) {
        if (Math.abs(mDotPosition.get(i) - eventX) < temp) {
          mPositon = i;
          temp = Math.abs(mDotPosition.get(i) - eventX);
        }
      }
      mThumbCenterX = mDotPosition.get(mPositon);
      if (mOnSeekBarChangeListener != null) {
        mOnSeekBarChangeListener.onProgressChanged(this, mPositon, true);
      }
    } else {
      mThumbCenterX = eventX;
    }

    invalidate();
    return true;
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    if ((mViewWidth = getMeasuredWidth()) > 0 && (mViewHeight = getMeasuredHeight()) > 0) {
      //计算轨道宽度 两个小圆点之间的距离
      mTrackWidth = mViewWidth - mThumbWidth * 2;
      mDotBetween = mTrackWidth / (mDotNum - 1);
      mDotPosition.clear();
      //设置所有小圆点的坐标
      for (int i = 0; i < mDotNum; i++) {
        mDotPosition.add(mThumbWidth + mDotBetween * i);
      }
      mThumbCenterX = mDotPosition.get(mPositon);
      mInit = true;
    }
  }

  private void init(AttributeSet attrs) {
    mInit = false;
    TypedArray typedArray = mContext.obtainStyledAttributes(attrs, R.styleable.FilterSizeSeekBar);
    //初始化thumbdrawable及其状态
    Drawable thumb = Theme.getTintThumb(mContext);
    Drawable thumbPress = Theme.getTintThumb(mContext);
    if (thumb == null) {
      thumb = getResources().getDrawable(R.drawable.bg_circleseekbar_thumb);
    }
    if (thumbPress == null) {
      thumbPress = getResources().getDrawable(R.drawable.bg_circleseekbar_thumb);
    }

    mThumbNormal = new int[]{-android.R.attr.state_focused, -android.R.attr.state_pressed,
        -android.R.attr.state_selected, -android.R.attr.state_checked};
    mThumbPressed = new int[]{android.R.attr.state_focused, android.R.attr.state_pressed,
        android.R.attr.state_selected, android.R.attr.state_checked};
    mThumbDrawable = new StateListDrawable();
    mThumbDrawable.addState(mThumbNormal, thumb);
    mThumbDrawable.addState(mThumbPressed, thumbPress);

    //计算thumb的大小
    mThumbHeight = mThumbDrawable.getIntrinsicHeight();
    mThumbWidth = mThumbDrawable.getIntrinsicWidth();
    mThumbCenterY = mThumbHeight / 2;
    mTrackCenterY = mThumbHeight / 2;

    //轨道 已完成轨道 文字颜色
    mTrackColor = ColorUtil.getColor(
        ThemeStore.isLightTheme() ? R.color.light_scan_track_color : R.color.dark_scan_track_color);
    mProgressColor = ThemeStore.getAccentColor();
    mTextColor = ThemeStore.getTextColorPrimary();

    //小圆点数量与宽度
    mDotNum = typedArray
        .getInteger(R.styleable.FilterSizeSeekBar_dotnum, DensityUtil.dip2px(mContext, 3));
    mDotWidth = (int) typedArray
        .getDimension(R.styleable.FilterSizeSeekBar_dotwidth, DensityUtil.dip2px(mContext, 2));

    //轨道高度
    mTrackHeigh = (int) typedArray
        .getDimension(R.styleable.FilterSizeSeekBar_trackheight, DensityUtil.dip2px(mContext, 2));

    //小圆点画笔
    mDotColor = ColorUtil.shiftColor(ThemeStore.getAccentColor(), 0.8f);
    mDotPaint = new Paint();
    mDotPaint.setAntiAlias(true);
    mDotPaint.setColor(mDotColor);
    mDotPaint.setStyle(Paint.Style.FILL);

    //提示文字画笔
    mTextPaint = new Paint();
    mTextPaint.setAntiAlias(true);
    mTextPaint.setColor(mTextColor);
    mTextPaint.setStyle(Paint.Style.STROKE);
    mTextPaint.setTextSize(DensityUtil.dip2px(getContext(), 13));
    mTextPaint.setTextAlign(Paint.Align.CENTER);

    //整个轨道的画笔
    mTrackPaint = new Paint();
    mTrackPaint.setAntiAlias(true);
    mTrackPaint.setColor(mTrackColor);
    mTrackPaint.setStyle(Paint.Style.STROKE);
    mTrackPaint.setStrokeWidth(mTrackHeigh);

    //已完成轨道的画笔
    mProgressPaint = new Paint();
    mProgressPaint.setAntiAlias(true);
    mProgressPaint.setColor(mProgressColor);
    mProgressPaint.setStyle(Paint.Style.STROKE);
    mProgressPaint.setStrokeWidth(mTrackHeigh);

    typedArray.recycle();
  }

  public long getPosition() {
    return mPositon;
  }

  public void setPosition(int position) {
    if (position > mDotPosition.size()) {
      position = mDotPosition.size();
    }
    if (position < 0) {
      position = 0;
    }
    mPositon = position;
    mThumbCenterX = mDotPosition.get(mPositon);
    invalidate();
  }

  public interface OnSeekBarChangeListener {

    void onProgressChanged(FilterSizeSeekBar seekBar, int position, boolean fromUser);

    void onStartTrackingTouch(FilterSizeSeekBar seekBar);

    void onStopTrackingTouch(FilterSizeSeekBar seekBar);
  }

  public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
    mOnSeekBarChangeListener = l;
  }

  //是否初始化完成
  public boolean isInit() {
    return mInit;
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
  }
}
