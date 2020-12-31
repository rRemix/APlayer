package remix.myplayer.ui.widget.desktop

import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Message
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.text.TextUtils
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.internal.MDTintHelper
import remix.myplayer.R
import remix.myplayer.lyric.bean.LrcRow
import remix.myplayer.misc.handler.MsgHandler
import remix.myplayer.misc.handler.OnHandleMessage
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicService.Companion.EXTRA_DESKTOP_LYRIC
import remix.myplayer.theme.ThemeStore.getFloatLyricTextColor
import remix.myplayer.theme.ThemeStore.saveFloatLyricTextColor
import remix.myplayer.ui.adapter.DesktopLyricColorAdapter
import remix.myplayer.ui.adapter.DesktopLyricColorAdapter.COLORS
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.MusicUtil.makeCmdIntent
import remix.myplayer.util.SPUtil
import remix.myplayer.util.SPUtil.SETTING_KEY
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util
import remix.myplayer.util.Util.sendLocalBroadcast
import timber.log.Timber
import kotlin.math.abs


/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/3/22 15:47
 */

class DesktopLyricView(service: MusicService) : RelativeLayout(service) {

  private lateinit var mWindowManager: WindowManager
  private lateinit var mService: MusicService
  private val mLastPoint = PointF()
  var isLocked = false
    private set
  private val mUIHandler = MsgHandler(this)
  @BindView(R.id.widget_line1)
  lateinit var mText1: DesktopLyricTextView
  @BindView(R.id.widget_line2)
  lateinit var mText2: TextView
  @BindView(R.id.widget_pannel)
  lateinit var mPanel: ViewGroup
  @BindView(R.id.widget_lock)
  lateinit var mLock: ImageView
  @BindView(R.id.widget_close)
  lateinit var mClose: ImageView
  @BindView(R.id.widget_next)
  lateinit var mNext: ImageView
  @BindView(R.id.widget_play)
  lateinit var mPlay: ImageView
  @BindView(R.id.widget_prev)
  lateinit var mPrev: ImageView
  @BindView(R.id.widget_color_recyclerview)
  lateinit var mColorRecyclerView: RecyclerView
  @BindView(R.id.widget_control_container)
  lateinit var mControlContainer: View
  @BindView(R.id.widget_lrc_container)
  lateinit var mLrcSettingContainer: View
  @BindView(R.id.widget_root)
  lateinit var mRoot: ViewGroup
  @BindView(R.id.widget_seekbar_r)
  lateinit var mSeekBarR: SeekBar
  @BindView(R.id.widget_seekbar_g)
  lateinit var mSeekBarG: SeekBar
  @BindView(R.id.widget_seekbar_b)
  lateinit var mSeekBarB: SeekBar
  @BindView(R.id.widget_text_r)
  lateinit var mTextR: TextView
  @BindView(R.id.widget_text_g)
  lateinit var mTextG: TextView
  @BindView(R.id.widget_text_b)
  lateinit var mTextB: TextView


  private lateinit var mColorAdapter: DesktopLyricColorAdapter

  private var mTextSizeType = MEDIUM
  private val mHideRunnable = Runnable {
    mPanel.visibility = View.GONE
    mLrcSettingContainer.visibility = View.GONE
  }

  private val mOnSeekBarChangeListener = object : OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
      val temp = Color.rgb(mSeekBarR.progress, mSeekBarG.progress, mSeekBarB.progress)
      val color = if (ColorUtil.isColorCloseToWhite(temp)) Color.parseColor("#F9F9F9") else temp
      mText1.setTextColor(color)
      MDTintHelper.setTint(mSeekBarR, color)
      MDTintHelper.setTint(mSeekBarG, color)
      MDTintHelper.setTint(mSeekBarB, color)
      mTextR.setTextColor(color)
      mTextG.setTextColor(color)
      mTextB.setTextColor(color)
      resetHide()

