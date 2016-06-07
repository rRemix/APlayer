package remix.myplayer.utils;

/**
 * Created by taeja on 16-6-7.
 */

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;

/**
 * 权限工具类
 */

public class PermissionUtil {
    private static final String TAG = "PermissionUtil";
    private static Context mContext;
    private PermissionUtil(){}
    public static void setContext(Context context){
        mContext = context;
    }

    /**
     * 判断是否系统为6.0及以上
     * @return
     */
    public static boolean isAndroidM(){
        return Build.VERSION.SDK_INT >= 23;
    }

    /**
     * 判断是否拥有权限
     * @param permission 所需权限
     * @return
     */
    private boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(mContext, permission) ==
                PackageManager.PERMISSION_GRANTED;
    }


}
