package remix.myplayer.ui.widget.desktop

import android.app.Service
import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.hardware.input.InputManager
import android.os.Build
import android.os.Message
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.internal.MDTintHelper
import remix.myplayer.R
import remix.myplayer.databinding.LayoutDesktopLyricBinding
import remix.myplayer.lyric.bean.LrcRow
import remix.myplayer.misc.handler.MsgHandler
import remix.myplayer.misc.handler.OnHandleMessage
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicService.Companion.EXTRA_DESKTOP_LYRIC
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.adapter.DesktopLyricColorAdapter
import remix.myplayer.ui.adapter.DesktopLyricColorAdapter.Companion.COLORS
import remix.myplayer.util.*
import remix.myplayer.util.MusicUtil.makeCmdIntent
import remix.myplayer.util.SPUtil.SETTING_KEY
import remix.myplayer.util.Util.sendLocalBroadcast
import timber.log.Timber
import kotlin.math.abs


/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/3/22 15:47
 */

class DesktopLyricView(private val service: MusicService) : RelativeLayout(service), View.OnClickListener {
  private val binding: LayoutDesktopLyricBinding =
    LayoutDesktopLyricBinding.inflate(LayoutInflater.from(service), this, true)

  private val windowManager: WindowManager by lazy {
    service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  }
  private val lastPoint = PointF()
  var isLocked = false
    private set
  private val handler = MsgHandler(this)
  private val colorAdapter: DesktopLyricColorAdapter by lazy {
    DesktopLyricColorAdapter(
        service,
        R.layout.item_float_lrc_color,
        binding.widgetColorRecyclerview.measuredWidth
    )
  }

  private var textSizeType = MEDIUM
  private val hideRunnable = Runnable {
    binding.widgetPanel.visibility = View.GONE
    binding.widgetLrcContainer.visibility = View.GONE
  }

