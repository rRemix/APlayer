package remix.myplayer.misc.update

import android.annotation.TargetApi
import android.app.IntentService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.support.v4.app.NotificationCompat
import cn.bmob.v3.update.UpdateResponse
import remix.myplayer.R
import remix.myplayer.ui.activity.MainActivity
import remix.myplayer.util.LogUtil
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL

/**
 * Creates an IntentService.  Invoked by your subclass's constructor.
 *
 * @param name Used to name the worker thread, important only for debugging.
 */
class UpdateService : IntentService("UpdateService") {
    private val mNotificationManager:NotificationManager by lazy {
        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onCreate() {
        super.onCreate()
        //todo 创建通知栏通道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannelIfNeed()
        }
    }


    @TargetApi(Build.VERSION_CODES.O)
    private fun createNotificationChannelIfNeed() {
        val playingNotificationChannel = NotificationChannel(UPDATE_NOTIFICATION_CHANNEL_ID, getString(R.string.update_notification), NotificationManager.IMPORTANCE_LOW)
        playingNotificationChannel.setShowBadge(false)
        playingNotificationChannel.enableLights(false)
        playingNotificationChannel.enableVibration(false)
        playingNotificationChannel.description = getString(R.string.update_notification_description)
        mNotificationManager.createNotificationChannel(playingNotificationChannel)
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null || !intent.hasExtra(EXTRA_RESPONSE)) {
            return
        }
        val updateResponse = intent.getSerializableExtra(EXTRA_RESPONSE) as UpdateResponse
        var inStream: InputStream? = null
        var outStream: FileOutputStream? = null
        try {
            val downloadUrl = updateResponse.path
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val downloadDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                    ?: throw RuntimeException("下载目录不存在")
            if (!downloadDir.exists() && !downloadDir.mkdirs()) {
                throw RuntimeException("下载目录创建失败")
            }
            val downloadFile = File(downloadDir, updateResponse.path_md5 + ".apk")
            //已经下载完成
            if (downloadFile.exists()) {
                val length = downloadFile.length()
                if (downloadFile.length() == updateResponse.target_size) {
                    sendCompleteBroadcast(downloadFile.absolutePath)
                    return
                } else {
                    //删除原来的文件并重新创建
                    if (!downloadFile.delete() && !downloadFile.createNewFile()) {
                        throw RuntimeException("无法重新创建文件")
                    }
                }
            }
            val url = URL(downloadUrl)
            val conn = url.openConnection()
            conn.connect()
            inStream = conn.getInputStream()
            val fileSize = conn.contentLength//根据响应获取文件大小
            if (fileSize <= 0) throw RuntimeException("无法获知文件大小")
            if (inStream == null) throw RuntimeException("无法获取输入流")
            outStream = FileOutputStream(downloadFile)
            val buf = ByteArray(1024)
            var downloadSize = 0L
            do {
                //循环读取
                val numRead = inStream.read(buf)
                if (numRead == -1) {
                    break
                }
                outStream.write(buf, 0, numRead)
                downloadSize += numRead
                postNotification(updateResponse.target_size, downloadSize,downloadFile.absolutePath)
                //更新进度条
            } while (true)
            outStream.flush()
            sendCompleteBroadcast(downloadFile.absolutePath)
        } catch (ex: Exception) {
            ToastUtil.show(this,"更新失败: " + ex.toString())
        } finally {
            Util.closeStream(inStream)
            Util.closeStream(outStream)
        }
        mNotificationManager.cancel(UPDATE_NOTIFICATION_ID)
    }

    private fun sendCompleteBroadcast(path:String){
        sendBroadcast(Intent(MainActivity.DownloadReceiver.ACTION_DOWNLOAD_COMPLETE)
                .putExtra(EXTRA_PATH,path))
    }

    private fun postNotification(targetSize: Long, downloadSize: Long,path: String) {
        val builder = NotificationCompat.Builder(this, UPDATE_NOTIFICATION_CHANNEL_ID)
                .setContentIntent(null)
                .setContentTitle(getString(R.string.downloading))
//                .setContentText(getString(if(isFinish) R.string.download_complete_to_do else R.string.please_wait))
                .setProgress(targetSize.toInt(), downloadSize.toInt(),false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(false)
                .setShowWhen(false)
                .setOngoing(true)
        mNotificationManager.notify(UPDATE_NOTIFICATION_ID, builder.build())
        LogUtil.d(TAG, "TargetSize: $targetSize DownloadSize: $downloadSize")
    }

    private fun getContentIntent(isFinish:Boolean,path:String): PendingIntent? {
        return null
//        return if(isFinish){
//            val intent = Intent(MainActivity.DownloadReceiver.ACTION_DOWNLOAD_COMPLETE)
//                    .putExtra(EXTRA_PATH,path)
//            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
//        }else{
//            val intent = Intent(this, MainActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
//            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
//        }
    }

    companion object {
        const val EXTRA_RESPONSE = "update_response"
        const val EXTRA_PATH = "file_path"
        private const val TAG = "UpdateService"
        private const val UPDATE_NOTIFICATION_CHANNEL_ID = "update_notification"
        private const val UPDATE_NOTIFICATION_ID = 10
    }
}
