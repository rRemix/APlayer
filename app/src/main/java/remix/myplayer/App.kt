package remix.myplayer

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.hjq.permissions.XXPermissions
import dagger.hilt.android.HiltAndroidApp
import remix.myplayer.helper.AppMigration
import remix.myplayer.helper.LanguageHelper.onConfigurationChanged
import remix.myplayer.helper.LanguageHelper.saveSystemCurrentLanguage
import remix.myplayer.helper.LanguageHelper.setApplicationLanguage
import remix.myplayer.helper.LanguageHelper.setLocal
import remix.myplayer.helper.ThirdPartyInitializer
import remix.myplayer.misc.manager.APlayerActivityManager
import remix.myplayer.ui.appshortcuts.DynamicShortcutManager
import remix.myplayer.ui.screen.home.hackTabMinWidth
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by Remix on 16-3-16.
 */
@HiltAndroidApp
class App : MultiDexApplication() {

  @Inject
  lateinit var appMigration: AppMigration

  override fun attachBaseContext(base: Context) {
    saveSystemCurrentLanguage()
    super.attachBaseContext(setLocal(base))
    MultiDex.install(this)
  }

  override fun onCreate() {
    super.onCreate()
    context = this

    appMigration.check()
    setUp()

    // AppShortcut
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
      DynamicShortcutManager(this).setUpShortcut()
    }

    // 加载第三方库
    ThirdPartyInitializer.init(this@App)

    registerActivityLifecycleCallbacks(APlayerActivityManager())

    hackTabMinWidth()
  }

  private fun setUp() {
    XXPermissions.setCheckMode(false)
    setApplicationLanguage(this)
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    onConfigurationChanged(applicationContext)
  }

  override fun onLowMemory() {
    super.onLowMemory()
    Timber.v("onLowMemory")
  }

  override fun onTrimMemory(level: Int) {
    super.onTrimMemory(level)
    Timber.v("onTrimMemory, %s", level)
  }

  companion object {

    @JvmStatic
    lateinit var context: App
      private set
  }
}