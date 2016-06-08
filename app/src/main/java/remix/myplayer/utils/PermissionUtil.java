package remix.myplayer.utils;

/**
 * Created by taeja on 16-6-7.
 */

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.Toast;

/**
 * 权限工具类
 */

public class PermissionUtil {
    private static final String TAG = "PermissionUtil";
    private static Context mContext;
    private PermissionUtil(){}
    private static boolean mHasPermission = false;

    public static void setContext(Context context){
        mContext = context;
    }

    private static final int PERMISSIONCODE = 100;
    private static final String[] PERMISSIONS = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE};

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
    public static boolean hasPermission(String permission) {
        return ContextCompat.checkSelfPermission(mContext, permission) ==
                PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 申请某个权限
     */
    public static void RequestPermission(final Activity activity, String permission){
        if (!hasPermission(permission)) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                Toast.makeText(activity,"应用需要必要的运行权限",Toast.LENGTH_SHORT);
            }
            ActivityCompat.requestPermissions(activity, new String[]{permission}, PERMISSIONCODE);

        }
    }


}
