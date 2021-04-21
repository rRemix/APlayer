package remix.myplayer.misc.manager

import android.app.Activity
import java.util.*

/**
 * Created by taeja on 16-3-21.
 */
/**
 * 管理所有Activity 当程序退出时，关闭所有activity
 */
object ActivityManager {
  private val activityList = ArrayList<Activity>()

  fun addActivity(activity: Activity) {
    activityList.add(activity)
  }

  fun removeActivity(activity: Activity?) {
    activityList.remove(activity)
  }

  fun finishAll() {
    for (activity in activityList) {
      if (!activity.isFinishing) {
        activity.finish()
      }
    }
  }
}