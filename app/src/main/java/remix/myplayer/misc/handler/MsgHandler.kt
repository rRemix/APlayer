package remix.myplayer.misc.handler

import android.os.Handler
import android.os.Looper
import android.os.Message
import timber.log.Timber
import java.lang.ref.WeakReference
import java.lang.reflect.Method

/**
 * Created by Remix on 2017/11/16.
 */
class MsgHandler(looper: Looper, from: Any, clazz: Class<*>) : Handler(looper) {
  private var method: Method? = null
  private val ref: WeakReference<Any> = WeakReference(from)

  @JvmOverloads
  constructor(from: Any, clazz: Class<*> = from.javaClass) : this(Looper.getMainLooper(), from, clazz)

  init {
    for (method in clazz.declaredMethods) {
      if (method.isAnnotationPresent(OnHandleMessage::class.java)) {
        this.method = method
      }
    }
  }

  override fun handleMessage(msg: Message) {
    if (method == null || ref.get() == null) {
      return
    }
    try {
      method?.invoke(ref.get(), msg)
    } catch (e: Exception) {
      Timber.w(e)
    }
  }

  fun remove() {
    removeCallbacksAndMessages(null)
  }

}