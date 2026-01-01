package remix.myplayer.baselineprofile

import android.Manifest
import android.os.Build
import android.os.SystemClock
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * This test class generates a basic startup baseline profile for the target package.
 *
 * We recommend you start with this but add important user flows to the profile to improve their performance.
 * Refer to the [baseline profile documentation](https://d.android.com/topic/performance/baselineprofiles)
 * for more information.
 *
 * You can run the generator with the "Generate Baseline Profile" run configuration in Android Studio or
 * the equivalent `generateBaselineProfile` gradle task:
 * ```
 * ./gradlew :app:generateReleaseBaselineProfile
 * ```
 * The run configuration runs the Gradle task and applies filtering to run only the generators.
 *
 * Check [documentation](https://d.android.com/topic/performance/benchmarking/macrobenchmark-instrumentation-args)
 * for more information about available instrumentation arguments.
 *
 * After you run the generator, you can verify the improvements running the [StartupBenchmarks] benchmark.
 *
 * When using this class to generate a baseline profile, only API 33+ or rooted API 28+ are supported.
 *
 * The minimum required version of androidx.benchmark to generate a baseline profile is 1.2.0.
 **/
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

  @get:Rule
  val rule = BaselineProfileRule()

  @Test
  fun generate() {
    // The application id for the running build variant is read from the instrumentation arguments.
    val pkg = InstrumentationRegistry.getArguments().getString("targetAppId")
      ?: throw Exception("targetAppId not passed as instrumentation runner arg")
    rule.collect(
      packageName = pkg,

      // See: https://d.android.com/topic/performance/baselineprofiles/dex-layout-optimizations
      includeInStartupProfile = true
    ) {
      // This block defines the app's critical user journey. Here we are interested in
      // optimizing for app startup. But you can also navigate and scroll through your most important UI.

      // Start default activity for your app
      grantPermissions(pkg)
      pressHome()
      startActivityAndWait()

      waitForHomeContent()
      scrollSongScreen()
      openAlbumScreen()
      scrollAlbumScreen()
      openPlayingScreen()
      if (device.displayHeight > device.displayWidth) {
        swipeToPlayingLyric()
      }
      device.waitForIdle()
    }
  }
}

private const val DEFAULT_WAIT = 5_000L
private const val SLEEP = 2_000L

private fun MacrobenchmarkScope.waitForHomeContent() {
  device.wait(Until.hasObject(By.desc("BottomBar")), DEFAULT_WAIT)
  device.waitForIdle()
}

private fun MacrobenchmarkScope.scrollSongScreen() {
  val width = device.displayWidth / 2
  val startY = (device.displayHeight * 0.85f).toInt()
  val endY = (device.displayHeight * 0.15f).toInt()
  device.swipe(width, startY, width, endY, /*steps=*/ 20)
  SystemClock.sleep(SLEEP)
}

private fun MacrobenchmarkScope.openPlayingScreen() {
  device.findObject(By.desc("PlayPause")).click()

  SystemClock.sleep(SLEEP)
  device.findObject(By.desc("BottomBar")).click()
  device.wait(Until.hasObject(By.desc("PlayingBack")), DEFAULT_WAIT)
}

private fun MacrobenchmarkScope.openAlbumScreen() {
  val startX = (device.displayWidth * 0.8f).toInt()
  val endX = (device.displayWidth * 0.2f).toInt()
  val y = device.displayHeight / 2
  device.swipe(startX, y, endX, y, /*steps=*/ 20)
}

private fun MacrobenchmarkScope.scrollAlbumScreen() {
  val width = device.displayWidth / 2
  val startY = (device.displayHeight * 0.85f).toInt()
  val endY = (device.displayHeight * 0.15f).toInt()
  device.swipe(width, startY, width, endY, /*steps=*/ 20)

  SystemClock.sleep(SLEEP)
}

private fun MacrobenchmarkScope.swipeToPlayingLyric() {
  val startX = (device.displayWidth * 0.8f).toInt()
  val endX = (device.displayWidth * 0.2f).toInt()
  val y = device.displayHeight / 2
  device.swipe(startX, y, endX, y, /*steps=*/ 20)
}

private fun MacrobenchmarkScope.grantPermissions(packageName: String) {
  when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
      listOf(
        Manifest.permission.READ_MEDIA_AUDIO,
        Manifest.permission.POST_NOTIFICATIONS,
      )
    }

    Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q -> {
      listOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
      )
    }

    else -> {
      listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
  }.forEach { perm ->
    device.executeShellCommand("pm grant $packageName $perm")
  }
}
