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
import androidx.test.uiautomator.UiObject2
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
      openPlayingPanel()
      playNextThenDelay()
      if (device.displayHeight > device.displayWidth) {
        swipeToPlayingLyric()
      }
      device.waitForIdle()
    }
  }
}

private const val DEFAULT_WAIT = 5_000L
private const val SWIPE_STEPS = 20
private const val NEXT_TO_LYRIC_DELAY_MS = 1_000L

private fun MacrobenchmarkScope.waitForHomeContent() {
  waitForObject("BottomBar")
  waitForObject("PlayPause")
  device.waitForIdle()
}

private fun MacrobenchmarkScope.scrollSongScreen() {
  val width = device.displayWidth / 2
  val startY = (device.displayHeight * 0.85f).toInt()
  val endY = (device.displayHeight * 0.15f).toInt()
  device.swipe(width, startY, width, endY, SWIPE_STEPS)
  device.waitForIdle()
}

private fun MacrobenchmarkScope.openPlayingPanel() {
  waitForObject("PlayPause").click()
  device.waitForIdle()

  waitForObject("BottomBar").click()
  check(device.wait(Until.hasObject(By.desc("PlayingBack")), DEFAULT_WAIT)) {
    "Failed to open playing panel from BottomBar. Ensure the test device has at least one playable local song."
  }
  device.waitForIdle()
}

private fun MacrobenchmarkScope.playNextThenDelay() {
  waitForObject("PlayingNext").click()
  device.waitForIdle()
  SystemClock.sleep(NEXT_TO_LYRIC_DELAY_MS)
}

private fun MacrobenchmarkScope.openAlbumScreen() {
  val startX = (device.displayWidth * 0.8f).toInt()
  val endX = (device.displayWidth * 0.2f).toInt()
  val y = device.displayHeight / 2
  device.swipe(startX, y, endX, y, SWIPE_STEPS)
  device.waitForIdle()
}

private fun MacrobenchmarkScope.scrollAlbumScreen() {
  val width = device.displayWidth / 2
  val startY = (device.displayHeight * 0.85f).toInt()
  val endY = (device.displayHeight * 0.15f).toInt()
  device.swipe(width, startY, width, endY, SWIPE_STEPS)
  device.waitForIdle()
}

private fun MacrobenchmarkScope.swipeToPlayingLyric() {
  val startX = (device.displayWidth * 0.8f).toInt()
  val endX = (device.displayWidth * 0.2f).toInt()
  val y = device.displayHeight / 2
  device.swipe(startX, y, endX, y, SWIPE_STEPS)
  device.waitForIdle()
}

private fun MacrobenchmarkScope.grantPermissions(packageName: String) {
  val permissions = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
      listOf(
        Manifest.permission.READ_MEDIA_AUDIO to true,
        Manifest.permission.POST_NOTIFICATIONS to true,
      )
    }

    Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q -> {
      listOf(
        Manifest.permission.READ_EXTERNAL_STORAGE to true,
        Manifest.permission.WRITE_EXTERNAL_STORAGE to false,
      )
    }

    else -> {
      listOf(Manifest.permission.READ_EXTERNAL_STORAGE to true)
    }
  }

  permissions.forEach { (permission, required) ->
    val output = device.executeShellCommand("pm grant $packageName $permission").trim()
    check(!required || output.isEmpty()) {
      "Failed to grant required permission $permission for $packageName. shell output: $output"
    }
  }
}

private fun MacrobenchmarkScope.waitForObject(
  contentDescription: String,
  timeoutMs: Long = DEFAULT_WAIT
): UiObject2 {
  return device.wait(Until.findObject(By.desc(contentDescription)), timeoutMs)
    ?: throw AssertionError("Timed out waiting for object with contentDescription=$contentDescription")
}
