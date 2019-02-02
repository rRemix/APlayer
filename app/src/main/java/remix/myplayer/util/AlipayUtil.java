package remix.myplayer.util;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;
import remix.myplayer.R;

/**
 * Created by codeest on 16/10/17. https://github.com/fython/AlipayZeroSdk/blob/master/library/src/main/java/moe/feng/alipay/zerosdk/AlipayZeroSdk.java
 */

public class AlipayUtil {

  // 支付宝包名
  private static final String ALIPAY_PACKAGE_NAME = "com.eg.android.AlipayGphone";

  // 旧版支付宝二维码通用 Intent Scheme Url 格式
  private static final String INTENT_URL_FORMAT = "intent://platformapi/startapp?saId=10000007&" +
      "clientVersion=3.7.0.0718&qrcode=https%3A%2F%2Fqr.alipay.com%2F{urlCode}%3F_s" +
      "%3Dweb-other&_t=1472443966571#Intent;" +
      "scheme=alipayqr;package=com.eg.android.AlipayGphone;end";

  /**
   * 打开转账窗口 旧版支付宝二维码方法，需要使用 https://fama.alipay.com/qrcode/index.htm 网站生成的二维码 这个方法最好，但在 2016 年 8 月发现新用户可能无法使用
   *
   * @param activity Parent Activity
   * @return 是否成功调用
   */
  //568920427@qq.com FKX01752E4HENBODS0YAA6 lin_kin_p@163.com FKX01908X8ECOECIQZIL43
  public static boolean startAlipayClient(Activity activity) {
    return startIntentUrl(activity,
        INTENT_URL_FORMAT.replace("{urlCode}", "FKX01752E4HENBODS0YAA6"));
  }

  /**
   * 打开 Intent Scheme Url
   *
   * @param activity Parent Activity
   * @param intentFullUrl Intent 跳转地址
   * @return 是否成功调用
   */
  public static boolean startIntentUrl(Activity activity, String intentFullUrl) {
    try {
      activity.startActivity(Intent.parseUri(intentFullUrl, Intent.URI_INTENT_SCHEME));
      return true;
    } catch (Exception e) {
      ClipboardManager clipboardManager = (ClipboardManager) activity
          .getSystemService(Context.CLIPBOARD_SERVICE);
      ClipData clipData = ClipData.newPlainText("text", "lin_kin_p@163.com");
      clipboardManager.setPrimaryClip(clipData);
      Toast toast = Toast
          .makeText(activity, activity.getString(R.string.jump_alipay_error), Toast.LENGTH_SHORT);
      ((TextView) toast.getView()
          .findViewById(Resources.getSystem().getIdentifier("message", "id", "android")))
          .setGravity(Gravity.CENTER);
      toast.show();
      return false;
    }
  }

  /**
   * 判断支付宝客户端是否已安装，建议调用转账前检查
   *
   * @param context Context
   * @return 支付宝客户端是否已安装
   */
  public static boolean hasInstalledAlipayClient(Context context) {
    PackageManager pm = context.getPackageManager();
    try {
      PackageInfo info = pm.getPackageInfo(ALIPAY_PACKAGE_NAME, 0);
      return info != null;
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
      return false;
    }
  }

}
