package remix.myplayer.ui.fragment

import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import kotlinx.android.synthetic.main.fragment_lrc.*
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.helper.MusicServiceRemote
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
class LyricFragment : BaseMusicFragment(), View.OnClickListener {
  private var onFindListener: OnInflateFinishListener? = null
  private var song: Song? = null

  private var disposable: Disposable? = null
  private val msgHandler = MsgHandler(this)

  private val lyricSearcher = LyricSearcher()

  fun setOnInflateFinishListener(l: OnInflateFinishListener) {
    onFindListener = l
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pageName = LyricFragment::class.java.simpleName
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_lrc, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    offsetReduce.setOnClickListener(this)
    offsetAdd.setOnClickListener(this)
    offsetReset.setOnClickListener(this)
    onFindListener?.onViewInflateFinish(lrcView)
    //黑色主题着色按钮
    val themeRes = ThemeStore.themeRes
    if (themeRes == R.style.Theme_APlayer_Black || themeRes == R.style.Theme_APlayer_Dark) {
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
    this.song = song
    getLrc(Uri.EMPTY, clearCache)
  }

  fun updateLrc(uri: Uri) {
    getLrc(uri, true)
  }

  private fun getLrc(uri: Uri, clearCache: Boolean) {
    if (!isVisible)
      return
    if (song == null) {
      lrcView.setText(getStringSafely(R.string.no_lrc))
      return
    }
    if (clearCache) {
      //清除offset
      SPUtil.putValue(App.context, SPUtil.LYRIC_OFFSET_KEY.NAME, song?.id.toString() + "", 0)
      lrcView.setOffset(0)
    }
    val id = song?.id

    disposable?.dispose()
    Timber.v("setSearching")
    lrcView.setText(getStringSafely(R.string.searching))
    disposable = lyricSearcher.setSong(song ?: return)
      .getLyricObservable(uri, clearCache)
      .subscribe(Consumer {
        Timber.v("setLrcRows")
        if (id == song?.id) {
          if (it == null || it.isEmpty()) {
            lrcView.setText(getStringSafely(R.string.no_lrc))
            return@Consumer
          }
          lrcView.setOffset(
            SPUtil.getValue(
              requireContext(),
              SPUtil.LYRIC_OFFSET_KEY.NAME,
              song?.id.toString() + "",
              0
            )
          )
          lrcView.setLrcRows(it)
        }
      }, Consumer {
        Timber.v(it)
        if (id == song?.id) {
          lrcView.setLrcRows(null)
          lrcView.setText(getStringSafely(R.string.no_lrc))
        }
      })
  }

  override fun onClick(view: View) {
    msgHandler.removeMessages(MESSAGE_HIDE)
    msgHandler.sendEmptyMessageDelayed(MESSAGE_HIDE, DELAY_HIDE)

    val originalOffset =
      SPUtil.getValue(requireContext(), SPUtil.LYRIC_OFFSET_KEY.NAME, song?.id.toString() + "", 0)
    var newOffset = originalOffset
    when (view.id) {
      R.id.offsetReset -> {
        newOffset = 0
        ToastUtil.show(requireContext(), R.string.lyric_offset_reset)
      }
      R.id.offsetAdd -> newOffset += 500
      R.id.offsetReduce -> newOffset -= 500
    }
    if (originalOffset != newOffset) {
      SPUtil.putValue(
        requireContext(),
        SPUtil.LYRIC_OFFSET_KEY.NAME,
        song?.id.toString() + "",
        newOffset
      )
      val toastMsg = msgHandler.obtainMessage(MESSAGE_SHOW_TOAST)
      toastMsg.arg1 = newOffset
      msgHandler.removeMessages(MESSAGE_SHOW_TOAST)
      msgHandler.sendMessageDelayed(toastMsg, DELAY_SHOW_TOAST)
      lrcView.setOffset(newOffset)
      MusicServiceRemote.setLyricOffset(newOffset)
    }

  }

  fun showLyricOffsetView() {
    if (lrcView.getLrcRows() == null || lrcView.getLrcRows()?.isEmpty() == true) {
      ToastUtil.show(requireContext(), R.string.no_lrc)
      return
    }
    offsetContainer.visibility = View.VISIBLE
    msgHandler.sendEmptyMessageDelayed(MESSAGE_HIDE, DELAY_HIDE)
  }

  fun setLyricScalingFactor(choose: Int) {
    val factor = when (choose) {
      0 -> {
        1f
      }
      1 -> {
        1.5f
      }
      2 -> {
        2f
      }
      else -> {
        1f
      }
    }
    lrcView.setLrcScalingFactor(factor)
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
          ToastUtil.show(
            requireContext(),
            if (newOffset > 0) R.string.lyric_advance_x_second else R.string.lyric_delay_x_second,
            String.format(Locale.getDefault(), "%.1f", newOffset / 1000f)
          )
        }
      }
    }
  }


  companion object {
    private const val DELAY_HIDE = 5000L
    private const val DELAY_SHOW_TOAST = 100L

    private const val MESSAGE_HIDE = 1
    private const val MESSAGE_SHOW_TOAST = 2
  }

}
