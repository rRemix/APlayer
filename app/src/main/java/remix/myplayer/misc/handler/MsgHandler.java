package remix.myplayer.misc.handler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import timber.log.Timber;

/**
 * Created by Remix on 2017/11/16.
 */
public class MsgHandler extends Handler {

  private Method mMethod;
  private WeakReference<Object> mRef;

  public MsgHandler(Looper looper, Object from, Class clazz) {
    super(looper);
    mRef = new WeakReference<>(from);
    for (Method method : clazz.getDeclaredMethods()) {
      if (method.isAnnotationPresent(OnHandleMessage.class)) {
        mMethod = method;
      }
    }
  }

  public MsgHandler(Object from, Class clazz) {
    this(Looper.getMainLooper(), from, clazz);
  }

  public MsgHandler(Object from) {
    this(from, from.getClass());
  }

  @Override
  public void handleMessage(Message msg) {
    if (mMethod == null || mRef.get() == null) {
      return;
    }
    try {
      mMethod.invoke(mRef.get(), msg);
    } catch (Exception e) {
      Timber.w(e);
    }
  }

  public void remove() {
    removeCallbacksAndMessages(null);
  }
}
