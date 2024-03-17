package remix.myplayer.ui.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import com.bumptech.glide.signature.ObjectKey
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.databinding.BottomActionbarBinding
import remix.myplayer.glide.GlideApp
import remix.myplayer.glide.UriFetcher
import remix.myplayer.helper.MusicServiceRemote.getCurrentSong
import remix.myplayer.helper.MusicServiceRemote.isPlaying
import remix.myplayer.misc.menu.CtrlButtonListener
import remix.myplayer.service.MusicService
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.activity.PlayerActivity
import remix.myplayer.ui.fragment.base.BaseMusicFragment
import remix.myplayer.util.DensityUtil
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Created by Remix on 2015/12/1.
 */
/**
 * 底部控制的Fragment
 */
class BottomActionBarFragment : BaseMusicFragment<BottomActionbarBinding>() {
  override val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> BottomActionbarBinding
    get() = BottomActionbarBinding::inflate

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pageName = BottomActionBarFragment::class.java.simpleName
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    //设置整个背景着色
    Theme.tintDrawable(view, R.drawable.commom_playercontrols_bg, ThemeStore.getBackgroundColorDialog(requireContext()))
    Theme.tintDrawable(binding.playbarNext, R.drawable.bf_btn_next, ThemeStore.bottomBarBtnColor)

    //手势检测
    gestureDetector = GestureDetector(requireContext(), GestureListener(this))
    binding.bottomActionBar.setOnTouchListener { v: View?, event: MotionEvent -> gestureDetector.onTouchEvent(event) }

    //播放按钮
    val listener = CtrlButtonListener(App.context)
    binding.playbarPlay.setOnClickListener(listener)
    binding.playbarNext.setOnClickListener(listener)
  }

  override fun onMetaChanged() {
    super.onMetaChanged()
    updateSong()
  }

  override fun onMediaStoreChanged() {
    super.onMediaStoreChanged()
    onMetaChanged()
  }

  override fun onPlayStateChange() {
    super.onPlayStateChange()
    updatePlayStatus()
  }

  override fun onServiceConnected(service: MusicService) {
    super.onServiceConnected(service)
    onMetaChanged()
    onPlayStateChange()
  }

  private fun updatePlayStatus() {
    Theme.tintDrawable(
        binding.playbarPlay,
        if (isPlaying()) R.drawable.bf_btn_stop else R.drawable.bf_btn_play,
        ThemeStore.bottomBarBtnColor
    )
  }

  //更新界面
  private fun updateSong() {
    val song = getCurrentSong()
    Timber.v("updateSong()")
    //歌曲名 艺术家
    binding.bottomTitle.text = song.title
    binding.bottomArtist.text = song.artist
    //封面
    GlideApp.with(this)
        .load(song)
        .centerCrop()
        .placeholder(Theme.resolveDrawable(requireContext(), R.attr.default_album))
        .error(Theme.resolveDrawable(requireContext(), R.attr.default_album))
        .signature(ObjectKey(UriFetcher.albumVersion))
        .dontAnimate()
        .into(binding.iv)
  }

  fun startPlayerActivity() {
    if (getCurrentSong().id == 0L) {
      return
    }
    val intent = Intent(requireContext(), PlayerActivity::class.java)
    val bundle = Bundle()
    intent.putExtras(bundle)
    val activity: Activity? = activity
    if (activity != null && !activity.isDestroyed) {
      activity.startActivity(intent)
    }
  }

  lateinit var gestureDetector: GestureDetector

  internal class GestureListener(fragment: BottomActionBarFragment) : SimpleOnGestureListener() {
    private val reference: WeakReference<BottomActionBarFragment> = WeakReference(fragment)
    override fun onDoubleTap(e: MotionEvent): Boolean {
      return true
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
      return true
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
      reference.get()?.startPlayerActivity()
      return true
    }

    override fun onContextClick(e: MotionEvent): Boolean {
      return true
    }

    override fun onDown(e: MotionEvent): Boolean {
      return true
    }

    override fun onShowPress(e: MotionEvent) {}
    override fun onSingleTapUp(e: MotionEvent): Boolean {
      return true
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
      return true
    }

    override fun onLongPress(e: MotionEvent) {}
    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
      if (reference.get() != null && velocityY < 0 && e1.y - e2.y > Y_THRESHOLD) {
        reference.get()?.startPlayerActivity()
      }
      return true
    }

    companion object {
      private val Y_THRESHOLD = DensityUtil.dip2px(App.context, 10f)
    }

  }

  companion object {
    private const val TAG = "GestureListener"
  }
}