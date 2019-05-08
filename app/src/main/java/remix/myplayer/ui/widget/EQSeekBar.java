//package remix.myplayer.ui.widget;
//
//import android.content.Context;
//import android.content.res.TypedArray;
//import android.graphics.Canvas;
//import android.graphics.Paint;
//import android.graphics.drawable.Drawable;
//import android.util.AttributeSet;
//import android.view.MotionEvent;
//import android.view.View;
//import java.util.ArrayList;
//import remix.myplayer.R;
//import remix.myplayer.theme.Theme;
//import remix.myplayer.theme.ThemeStore;
//import remix.myplayer.util.ColorUtil;
//import remix.myplayer.util.DensityUtil;
//
///**
// * Created by taeja on 16-4-13.
// */
//public class EQSeekBar extends View {
//
//  public interface OnSeekBarChangeListener {
//
//    void onProgressChanged(EQSeekBar seekBar, int position, boolean fromUser);
//
//    void onStartTrackingTouch(EQSeekBar seekBar);
//
//    void onStopTrackingTouch(EQSeekBar seekBar);
//  }
//
//  private OnSeekBarChangeListener mOnSeekBarChangeListener;
//  private static final String TAG = "CustomSeekBar";
//  private Context mContext;
//  /**
//   * 控件高度与宽度
//   */
//  private int mViewWidth;
//  private int mViewHeight;
//
//
//  /**
//   * 底部提示字体画笔
//   */
//  private Paint mTipTextPaint;
//
//  /**
//   * 底部提示字体大小
//   */
//  private int mTipTextSize = 12;
//
//  /**
//   * 顶部提示字体大小
//   */
//  private int mFreTextSize = 12;
//
//  /**
//   * 顶部提示字体画笔
//   */
//  private Paint mFreTextPaint;
//
//  /**
//   * 轨道垂直中心点
//   */
//  private int mTrackCenterY;
//
//  /**
//   * 使能状态下轨道的颜色
//   */
//  private int mEnableTrackColor;
//
//  /**
//   * 非使能状态下轨道颜色
//   */
//  private int mTrackColor;
//
//  /**
//   * 使能状态下文字颜色
//   */
//
//  private int mEnableTextColor;
//  /**
//   * 非使能状态下文字颜色
//   */
//  private int mTextColor;
//
//  /**
//   * 轨道画笔
//   */
//  private Paint mTrackPaint;
//
//  /**
//   * 使能状态下已完成轨道的颜色
//   */
//  private int mEnableProgressColor;
//
//  /**
//   * 非使能状态下已完成轨道颜色
//   */
//  private int mProgressColor;
//
//  /**
//   * 已完成轨道的画笔
//   */
//  private Paint mProgressPaint;
//
//  /**
//   * 轨道的高度与长度
//   */
//  private int mTrackHeigh;
//  private int mTrackWidth;
//
//  /**
//   * 总共几个小圆点
//   */
//  private int mDotNum;
//
//  /**
//   * 两个小圆点之间的距离
//   */
//  private int mDotBetween;
//
//  /**
//   * 所有小圆点的坐标
//   */
//  private ArrayList<Integer> mDotPosition = new ArrayList<>();
//
//  /**
//   * Thumb的高度与宽度
//   */
//  private int mThumbWidth = 50;
//  private int mThumbHeight = 50;
//
//  /**
//   * ThumbDrawable 以及两个状态
//   */
//  private Drawable mThumbDrawable = null;
//  private int[] mThumbNormal = null;
//  private int[] mThumbPressed = null;
//
//  /**
//   * Thumb所在位置
//   */
//  private int mThumbCenterX;
//  private int mThumbCenterY;
//
//  /**
//   * 是否初始化完成
//   */
//  private boolean mInit = false;
//
//  /**
//   * 当前索引
//   */
//  private int mPositon;
//
//  /**
//   * 底部提示文字
//   */
//  private String mFreText = "100";
//
//  /**
//   * 顶部提示文字
//   */
//  private String mDBText = "0";
//
//  /**
//   * DB值
//   */
//  private int mDB = 0;
//
//  /**
//   * 当前进度
//   */
//  private int mProgress = 0;
//
//  /**
//   * 进度最大值
//   */
//  private int mMax = 3000;
//
//  /**
//   * 是否能够拖动
//   */
//  private boolean mCanDrag = true;
//
//  /**
//   * 是否使能
//   */
//  private boolean mEnable = false;
//
//  public EQSeekBar(Context context) {
//    super(context);
//    init(context, null);
//
//  }
//
//  public EQSeekBar(Context context, AttributeSet attrs) {
//    super(context, attrs);
//    init(context, attrs);
//  }
//
//  public EQSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
//    super(context, attrs, defStyleAttr);
//    init(context, attrs);
//  }
//
//  private void init(Context context, AttributeSet attributeSet) {
//    mInit = false;
//    mContext = context;
//    TypedArray typedArray = mContext.obtainStyledAttributes(attributeSet, R.styleable.EQSeekBar);
//    //初始化thumbdrawable及其状态
//    mThumbDrawable = Theme.getTintThumb(mContext);
//    if (mThumbDrawable == null) {
//      mThumbDrawable = getResources().getDrawable(R.drawable.bg_circleseekbar_thumb);
//    }
//
//    //计算thumb的大小
//    mThumbHeight = mThumbDrawable.getIntrinsicHeight();
//    mThumbWidth = mThumbDrawable.getIntrinsicWidth();
//    mThumbCenterX = mThumbHeight / 2;
//    mTrackCenterY = mThumbHeight / 2;
//
//    //todo
//    //使能状态下的颜色
//    mEnableTrackColor = typedArray.getColor(R.styleable.EQSeekBar_eqenabletrackcolor,
//        ColorUtil.getColor(ThemeStore.isLightTheme() ? R.color.light_enable_track_color
//            : R.color.dark_enable_track_color));
////        mEnableProgressColor = typedArray.getColor(R.styleable.EQSeekBar_eqenableprogresscolor,
////                ColorUtil.getColor(ThemeStore.isLightTheme()() ? ThemeStore.getMaterialPrimaryColorRes() : R.color.night_nonenable_progress_color));
//    mEnableTextColor = typedArray.getColor(R.styleable.EQSeekBar_eqtextcolor,
//        ColorUtil.getColor(ThemeStore.isLightTheme() ? R.color.light_enable_text_color
//            : R.color.dark_enable_text_color));
//
//    //非使能状态下的颜色
//    mTrackColor = typedArray.getColor(R.styleable.EQSeekBar_eqtrackcolor,
//        ColorUtil.getColor(ThemeStore.isLightTheme() ? R.color.light_nonenable_track_color
//            : R.color.dark_nonenable_track_color));
//    mProgressColor = typedArray.getColor(R.styleable.EQSeekBar_eqprogresscolor,
//        ColorUtil.getColor(ThemeStore.isLightTheme() ? R.color.light_nonenable_progress_color
//            : R.color.dark_nonenable_progress_color));
//    mTextColor = typedArray.getColor(R.styleable.EQSeekBar_eqtextcolor,
//        ColorUtil.getColor(ThemeStore.isLightTheme() ? R.color.light_nonenable_text_color
//            : R.color.dark_nonenable_text_color));
//
//    //间隔点数量
//    mDotNum = typedArray.getInteger(R.styleable.EQSeekBar_eqdotnum, 31);
//
//    //轨道宽度
//    mTrackWidth = (int) typedArray
//        .getDimension(R.styleable.EQSeekBar_eqtrackwidth, DensityUtil.dip2px(mContext, 3));
//
//    //顶部提示文字画笔
//    mFreTextSize = DensityUtil.dip2px(getContext(), 13);
//    mFreTextPaint = new Paint();
//    mFreTextPaint.setAntiAlias(true);
//    mFreTextPaint.setColor(mEnable ? mEnableTextColor : mTextColor);
//    mFreTextPaint.setStyle(Paint.Style.STROKE);
//    mFreTextPaint.setTextSize(mFreTextSize);
//    mFreTextPaint.setTextAlign(Paint.Align.CENTER);
//
//    //底部提示文字画笔
//    mTipTextSize = DensityUtil.dip2px(getContext(), 14);
//    mTipTextPaint = new Paint();
//    mTipTextPaint.setAntiAlias(true);
//    mTipTextPaint.setColor(mEnable ? mEnableTextColor : mTextColor);
//    mTipTextPaint.setStyle(Paint.Style.STROKE);
//    mTipTextPaint.setTextSize(mTipTextSize);
//    mTipTextPaint.setTextAlign(Paint.Align.CENTER);
//
//    //整个轨道的画笔
//    mTrackPaint = new Paint();
//    mTrackPaint.setAntiAlias(true);
//    mTrackPaint.setColor(mEnable ? mEnableTrackColor : mTrackColor);
//    mTrackPaint.setStyle(Paint.Style.STROKE);
//    mTrackPaint.setStrokeWidth(mTrackWidth);
//
//    //已完成轨道的画笔
//    mProgressPaint = new Paint();
//    mProgressPaint.setAntiAlias(true);
//    mProgressPaint.setColor(mEnable ? mEnableProgressColor : mProgressColor);
//    mProgressPaint.setStyle(Paint.Style.STROKE);
//    mProgressPaint.setStrokeWidth(mTrackWidth);
//
//    typedArray.recycle();
//  }
//
//
//  public void setFreText(String freText) {
//    mFreText = freText;
//  }
//
//  private int getMaxTextSize() {
//    return mTipTextSize > mFreTextSize ? mTipTextSize : mFreTextSize;
//  }
//
//  @Override
//  protected synchronized void onDraw(Canvas canvas) {
//    //整个轨道
//    canvas.drawLine(mViewWidth / 2, mThumbWidth * 2, mViewWidth / 2, mThumbWidth * 2 + mTrackHeigh,
//        mTrackPaint);
//    //已完成轨道
//    canvas.drawLine(mViewWidth / 2, mThumbWidth * 2, mViewWidth / 2, mThumbCenterY, mProgressPaint);
//
////      //顶部与底部文字
//    int y = mThumbWidth > getMaxTextSize() ? mThumbWidth : getMaxTextSize();
//    canvas.drawText(mDBText, mViewWidth / 2, y, mFreTextPaint);
//    canvas.drawText(mFreText, mViewWidth / 2, y + mTrackHeigh + mThumbWidth * 2.5f, mTipTextPaint);
//
//    //thumb
//    mThumbDrawable.setBounds((mViewWidth - mThumbWidth) / 2,
//        mThumbCenterY - mThumbWidth / 2,
//        (mViewWidth - mThumbWidth) / 2 + mThumbWidth,
//        mThumbCenterY + mThumbWidth / 2);
//    mThumbDrawable.draw(canvas);
//  }
//
//  private void seekTo(int eventY, boolean isUp) {
//    if (!mCanDrag) {
//      return;
//    }
//
//    //设置thumb状态
//    mThumbDrawable.setState(isUp ? mThumbNormal : mThumbPressed);
//    if (eventY > mDotPosition.get(mDotPosition.size() - 1) || eventY < mDotPosition.get(0)) {
//      invalidate();
//      return;
//    }
//
//    float temp = Integer.MAX_VALUE;
//    for (int i = 0; i < mDotPosition.size(); i++) {
//      if (Math.abs(mDotPosition.get(i) - eventY) < temp) {
//        mPositon = i;
//        temp = Math.abs(mDotPosition.get(i) - eventY);
//      }
//    }
//    //寻找与当前触摸点最近的值
//    if (isUp) {
//      mThumbCenterY = Math.round(mDotPosition.get(mPositon));
//    } else {
//      mThumbCenterY = eventY;
//    }
//
//    //计算progress
//    mProgress = (int) (1.0 * (eventY - mThumbHeight) / mTrackHeigh * mMax);
//
//    if (mOnSeekBarChangeListener != null) {
//      mOnSeekBarChangeListener.onProgressChanged(this, mPositon, true);
//    }
//
//    //确定DB值
//    mDB = 15 - mPositon;
//
//    if (mDB > 0) {
//      mDBText = "+" + mDB;
//    } else if (mDB < 0) {
//      mDBText = mDB + "";
//    } else {
//      mDBText = "0";
//    }
//
//    invalidate();
//  }
//
//  @Override
//  public boolean onTouchEvent(MotionEvent event) {
//    int eventY = (int) event.getY();
//    seekTo(eventY, event.getAction() == MotionEvent.ACTION_UP);
//    return true;
//  }
//
//  @Override
//  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//
//    if ((mViewWidth = getMeasuredWidth()) > 0 && (mViewHeight = getMeasuredHeight()) > 0) {
//      int paddingtop = getPaddingTop();
//      int paddingbottom = getPaddingBottom();
//      mTrackHeigh = mViewHeight - paddingtop - paddingbottom - mThumbWidth * 4;
//      //计算轨道宽度 两个小圆点之间的距离
//      mDotBetween = Math.round(mTrackHeigh * 1.0f / (mDotNum - 1));
//      mDotPosition.clear();
//      //设置所有小圆点的坐标
//      for (int i = 0; i < mDotNum; i++) {
//        mDotPosition.add(mThumbWidth * 2 + mDotBetween * i);
//      }
//      mThumbCenterY = Math.round(mDotPosition.get(mPositon));
//      setProgress(mProgress);
//      mInit = true;
//    }
//  }
//
//  public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
//    mOnSeekBarChangeListener = l;
//  }
//
//
//  public void setMax(int max) {
//    mMax = max;
//  }
//
//  public int getMax() {
//    return mMax;
//  }
//
//  public void setProgress(int progress) {
//    if (progress > mMax) {
//      progress = mMax;
//    }
//    if (progress < 0) {
//      progress = 0;
//    }
//    mProgress = progress;
//    int eventY = (int) ((1.0 * mProgress / mMax) * mTrackHeigh + mThumbHeight * 2);
//    if (mInit) {
//      seekTo(eventY, true);
//    }
//    //mThumbCenterY
////        int temp = Integer.MAX_VALUE;
////        for(int i = 0 ; i < mDotPosition.size() ;i++){
////            if(Math.abs(mDotPosition.get(i) - eventY) < temp){
////                mPositon = i;
////                temp = Math.abs(mDotPosition.get(i) - eventY);
////            }
////        }
//  }
//
//  public int getProgress() {
//    return mProgress;
//  }
//
//  public void setDrag(boolean canDrag) {
//    mCanDrag = canDrag;
//  }
//
//  public boolean canDrag() {
//    return mCanDrag;
//  }
//
//  public void setProgressColor(int color) {
//    mProgressColor = color;
//    if (mProgressPaint != null) {
//      mProgressPaint.setColor(mProgressColor);
//      invalidate();
//    }
//  }
//
//  public void setTrackColor(int color) {
//    mEnableTrackColor = color;
//    if (mTrackPaint != null) {
//      mTrackPaint.setColor(color);
//      invalidate();
//    }
//  }
//
//  @Override
//  public void setEnabled(boolean enabled) {
//    mCanDrag = enabled;
//    mProgressPaint.setColor(enabled ? mEnableProgressColor : mProgressColor);
//    mTrackPaint.setColor(enabled ? mEnableTrackColor : mTrackColor);
//    mFreTextPaint.setColor(enabled ? mEnableTextColor : mTextColor);
//    mTipTextPaint.setColor(enabled ? mEnableTextColor : mTextColor);
//    invalidate();
//  }
//
//  //是否初始化完成
//  public boolean isInit() {
//    return mInit;
//  }
//}
//
