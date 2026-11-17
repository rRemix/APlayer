package remix.myplayer

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.hjq.permissions.XXPermissions
import dagger.hilt.android.HiltAndroidApp
import remix.myplayer.helper.AppMigration
import remix.myplayer.coil.CoverFetcher
import remix.myplayer.coil.CoverInterceptor
import remix.myplayer.coil.CoverUriResolver
import remix.myplayer.coil.EmbeddedFetcher
import remix.myplayer.helper.LanguageHelper.onConfigurationChanged
import remix.myplayer.helper.LanguageHelper.saveSystemCurrentLanguage
import remix.myplayer.helper.LanguageHelper.setApplicationLanguage
import remix.myplayer.helper.LanguageHelper.setLocal
import remix.myplayer.helper.ThirdPartyInitializer
import remix.myplayer.misc.manager.APlayerActivityManager
import remix.myplayer.ui.appshortcuts.DynamicShortcutManager
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by Remix on 16-3-16.
 */
@HiltAndroidApp
class App : Application(), SingletonImageLoader.Factory {

  @Inject
  lateinit var appMigration: AppMigration

  @Inject
  lateinit var coverUriResolver: dagger.Lazy<CoverUriResolver>

  override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
    .components {
      add(CoverInterceptor(coverUriResolver.get()))
      add(CoverFetcher.Factory())
      add(EmbeddedFetcher.Factory())
    }
    .build()

  override fun attachBaseContext(base: Context) {
    saveSystemCurrentLanguage()
    super.attachBaseContext(setLocal(base))
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
