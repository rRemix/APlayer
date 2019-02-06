//package remix.myplayer.util;
//
//import android.content.Context;
//import android.content.pm.PackageInfo;
//import android.content.pm.PackageManager;
//import android.os.Build;
//import android.os.Environment;
//import android.os.Process;
//import android.util.Log;
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.text.SimpleDateFormat;
//import java.util.Date;
//
///**
// * Created by taeja on 16-3-16.
// */
//
///**
// * 处理未捕获异常 记录到sd卡并上传到有盟
// */
//public class CrashHandler implements Thread.UncaughtExceptionHandler {
//
//  private static final String TAG = "CrashHandler";
//  private static final boolean DEBUG = true;
//
//  private String PATH;
//  //log文件的后缀名
//  private static final String FILE_NAME_SUFFIX = ".txt";
//
//  private static CrashHandler sInstance = new CrashHandler();
//
//  //系统默认的异常处理（默认情况下，系统会终止当前的异常程序）
//  private Thread.UncaughtExceptionHandler mDefaultCrashHandler;
//
//  private Context mContext;
//
//  //构造方法私有，防止外部构造多个实例，即采用单例模式
//  private CrashHandler() {
//  }
//
//  public static CrashHandler getInstance() {
//    return sInstance;
//
//  }
//
//  //这里主要完成初始化工作
//  public void init(Context context) {
//    //获取系统默认的异常处理器
//    mDefaultCrashHandler = Thread.getDefaultUncaughtExceptionHandler();
//    //将当前实例设为系统默认的异常处理器
//    Thread.setDefaultUncaughtExceptionHandler(this);
//    //获取Context，方便内部使用
//    mContext = context.getApplicationContext();
//  }
//
//  /**
//   * 这个是最关键的函数，当程序中有未被捕获的异常，系统将会自动调用#uncaughtException方法 thread为出现未捕获异常的线程，ex为未捕获的异常，有了这个ex，我们就可以得到异常信息。
//   */
//  @Override
//  public void uncaughtException(Thread thread, Throwable ex) {
//    try {
//      //导出异常信息到SD卡中
//      dumpExceptionToSDCard(ex);
//      //这里可以通过网络上传异常信息到服务器，便于开发人员分析日志从而解决bug
//      uploadExceptionToServer(ex);
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
//
//    //打印出当前调用栈信息
//    ex.printStackTrace();
//
//    //如果系统提供了默认的异常处理器，则交给系统去结束我们的程序，否则就由我们自己结束自己
//    if (mDefaultCrashHandler != null) {
//      mDefaultCrashHandler.uncaughtException(thread, ex);
//    } else {
//      Process.killProcess(Process.myPid());
//    }
//
//  }
//
//
//  /**
//   * 将错误信息保存到SD卡上
//   */
//  private void dumpExceptionToSDCard(Throwable ex) throws IOException {
//    //如果SD卡不存在或无法使用，则无法把异常信息写入SD卡
//    if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//      if (DEBUG) {
//        Log.w(TAG, "sdcard unmounted,skip dump exception");
//        return;
//      }
//    }
//
//    long current = System.currentTimeMillis();
//    String time = new SimpleDateFormat("HH:mm:ss").format(new Date(current));
//    String date = new SimpleDateFormat("yyyy-MM-dd").format(new Date(current));
//
//    PATH = Environment.getExternalStorageDirectory().getPath() + "/Android/data/" + mContext
//        .getPackageName() + "/log/" + date + "/";
//    File dir = new File(PATH);
//    if (!dir.exists()) {
//      dir.mkdirs();
//    }
//
//    //以当前时间创建log文件
//    File file = new File(PATH + time + FILE_NAME_SUFFIX);
//    PrintWriter pw = null;
//    try {
//      pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
//      //导出发生异常的时间
//      pw.println(time);
//      //导出手机信息
//      dumpPhoneInfo(pw);
//      pw.println();
//      //导出异常的调用栈信息
//      ex.printStackTrace(pw);
//      pw.close();
//    } catch (Exception e) {
//      Log.e(TAG, "dump crash info failed");
//    } finally {
//      if (pw != null) {
//        pw.close();
//      }
//    }
//  }
//
//  private void dumpPhoneInfo(PrintWriter pw) throws PackageManager.NameNotFoundException {
//    //应用的版本名称和版本号
//    PackageManager pm = mContext.getPackageManager();
//    PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);
//    pw.println("App Version: " + pi.versionName);
//
//    pw.println("App Version code: " + pi.versionCode);
//    //android版本号
//    pw.println("Android release version: " + Build.VERSION.RELEASE);
//    pw.println("Android Sdk version: " + Build.VERSION.SDK_INT);
//
//    //手机制造商
//    pw.println("Device manufacturer: " + Build.MANUFACTURER);
//
//    //手机型号
//    pw.println(Build.MODEL);
//    //cpu架构
//    pw.print("CPU ABI: " + Build.CPU_ABI);
//  }
//
//
//  /**
//   * 上传到友盟
//   */
//  private void uploadExceptionToServer(Throwable ex) {
////        MobclickAgent.reportError(mContext,ex);
//  }
//
//}
