package remix.myplayer.misc.handler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.lang.reflect.Method;

/**
 * Created by Remix on 2017/11/16.
 */
public class MsgHandler extends Handler {
    private Method mMethod;
    private Object mFrom;

    public MsgHandler(Looper looper, Object from, Class clazz) {
        super(looper);
        mFrom = from;
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(OnHandleMessage.class)) {
                mMethod = method;
            }
        }
    }

    public MsgHandler(Object from, Class clazz) {
        this(Looper.getMainLooper(), from, clazz);
    }

    @Override
    public void handleMessage(Message msg) {
        if (mMethod == null)
            return;
        try {
            mMethod.invoke(mFrom, msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
