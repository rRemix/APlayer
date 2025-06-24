package remix.myplayer.compose.activity.base

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import kotlinx.coroutines.cancel
import remix.myplayer.bean.mp3.Song
import remix.myplayer.helper.MusicEventCallback
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.service.MusicService
import remix.myplayer.util.Util.isAppOnForeground
import remix.myplayer.util.Util.registerLocalReceiver
import remix.myplayer.util.Util.unregisterLocalReceiver
import timber.log.Timber
import java.lang.ref.WeakReference

open class BaseMusicActivity : BaseActivity(), MusicEventCallback {
  private var serviceToken: MusicServiceRemote.ServiceToken? = null
  private val serviceEventListeners = ArrayList<MusicEventCallback>()
  private var musicStateReceiver: MusicStateReceiver? = null
  private var receiverRegistered: Boolean = false
  private var pendingBindService = false
  protected var hasNewIntent: Boolean = false

  private val TAG = this.javaClass.simpleName

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Timber.tag(TAG).v("onCreate")
    bindToService()
  }

  override fun onStart() {
    super.onStart()
    Timber.tag(TAG).v("onStart(), $pendingBindService")
//    if (pendingBindService) {
//      bindToService()
//    }
  }

  override fun onRestart() {
    super.onRestart()
    Timber.tag(TAG).v("onRestart")
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    Timber.tag(TAG).v("onNewIntent")
    setIntent(intent)
    hasNewIntent = true
  }

  override fun onResume() {
    super.onResume()
    Timber.tag(TAG).v("onResume")
    if (pendingBindService) {
      bindToService()
    }
  }

  override fun onPause() {
    super.onPause()
    Timber.tag(TAG).v("onPause")
  }

  override fun onDestroy() {
    super.onDestroy()
    Timber.tag(TAG).v("onDestroy")
    cancel()
    MusicServiceRemote.unbindFromService(serviceToken)
    musicStateHandler?.removeCallbacksAndMessages(null)
    if (receiverRegistered) {
      unregisterLocalReceiver(musicStateReceiver)
      receiverRegistered = true
    }
  }

  private fun bindToService() {
    if (!isAppOnForeground) {
      Timber.tag(TAG).v("bindToService(),app isn't on foreground")
      pendingBindService = true
      return
    }
    serviceToken = MusicServiceRemote.bindToService(this, object : ServiceConnection {
      override fun onServiceConnected(name: ComponentName, service: IBinder) {
        val musicService = (service as MusicService.MusicBinder).service
        this@BaseMusicActivity.onServiceConnected(musicService)
      }

      override fun onServiceDisconnected(name: ComponentName) {
        this@BaseMusicActivity.onServiceDisConnected()
      }
    })
    pendingBindService = false
  }

  fun addMusicServiceEventListener(listener: MusicEventCallback?) {
    if (listener != null) {
      serviceEventListeners.add(listener)
    }
  }

  fun removeMusicServiceEventListener(listener: MusicEventCallback?) {
    if (listener != null) {
      serviceEventListeners.remove(listener)
    }
  }

  override fun onMediaStoreChanged() {
    Timber.tag(TAG).v("onMediaStoreChanged")
    for (listener in serviceEventListeners) {
      listener.onMediaStoreChanged()
    }
  }

  override fun onPermissionChanged(has: Boolean) {
    Timber.tag(TAG).v("onPermissionChanged(), $has")
    hasPermission = has
    for (listener in serviceEventListeners) {
      listener.onPermissionChanged(has)
    }
  }

  override fun onPlayListChanged(name: String) {
    Timber.tag(TAG).v("onMediaStoreChanged(), $name")
    for (listener in serviceEventListeners) {
      listener.onPlayListChanged(name)
    }
  }

  override fun onMetaChanged() {
    Timber.tag(TAG).v("onMetaChange")
    for (listener in serviceEventListeners) {
      listener.onMetaChanged()
    }
  }

  override fun onPlayStateChange() {
    Timber.tag(TAG).v("onPlayStateChange")
    for (listener in serviceEventListeners) {
      listener.onPlayStateChange()
    }
  }

  override fun onTagChanged(oldSong: Song, newSong: Song) {
    Timber.tag(TAG).v("onTagChanged")
    for (listener in serviceEventListeners) {
      listener.onTagChanged(oldSong, newSong)
    }
  }

  override fun onServiceConnected(service: MusicService) {
    Timber.tag(TAG).v("onServiceConnected(), $service")
    if (!receiverRegistered) {
      musicStateReceiver = MusicStateReceiver(this)
      val filter = IntentFilter()
      filter.addAction(MusicService.PLAYLIST_CHANGE)
      filter.addAction(MusicService.PERMISSION_CHANGE)
      filter.addAction(MusicService.MEDIA_STORE_CHANGE)
      filter.addAction(MusicService.META_CHANGE)
      filter.addAction(MusicService.PLAY_STATE_CHANGE)
      filter.addAction(MusicService.TAG_CHANGE)
      registerLocalReceiver(musicStateReceiver, filter)
      receiverRegistered = true
    }
    for (listener in serviceEventListeners) {
      listener.onServiceConnected(service)
    }
    musicStateHandler = MusicStateHandler(this)
  }

  override fun onServiceDisConnected() {
    if (receiverRegistered) {
      unregisterLocalReceiver(musicStateReceiver)
      receiverRegistered = false
    }
    for (listener in serviceEventListeners) {
      listener.onServiceDisConnected()
    }
    musicStateHandler?.removeCallbacksAndMessages(null)
  }

  private var musicStateHandler: MusicStateHandler? = null

  private class MusicStateHandler(activity: BaseMusicActivity) : Handler() {
    private val ref: WeakReference<BaseMusicActivity> = WeakReference(activity)

    override fun handleMessage(msg: Message) {
      val action = msg.obj?.toString()
      val activity = ref.get()
      if (action != null && activity != null) {
        when (action) {
          MusicService.MEDIA_STORE_CHANGE -> {
            activity.onMediaStoreChanged()
          }
          MusicService.PERMISSION_CHANGE -> {
            activity.onPermissionChanged(msg.data.getBoolean(EXTRA_PERMISSION))
          }
          MusicService.PLAYLIST_CHANGE -> {
            activity.onPlayListChanged(msg.data.getString(EXTRA_PLAYLIST) ?: "")
          }
          MusicService.META_CHANGE -> {
            activity.onMetaChanged()
          }
          MusicService.PLAY_STATE_CHANGE -> {
            activity.onPlayStateChange()
          }
          MusicService.TAG_CHANGE -> {
            val newSong = msg.data.getSerializable(EXTRA_NEW_SONG) as Song?
            val oldSong = msg.data.getSerializable(EXTRA_OLD_SONG) as Song?

            if (newSong != null && oldSong != null) {
              activity.onTagChanged(oldSong, newSong)
            }
          }
        }
      }

    }
  }

  private class MusicStateReceiver(activity: BaseMusicActivity) : BroadcastReceiver() {
    private val ref: WeakReference<BaseMusicActivity> = WeakReference(activity)

    override fun onReceive(context: Context, intent: Intent) {
      ref.get()?.musicStateHandler?.let {
        val action = intent.action
        val msg = it.obtainMessage(action.hashCode())
        msg.obj = action
        msg.data = intent.extras
        it.removeMessages(msg.what)
        it.sendMessageDelayed(msg, 50)
      }
//            if (activity != null && action != null) {
//                when (action) {
//                    MusicService.MEDIA_STORE_CHANGE -> {
//                        activity.onMediaStoreChanged()
//                    }
//                    MusicService.PERMISSION_CHANGE -> {
//                        activity.onPermissionChanged(intent.getBooleanExtra("permission", false))
//                    }
//                    MusicService.PLAYLIST_CHANGE -> {
//                        activity.onPlayListChanged()
//                    }
//                    MusicService.META_CHANGE ->{
//                        activity.onMetaChanged()
//                    }
//                    MusicService.PLAY_STATE_CHANGE ->{
//                        activity.onPlayStateChange()
//                    }
//                }
//            }
    }
  }


  companion object {
    const val EXTRA_PLAYLIST = "extra_playlist"
    const val EXTRA_PERMISSION = "extra_permission"
    const val EXTRA_NEW_SONG = "extra_new_song"
    const val EXTRA_OLD_SONG = "extra_old_song"
  }
}
