package remix.myplayer.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

import remix.myplayer.application.APlayerApplication;
import remix.myplayer.manager.ServiceManager;

/**
 * Created by Remix on 2016/3/26.
 */
public class BaseService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ServiceManager.AddService(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ServiceManager.RemoveService(this);
    }
}
