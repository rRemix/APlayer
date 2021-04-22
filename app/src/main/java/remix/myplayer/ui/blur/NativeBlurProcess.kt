package remix.myplayer.ui.blur

import android.graphics.Bitmap
import java.util.*
import java.util.concurrent.Callable

/**
 * @see JavaBlurProcess Blur using the NDK and native code.
 */
class NativeBlurProcess : BlurProcess {
  companion object {
    private external fun functionToBlur(bitmapOut: Bitmap, radius: Int, threadCount: Int,
                                        threadIndex: Int, round: Int)
    init {
      System.loadLibrary("blur")
    }
  }

  override fun blur(original: Bitmap, radius: Float): Bitmap? {
    val bitmapOut = original.copy(Bitmap.Config.ARGB_8888, true)
    val cores = StackBlurManager.EXECUTOR_THREADS
    val horizontal = ArrayList<NativeTask>(cores)
    val vertical = ArrayList<NativeTask>(cores)
    for (i in 0 until cores) {
      horizontal.add(NativeTask(bitmapOut, radius.toInt(), cores, i, 1))
      vertical.add(NativeTask(bitmapOut, radius.toInt(), cores, i, 2))
    }
    try {
      StackBlurManager.EXECUTOR.invokeAll<Void>(horizontal)
    } catch (e: InterruptedException) {
      return bitmapOut
    }
    try {
      StackBlurManager.EXECUTOR.invokeAll<Void>(vertical)
    } catch (e: InterruptedException) {
      return bitmapOut
    }
    return bitmapOut
  }

  private class NativeTask(private val _bitmapOut: Bitmap, private val _radius: Int, private val _totalCores: Int, private val _coreIndex: Int, private val _round: Int) : Callable<Void?> {
    @Throws(Exception::class)
    override fun call(): Void? {
      functionToBlur(_bitmapOut, _radius, _totalCores, _coreIndex, _round)
      return null
    }
  }
}