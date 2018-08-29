package remix.myplayer.helper;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.WeakHashMap;

import remix.myplayer.service.MusicService;

public class MusicServiceRemote {
    public static final String TAG = MusicServiceRemote.class.getSimpleName();

    @Nullable
    public static MusicService sService;

    private static final WeakHashMap<Context, ServiceBinder> mConnectionMap = new WeakHashMap<>();

    public static ServiceToken bindToService(@NonNull final Context context, final ServiceConnection callback) {
        Activity realActivity = ((Activity)context).getParent();
        if(realActivity == null)
            realActivity = (Activity) context;

        final ContextWrapper contextWrapper = new ContextWrapper(realActivity);
        contextWrapper.startService(new Intent(contextWrapper, MusicService.class));

        final ServiceBinder binder = new ServiceBinder(callback);

        if(contextWrapper.bindService(new Intent().setClass(contextWrapper, MusicService.class), binder,Context.BIND_AUTO_CREATE)){
            mConnectionMap.put(contextWrapper,binder);
            return new ServiceToken(contextWrapper);
        }
        return null;
    }

    public static final class ServiceBinder implements ServiceConnection {
        private final ServiceConnection mCallback;

        public ServiceBinder(final ServiceConnection callback) {
            mCallback = callback;
        }

        @Override
        public void onServiceConnected(final ComponentName className, final IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            sService = binder.getService();
            if (mCallback != null) {
                mCallback.onServiceConnected(className, service);
            }
        }

        @Override
        public void onServiceDisconnected(final ComponentName className) {
            if (mCallback != null) {
                mCallback.onServiceDisconnected(className);
            }
            sService = null;
        }
    }

    public static void unbindFromService(@Nullable final ServiceToken token) {
        if (token == null) {
            return;
        }
        final ContextWrapper contextWrapper = token.mWrapperContext;
        final ServiceBinder binder = mConnectionMap.remove(contextWrapper);
        if (binder == null) {
            return;
        }
        contextWrapper.unbindService(binder);
        if (mConnectionMap.isEmpty()) {
            sService = null;
        }
    }

    public static final class ServiceToken {
        public ContextWrapper mWrapperContext;

        public ServiceToken(final ContextWrapper contextWrapper) {
            mWrapperContext = contextWrapper;
        }
    }
}
