package remix.myplayer.util

import android.app.Activity
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.Service
import android.content.*
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaFormat
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.os.Vibrator
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.text.HtmlCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import remix.myplayer.App
import remix.myplayer.App.Companion.context
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.misc.floatpermission.rom.RomUtils
import remix.myplayer.misc.manager.APlayerActivityManager
import timber.log.Timber
import java.io.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Created by Remix on 2015/11/30.
 */
/**
 * 通用工具类
 */
object Util {
  /**
   * 注册本地Receiver
   */
  fun registerLocalReceiver(receiver: BroadcastReceiver?, filter: IntentFilter?) {
    LocalBroadcastManager.getInstance(context).registerReceiver(receiver!!, filter!!)
  }

  /**
   * 注销本地Receiver
   */
  fun unregisterLocalReceiver(receiver: BroadcastReceiver?) {
    LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver!!)
  }

  @JvmStatic
  fun sendLocalBroadcast(intent: Intent?) {
    LocalBroadcastManager.getInstance(context).sendBroadcast(intent!!)
  }

  fun sendCMDLocalBroadcast(cmd: Int) {
    LocalBroadcastManager.getInstance(context).sendBroadcast(MusicUtil.makeCmdIntent(cmd))
  }

  /**
   * 注销Receiver
   */
  fun unregisterReceiver(context: Context?, receiver: BroadcastReceiver?) {
    try {
      context?.unregisterReceiver(receiver)
    } catch (e: Exception) {
    }
  }

  /**
   * 判断app是否运行在前台
   */
  val isAppOnForeground: Boolean
    get() {
      try {
        val activityManager = context
          .getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager? ?: return false
        val packageName = context.packageName
        val appProcesses: MutableList<RunningAppProcessInfo> = activityManager.runningAppProcesses
          ?: return false
        for (appProcess in appProcesses) {
          if (appProcess.processName == packageName &&
            appProcess.importance == RunningAppProcessInfo.IMPORTANCE_FOREGROUND
          ) {
            return true
          }
        }
      } catch (e: Exception) {
        Timber.w("isAppOnForeground(), ex: %s", e.message)
        return APlayerActivityManager.isAppForeground
      }
      return false
    }

  /**
   * 震动
   */
  fun vibrate(context: Context?, milliseconds: Long) {
    if (context == null) {
      return
    }
    try {
      val vibrator = context.getSystemService(Service.VIBRATOR_SERVICE) as Vibrator
      vibrator.vibrate(milliseconds)
    } catch (ignore: Exception) {
    }
  }

  /**
   * 获得目录大小
   */
  fun getFolderSize(file: File?): Long {
    var size: Long = 0
    try {
      val fileList = file?.listFiles() ?: return size
      for (i in fileList.indices) {
        // 如果下面还有文件
        size = if (fileList[i].isDirectory) {
          size + getFolderSize(fileList[i])
        } else {
          size + fileList[i].length()
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return size
  }

  /**
   * 删除某个目录
   */
  fun deleteFilesByDirectory(directory: File?) {
    if (directory == null) {
      return
    }
    if (directory.isFile) {
      deleteFileSafely(directory)
      return
    }
    if (directory.isDirectory) {
      val childFile = directory.listFiles()
      if (childFile == null || childFile.isEmpty()) {
        deleteFileSafely(directory)
        return
      }
      for (f in childFile) {
        deleteFilesByDirectory(f)
      }
      deleteFileSafely(directory)
    }
  }

  /**
   * 安全删除文件 小米、华为等手机极有可能在删除一个文件后再创建同名文件出现bug
   */
  fun deleteFileSafely(file: File?): Boolean {
    if (file != null) {
      val tmpPath = (file.parent ?: return false) + File.separator + System.currentTimeMillis()
      val tmp = File(tmpPath)
      return file.renameTo(tmp) && tmp.delete()
    }
    return false
  }

  /**
   * 防止修改字体大小
   */
  fun setFontSize(Application: App) {
    val resource = Application.resources
    val c = resource.configuration
    c.fontScale = 1.0f
    resource.updateConfiguration(c, resource.displayMetrics)
  }

  /**
   * 获得歌曲格式
   */
  fun getType(mimeType: String): String {
    return when {
      mimeType == MediaFormat.MIMETYPE_AUDIO_MPEG -> {
        "mp3"
      }
      mimeType == MediaFormat.MIMETYPE_AUDIO_FLAC -> {
        "flac"
      }
      mimeType == MediaFormat.MIMETYPE_AUDIO_AAC -> {
        "aac"
      }
      mimeType.contains("ape") -> {
        "ape"
      }
      else -> {
        try {
          if (mimeType.contains("audio/")) {
            mimeType.substring(6, mimeType.length - 1)
          } else {
            mimeType
          }
        } catch (e: Exception) {
          mimeType
        }
      }
    }
  }

  /**
   * 转换时间
   *
   * @return 00:00格式的时间
   */
  fun getTime(duration: Long): String {
    val minute = duration.toInt() / 1000 / 60
    val second = (duration / 1000).toInt() % 60
    //如果分钟数小于10
    return if (minute < 10) {
      if (second < 10) {
        "0$minute:0$second"
      } else {
        "0$minute:$second"
      }
    } else {
      if (second < 10) {
        "$minute:0$second"
      } else {
        "$minute:$second"
      }
    }
  }

  /**
   * 检测 响应某个意图的Activity 是否存在
   */
  fun isIntentAvailable(context: Context, intent: Intent?): Boolean {
    val packageManager = context.packageManager
    val list = packageManager.queryIntentActivities(
      intent!!,
      PackageManager.MATCH_DEFAULT_ONLY
    )
    return list != null && list.size > 0
  }

  /**
   * 启动 Activity，失败时 toast
   */
  fun startActivitySafely(context: Context, intent: Intent) {
    try {
      context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
      ToastUtil.show(context, R.string.activity_not_found_tip)
    }
  }

  fun startActivityForResultSafely(
    activity: Activity,
    intent: Intent,
    requestCode: Int
  ) {
    try {
      activity.startActivityForResult(intent, requestCode)
    } catch (e: ActivityNotFoundException) {
      ToastUtil.show(activity, R.string.activity_not_found_tip)
    }
  }

  /**
   * 判断网路是否连接
   */
  val isNetWorkConnected: Boolean
    get() {
      val connectivityManager = context
        .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
      if (connectivityManager != null) {
        val netWorkInfo = connectivityManager.activeNetworkInfo
        if (netWorkInfo != null) {
          return netWorkInfo.isAvailable && netWorkInfo.isConnected
        }
      }
      return false
    }

  /**
   * 删除歌曲
   *
   * @param path 歌曲路径
   * @return 是否删除成功
   */
  fun deleteFile(path: String?): Boolean {
    val file = File(path ?: return false)
    return file.exists() && file.delete()
  }

  /**
   * 处理歌曲名、歌手名或者专辑名
   *
   * @param origin 原始数据
   * @param type 处理类型 0:歌曲名 1:歌手名 2:专辑名 3:文件名
   * @return
   */
  const val TYPE_SONG = 0
  const val TYPE_ARTIST = 1
  const val TYPE_ALBUM = 2
  const val TYPE_DISPLAYNAME = 3
  fun processInfo(origin: String?, type: Int): String {
    return if (type == TYPE_SONG) {
      if (origin == null || origin == "") {
        context.getString(R.string.unknown_song)
      } else {
//                return origin.lastIndexOf(".") > 0 ? origin.substring(0, origin.lastIndexOf(".")) : origin;
        origin
      }
    } else if (type == TYPE_DISPLAYNAME) {
      if (origin == null || origin == "") {
        context.getString(R.string.unknown_song)
      } else {
        if (origin.lastIndexOf(".") > 0) origin.substring(0, origin.lastIndexOf(".")) else origin
      }
    } else {
      if (origin == null || origin == "") {
        context
          .getString(if (type == TYPE_ARTIST) R.string.unknown_artist else R.string.unknown_album)
      } else {
        origin
      }
    }
  }

  /**
   * 判断是否连续点击
   *
   * @return
   */
  private var mLastClickTime: Long = 0
  private const val INTERVAL = 500
  val isFastDoubleClick: Boolean
    get() {
      val time = System.currentTimeMillis()
      val timeInterval = time - mLastClickTime
      if (timeInterval in 1 until INTERVAL) {
        return true
      }
      mLastClickTime = time
      return false
    }

  /**
   * 返回关键词的MD值
   */
  @JvmStatic
  fun hashKeyForDisk(key: String): String {
    val cacheKey: String = try {
      val mDigest = MessageDigest.getInstance("MD5")
      mDigest.update(key.toByteArray())
      bytesToHexString(mDigest.digest())
    } catch (e: NoSuchAlgorithmException) {
      key.hashCode().toString()
    }
    return cacheKey
  }

  private fun bytesToHexString(bytes: ByteArray): String {
    val sb = StringBuilder()
    for (i in bytes.indices) {
      val hex = Integer.toHexString(0xFF and bytes[i].toInt())
      if (hex.length == 1) {
        sb.append('0')
      }
      sb.append(hex)
    }
    return sb.toString()
  }

  /**
   * 浏览器打开指定地址
   */
  fun openUrl(url: String?) {
    if (TextUtils.isEmpty(url)) {
      return
    }
    val uri = Uri.parse(url)
    val it = Intent(Intent.ACTION_VIEW, uri)
    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(it)
  }

  /**
   * 判断wifi是否打开
   */
  fun isWifi(context: Context): Boolean {
    val activeNetInfo = (context
      .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager).activeNetworkInfo
    return activeNetInfo != null && activeNetInfo.type == ConnectivityManager.TYPE_WIFI
  }

  /**
   * 获取app当前的渠道号或application中指定的meta-data
   *
   * @return 如果没有获取成功(没有对应值 ， 或者异常)，则返回值为空
   */
  fun getAppMetaData(key: String?): String? {
    if (TextUtils.isEmpty(key)) {
      return null
    }
    var channelNumber: String? = null
    try {
      val packageManager = context.packageManager
      if (packageManager != null) {
        val applicationInfo =
          packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
        if (applicationInfo != null) {
          if (applicationInfo.metaData != null) {
            channelNumber = applicationInfo.metaData.getString(key)
          }
        }
      }
    } catch (e: PackageManager.NameNotFoundException) {
      e.printStackTrace()
    }
    return channelNumber
  }

  fun createShareSongFileIntent(song: Song, context: Context): Intent {
    return try {
      val parcelable: Parcelable = FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        File(song.data)
      )
      Intent()
        .setAction(Intent.ACTION_SEND)
        .putExtra(
          Intent.EXTRA_STREAM,
          parcelable
        )
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .setType("audio/*")
    } catch (e: IllegalArgumentException) {
      //the path is most likely not like /storage/emulated/0/... but something like /storage/28C7-75B0/...
      e.printStackTrace()
      Toast.makeText(context, context.getString(R.string.cant_share_song), Toast.LENGTH_SHORT)
        .show()
      Intent()
    }
  }

  fun createShareImageFileIntent(file: File, context: Context): Intent {
    return try {
      val parcelable: Parcelable = FileProvider.getUriForFile(
        context,
        context.packageName + ".fileprovider",
        file
      )
      Intent()
        .setAction(Intent.ACTION_SEND)
        .putExtra(
          Intent.EXTRA_STREAM,
          parcelable
        )
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .setType("image/*")
    } catch (e: IllegalArgumentException) {
      e.printStackTrace()
      Toast.makeText(context, context.getString(R.string.cant_share_song), Toast.LENGTH_SHORT)
        .show()
      Intent()
    }
  }

  @JvmStatic
  fun closeSafely(closeable: Closeable?) {
    if (closeable != null) {
      if (closeable is Cursor && closeable.isClosed) {
        return
      }
      try {
        closeable.close()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  fun installApk(context: Context, path: String?) {
    if (path == null) {
      ToastUtil.show(context, context.getString(R.string.empty_path_report_to_developer))
      return
    }
    val installFile = File(path)
    val intent = Intent(Intent.ACTION_VIEW)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      val apkUri = FileProvider.getUriForFile(
        context,
        context.applicationContext.packageName + ".fileprovider", installFile
      )
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
      context.startActivity(intent)
    } else {
      intent.setDataAndType(Uri.fromFile(installFile), "application/vnd.android.package-archive")
      context.startActivity(intent)
    }
  }

  /**
   * 获取进程号对应的进程名
   *
   * @param pid 进程号
   * @return 进程名
   */
  fun getProcessName(pid: Int): String? {
    var reader: BufferedReader? = null
    try {
      reader = BufferedReader(FileReader("/proc/$pid/cmdline"))
      var processName = reader.readLine()
      if (!TextUtils.isEmpty(processName)) {
        processName = processName.trim { it <= ' ' }
      }
      return processName
    } catch (throwable: Throwable) {
      throwable.printStackTrace()
    } finally {
      try {
        reader?.close()
      } catch (exception: IOException) {
        exception.printStackTrace()
      }
    }
    return null
  }

  /**
   * 判断是否支持状态栏歌词
   */
  fun isSupportStatusBarLyric(context: Context): Boolean {
    return RomUtils.checkIsMeizuRom() || Settings.System.getInt(
      context.contentResolver,
      "status_bar_show_lyric",
      0
    ) != 0 || RomUtils.checkIsbaolong24Rom() || RomUtils.checkIsexTHmUIRom()
  }

  /**
   * HTML 转纯文本
   *
   * 用于处理 QQ 歌词中的“&apos;”等
   */
  fun htmlToText(source: String?): String {
    return HtmlCompat.fromHtml(source!!, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
  }

  fun hideKeyboard(view: View?): Boolean {
    if (view == null) {
      return false
    }
    try {
      val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
      return if (!imm.isActive) {
        false
      } else imm.hideSoftInputFromWindow(view.windowToken, 0)
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return false
  }
}