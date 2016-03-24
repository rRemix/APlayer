package remix.myplayer.utils;

import android.app.Activity;
import android.app.Service;

import java.util.ArrayList;

/**
 * Created by taeja on 16-3-24.
 */

/**
 * 管理所有Service
 * 当程序退出时，停止所有Service
 */
public class ServiceManager {
    private static ArrayList<Service> mActivityList = new ArrayList<>();

    public static void AddService(Service service){
        mActivityList.add(service);
    }
    public static void RemoveService(Service service){
        mActivityList.remove(service);
    }

    public static void StopAll(){
        for(Service service : mActivityList){
            if(service != null)
                service.stopSelf();
        }
    }
}
