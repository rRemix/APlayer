package remix.myplayer.ui.activity.base

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.text.TextUtils
import com.facebook.drawee.backends.pipeline.Fresco
import com.soundcloud.android.crop.Crop
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import remix.myplayer.R
import remix.myplayer.bean.misc.CustomCover
import remix.myplayer.helper.MusicEventCallback
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.misc.cache.DiskCache
import remix.myplayer.request.SimpleUriRequest
import remix.myplayer.request.network.RxUtil
import remix.myplayer.service.MusicService
import remix.myplayer.util.Constants
import remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType
import remix.myplayer.util.MediaStoreUtil
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util
import remix.myplayer.util.Util.registerLocalReceiver
import remix.myplayer.util.Util.unregisterLocalReceiver
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference
import java.util.*

@SuppressLint("Registered")
open class BaseMusicActivity : BaseActivity(), MusicEventCallback {
  private var serviceToken: MusicServiceRemote.ServiceToken? = null
  private val serviceEventListeners = ArrayList<MusicEventCallback>()
  private var musicStateReceiver: MusicStateReceiver? = null
  private var receiverRegistered: Boolean = false
  private var pendingBindService = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    bindToService()
  }

  override fun onRestart() {
    super.onRestart()
    if(pendingBindService){
      bindToService()
      pendingBindService = false
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    MusicServiceRemote.unbindFromService(serviceToken)
    mMusicStateHandler?.removeCallbacksAndMessages(null)
    if (receiverRegistered) {
      unregisterLocalReceiver(musicStateReceiver)
      receiverRegistered = true
    }
  }

  private fun bindToService(){
    if(!Util.isAppOnForeground()){
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
    Timber.v("onMediaStoreChanged: ${this.javaClass.simpleName}")
    for (listener in serviceEventListeners) {
      listener.onMediaStoreChanged()
    }
  }

  override fun onPermissionChanged(has: Boolean) {
    Timber.v("onPermissionChanged: ${this.javaClass.simpleName}")
    mHasPermission = has
    for (listener in serviceEventListeners) {
      listener.onPermissionChanged(has)
    }
  }

  override fun onPlayListChanged() {
    Timber.v("onMediaStoreChanged: ${this.javaClass.simpleName}")
    for (listener in serviceEventListeners) {
      listener.onPlayListChanged()
    }
  }

  override fun onMetaChanged() {
    Timber.v("onMetaChange: ${this.javaClass.simpleName}")
    for (listener in serviceEventListeners) {
      listener.onMetaChanged()
    }
  }

  override fun onPlayStateChange() {
    Timber.v("onPlayStateChange: ${this.javaClass.simpleName}")
    for (listener in serviceEventListeners) {
      listener.onPlayStateChange()
    }
  }

  override fun onServiceConnected(service: MusicService) {
    Timber.v("onServiceConnected: ${this.javaClass.simpleName}")
    if (!receiverRegistered) {
      musicStateReceiver = MusicStateReceiver(this)
      val filter = IntentFilter()
      filter.addAction(MusicService.PLAYLIST_CHANGE)
      filter.addAction(MusicService.PERMISSION_CHANGE)
      filter.addAction(MusicService.MEDIA_STORE_CHANGE)
      filter.addAction(MusicService.META_CHANGE)
      filter.addAction(MusicService.PLAY_STATE_CHANGE)
      registerLocalReceiver(musicStateReceiver, filter)
      receiverRegistered = true
    }
    for (listener in serviceEventListeners) {
      listener.onServiceConnected(service)
    }
    mMusicStateHandler = MusicStateHandler(this)
  }

  override fun onServiceDisConnected() {
    if (receiverRegistered) {
      unregisterLocalReceiver(musicStateReceiver)
      receiverRegistered = false
    }
    for (listener in serviceEventListeners) {
      listener.onServiceDisConnected()
    }
    mMusicStateHandler?.removeCallbacksAndMessages(null)
  }

  private var mMusicStateHandler: MusicStateHandler? = null

  private class MusicStateHandler(activity: BaseMusicActivity) : Handler() {
    private val mRef: WeakReference<BaseMusicActivity> = WeakReference(activity)

    override fun handleMessage(msg: Message?) {
      val action = msg?.obj?.toString()
      val activity = mRef.get()
      if (action != null && activity != null) {
        when (action) {
          MusicService.MEDIA_STORE_CHANGE -> {
            activity.onMediaStoreChanged()
          }
          MusicService.PERMISSION_CHANGE -> {
            activity.onPermissionChanged(msg.arg1 == PERMISSION_GRANT)
          }
          MusicService.PLAYLIST_CHANGE -> {
            activity.onPlayListChanged()
          }
          MusicService.META_CHANGE -> {
            activity.onMetaChanged()
          }
          MusicService.PLAY_STATE_CHANGE -> {
            activity.onPlayStateChange()
          }
        }
      }

    }
  }

  private class MusicStateReceiver(activity: BaseMusicActivity) : BroadcastReceiver() {
    private val mRef: WeakReference<BaseMusicActivity> = WeakReference(activity)

    override fun onReceive(context: Context, intent: Intent) {
      mRef.get()?.mMusicStateHandler?.let {
        val action = intent.action
        val msg = it.obtainMessage(action.hashCode())
        msg.obj = action
        msg.arg1 = if (intent.getBooleanExtra("permission", false)) PERMISSION_GRANT else PERMISSION_NOT_GRANT
        it.removeMessages(msg.what)
        it.sendMessageDelayed(msg, 200)
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

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      Crop.REQUEST_CROP, Crop.REQUEST_PICK -> {
        val intent = intent
        val customCover = intent.getParcelableExtra<CustomCover>("thumb") ?: return
        val errorTxt = getString(
            when {
              customCover.type == Constants.ALBUM -> R.string.set_album_cover_error
              customCover.type == Constants.ARTIST -> R.string.set_artist_cover_error
              else -> R.string.set_playlist_cover_error
            })
        val id = customCover.id //专辑、艺术家、播放列表封面

        if (resultCode != Activity.RESULT_OK) {
          ToastUtil.show(this, errorTxt)
          return
        }
        if (requestCode == Crop.REQUEST_PICK) {
          //选择图片
          val cacheDir = DiskCache.getDiskCacheDir(this,
              "thumbnail/" + when {
                customCover.type == Constants.ALBUM -> "album"
                customCover.type == Constants.ARTIST -> "artist"
                else -> "playlist"
              })
          if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            ToastUtil.show(this, errorTxt)
            return
          }
          val destination = Uri.fromFile(File(cacheDir, Util.hashKeyForDisk(id.toString() + "")))
          Crop.of(data?.data, destination).asSquare().start(this)
        } else {
          //图片裁剪
          //裁剪后的图片路径
          if (data == null) {
            return
          }
          if (Crop.getOutput(data) == null) {
            return
          }

          val path = Crop.getOutput(data).encodedPath
          if (TextUtils.isEmpty(path) || id == -1) {
            ToastUtil.show(mContext, errorTxt)
            return
          }
          Observable
              .create(ObservableOnSubscribe<Uri> { emitter ->
                //获取以前的图片
                if (customCover.type == Constants.ALBUM) {
                  object : SimpleUriRequest(getSearchRequestWithAlbumType(
                      MediaStoreUtil.getSongByAlbumId(customCover.id))) {
                    override fun onError(errMsg: String) {
                      emitter.onError(Throwable(errMsg))
                    }

                    override fun onSuccess(result: Uri?) {
                      emitter.onNext(result!!)
                      emitter.onComplete()
                    }
                  }.load()
                } else {
                  emitter.onNext(Uri.parse("file://$path"))
                  emitter.onComplete()
                }
              })
              .doOnSubscribe {
                //如果设置的是专辑封面 修改内嵌封面
                if (customCover.type == Constants.ALBUM) {
                  MediaStoreUtil.saveArtwork(mContext, customCover.id, File(path))
                }
              }
              .compose(RxUtil.applyScheduler())
              .doFinally {
                onMediaStoreChanged()
              }
              .subscribe({ uri ->
                val imagePipeline = Fresco.getImagePipeline()
                imagePipeline.evictFromCache(uri)
                imagePipeline.evictFromDiskCache(uri)
              }, { throwable -> ToastUtil.show(mContext, R.string.save_error, throwable.toString()) })
        }
      }
    }
  }

  companion object {
    private const val PERMISSION_GRANT = 1
    private const val PERMISSION_NOT_GRANT = 0

    //更新适配器
    const val UPDATE_ADAPTER = 100
    //多选更新
    const val CLEAR_MULTI = 101
    //重建activity
    const val RECREATE_ACTIVITY = 102
  }
}
