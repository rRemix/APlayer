package remix.myplayer.helper

import android.app.Activity
import android.content.*
import android.media.MediaPlayer
import android.os.IBinder
import androidx.core.content.ContextCompat
import remix.myplayer.bean.mp3.Song
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.util.Constants
import java.util.*

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

    if (contextWrapper.bindService(Intent().setClass(contextWrapper, MusicService::class.java), binder, Context.BIND_AUTO_CREATE)) {
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

  class ServiceBinder(private val mCallback: ServiceConnection?) : ServiceConnection {

    override fun onServiceConnected(className: ComponentName, service: IBinder) {
      val binder = service as MusicService.MusicBinder
      MusicServiceRemote.service = binder.service
      mCallback?.onServiceConnected(className, service)
    }

    override fun onServiceDisconnected(className: ComponentName) {
      mCallback?.onServiceDisconnected(className)
      MusicServiceRemote.service = null
    }
  }

  class ServiceToken(var wrapperContext: ContextWrapper)

  @JvmStatic
  fun setPlayQueue(newQueue: List<Song>) {
    service?.setPlayQueue(newQueue)
  }

  @JvmStatic
  fun setPlayQueue(newQueues: List<Song>?, intent: Intent) {
    service?.setPlayQueue(newQueues, intent)
  }

//  @JvmStatic
//  fun setAllSong(allSong: List<Int>?) {
//    service?.setAllSong(allSong)
//  }
//
//  @JvmStatic
//  fun getAllSong(): List<Int>? {
//    return service?.allSong
//  }

//  @JvmStatic
//  fun setAllSongAsPlayQueue(intent: Intent) {
//    service?.setPlayQueue(service?.allSong, intent)
//  }

  @JvmStatic
  fun setPlayModel(model: Int) {
    service?.playModel = model
  }

  @JvmStatic
  fun getPlayModel(): Int {
    return service?.playModel ?: Constants.MODE_LOOP
  }

  @JvmStatic
  fun getMediaPlayer(): MediaPlayer? {
    return service?.mediaPlayer
  }

  @JvmStatic
  fun getNextSong(): Song {
    return service?.nextSong ?: Song.EMPTY_SONG
  }

  @JvmStatic
  fun getCurrentSong(): Song {
    return service?.currentSong ?: Song.EMPTY_SONG
  }

  @JvmStatic
  fun getDuration(): Int {
    return service?.duration ?: 0
  }

  @JvmStatic
  fun getProgress(): Int {
    return service?.progress ?: 0
  }

  @JvmStatic
  fun setProgress(progress: Int) {
    service?.setProgress(progress)
  }

  @JvmStatic
  fun isPlaying(): Boolean {
    return service?.isPlaying ?: false
  }

  @JvmStatic
  fun playNext(next: Boolean) {
    service?.playNextOrPrev(next)
  }

  @JvmStatic
  fun deleteFromService(songs: List<Song>) {
    service?.deleteSongFromService(songs)
  }

  @JvmStatic
  fun setOperation(operation: Int) {
    service?.operation = operation
  }

  @JvmStatic
  fun getOperation(): Int {
    return service?.operation ?: Command.NEXT
  }
}
