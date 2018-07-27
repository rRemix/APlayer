package remix.myplayer.appwidgets;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;

import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;
import remix.myplayer.util.LogUtil;

public class Recycler {
    private static final Object mLock = new Object();
    private static final String TAG = "BitmapRecycler";


    @SuppressLint("CheckResult")
    public static void recycleBitmap(final Bitmap bitmap){
        LogUtil.d(TAG,"开始回收");
        if(bitmap == null || bitmap.isRecycled()){
            LogUtil.d(TAG,"Bitmap已经回收或为空: " + bitmap);
            return;
        }
        Completable.timer(5000, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(() -> {
                    synchronized (mLock){
                        if(bitmap.isRecycled()){
                            LogUtil.d(TAG,"Bitmap已经回收: " + bitmap);
                            return;
                        }
                        bitmap.recycle();
                        LogUtil.d(TAG,"回收完成");
                    }

                });
    }
}
