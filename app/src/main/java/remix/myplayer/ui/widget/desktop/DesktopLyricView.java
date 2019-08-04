package remix.myplayer.ui.widget.desktop;

import static remix.myplayer.service.MusicService.EXTRA_DESKTOP_LYRIC;
import static remix.myplayer.theme.ThemeStore.getFloatLyricTextColor;
import static remix.myplayer.theme.ThemeStore.saveFloatLyricTextColor;
import static remix.myplayer.ui.adapter.DesktopLyricColorAdapter.COLORS;
import static remix.myplayer.util.MusicUtil.makeCmdIntent;
import static remix.myplayer.util.Util.sendLocalBroadcast;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.afollestad.materialdialogs.internal.MDTintHelper;
import remix.myplayer.R;
import remix.myplayer.lyric.bean.LrcRow;
import remix.myplayer.misc.handler.MsgHandler;
import remix.myplayer.misc.handler.OnHandleMessage;
import remix.myplayer.misc.interfaces.OnItemClickListener;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
import remix.myplayer.ui.adapter.DesktopLyricColorAdapter;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.SPUtil.SETTING_KEY;
import remix.myplayer.util.ToastUtil;
import remix.myplayer.util.Util;
import timber.log.Timber;


/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/3/22 15:47
 */

public class DesktopLyricView extends RelativeLayout {

  private WindowManager mWindowManager;
  private MusicService mService;
  private PointF mLastPoint = new PointF();
  private boolean mIsLock = false;
  private MsgHandler mUIHandler = new MsgHandler(this);
  @BindView(R.id.widget_line1)
  DesktopLyricTextView mText1;
  @BindView(R.id.widget_line2)
  TextView mText2;
  @BindView(R.id.widget_pannel)
  ViewGroup mPanel;
  @BindView(R.id.widget_lock)
  ImageView mLock;
  @BindView(R.id.widget_close)
  ImageView mClose;
  @BindView(R.id.widget_next)
  ImageView mNext;
  @BindView(R.id.widget_play)
  ImageView mPlay;
  @BindView(R.id.widget_prev)
  ImageView mPrev;
  @BindView(R.id.widget_color_recyclerview)
  RecyclerView mColorRecyclerView;
  @BindView(R.id.widget_control_container)
  View mControlContainer;
  @BindView(R.id.widget_lrc_container)
  View mLrcSettingContainer;
  @BindView(R.id.widget_root)
  ViewGroup mRoot;
  @BindView(R.id.widget_seekbar_r)
  SeekBar mSeekBarR;
  @BindView(R.id.widget_seekbar_g)
  SeekBar mSeekBarG;
  @BindView(R.id.widget_seekbar_b)
  SeekBar mSeekBarB;
  @BindView(R.id.widget_text_r)
  TextView mTextR;
  @BindView(R.id.widget_text_g)
  TextView mTextG;
  @BindView(R.id.widget_text_b)
  TextView mTextB;


  private DesktopLyricColorAdapter mColorAdapter;

//  private UnLockNotify mNotify;

  //当前字体大小
  private static final int TINY = 0;
  private static final int SMALL = 1;
  private static final int MEDIUM = 2;
  private static final int BIG = 3;
  private static final int HUGE = 4;

  //第一行歌词字体大小
  private static final int FIRST_LINE_HUGE = 20;
  private static final int FIRST_LINE_BIG = 19;
  private static final int FIRST_LINE_MEDIUM = 18;
  private static final int FIRST_LINE_SMALL = 17;
  private static final int FIRST_LINE_TINY = 16;
  //第二行歌词字体大小
  private static final int SECOND_LINE_HUGE = 18;
  private static final int SECOND_LINE_BIG = 17;
  private static final int SECOND_LINE_MEDIUM = 16;
  private static final int SECOND_LINE_SMALL = 15;
  private static final int SECOND_LINE_TINY = 14;