      mUIHandler.removeMessages(MESSAGE_SAVE_COLOR)
      mUIHandler.sendMessageDelayed(Message.obtain(mUIHandler, MESSAGE_SAVE_COLOR, color, 0), 100)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {

    }
  }
  /**
   * 当前是否正在拖动
   */
  private var mIsDragging = false

  init {
    init(service)
  }


  private fun init(context: Context) {
    mService = context as MusicService
    //    mNotify = new UnLockNotify();
    mWindowManager = mService.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    ButterKnife.bind(this, View.inflate(mService, R.layout.layout_desktop_lyric, this))
    setUpView()
  }

  private fun setUpColor() {
    mColorRecyclerView.viewTreeObserver
        .addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
          override fun onPreDraw(): Boolean {
            mColorRecyclerView.viewTreeObserver.removeOnPreDrawListener(this)
            mColorAdapter = DesktopLyricColorAdapter(mService, R.layout.item_float_lrc_color, mColorRecyclerView.measuredWidth)
            mColorAdapter.setOnItemClickListener(object : OnItemClickListener {
              override fun onItemClick(view: View, position: Int) {
                val color = ColorUtil.getColor(COLORS[position])
                mText1.setTextColor(color)
                mColorAdapter.setCurrentColor(color)
                mColorAdapter.notifyDataSetChanged()
                resetHide()
              }

              override fun onItemLongClick(view: View, position: Int) {

              }
            })
            mColorRecyclerView.layoutManager = LinearLayoutManager(mService, LinearLayoutManager.HORIZONTAL, false)
            mColorRecyclerView.overScrollMode = View.OVER_SCROLL_NEVER
            mColorRecyclerView.adapter = mColorAdapter
            return true
          }
        })
  }

  private fun setUpView() {
    val temp = getFloatLyricTextColor()
    val color = if (ColorUtil.isColorCloseToWhite(temp)) Color.parseColor("#F9F9F9") else temp
    val red = color and 0xff0000 shr 16
    val green = color and 0x00ff00 shr 8
    val blue = color and 0x0000ff
    mSeekBarR.max = 255
    mSeekBarR.progress = red
    mSeekBarG.max = 255
    mSeekBarG.progress = green
    mSeekBarB.max = 255
    mSeekBarB.progress = blue
    mTextR.setTextColor(color)
    mTextG.setTextColor(color)
    mTextB.setTextColor(color)
    mSeekBarR.setOnSeekBarChangeListener(mOnSeekBarChangeListener)
    mSeekBarG.setOnSeekBarChangeListener(mOnSeekBarChangeListener)
    mSeekBarB.setOnSeekBarChangeListener(mOnSeekBarChangeListener)
    MDTintHelper.setTint(mSeekBarR, color)
    MDTintHelper.setTint(mSeekBarG, color)
    MDTintHelper.setTint(mSeekBarB, color)

    mTextSizeType = SPUtil.getValue(mService, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_TEXT_SIZE, MEDIUM)
    mText1.setTextColor(color)
    mText1.textSize = getTextSize(TYPE_TEXT_SIZE_FIRST_LINE)
    mText2.textSize = getTextSize(TYPE_TEXT_SIZE_SECOND_LINE)
    isLocked = SPUtil.getValue(mService, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_LOCK, false)

    setPlayIcon(mService.isPlaying)

    viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
      override fun onPreDraw(): Boolean {
        viewTreeObserver.removeOnPreDrawListener(this)
        saveLock(isLocked, false)
        return true
      }
    })
  }

  /**
   * @param type 0: 第一行 1:第二行
   */
  private fun getTextSize(type: Int): Float {
    return when (type) {
      TYPE_TEXT_SIZE_FIRST_LINE -> {
        (when (mTextSizeType) {
          TINY -> FIRST_LINE_TINY
          SMALL -> FIRST_LINE_SMALL
          MEDIUM -> FIRST_LINE_MEDIUM
          BIG -> FIRST_LINE_BIG
          else -> FIRST_LINE_HUGE
        }).toFloat()
      }
      TYPE_TEXT_SIZE_SECOND_LINE -> {
        (when (mTextSizeType) {
          TINY -> SECOND_LINE_TINY
          SMALL -> SECOND_LINE_SMALL
          MEDIUM -> SECOND_LINE_MEDIUM
          BIG -> SECOND_LINE_BIG
          else -> SECOND_LINE_HUGE
        }).toFloat()
      }
      else -> throw IllegalArgumentException("unknown textSize type")
    }
  }

  fun setText(lrc1: LrcRow?, lrc2: LrcRow?) {
    if (lrc1 != null) {
      if (TextUtils.isEmpty(lrc1.content)) {
        lrc1.content = "......"
      }
      mText1.setLrcRow(lrc1)
    }
    if (lrc2 != null) {
      if (TextUtils.isEmpty(lrc2.content)) {
        lrc2.content = "....."
      }
      mText2.text = lrc2.content
    }
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_DOWN -> if (!isLocked) {
        mIsDragging = false
        mLastPoint.set(event.rawX, event.rawY)
        mUIHandler.removeCallbacks(mHideRunnable)
      } else {
//        mUIHandler.postDelayed(mLongClickRunnable, LONGCLICK_THRESHOLD);
      }
      MotionEvent.ACTION_MOVE -> if (!isLocked) {
        val params = layoutParams as WindowManager.LayoutParams

        if (abs(event.rawY - mLastPoint.y) > DISTANCE_THRESHOLD) {
          params.y += (event.rawY - mLastPoint.y).toInt()
          mIsDragging = true
          if (VERSION.SDK_INT >= VERSION_CODES.KITKAT && isAttachedToWindow) {
            mWindowManager.updateViewLayout(this, params)
          }
        }
        mLastPoint.set(event.rawX, event.rawY)
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (!isLocked) {
        if (!mIsDragging) {
          //点击后隐藏或者显示操作栏
          if (mPanel.isShown) {
            mPanel.visibility = View.INVISIBLE
          } else {
            mPanel.visibility = View.VISIBLE
            mUIHandler.postDelayed(mHideRunnable, DISMISS_THRESHOLD.toLong())
          }
        } else {
          //滑动
          if (mPanel.isShown) {
            mUIHandler.postDelayed(mHideRunnable, DISMISS_THRESHOLD.toLong())
          }
          mIsDragging = false
        }
        //保存y坐标
        val params = layoutParams as WindowManager.LayoutParams
        SPUtil.putValue(mService, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_Y, params.y)
      } else {
//        mUIHandler.removeCallbacks(mLongClickRunnable)
      }
    }
    return true
  }

  fun setPlayIcon(play: Boolean) {
    mPlay.setImageResource(
        if (play) R.drawable.widget_btn_stop_normal else R.drawable.widget_btn_play_normal)
  }

  fun stopAnimation() {
    mText1.stopAnimation()
  }

  @OnClick(R.id.widget_close, R.id.widget_lock, R.id.widget_next, R.id.widget_play, R.id.widget_prev, R.id.widget_lrc_bigger, R.id.widget_lrc_smaller, R.id.widget_setting)
  fun onViewClicked(view: View) {
    when (view.id) {
      //关闭桌面歌词
      R.id.widget_close -> {
        SPUtil.putValue(mService, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_SHOW,
            false)
        sendLocalBroadcast(
            makeCmdIntent(Command.TOGGLE_DESKTOP_LYRIC).putExtra(EXTRA_DESKTOP_LYRIC, false))
      }
      //锁定
      R.id.widget_lock -> {
        saveLock(lock = true, toast = true)
        mUIHandler.postDelayed(mHideRunnable, 0)
        Util.sendCMDLocalBroadcast(Command.LOCK_DESKTOP_LYRIC)
      }
      //歌词字体、大小设置
      R.id.widget_setting -> {
        mLrcSettingContainer.visibility = if (mLrcSettingContainer.isShown) View.GONE else View.VISIBLE
        setUpColor()
        //操作后重置消息的时间
        resetHide()
      }
      R.id.widget_next, R.id.widget_play, R.id.widget_prev -> {
        sendLocalBroadcast(makeCmdIntent(when {
          view.id == R.id.widget_next -> Command.NEXT
          view.id == R.id.widget_prev -> Command.PREV
          else -> Command.TOGGLE
        }))
        mUIHandler.postDelayed({
          mPlay.setImageResource(
              if (mService.isPlaying)
                R.drawable.widget_btn_stop_normal
              else
                R.drawable.widget_btn_play_normal)
        }, 100)
        //操作后重置消息的时间
        resetHide()
      }
      //字体放大、缩小
      R.id.widget_lrc_bigger, R.id.widget_lrc_smaller -> {
        var needRefresh = false
        if (view.id == R.id.widget_lrc_bigger) {
          //当前已经是最大字体
          if (mTextSizeType == HUGE) {
            return
          }
          mTextSizeType++
          needRefresh = true
        }
        if (view.id == R.id.widget_lrc_smaller) {
          //当前已经是最小字体
          if (mTextSizeType == TINY) {
            return
          }
          mTextSizeType--
          needRefresh = true
        }
        if (needRefresh) {
          mText1.textSize = getTextSize(TYPE_TEXT_SIZE_FIRST_LINE)
          mText2.textSize = getTextSize(TYPE_TEXT_SIZE_SECOND_LINE)
          SPUtil.putValue(mService, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_TEXT_SIZE, mTextSizeType)
          //操作后重置消息的时间
          resetHide()
        }
      }
    }
  }

  fun saveLock(lock: Boolean, toast: Boolean) {
    isLocked = lock
    SPUtil.putValue(mService, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_LOCK, isLocked)
    if (toast) {
      ToastUtil.show(mService, if (!isLocked) R.string.desktop_lyric__unlock else R.string.desktop_lyric_lock)
    }
    val params = layoutParams as WindowManager.LayoutParams?
    if (params != null) {
      if (lock) {
        //锁定后点击通知栏解锁
        //        mNotify.notifyToUnlock();
        params.flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
      } else {
        //        mNotify.cancel();
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
      }
      if (VERSION.SDK_INT >= VERSION_CODES.KITKAT && isAttachedToWindow) {
        mWindowManager.updateViewLayout(this, params)
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
  private fun resetHide() {
    mUIHandler.removeCallbacks(mHideRunnable)
    mUIHandler.postDelayed(mHideRunnable, DISMISS_THRESHOLD.toLong())
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    mUIHandler.removeCallbacksAndMessages(null)
    mSeekBarR.setOnSeekBarChangeListener(null)
    mSeekBarG.setOnSeekBarChangeListener(null)
    mSeekBarB.setOnSeekBarChangeListener(null)
    Timber.v("onDetachedFromWindow")
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    Timber.v("onAttachedToWindow")
  }

  @OnHandleMessage
  fun handleMsg(msg: Message) {
    when (msg.what) {
      MESSAGE_SAVE_COLOR -> saveFloatLyricTextColor(msg.arg1)
    }
  }

  companion object {

    //  private UnLockNotify mNotify;

    //当前字体大小
    private const val TINY = 0
    private const val SMALL = 1
    private const val MEDIUM = 2
    private const val BIG = 3
    private const val HUGE = 4

    //第一行歌词字体大小
    private const val FIRST_LINE_HUGE = 20
    private const val FIRST_LINE_BIG = 19
    private const val FIRST_LINE_MEDIUM = 18
    private const val FIRST_LINE_SMALL = 17
    private const val FIRST_LINE_TINY = 16
    //第二行歌词字体大小
    private const val SECOND_LINE_HUGE = 18
    private const val SECOND_LINE_BIG = 17
    private const val SECOND_LINE_MEDIUM = 16
    private const val SECOND_LINE_SMALL = 15
    private const val SECOND_LINE_TINY = 14

    private const val TYPE_TEXT_SIZE_FIRST_LINE = 0
    private const val TYPE_TEXT_SIZE_SECOND_LINE = 1

    private const val DISTANCE_THRESHOLD = 10
    private const val DISMISS_THRESHOLD = 4500
    private const val LONGCLICK_THRESHOLD = 1000


    private const val MESSAGE_SAVE_COLOR = 1
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
