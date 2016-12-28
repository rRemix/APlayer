package remix.myplayer.appwidgets;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.widget.RemoteViews;

import remix.myplayer.R;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.util.Constants;
import remix.myplayer.util.MediaStoreUtil;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/12/28 15:50
 */

public class BaseAppwidget extends AppWidgetProvider {
    protected PendingIntent buildPendingIntent(Context context,int operation, ComponentName componentName) {
        Intent intent = new Intent(Constants.CTL_ACTION);
        intent.putExtra("Control",operation);
        intent.setComponent(componentName);
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
