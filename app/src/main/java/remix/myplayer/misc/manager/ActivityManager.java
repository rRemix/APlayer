package remix.myplayer.misc.manager;

import android.app.Activity;
import java.util.ArrayList;

/**
 * Created by taeja on 16-3-21.
 */

/**
 * 管理所有Activity 当程序退出时，关闭所有activity
 */
public class ActivityManager {

  private static ArrayList<Activity> mActivityList = new ArrayList<>();

  public static void AddActivity(Activity activity) {
    mActivityList.add(activity);
  }

  public static void RemoveActivity(Activity activity) {
    mActivityList.remove(activity);
  }

  public static void FinishAll() {
    for (Activity activity : mActivityList) {
      if (activity != null && !activity.isFinishing()) {
        activity.finish();
      }
    }
  }
}
