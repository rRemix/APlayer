package remix.myplayer.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.core.view.isVisible
import remix.myplayer.R
import remix.myplayer.databinding.FragmentLrcBinding
import remix.myplayer.lyrics.LyricsLine
import remix.myplayer.lyrics.LyricsManager
import remix.myplayer.ui.fragment.base.BaseMusicFragment
import remix.myplayer.ui.widget.LyricsView
import timber.log.Timber
import kotlin.math.sign
import kotlin.time.Duration.Companion.milliseconds

class LyricsFragment : BaseMusicFragment<FragmentLrcBinding>(), View.OnClickListener {
  override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentLrcBinding
    get() = FragmentLrcBinding::inflate

  companion object {
    private const val TAG = "LyricsFragment"

    private val HIDE_PANEL_DELAY = 5000.milliseconds
  }

  init {
    pageName = TAG
  }

  var onSeekToListener: LyricsView.OnSeekToListener? = null

  @UiThread
  fun setLyricsSearching() {
    view ?: return
    binding.offsetPanel.visibility = View.GONE
    binding.lyrics.visibility = View.GONE
    binding.lyricsNoLrc.visibility = View.GONE
    binding.lyricsSearching.visibility = View.VISIBLE
  }

  @UiThread
  fun setLyrics(lyrics: List<LyricsLine>) {
    view ?: return
    binding.offsetPanel.visibility = View.GONE
    binding.lyricsSearching.visibility = View.GONE
    if (lyrics.isEmpty()) {
      binding.lyrics.visibility = View.GONE
      binding.lyricsNoLrc.visibility = View.VISIBLE
    } else {
      binding.lyricsNoLrc.visibility = View.GONE
      binding.lyrics.visibility = View.VISIBLE
      binding.lyrics.lyrics = lyrics
    }
  }

  private fun hasLyrics(): Boolean {
    return binding.lyrics.isVisible
  }

  @UiThread
  fun setOffset(offset: Int) {
    view ?: return
    if (hasLyrics()) {
      binding.lyrics.offset = offset
    }
  }

  @UiThread
  fun setProgress(progress: Int, duration: Int) {
    view ?: return
    if (hasLyrics()) {
      binding.lyrics.setProgress(progress, duration)
    }
  }

  private val hideOffsetPanelRunnable = Runnable {
    binding.offsetPanel.visibility = View.GONE
  }
  private var toast: Toast? = null

  override fun onClick(v: View) {
    if (!hasLyrics()) {
      Timber.tag(TAG).w("Trying to set offset when no lyrics")
      return
    }
    when (v.id) {
      R.id.offset_inc -> LyricsManager.offset += 500
      R.id.offset_dec -> LyricsManager.offset -= 500
      R.id.offset_reset -> LyricsManager.offset = 0
      else -> check(false)
    }
    val message = when (LyricsManager.offset.sign) {
      +1 -> resources.getString(R.string.lyric_advance_x_second, LyricsManager.offset / 1000.0)
      -1 -> resources.getString(R.string.lyric_delay_x_second, LyricsManager.offset / 1000.0)
      0 -> resources.getString(R.string.lyric_offset_reset)
      else -> {
        Timber.tag(TAG).wtf("offset sign??")
        return
      }
    }
    // 取消上一次的通知，及时显示最新的
    // TODO: ToastUtil ?
    toast?.cancel()
    toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
    toast!!.show()
    binding.root.handler.removeCallbacks(hideOffsetPanelRunnable)
    binding.root.handler.postDelayed(hideOffsetPanelRunnable, HIDE_PANEL_DELAY.inWholeMilliseconds)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    binding.offsetInc.setOnClickListener(this)
    binding.offsetDec.setOnClickListener(this)
    binding.offsetReset.setOnClickListener(this)
    binding.lyrics.onSeekToListener = LyricsView.OnSeekToListener {
      onSeekToListener?.onSeekTo(it)
    }
    LyricsManager.setLyricsFragment(this)
  }

  @UiThread
  fun showOffsetPanel() {
    if (hasLyrics()) {
      binding.offsetPanel.visibility = View.VISIBLE
      binding.root.handler.run {
        removeCallbacks(hideOffsetPanelRunnable)
        postDelayed(hideOffsetPanelRunnable, HIDE_PANEL_DELAY.inWholeMilliseconds)
      }
    } else {
      // 搜索中 / 没有歌词 不应显示偏移设置
      // TODO: maybe toast?
    }
  }
}
