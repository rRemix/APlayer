package remix.myplayer.service

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import remix.myplayer.data.model.audio.Song
import java.util.WeakHashMap

object MusicServiceRemote {
  val TAG = MusicServiceRemote::class.java.simpleName

  @JvmStatic
  var service: MusicService? = null

  private val connectionMap = WeakHashMap<Context, ServiceBinder>()

  @JvmStatic
  fun bindToService(context: Context, callback: ServiceConnection): ServiceToken? {
//    if (!Util.isAppOnForeground()) {
//      return null
//    }
    val realActivity = (context as Activity).parent ?: context
    val contextWrapper = ContextWrapper(realActivity)
    val intent = Intent(contextWrapper, MusicService::class.java)

    try {
      context.startService(intent)
    } catch (e: Exception) {
      ContextCompat.startForegroundService(context, intent)
    }

    val binder = ServiceBinder(callback)

    if (contextWrapper.bindService(
        Intent().setClass(contextWrapper, MusicService::class.java),
        binder,
        Context.BIND_AUTO_CREATE
      )
    ) {
      connectionMap[contextWrapper] = binder
      return ServiceToken(contextWrapper)
    }

    return null
  }


  @JvmStatic
  fun unbindFromService(token: ServiceToken?) {
    if (token == null) {
      return
    }
    val contextWrapper = token.wrapperContext
    val binder = connectionMap.remove(contextWrapper) ?: return
    contextWrapper.unbindService(binder)
    if (connectionMap.isEmpty()) {
      service = null
    }
  }

  class ServiceBinder(private val callback: ServiceConnection?) : ServiceConnection {

    override fun onServiceConnected(className: ComponentName, service: IBinder) {
      val binder = service as MusicService.MusicBinder
      MusicServiceRemote.service = binder.service
      callback?.onServiceConnected(className, service)
    }

    override fun onServiceDisconnected(className: ComponentName) {
      callback?.onServiceDisconnected(className)
      service = null
    }
  }

  class ServiceToken(var wrapperContext: ContextWrapper)

  fun setPlayQueue(newQueue: List<Song>) {
    service?.setPlayQueue(newQueue)
  }

  fun setPlayQueue(newQueues: List<Song>?, intent: Intent) {
    service?.setPlayQueue(newQueues, intent)
  }

  fun removeFromQueue(ids: List<Long>) {
    service?.removeFromQueue(ids)
  }

  fun insertToQueue(songs: List<Song>) {
    service?.insertToQueue(songs)
  }

  fun getAudioSessionId(): Int? {
    return service?.playback?.audioSessionId
  }
}
