/**
 * StackBlur v1.0 for Android
 *
 * @Author: Enrique López Mañas <eenriquelopez></eenriquelopez>@gmail.com> http://www.lopez-manas.com
 *
 *
 * Author of the original algorithm: Mario Klingemann <mario.quasimondo.com>
</mario.quasimondo.com> *
 *
 * This is a compromise between Gaussian Blur and Box blur It creates much better looking blurs than Box Blur, but is 7x
 * faster than my Gaussian Blur implementation.
 *
 *
 * I called it Stack Blur because this describes best how this filter works internally: it creates a kind of moving
 * stack of colors whilst scanning through the image. Thereby it just has to add one new block of color to the right
 * side of the stack and remove the leftmost color. The remaining colors on the topmost layer of the stack are either
 * added on or reduced by one, depending on if they are on the right or on the left side of the stack.
 * @copyright: Enrique López Mañas
 * @license: Apache License 2.0
 */
package remix.myplayer.ui.blur

import android.graphics.Bitmap
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StackBlurManager(val image: Bitmap) {
  /**
   * Method of blurring
   */
  private val _blurProcess: BlurProcess = JavaBlurProcess()

  /**
   * Most recent result of blurring
   */
  private var _result: Bitmap? = null

  /**
   * Process the image on the given radius. Radius must be at least 1
   * @param radius
   */
  fun process(radius: Int): Bitmap? {
    _result = _blurProcess.blur(image, radius.toFloat())
    return _result
  }

  /**
   * Returns the blurred image as a bitmap
   * @return blurred image
   */
  fun returnBlurredImage(): Bitmap? {
    return _result
  }

  /**
   * Save the image into the file system
   * @param path The path where to save the image
   */
  fun saveIntoFile(path: String?) {
    try {
      val out = FileOutputStream(path)
      _result?.compress(Bitmap.CompressFormat.PNG, 90, out)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  /**
   * Process the image using a native library
   */
  fun processNatively(radius: Int): Bitmap? {
    val blur = NativeBlurProcess()
    _result = blur.blur(image, radius.toFloat())
    return _result
  }

  companion object {
    val EXECUTOR_THREADS = Runtime.getRuntime().availableProcessors()
    val EXECUTOR: ExecutorService = Executors.newFixedThreadPool(EXECUTOR_THREADS)

    @Volatile
    private var hasRS = true
  }
}