  private val onSeekBarChangeListener = object : OnSeekBarChangeListener {
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
      val temp = Color.rgb(
          binding.widgetSeekbarR.progress,
          binding.widgetSeekbarG.progress,
          binding.widgetSeekbarB.progress
      )
      val color = if (ColorUtil.isColorCloseToWhite(temp)) Color.parseColor("#F9F9F9") else temp
      binding.widgetLine1.setTextColor(color)
      MDTintHelper.setTint(binding.widgetSeekbarR, color)
      MDTintHelper.setTint(binding.widgetSeekbarG, color)
      MDTintHelper.setTint(binding.widgetSeekbarB, color)
      binding.widgetTextR.setTextColor(color)
      binding.widgetTextG.setTextColor(color)
      binding.widgetTextB.setTextColor(color)
      resetHide()

      handler.removeMessages(MESSAGE_SAVE_COLOR)
      handler.sendMessageDelayed(Message.obtain(handler, MESSAGE_SAVE_COLOR, color, 0), 100)
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {

    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {

    }
  }

  /**
   * 当前是否正在拖动
   */
  private var isDragging = false

  init {
    setUpView()
  }

  private fun setUpColor() {
    binding.widgetColorRecyclerview.viewTreeObserver
        .addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
          override fun onPreDraw(): Boolean {
            binding.widgetColorRecyclerview.viewTreeObserver.removeOnPreDrawListener(this)
            colorAdapter.onItemClickListener = object : OnItemClickListener {
              override fun onItemClick(view: View, position: Int) {
                val color = ColorUtil.getColor(COLORS[position])
                binding.widgetLine1.setTextColor(color)
                colorAdapter.setCurrentColor(color)
                colorAdapter.notifyDataSetChanged()
                resetHide()
              }

              override fun onItemLongClick(view: View, position: Int) {

              }
            }
            binding.widgetColorRecyclerview.layoutManager = LinearLayoutManager(
                service,
                LinearLayoutManager.HORIZONTAL,
                false
            )
            binding.widgetColorRecyclerview.overScrollMode = View.OVER_SCROLL_NEVER
            binding.widgetColorRecyclerview.adapter = colorAdapter
            return true
          }
        })
  }

  private fun setUpView() {
    val temp = ThemeStore.floatLyricTextColor
    val color = if (ColorUtil.isColorCloseToWhite(temp)) Color.parseColor("#F9F9F9") else temp
    val red = color and 0xff0000 shr 16
    val green = color and 0x00ff00 shr 8
    val blue = color and 0x0000ff
    binding.widgetSeekbarR.max = 255
    binding.widgetSeekbarR.progress = red
    binding.widgetSeekbarG.max = 255
    binding.widgetSeekbarG.progress = green
    binding.widgetSeekbarB.max = 255
    binding.widgetSeekbarB.progress = blue
    binding.widgetTextR.setTextColor(color)
    binding.widgetTextG.setTextColor(color)
    binding.widgetTextB.setTextColor(color)
    binding.widgetSeekbarR.setOnSeekBarChangeListener(onSeekBarChangeListener)
    binding.widgetSeekbarG.setOnSeekBarChangeListener(onSeekBarChangeListener)
    binding.widgetSeekbarB.setOnSeekBarChangeListener(onSeekBarChangeListener)
    MDTintHelper.setTint(binding.widgetSeekbarR, color)
    MDTintHelper.setTint(binding.widgetSeekbarG, color)
    MDTintHelper.setTint(binding.widgetSeekbarB, color)

    textSizeType = SPUtil.getValue(service, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_TEXT_SIZE, MEDIUM)
    binding.widgetLine1.setTextColor(color)
    binding.widgetLine1.textSize = getTextSize(TYPE_TEXT_SIZE_FIRST_LINE)
    binding.widgetLine2.textSize = getTextSize(TYPE_TEXT_SIZE_SECOND_LINE)
    isLocked = SPUtil.getValue(service, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_LOCK, false)

    setPlayIcon(service.isPlaying)

    viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
      override fun onPreDraw(): Boolean {
        viewTreeObserver.removeOnPreDrawListener(this)
        saveLock(isLocked, false)
        return true
      }
    })

    arrayOf(
        binding.widgetClose,
        binding.widgetLock,
        binding.widgetNext,
        binding.widgetPlay,
        binding.widgetPrev,
        binding.widgetLrcBigger,
        binding.widgetLrcSmaller,
        binding.widgetSetting
    ).forEach {
      it.setOnClickListener(this)
    }
  }

  /**
   * @param type 0: 第一行 1:第二行
   */
  private fun getTextSize(type: Int): Float {
    return when (type) {
      TYPE_TEXT_SIZE_FIRST_LINE -> {
        (when (textSizeType) {
          TINY -> FIRST_LINE_TINY
          SMALL -> FIRST_LINE_SMALL
          MEDIUM -> FIRST_LINE_MEDIUM
          BIG -> FIRST_LINE_BIG
          else -> FIRST_LINE_HUGE
        }).toFloat()
      }
      TYPE_TEXT_SIZE_SECOND_LINE -> {
        (when (textSizeType) {
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
      binding.widgetLine1.setLrcRow(lrc1)
    }
    if (lrc2 != null) {
      if (TextUtils.isEmpty(lrc2.content)) {
        lrc2.content = "....."
      }
      binding.widgetLine2.text = lrc2.content
    }
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_DOWN -> if (!isLocked) {
        isDragging = false
        lastPoint.set(event.rawX, event.rawY)
        handler.removeCallbacks(hideRunnable)
      } else {
//        mUIHandler.postDelayed(mLongClickRunnable, LONGCLICK_THRESHOLD);
      }
      MotionEvent.ACTION_MOVE -> if (!isLocked) {
        val params = layoutParams as WindowManager.LayoutParams

        if (abs(event.rawY - lastPoint.y) > DISTANCE_THRESHOLD) {
          params.y += (event.rawY - lastPoint.y).toInt()
          isDragging = true
          if (isAttachedToWindow) {
            windowManager.updateViewLayout(this, params)
          }
        }
        lastPoint.set(event.rawX, event.rawY)
      }
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (!isLocked) {
        if (!isDragging) {
          //点击后隐藏或者显示操作栏
          if (binding.widgetPanel.isShown) {
            binding.widgetPanel.visibility = View.INVISIBLE
          } else {
            binding.widgetPanel.visibility = View.VISIBLE
            handler.postDelayed(hideRunnable, DISMISS_THRESHOLD.toLong())
          }
        } else {
          //滑动
          if (binding.widgetPanel.isShown) {
            handler.postDelayed(hideRunnable, DISMISS_THRESHOLD.toLong())
          }
          isDragging = false
        }
        //保存y坐标
        val params = layoutParams as WindowManager.LayoutParams
        SPUtil.putValue(service, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_Y, params.y)
      } else {
//        mUIHandler.removeCallbacks(mLongClickRunnable)
      }
    }
    return true
  }

  fun setPlayIcon(play: Boolean) {
    binding.widgetPlay.setImageResource(
        if (play) R.drawable.widget_btn_stop_normal
        else R.drawable.widget_btn_play_normal
    )
  }


  override fun onClick(view: View) {
    when (view.id) {
      //关闭桌面歌词
      R.id.widget_close -> {
        SPUtil.putValue(service, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_SHOW,
            false)
        sendLocalBroadcast(
            makeCmdIntent(Command.TOGGLE_DESKTOP_LYRIC).putExtra(EXTRA_DESKTOP_LYRIC, false))
      }
      //锁定
      R.id.widget_lock -> {
        saveLock(lock = true, toast = true)
        handler.postDelayed(hideRunnable, 0)
        Util.sendCMDLocalBroadcast(Command.LOCK_DESKTOP_LYRIC)
      }
      //歌词字体、大小设置
      R.id.widget_setting -> {
        binding.widgetLrcContainer.visibility = if (binding.widgetLrcContainer.isShown) View.GONE else View.VISIBLE
        setUpColor()
        //操作后重置消息的时间
        resetHide()
      }
      R.id.widget_next, R.id.widget_play, R.id.widget_prev -> {
        sendLocalBroadcast(makeCmdIntent(
            when (view.id) {
              R.id.widget_next -> Command.NEXT
              R.id.widget_prev -> Command.PREV
              else -> Command.TOGGLE
            }
        ))
        handler.postDelayed({
          binding.widgetPlay.setImageResource(
              if (service.isPlaying)
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
          if (textSizeType == HUGE) {
            return
          }
          textSizeType++
          needRefresh = true
        }
        if (view.id == R.id.widget_lrc_smaller) {
          //当前已经是最小字体
          if (textSizeType == TINY) {
            return
          }
          textSizeType--
          needRefresh = true
        }
        if (needRefresh) {
          binding.widgetLine1.textSize = getTextSize(TYPE_TEXT_SIZE_FIRST_LINE)
          binding.widgetLine2.textSize = getTextSize(TYPE_TEXT_SIZE_SECOND_LINE)
          SPUtil.putValue(service, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_TEXT_SIZE, textSizeType)
          //操作后重置消息的时间
          resetHide()
        }
      }
    }
  }

  fun saveLock(lock: Boolean, toast: Boolean) {
    isLocked = lock
    SPUtil.putValue(service, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_LOCK, isLocked)
    if (toast) {
      ToastUtil.show(service, if (!isLocked) R.string.desktop_lyric__unlock else R.string.desktop_lyric_lock)
    }
    val params = layoutParams as WindowManager.LayoutParams?
    if (params != null) {
      if (lock) {
        //锁定后点击通知栏解锁
        //        mNotify.notifyToUnlock();
        params.flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          val inputManager = context.getSystemService(Service.INPUT_SERVICE) as InputManager
          params.alpha = inputManager.maximumObscuringOpacityForTouch
        }
      } else {
        //        mNotify.cancel();
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
      }
      if (isAttachedToWindow) {
        windowManager.updateViewLayout(this, params)
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
    handler.removeCallbacks(hideRunnable)
    handler.postDelayed(hideRunnable, DISMISS_THRESHOLD.toLong())
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    handler.removeCallbacksAndMessages(null)
    binding.widgetSeekbarR.setOnSeekBarChangeListener(null)
    binding.widgetSeekbarG.setOnSeekBarChangeListener(null)
    binding.widgetSeekbarB.setOnSeekBarChangeListener(null)
    Timber.v("onDetachedFromWindow")
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    Timber.v("onAttachedToWindow")
  }

  @OnHandleMessage
  fun handleMsg(msg: Message) {
    when (msg.what) {
      MESSAGE_SAVE_COLOR -> ThemeStore.floatLyricTextColor = msg.arg1
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
    private const val FIRST_LINE_HUGE = 23
    private const val FIRST_LINE_BIG = 20
    private const val FIRST_LINE_MEDIUM = 18
    private const val FIRST_LINE_SMALL = 17
    private const val FIRST_LINE_TINY = 16

    //第二行歌词字体大小
    private const val SECOND_LINE_HUGE = 20
    private const val SECOND_LINE_BIG = 18
    private const val SECOND_LINE_MEDIUM = 16
    private const val SECOND_LINE_SMALL = 15
    private const val SECOND_LINE_TINY = 14

    private const val TYPE_TEXT_SIZE_FIRST_LINE = 0
    private const val TYPE_TEXT_SIZE_SECOND_LINE = 1

    private const val DISTANCE_THRESHOLD = 10
    private const val DISMISS_THRESHOLD = 4500
    private const val LONG_CLICK_THRESHOLD = 1000


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
