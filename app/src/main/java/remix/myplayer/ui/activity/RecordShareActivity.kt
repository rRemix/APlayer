package remix.myplayer.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Message
import android.text.TextUtils
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import com.afollestad.materialdialogs.MaterialDialog
import remix.myplayer.App.Companion.context
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.ActivityRecordshareBinding
import remix.myplayer.glide.GlideApp
import remix.myplayer.misc.cache.DiskCache
import remix.myplayer.misc.handler.MsgHandler
import remix.myplayer.misc.handler.OnHandleMessage
import remix.myplayer.theme.GradientDrawableMaker
import remix.myplayer.theme.Theme
import remix.myplayer.ui.activity.base.BaseMusicActivity
import remix.myplayer.util.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by taeja on 16-3-14.
 */
/**
 * 将分享内容与专辑封面进行处理用于分享
 */
class RecordShareActivity : BaseMusicActivity() {
  lateinit var binding: ActivityRecordshareBinding

  //截屏文件
  private var file: File? = null

  //更新处理结果的Handler
  private val handler: MsgHandler by lazy {
    MsgHandler(this)
  }

  @OnHandleMessage
  fun handleMessage(msg: Message) {
    when (msg.what) {
      START -> showLoading()
      STOP -> dismissLoading()
      COMPLETE -> if (file != null) {
        ToastUtil.show(this, R.string.screenshot_save_at, file!!.absoluteFile,
            Toast.LENGTH_LONG)
      }
      ERROR -> {
        ToastUtil.show(this, getString(R.string.share_error) + ":" + msg.obj)
        dismissLoading()
      }
    }
  }

  override fun setStatusBarColor() {
    StatusBarUtil.setTransparent(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    super.onCreate(savedInstanceState)
    binding = ActivityRecordshareBinding.inflate(layoutInflater)
    setContentView(binding.root)
    window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN)
    //初始化控件
    binding.recordshareContainer.isDrawingCacheEnabled = true

    //当前正在播放的歌曲
    val song: Song = intent.extras!!.getParcelable(EXTRA_SONG)
        ?: return
//    LibraryUriRequest(binding.recordshareImage,
//        ImageUriUtil.getSearchRequestWithAlbumType(song),
//        RequestConfig.Builder(IMAGE_SIZE, IMAGE_SIZE).build()).load()
    GlideApp.with(this)
        .load(song)
        .centerCrop()
        .placeholder(Theme.resolveDrawable(this, R.attr.default_album))
        .error(Theme.resolveDrawable(this, R.attr.default_album))
        .into(binding.iv)

    //设置歌曲名与分享内容
    val content = intent.extras!!.getString(EXTRA_CONTENT)
    binding.recordshareContent.text = if (TextUtils.isEmpty(content)) "" else content
    binding.recordshareName.text = String.format("《%s》", song.title)
    //背景
    binding.recordshareBackground1.background = GradientDrawableMaker()
        .color(Color.WHITE)
        .strokeSize(DensityUtil.dip2px(2f))
        .strokeColor(Color.parseColor("#2a2a2a"))
        .make()
    binding.recordshareBackground2.background = GradientDrawableMaker()
        .color(Color.WHITE)
        .strokeSize(DensityUtil.dip2px(1f))
        .strokeColor(Color.parseColor("#2a2a2a"))
        .make()
    binding.recordshareImageContainer.background = GradientDrawableMaker()
        .color(Color.WHITE)
        .strokeSize(DensityUtil.dip2px(1f))
        .strokeColor(Color.parseColor("#f6f6f5"))
        .make()
    binding.recordshareCancel.setOnClickListener { v: View? -> finish() }
    binding.recordshareShare.setOnClickListener { v: View? -> ProcessThread().start() }
  }

  override fun onDestroy() {
    super.onDestroy()
    handler.remove()
  }

  /**
   * 将图片保存到本地
   */
  private inner class ProcessThread : Thread() {
    var fileOutputStream: FileOutputStream? = null

    @SuppressLint("SimpleDateFormat")
    override fun run() {
      //开始处理,显示进度条
      if (!hasPermission) {
        val errMsg = handler.obtainMessage(ERROR)
        errMsg.obj = getString(R.string.plz_give_access_external_storage_permission)
        handler.sendMessage(errMsg)
        return
      }
      handler.sendEmptyMessage(START)
      bg = binding.recordshareContainer.getDrawingCache(true)
      file = null
      try {
        //将截屏内容保存到文件
        val shareDir = DiskCache.getDiskCacheDir(this@RecordShareActivity, "share")
        if (!shareDir.exists()) {
          shareDir.mkdirs()
        }
        file = File(String.format("%s/%s.png", DiskCache.getDiskCacheDir(this@RecordShareActivity, "share"),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(Date(System.currentTimeMillis()))))
        file?.let { file ->
          if (!file.exists()) {
            file.createNewFile()
          }
          fileOutputStream = FileOutputStream(file)
          bg?.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream)
          fileOutputStream?.flush()
          fileOutputStream?.close()
          //处理完成
          handler.sendEmptyMessage(COMPLETE)
          handler.sendEmptyMessage(STOP)

          //打开分享的Dialog
//                Intent intent = new Intent(this, ShareDialog.class);
//                Bundle arg = new Bundle();
//                arg.putInt("Type", Constants.SHARERECORD);
//                arg.putString("Url",mFile.getAbsolutePath());
//                arg.putParcelable("Song",mInfo);
//                intent.putExtras(arg);
//                startActivityForResult(intent,REQUEST_SHARE);
          startActivity(Intent.createChooser(Util.createShareImageFileIntent(file, this@RecordShareActivity), null))
        }

      } catch (e: Exception) {
        val errMsg = handler.obtainMessage(ERROR)
        errMsg.obj = e.toString()
        handler.sendMessage(errMsg)
      } finally {
        if (fileOutputStream != null) {
          try {
            fileOutputStream!!.close()
          } catch (e: IOException) {
            e.printStackTrace()
          }
        }
      }
    }
  }

  public override fun onPause() {
    super.onPause()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    setResult(RESULT_OK, data)
    finish()
  }

  companion object {
    private val IMAGE_SIZE = DensityUtil.dip2px(context, 268f)
    const val EXTRA_SONG = "Song"
    const val EXTRA_CONTENT = "Content"

    //保存截屏
    var bg: Bitmap? = null
      private set

    //处理状态
    private const val START = 0
    private const val STOP = 1
    private const val COMPLETE = 2
    private const val ERROR = 3
  }
}