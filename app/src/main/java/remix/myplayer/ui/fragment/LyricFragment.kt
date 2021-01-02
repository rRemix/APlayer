package remix.myplayer.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.fragment_lrc.*
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.lyric.LrcView
import remix.myplayer.lyric.LyricSearcher
import remix.myplayer.misc.handler.MsgHandler
import remix.myplayer.misc.handler.OnHandleMessage
import remix.myplayer.misc.interfaces.OnInflateFinishListener
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.fragment.base.BaseMusicFragment
import remix.myplayer.util.SPUtil
import remix.myplayer.util.ToastUtil
import timber.log.Timber
import java.util.*
import kotlin.math.abs

/**
 * Created by Remix on 2015/12/2.
 */

/**
 * 歌词界面Fragment
 */
class LyricFragment : BaseMusicFragment() {
  private var onFindListener: OnInflateFinishListener? = null
  private var info: Song? = null
  @BindView(R.id.lrcView)
  lateinit var lrcView: LrcView
  @BindView(R.id.offsetContainer)
  lateinit var offsetContainer: View

  private var disposable: Disposable? = null
  private val msgHandler = MsgHandler(this)

  private val lyricSearcher = LyricSearcher()

  fun setOnInflateFinishListener(l: OnInflateFinishListener) {
    onFindListener = l
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    mPageName = LyricFragment::class.java.simpleName
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val rootView = inflater.inflate(R.layout.fragment_lrc, container, false)
    mUnBinder = ButterKnife.bind(this, rootView)

    onFindListener?.onViewInflateFinish(lrcView)


    return rootView
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    //黑色主题着色按钮
    val themeRes = ThemeStore.getThemeRes()
    if(themeRes == R.style.Theme_APlayer_Black || themeRes == R.style.Theme_APlayer_Dark){
      iv_offset_reduce_arrow.setColorFilter(Color.WHITE)
      iv_offset_reduce_second.setColorFilter(Color.WHITE)
      offsetReset.setColorFilter(Color.WHITE)
      iv_offset_add_arrow.setColorFilter(Color.WHITE)
      iv_offset_add_second.setColorFilter(Color.WHITE)
    }
  }

  override fun onDestroyView() {
    msgHandler.remove()
    disposable?.dispose()
    onFindListener = null
    super.onDestroyView()
  }

  @JvmOverloads
  fun updateLrc(song: Song, clearCache: Boolean = false) {
    info = song
    getLrc("", clearCache)
  }

  fun updateLrc(lrcPath: String) {
    getLrc(lrcPath, true)
  }

  private fun getLrc(manualPath: String, clearCache: Boolean) {
    if (!isVisible)
      return
    if (info == null) {
      lrcView.setText(getStringSafely(R.string.no_lrc))
      return
    }
    if (clearCache) {
      //清除offset
      SPUtil.putValue(App.getContext(), SPUtil.LYRIC_OFFSET_KEY.NAME, info?.id.toString() + "", 0)
      lrcView.setOffset(0)
    }
    val id = info?.id

    disposable?.dispose()
    disposable = lyricSearcher.setSong(info ?: return)
        .getLyricObservable(manualPath, clearCache)
        .doOnSubscribe { lrcView.setText(getStringSafely(R.string.searching)) }
        .subscribe(Consumer {
          if (id == info?.id) {
            if (it == null || it.isEmpty()) {
              lrcView.setText(getStringSafely(R.string.no_lrc))
              return@Consumer
            }
            lrcView.setOffset(SPUtil.getValue(mContext, SPUtil.LYRIC_OFFSET_KEY.NAME, info?.id.toString() + "", 0))
            lrcView.lrcRows = it
          }
        }, Consumer {
          Timber.v(it)
          if (id == info?.id) {
            lrcView.lrcRows = null
            lrcView.setText(getStringSafely(R.string.no_lrc))
          }
        })

  }

  @OnClick(R.id.offsetReduce, R.id.offsetAdd, R.id.offsetReset)
  internal fun onClick(view: View) {
    msgHandler.removeMessages(MESSAGE_HIDE)
    msgHandler.sendEmptyMessageDelayed(MESSAGE_HIDE, DELAY_HIDE)

    val originalOffset = SPUtil.getValue(mContext, SPUtil.LYRIC_OFFSET_KEY.NAME, info?.id.toString() + "", 0)
    var newOffset = originalOffset
    when (view.id) {
      R.id.offsetReset -> {
        newOffset = 0
        ToastUtil.show(mContext, R.string.lyric_offset_reset)
      }
      R.id.offsetAdd -> newOffset += 500
      R.id.offsetReduce -> newOffset -= 500
    }
    if (originalOffset != newOffset) {
      SPUtil.putValue(mContext, SPUtil.LYRIC_OFFSET_KEY.NAME, info?.id.toString() + "", newOffset)
      val toastMsg = msgHandler.obtainMessage(MESSAGE_SHOW_TOAST)
      toastMsg.arg1 = newOffset
      msgHandler.removeMessages(MESSAGE_SHOW_TOAST)
      msgHandler.sendMessageDelayed(toastMsg, DELAY_SHOW_TOAST)
      lrcView.setOffset(newOffset)
      MusicServiceRemote.setLyricOffset(newOffset)
    }

  }

  fun showLyricOffsetView() {
    if (lrcView.lrcRows == null || lrcView.lrcRows.isEmpty()) {
      ToastUtil.show(mContext, R.string.no_lrc)
      return
    }
    offsetContainer.visibility = View.VISIBLE
    msgHandler.sendEmptyMessageDelayed(MESSAGE_HIDE, DELAY_HIDE)
  }

  @OnHandleMessage
  fun handleInternal(msg: Message) {
    when (msg.what) {
      MESSAGE_HIDE -> {
        offsetContainer.visibility = View.GONE
      }
      MESSAGE_SHOW_TOAST -> {
        val newOffset = msg.arg1
        if (newOffset != 0 && abs(newOffset) <= 60000) {//最大偏移60s
          ToastUtil.show(mContext, if (newOffset > 0) R.string.lyric_advance_x_second else R.string.lyric_delay_x_second,
              String.format(Locale.getDefault(), "%.1f", newOffset / 1000f))
        }
      }
    }
  }


  companion object {
    private const val DELAY_HIDE = 5000L
    private const val DELAY_SHOW_TOAST = 500L

    private const val MESSAGE_HIDE = 1
    private const val MESSAGE_SHOW_TOAST = 2
  }

}
