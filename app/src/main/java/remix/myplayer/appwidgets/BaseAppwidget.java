package remix.myplayer.appwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import remix.myplayer.util.Constants;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/12/28 15:50
 */

public class BaseAppwidget extends AppWidgetProvider {
    protected PendingIntent buildPendingIntent(Context context,ComponentName componentName,int operation) {
        Intent intent = new Intent(Constants.CTL_ACTION);
        intent.putExtra("Control",operation);
        intent.putExtra("FromWidget",true);
        intent.setComponent(componentName);
        return PendingIntent.getService(context,operation,intent,0);
    }

    protected boolean hasInstances(Context context) {
        int[] appIds = AppWidgetManager.getInstance(context).getAppWidgetIds(new ComponentName(context, getClass()));
        return appIds != null && appIds.length > 0;
    }
}
