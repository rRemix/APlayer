package remix.myplayer.ui.fragment.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import remix.myplayer.bean.mp3.Song
import remix.myplayer.helper.MusicEventCallback

import remix.myplayer.service.MusicService
import remix.myplayer.ui.activity.base.BaseMusicActivity

abstract class BaseMusicFragment<VB: ViewBinding> : BaseFragment(), MusicEventCallback {
  protected abstract val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> VB
  private var _binding: VB? = null
  protected val binding
    get() = _binding!!

  private var musicActivity: BaseMusicActivity? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)
    try {
      musicActivity = context as BaseMusicActivity?
    } catch (e: ClassCastException) {
      throw RuntimeException(context.javaClass.simpleName + " must be an instance of " + BaseMusicActivity::class.java.simpleName)
    }

  }

  override fun onDetach() {
    super.onDetach()
    musicActivity = null
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    _binding = bindingInflater.invoke(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
//        MusicEventHelper.addCallback(this)
    musicActivity?.addMusicServiceEventListener(this)
  }

  override fun onDestroyView() {
    super.onDestroyView()
//        MusicEventHelper.removeCallback(this)
    musicActivity?.removeMusicServiceEventListener(this)
    _binding = null
  }

  override fun onMediaStoreChanged() {

  }

  override fun onPermissionChanged(has: Boolean) {
    hasPermission = has
  }

  override fun onPlayListChanged(name: String) {

  }

  override fun onMetaChanged() {
  }

  override fun onPlayStateChange() {
  }

  override fun onTagChanged(oldSong: Song, newSong: Song) {

  }

  override fun onServiceConnected(service: MusicService) {}

  override fun onServiceDisConnected() {}
}
