package remix.myplayer.ui.fragment.base

import android.content.Context
import android.os.Bundle
import android.view.View
import remix.myplayer.bean.mp3.Song
import remix.myplayer.helper.MusicEventCallback

import remix.myplayer.service.MusicService
import remix.myplayer.ui.activity.base.BaseMusicActivity

open class BaseMusicFragment : BaseFragment(), MusicEventCallback {
  private var mMusicActivity: BaseMusicActivity? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)
    try {
      mMusicActivity = context as BaseMusicActivity?
    } catch (e: ClassCastException) {
      throw RuntimeException(context.javaClass.simpleName + " must be an instance of " + BaseMusicActivity::class.java.simpleName)
    }

  }

  override fun onDetach() {
    super.onDetach()
    mMusicActivity = null
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
//        MusicEventHelper.addCallback(this)
    mMusicActivity?.addMusicServiceEventListener(this)
  }

  override fun onDestroyView() {
    super.onDestroyView()
//        MusicEventHelper.removeCallback(this)
    mMusicActivity?.removeMusicServiceEventListener(this)
  }

  override fun onMediaStoreChanged() {

  }

  override fun onPermissionChanged(has: Boolean) {
    mHasPermission = has
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