  private static final int TYPE_TEXT_SIZE_FIRST_LINE = 0;
  private static final int TYPE_TEXT_SIZE_SECOND_LINE = 1;

  private int mTextSizeType = MEDIUM;
  private Runnable mHideRunnable = new Runnable() {
    @Override
    public void run() {
      mPanel.setVisibility(GONE);
      mLrcSettingContainer.setVisibility(GONE);
    }
  };

  private SeekBar.OnSeekBarChangeListener mOnSeekBarChangeListener = new OnSeekBarChangeListener() {
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
      final int temp = Color.rgb(mSeekBarR.getProgress(), mSeekBarG.getProgress(), mSeekBarB.getProgress());
      final int color = ColorUtil.isColorCloseToWhite(temp) ? Color.parseColor("#F9F9F9") : temp;
      mText1.setTextColor(color);
      MDTintHelper.setTint(mSeekBarR, color);
      MDTintHelper.setTint(mSeekBarG, color);
      MDTintHelper.setTint(mSeekBarB, color);
      mTextR.setTextColor(color);
      mTextG.setTextColor(color);
      mTextB.setTextColor(color);
      resetHide();

      mUIHandler.removeMessages(MESSAGE_SAVE_COLOR);
      mUIHandler.sendMessageDelayed(Message.obtain(mUIHandler, MESSAGE_SAVE_COLOR, color, 0), 100);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
  };

  public DesktopLyricView(MusicService service) {
    super(service);
    init(service);
  }


  private void init(Context context) {
    mService = (MusicService) context;
//    mNotify = new UnLockNotify();
    mWindowManager = (WindowManager) mService.getSystemService(Context.WINDOW_SERVICE);

    ButterKnife.bind(this, inflate(mService, R.layout.layout_desktop_lyric, this));
    setUpView();
  }

