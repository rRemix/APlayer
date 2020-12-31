package remix.myplayer.ui.dialog.base

import android.content.Context
import android.os.Bundle
import android.view.View
import remix.myplayer.bean.mp3.Song
import remix.myplayer.helper.MusicEventCallback
import remix.myplayer.service.MusicService
import remix.myplayer.ui.activity.base.BaseActivity.EXTERNAL_STORAGE_PERMISSIONS
import remix.myplayer.ui.activity.base.BaseMusicActivity
import remix.myplayer.util.Util

/**
 * Created by remix on 2019/1/31
 */
open class BaseMusicDialog : BaseDialog(), MusicEventCallback {
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

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    hasPermission = Util.hasPermissions(EXTERNAL_STORAGE_PERMISSIONS)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    musicActivity?.addMusicServiceEventListener(this)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    musicActivity?.removeMusicServiceEventListener(this)
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

  override fun onTagChanged(oldSong: Song, newSong: Song) {

  }

  override fun onPlayStateChange() {
  }

  override fun onServiceConnected(service: MusicService) {}

  override fun onServiceDisConnected() {}
}