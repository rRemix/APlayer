package remix.myplayer.misc.manager

import android.app.Service
import java.util.*

/**
 * Created by taeja on 16-3-24.
 */
/**
 * 管理所有Service 当程序退出时，停止所有Service
 */
object ServiceManager {
  private val serviceList = ArrayList<Service>()
  fun addService(service: Service) {
    serviceList.add(service)
  }

  fun removeService(service: Service) {
    serviceList.remove(service)
  }

  fun stopAll() {
    for (service in serviceList) {
      service.stopSelf()
    }
  }
}