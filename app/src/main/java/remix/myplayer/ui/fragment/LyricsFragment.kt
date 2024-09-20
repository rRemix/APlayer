package remix.myplayer.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.UiThread
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.databinding.FragmentLrcBinding
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.ui.fragment.base.BaseMusicFragment
import remix.myplayer.ui.widget.LyricsView
import remix.myplayer.util.ToastUtil
import kotlin.math.sign
import kotlin.time.Duration.Companion.milliseconds

class LyricsFragment : BaseMusicFragment<FragmentLrcBinding>(), View.OnClickListener {
  override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> FragmentLrcBinding
    get() = FragmentLrcBinding::inflate

  companion object {
    private val TAG = LyricsFragment::class.java.simpleName

    private val UPDATE_PROGRESS_INTERVAL = 50.milliseconds
    private val HIDE_PANEL_DELAY = 5000.milliseconds
  }

  init {
    pageName = TAG
  }

  var onSeekToListener: LyricsView.OnSeekToListener? = null

  private var loadLyricsJob: Job? = null
  private var updateProgressJob: Job? = null

  @UiThread
  fun updateLyrics() {
    view ?: return // TODO: Better check?

    loadLyricsJob?.cancel()
    updateProgressJob?.cancel()

    binding.lyrics.visibility = View.GONE
    binding.lyricsNoLrc.visibility = View.GONE
    binding.lyricsSearching.visibility = View.VISIBLE

    MusicServiceRemote.service?.let { service ->
      loadLyricsJob = lifecycleScope.launch {
        val lyrics = service.lyrics.await()
        binding.lyricsSearching.visibility = View.GONE
        if (lyrics.isEmpty()) {
          binding.lyricsNoLrc.visibility = View.VISIBLE
        } else {
          binding.lyrics.lyrics = lyrics
          binding.lyrics.offset = service.lyricsOffset
          binding.lyrics.visibility = View.VISIBLE
          updateProgressJob = lifecycleScope.launch {
            while (true) {
              updateProgress()
              delay(UPDATE_PROGRESS_INTERVAL)
            }
          }
        }
      }
    }
  }

  @UiThread
  fun updateProgress() {
    MusicServiceRemote.service?.run {
      binding.lyrics.updateProgress(progress, duration)
    }
  }

  private val hideOffsetPanelRunnable = Runnable {
    binding.offsetPanel.visibility = View.GONE
  }

  override fun onClick(v: View) {
    MusicServiceRemote.service?.run {
      val oldOffset = lyricsOffset
      when (v.id) {
        R.id.offset_inc -> lyricsOffset += 500
        R.id.offset_dec -> lyricsOffset -= 500
        R.id.offset_reset -> lyricsOffset = 0
        else -> return@onClick
      }
      if (lyricsOffset != oldOffset) {
        val seconds = lyricsOffset / 1000f
        when (seconds.sign) {
          +1f -> ToastUtil.show(context, R.string.lyric_advance_x_second, seconds)
          -1f -> ToastUtil.show(context, R.string.lyric_delay_x_second, -seconds)
          0f -> ToastUtil.show(context, R.string.lyric_offset_reset)
        }
        binding.lyrics.offset = lyricsOffset
      }
    }
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
  }

  override fun onMediaStoreChanged() {
    super.onMediaStoreChanged()
    updateLyrics()
    TODO("when is it called?")
  }

  override fun onMetaChanged() {
    super.onMetaChanged()
    updateLyrics()
    TODO("when is it called?")
  }

  @UiThread
  fun showOffsetPanel() {
    binding.offsetPanel.visibility = View.VISIBLE
    binding.root.handler.run {
      removeCallbacks(hideOffsetPanelRunnable)
      postDelayed(hideOffsetPanelRunnable, HIDE_PANEL_DELAY.inWholeMilliseconds)
    }
  }
}
