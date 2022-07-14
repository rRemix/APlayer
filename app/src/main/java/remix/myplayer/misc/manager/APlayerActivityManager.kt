package remix.myplayer.misc.manager

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference
import java.util.*

/**
 * <pre>
 * Used to Manage activities
 * <pre/>
 * created by:   yaochunfeng
 * on:           2022/7/13 13:36
 * Email:        yaocf@189.cn
 */
open class APlayerActivityManager :Application.ActivityLifecycleCallbacks{

    companion object {
        private lateinit var currentAc : WeakReference<Activity?>
        //不需要使用atomReference，这边的所有方法
        private var activitys : LinkedList<Activity> = LinkedList()
        fun finishAll(){
            val toFinish = LinkedList(activitys)
            for (ac : Activity in toFinish){
                if (!ac.isFinishing) {
                    ac.finish()
                }
            }
        }

        private var foregroundActivityCount = 0
        val isAppForeground: Boolean
            get() = foregroundActivityCount > 0
    }

    override fun onActivityCreated(p0: Activity, p1: Bundle?) {
        activitys.add(p0)
    }

    override fun onActivityStarted(p0: Activity) {
        foregroundActivityCount++
    }

    override fun onActivityResumed(p0: Activity) {
        currentAc = WeakReference(p0)
    }

    override fun onActivityPaused(p0: Activity) {
    }

    override fun onActivityStopped(p0: Activity) {
        foregroundActivityCount--
    }

    override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
    }

    override fun onActivityDestroyed(p0: Activity) {
        activitys.remove(p0)
    }

}