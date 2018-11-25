package remix.myplayer.ui.activity.base

import android.annotation.SuppressLint
import android.content.*
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import remix.myplayer.helper.MusicEventCallback
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.service.MusicService
import remix.myplayer.util.LogUtil
import remix.myplayer.util.Util.registerLocalReceiver
import remix.myplayer.util.Util.unregisterLocalReceiver
import java.lang.ref.WeakReference
import java.util.*

@SuppressLint("Registered")
open class BaseMusicActivity : BaseActivity(), MusicEventCallback {
    val TAG = "BaseMusicActivity"
    private var mServiceToken: MusicServiceRemote.ServiceToken? = null
    private val mMusicServiceEventListeners = ArrayList<MusicEventCallback>()
    private var mMusicStateReceiver: MusicStateReceiver? = null
    private var mReceiverRegistered: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mServiceToken = MusicServiceRemote.bindToService(this, object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                val musicService = (service as MusicService.MusicBinder).service
                this@BaseMusicActivity.onServiceConnected(musicService)
            }

            override fun onServiceDisconnected(name: ComponentName) {
                this@BaseMusicActivity.onServiceDisConnected()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        MusicServiceRemote.unbindFromService(mServiceToken)
        mMusicStateHandler?.removeCallbacksAndMessages(null)
        if (mReceiverRegistered) {
            unregisterLocalReceiver(mMusicStateReceiver)
            mReceiverRegistered = true
        }
    }

    fun addMusicServiceEventListener(listener: MusicEventCallback?) {
        if (listener != null) {
            mMusicServiceEventListeners.add(listener)
        }
    }

    fun removeMusicServiceEventListener(listener: MusicEventCallback?) {
        if (listener != null) {
            mMusicServiceEventListeners.remove(listener)
        }
    }

    override fun onMediaStoreChanged() {
        LogUtil.d(TAG, "onMediaStoreChanged:" + this.javaClass.name)
        for (listener in mMusicServiceEventListeners) {
            listener.onMediaStoreChanged()
        }
    }

    override fun onPermissionChanged(has: Boolean) {
        LogUtil.d(TAG, "onPermissionChanged:" + this.javaClass.name)
        mHasPermission = has
        for (listener in mMusicServiceEventListeners) {
            listener.onPermissionChanged(has)
        }
    }

    override fun onPlayListChanged() {
        LogUtil.d(TAG, "onPlayListChanged:" + this.javaClass.name)
        for (listener in mMusicServiceEventListeners) {
            listener.onPlayListChanged()
        }
    }

    override fun onMetaChanged() {
        LogUtil.d(TAG, "onMetaChange:" + this.javaClass.name)
        for (listener in mMusicServiceEventListeners) {
            listener.onMetaChanged()
        }
    }

    override fun onPlayStateChange() {
        LogUtil.d(TAG, "onPlayStateChange:" + this.javaClass.name)
        for (listener in mMusicServiceEventListeners) {
            listener.onPlayStateChange()
        }
    }

    override fun onServiceConnected(service: MusicService) {
        LogUtil.d(TAG, "onServiceConnected:" + this.javaClass.name)
        if (!mReceiverRegistered) {
            mMusicStateReceiver = MusicStateReceiver(this)
            val filter = IntentFilter()
            filter.addAction(MusicService.PLAYLIST_CHANGE)
            filter.addAction(MusicService.PERMISSION_CHANGE)
            filter.addAction(MusicService.MEDIA_STORE_CHANGE)
            filter.addAction(MusicService.META_CHANGE)
            filter.addAction(MusicService.PLAY_STATE_CHANGE)
            registerLocalReceiver(mMusicStateReceiver, filter)
            mReceiverRegistered = true
        }
        for (listener in mMusicServiceEventListeners) {
            listener.onServiceConnected(service)
        }
        mMusicStateHandler = MusicStateHandler(this)
    }

    override fun onServiceDisConnected() {
        if (mReceiverRegistered) {
            unregisterLocalReceiver(mMusicStateReceiver)
            mReceiverRegistered = false
        }
        for (listener in mMusicServiceEventListeners) {
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

    companion object {
        private const val PERMISSION_GRANT = 1
        private const val PERMISSION_NOT_GRANT = 0
    }
}