  private void setUpColor() {
    mColorRecyclerView.getViewTreeObserver()
        .addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
          @Override
          public boolean onPreDraw() {
            mColorRecyclerView.getViewTreeObserver().removeOnPreDrawListener(this);
            mColorAdapter = new DesktopLyricColorAdapter(mService, R.layout.item_float_lrc_color,
                mColorRecyclerView.getMeasuredWidth());
            mColorAdapter.setOnItemClickListener(new OnItemClickListener() {
              @Override
              public void onItemClick(View view, int position) {
                final int color = ColorUtil.getColor(COLORS.get(position));
                mText1.setTextColor(color);
                mColorAdapter.setCurrentColor(color);
                mColorAdapter.notifyDataSetChanged();
                resetHide();
              }

              @Override
              public void onItemLongClick(View view, int position) {

              }
            });
            mColorRecyclerView.setLayoutManager(
                new LinearLayoutManager(mService, LinearLayoutManager.HORIZONTAL, false));
            mColorRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            mColorRecyclerView.setAdapter(mColorAdapter);
            return true;
          }
        });
  }

  private void setUpView() {
    final int temp = getFloatLyricTextColor();
    final int color = ColorUtil.isColorCloseToWhite(temp) ? Color.parseColor("#F9F9F9") : temp;
    final int red = (color & 0xff0000) >> 16;
    final int green = (color & 0x00ff00) >> 8;
    final int blue = (color & 0x0000ff);
    mSeekBarR.setMax(255);
    mSeekBarR.setProgress(red);
    mSeekBarG.setMax(255);
    mSeekBarG.setProgress(green);
    mSeekBarB.setMax(255);
    mSeekBarB.setProgress(blue);
    mTextR.setTextColor(color);
    mTextG.setTextColor(color);
    mTextB.setTextColor(color);
    mSeekBarR.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
    mSeekBarG.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
    mSeekBarB.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
    MDTintHelper.setTint(mSeekBarR, color);
    MDTintHelper.setTint(mSeekBarG, color);
    MDTintHelper.setTint(mSeekBarB, color);

    mText1.setTextColor(color);
    mText1.setTextSize(getTextSize(TYPE_TEXT_SIZE_FIRST_LINE));
    mText2.setTextSize(getTextSize(TYPE_TEXT_SIZE_SECOND_LINE));
    mIsLock = SPUtil.getValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_LOCK, false);

    mTextSizeType = SPUtil
        .getValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_TEXT_SIZE, MEDIUM);
    setPlayIcon(mService.isPlaying());

    getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
      @Override
      public boolean onPreDraw() {
        getViewTreeObserver().removeOnPreDrawListener(this);
        saveLock(mIsLock, false);
        return true;
      }
    });
  }

  /**
   * @param type 0: 第一行 1:第二行
   */
  private float getTextSize(int type) {
    if (type == TYPE_TEXT_SIZE_FIRST_LINE) {
      return mTextSizeType == TINY ? FIRST_LINE_TINY : mTextSizeType == SMALL ? FIRST_LINE_SMALL
          : mTextSizeType == MEDIUM ? FIRST_LINE_MEDIUM : mTextSizeType == BIG ? FIRST_LINE_BIG : FIRST_LINE_HUGE;
    } else if (type == TYPE_TEXT_SIZE_SECOND_LINE) {
      return mTextSizeType == TINY ? SECOND_LINE_TINY : mTextSizeType == SMALL ? SECOND_LINE_SMALL
          : mTextSizeType == MEDIUM ? SECOND_LINE_MEDIUM : mTextSizeType == BIG ? SECOND_LINE_BIG : SECOND_LINE_HUGE;
    } else {
      throw new IllegalArgumentException("unknown textSize type");
    }
  }

  public void setText(LrcRow lrc1, LrcRow lrc2) {
    if (lrc1 != null) {
      if (TextUtils.isEmpty(lrc1.getContent())) {
        lrc1.setContent("......");
      }
      mText1.setLrcRow(lrc1);
    }
    if (lrc2 != null) {
      if (TextUtils.isEmpty(lrc2.getContent())) {
        lrc2.setContent(".....");
      }
      mText2.setText(lrc2.getContent());
    }
  }

  private static final int DISTANCE_THRESHOLD = 10;
  private static final int DISMISS_THRESHOLD = 4500;
  private static final int LONGCLICK_THRESHOLD = 1000;
  /**
   * 当前是否正在拖动
   */
  private boolean mIsDragging = false;

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    switch (event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        if (!mIsLock) {
          mIsDragging = false;
          mLastPoint.set(event.getRawX(), event.getRawY());
          mUIHandler.removeCallbacks(mHideRunnable);
        } else {
//                    mUIHandler.postDelayed(mLongClickRunnable,LONGCLICK_THRESHOLD);
        }
        break;
      case MotionEvent.ACTION_MOVE:
        if (!mIsLock) {
          WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();

          if (Math.abs(event.getRawY() - mLastPoint.y) > DISTANCE_THRESHOLD) {
            params.y += (int) (event.getRawY() - mLastPoint.y);
            mIsDragging = true;
            if (VERSION.SDK_INT >= VERSION_CODES.KITKAT && isAttachedToWindow()) {
              mWindowManager.updateViewLayout(this, params);
            }
          }
          mLastPoint.set(event.getRawX(), event.getRawY());
        }
        break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
        if (!mIsLock) {
          if (!mIsDragging) {
            //点击后隐藏或者显示操作栏
            if (mPanel.isShown()) {
              mPanel.setVisibility(INVISIBLE);
            } else {
              mPanel.setVisibility(VISIBLE);
              mUIHandler.postDelayed(mHideRunnable, DISMISS_THRESHOLD);
            }
          } else {
            //滑动
            if (mPanel.isShown()) {
              mUIHandler.postDelayed(mHideRunnable, DISMISS_THRESHOLD);
            }
            mIsDragging = false;
          }
          //保存y坐标
          WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
          SPUtil.putValue(mService, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_Y, params.y);
        } else {
//                    mUIHandler.removeCallbacks(mLongClickRunnable);
        }
        break;
    }
    return true;
  }

  public void setPlayIcon(boolean play) {
    mPlay.setImageResource(
        play ? R.drawable.widget_btn_stop_normal : R.drawable.widget_btn_play_normal);
  }

  public void stopAnimation() {
    mText1.stopAnimation();
  }

  @OnClick({R.id.widget_close, R.id.widget_lock, R.id.widget_next, R.id.widget_play,
      R.id.widget_prev, R.id.widget_lrc_bigger, R.id.widget_lrc_smaller, R.id.widget_setting})
  public void onViewClicked(View view) {
    switch (view.getId()) {
      //关闭桌面歌词
      case R.id.widget_close:
        SPUtil.putValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_SHOW,
            false);
        sendLocalBroadcast(
            makeCmdIntent(Command.TOGGLE_DESKTOP_LYRIC).putExtra(EXTRA_DESKTOP_LYRIC, false));
        break;
      //锁定
      case R.id.widget_lock:
        saveLock(true, true);
        mUIHandler.postDelayed(mHideRunnable, 0);
        Util.sendCMDLocalBroadcast(Command.LOCK_DESKTOP_LYRIC);
//                mUnLock.setVisibility(VISIBLE);
        break;
      //歌词字体、大小设置
      case R.id.widget_setting:
        mLrcSettingContainer.setVisibility(mLrcSettingContainer.isShown() ? GONE : VISIBLE);
        setUpColor();
        //操作后重置消息的时间
        resetHide();
        break;
      case R.id.widget_next:
      case R.id.widget_play:
      case R.id.widget_prev:
        sendLocalBroadcast(makeCmdIntent(view.getId() == R.id.widget_next ? Command.NEXT
            : view.getId() == R.id.widget_prev ? Command.PREV : Command.TOGGLE));
        mUIHandler.postDelayed(() -> mPlay.setImageResource(
            mService.isPlaying() ? R.drawable.widget_btn_stop_normal
                : R.drawable.widget_btn_play_normal), 100);
        //操作后重置消息的时间
        resetHide();
        break;
      //字体放大、缩小
      case R.id.widget_lrc_bigger:
      case R.id.widget_lrc_smaller:
        boolean needRefresh = false;
        if (view.getId() == R.id.widget_lrc_bigger) {
          //当前已经是最大字体
          if (mTextSizeType == HUGE) {
            break;
          }
          mTextSizeType++;
          needRefresh = true;
        }
        if (view.getId() == R.id.widget_lrc_smaller) {
          //当前已经是最小字体
          if (mTextSizeType == TINY) {
            break;
          }
          mTextSizeType--;
          needRefresh = true;
        }
        if (needRefresh) {
          mText1.setTextSize(getTextSize(TYPE_TEXT_SIZE_FIRST_LINE));
          mText2.setTextSize(getTextSize(TYPE_TEXT_SIZE_SECOND_LINE));
          SPUtil.putValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_TEXT_SIZE, mTextSizeType);
          //操作后重置消息的时间
          resetHide();
        }
        break;
    }
  }

  public void saveLock(boolean lock, boolean toast) {
    mIsLock = lock;
    SPUtil.putValue(mService, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DESKTOP_LYRIC_LOCK, mIsLock);
    if (toast) {
      ToastUtil.show(mService, !mIsLock ? R.string.desktop_lyric__unlock : R.string.desktop_lyric_lock);
    }
    WindowManager.LayoutParams params = (WindowManager.LayoutParams) getLayoutParams();
    if (params != null) {
      if (lock) {
        //锁定后点击通知栏解锁
//        mNotify.notifyToUnlock();
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
      } else {
//        mNotify.cancel();
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
      }
      if (VERSION.SDK_INT >= VERSION_CODES.KITKAT && isAttachedToWindow()) {
        mWindowManager.updateViewLayout(this, params);
      }
    }
  }

  /**
   * 应用退出后清除通知
   */
