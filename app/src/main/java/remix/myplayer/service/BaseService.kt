package remix.myplayer.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

import remix.myplayer.misc.manager.ServiceManager

/**
 * Created by Remix on 2016/3/26.
 */
abstract class BaseService : Service() {
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        ServiceManager.AddService(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceManager.RemoveService(this)
    }
}
