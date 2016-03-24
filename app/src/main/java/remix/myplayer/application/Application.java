package remix.myplayer.application;

import android.content.Context;

import remix.myplayer.utils.CrashHandler;

/**
 * Created by taeja on 16-3-16.
 */

/**
 * 错误收集与上报
 */
public class Application extends android.app.Application {
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);
    }

    public static Context getContext(){
        return mContext;
    }
}
