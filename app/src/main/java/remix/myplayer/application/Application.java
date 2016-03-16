package remix.myplayer.application;

import remix.myplayer.utils.CrashHandler;

/**
 * Created by taeja on 16-3-16.
 */
public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();

        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);
    }
}
