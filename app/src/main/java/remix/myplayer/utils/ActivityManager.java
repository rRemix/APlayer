package remix.myplayer.utils;

import android.app.Activity;

import java.util.ArrayList;

/**
 * Created by taeja on 16-3-21.
 */
public class ActivityManager {
    private static ArrayList<Activity> mActivityList = new ArrayList<>();

    public static void AddActivity(Activity activity){
        mActivityList.add(activity);
    }
    public static void RemoveActivity(Activity activity){
        mActivityList.remove(activity);
    }

    public static void FinishAll(){
        for(Activity activity : mActivityList){
            if(activity != null && !activity.isFinishing())
                activity.finish();
        }
    }
}