//  public void cancelNotify() {
//    Timber.v("取消解锁通知");
//    if (mNotify != null) {
//      mNotify.cancel();
//    }
//  }

  /**
   * 操作后重置消失的时间
   */
  private void resetHide() {
    mUIHandler.removeCallbacks(mHideRunnable);
    mUIHandler.postDelayed(mHideRunnable, DISMISS_THRESHOLD);
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    mUIHandler.removeCallbacksAndMessages(null);
    mSeekBarR.setOnSeekBarChangeListener(null);
    mSeekBarG.setOnSeekBarChangeListener(null);
    mSeekBarB.setOnSeekBarChangeListener(null);
    Timber.v("onDetachedFromWindow");
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    Timber.v("onAttachedToWindow");
  }

  public boolean isLocked() {
    return mIsLock;
  }


  private static final int MESSAGE_SAVE_COLOR = 1;

  @OnHandleMessage
  public void handleMsg(Message msg) {
    switch (msg.what) {
      case MESSAGE_SAVE_COLOR:
        saveFloatLyricTextColor(msg.arg1);
        break;
    }
  }

//  private static class UnLockNotify {
//
//    private static final String UNLOCK_NOTIFICATION_CHANNEL_ID = "unlock_notification";
//    private static final int UNLOCK_NOTIFICATION_ID = 2;
//    private Context mContext;
//    private NotificationManager mNotificationManager;
//
//    UnLockNotify() {
//      mContext = App.getContext();
//      mNotificationManager = (NotificationManager) mContext
//          .getSystemService(Context.NOTIFICATION_SERVICE);
//      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//        initNotificationChanel();
//      }
//    }
//
//    @RequiresApi(api = Build.VERSION_CODES.O)
//    private void initNotificationChanel() {
//      NotificationChannel notificationChannel = new NotificationChannel(
//          UNLOCK_NOTIFICATION_CHANNEL_ID, mContext.getString(R.string.unlock_notification),
//          NotificationManager.IMPORTANCE_LOW);
//      notificationChannel.setShowBadge(false);
//      notificationChannel.enableLights(false);
//      notificationChannel.enableVibration(false);
//      notificationChannel
//          .setDescription(mContext.getString(R.string.unlock_notification_description));
//      mNotificationManager.createNotificationChannel(notificationChannel);
//    }
//
//    void notifyToUnlock() {
//      Notification notification = new NotificationCompat.Builder(mContext,
//          UNLOCK_NOTIFICATION_CHANNEL_ID)
//          .setContentText(mContext.getString(R.string.desktop_lyric_lock))
//          .setContentTitle(mContext.getString(R.string.click_to_unlock))
//          .setShowWhen(false)
//          .setPriority(NotificationCompat.PRIORITY_DEFAULT)
//          .setOngoing(true)
//          .setTicker(mContext.getString(R.string.desktop_lyric__lock_ticker))
//          .setContentIntent(buildPendingIntent())
//          .setSmallIcon(R.drawable.icon_notifbar)
//          .build();
//      mNotificationManager.notify(UNLOCK_NOTIFICATION_ID, notification);
//    }
//
//    void cancel() {
//      mNotificationManager.cancel(UNLOCK_NOTIFICATION_ID);
//    }
//
//    PendingIntent buildPendingIntent() {
//      Intent intent = new Intent(MusicService.ACTION_CMD);
//      intent.putExtra("Control", Command.UNLOCK_DESKTOP_LYRIC);
//      intent.setComponent(new ComponentName(mContext, MusicService.class));
//      return PendingIntent.getService(mContext, Command.UNLOCK_DESKTOP_LYRIC, intent,
//          PendingIntent.FLAG_UPDATE_CURRENT);
//    }
//  }
}
