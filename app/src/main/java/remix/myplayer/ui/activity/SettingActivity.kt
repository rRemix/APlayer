package remix.myplayer.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.provider.MediaStore
import android.provider.Settings
import android.support.v4.content.FileProvider
import android.support.v7.widget.SwitchCompat
import android.text.TextUtils
import android.view.View
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import butterknife.BindViews
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.DialogAction
import com.facebook.common.util.ByteConstants
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.App.IS_GOOGLEPLAY
import remix.myplayer.BuildConfig
import remix.myplayer.R
import remix.myplayer.bean.misc.Category
import remix.myplayer.bean.misc.Feedback
import remix.myplayer.bean.mp3.Song
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.helper.EQHelper
import remix.myplayer.helper.EQHelper.REQUEST_EQ
import remix.myplayer.helper.LanguageHelper
import remix.myplayer.helper.LanguageHelper.AUTO
import remix.myplayer.helper.M3UHelper.exportPlayListToFile
import remix.myplayer.helper.M3UHelper.importLocalPlayList
import remix.myplayer.helper.M3UHelper.importM3UFile
import remix.myplayer.helper.ShakeDetector
import remix.myplayer.misc.MediaScanner
import remix.myplayer.misc.floatpermission.FloatWindowManager
import remix.myplayer.misc.handler.MsgHandler
import remix.myplayer.misc.handler.OnHandleMessage
import remix.myplayer.misc.receiver.HeadsetPlugReceiver
import remix.myplayer.misc.tryLaunch
import remix.myplayer.misc.update.UpdateAgent
import remix.myplayer.misc.update.UpdateListener
import remix.myplayer.misc.zipFrom
import remix.myplayer.misc.zipOutputStream
import remix.myplayer.request.ImageUriRequest
import remix.myplayer.request.ImageUriRequest.DOWNLOAD_LASTFM
import remix.myplayer.request.network.RxUtil.applySingleScheduler
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService.Companion.EXTRA_DESKTOP_LYRIC
import remix.myplayer.theme.Theme
import remix.myplayer.theme.Theme.getBaseDialog
import remix.myplayer.theme.ThemeStore
import remix.myplayer.theme.ThemeStore.*
import remix.myplayer.theme.TintHelper
import remix.myplayer.ui.activity.MainActivity.Companion.EXTRA_CATEGORY
import remix.myplayer.ui.activity.MainActivity.Companion.EXTRA_RECREATE
import remix.myplayer.ui.activity.MainActivity.Companion.EXTRA_REFRESH_ADAPTER
import remix.myplayer.ui.activity.MainActivity.Companion.EXTRA_REFRESH_LIBRARY
import remix.myplayer.ui.dialog.FileChooserDialog
import remix.myplayer.ui.dialog.FolderChooserDialog
import remix.myplayer.ui.dialog.FolderChooserDialog.Builder
import remix.myplayer.ui.dialog.LyricPriorityDialog
import remix.myplayer.ui.dialog.color.ColorChooserDialog
import remix.myplayer.util.*
import remix.myplayer.util.SPUtil.SETTING_KEY
import remix.myplayer.util.SPUtil.SETTING_KEY.BOTTOM_OF_NOW_PLAYING_SCREEN
import remix.myplayer.util.Util.sendLocalBroadcast
import timber.log.Timber
import java.io.File
import java.util.*

/**
 * @ClassName SettingActivity
 * @Description 设置界面
 * @Author Xiaoborui
 * @Date 2016/8/23 13:51
 */
//todo 重构整个界面
class SettingActivity : ToolbarActivity(), FolderChooserDialog.FolderCallback, FileChooserDialog.FileCallback, ColorChooserDialog.ColorCallback {

  @BindView(R.id.setting_color_primary_indicator)
  lateinit var mPrimaryColorSrc: ImageView
  @BindView(R.id.setting_color_accent_indicator)
  lateinit var mAccentColorSrc: ImageView
  @BindView(R.id.setting_clear_text)
  lateinit var mCache: TextView
  @BindView(R.id.setting_navaigation_switch)
  lateinit var mNaviSwitch: SwitchCompat
  @BindView(R.id.setting_shake_switch)
  lateinit var mShakeSwitch: SwitchCompat
  @BindView(R.id.setting_lrc_float_switch)
  lateinit var mFloatLrcSwitch: SwitchCompat
  @BindView(R.id.setting_lrc_float_tip)
  lateinit var mFloatLrcTip: TextView
  @BindView(R.id.setting_screen_switch)
  lateinit var mScreenSwitch: SwitchCompat
  @BindView(R.id.setting_notify_switch)
  lateinit var mNotifyStyleSwitch: SwitchCompat
  @BindView(R.id.setting_notify_color_container)
  lateinit var mNotifyColorContainer: View
  @BindView(R.id.setting_album_cover_text)
  lateinit var mAlbumCoverText: TextView
  @BindView(R.id.setting_lockscreen_text)
  lateinit var mLockScreenTip: TextView
  @BindView(R.id.setting_immersive_switch)
  lateinit var mImmersiveSwitch: SwitchCompat
  @BindView(R.id.setting_breakpoint_switch)
  lateinit var mBreakpointSwitch: SwitchCompat
  @BindView(R.id.setting_ignore_mediastore_switch)
  lateinit var mIgnoreMediastoreSwitch: SwitchCompat
  @BindView(R.id.setting_displayname_switch)
  lateinit var mShowDisplaynameSwitch: SwitchCompat
  @BindView(R.id.setting_general_theme_text)
  lateinit var mThemeText: TextView
  @BindView(R.id.setting_audio_focus_switch)
  lateinit var mAudioFocusSwitch: SwitchCompat

  @BindViews(R.id.setting_common_title, R.id.setting_color_title, R.id.setting_cover_title, R.id.setting_library_title, R.id.setting_lrc_title, R.id.setting_notify_title, R.id.setting_other_title, R.id.setting_player_title, R.id.setting_play_title)
  lateinit var mTitles: Array<TextView>

  @BindViews(R.id.setting_eq_arrow, R.id.setting_feedback_arrow, R.id.setting_about_arrow, R.id.setting_update_arrow)
  lateinit var mArrows: Array<ImageView>

  //是否需要重建activity
  private var mNeedRecreate = false
  //是否需要刷新adapter
  private var mNeedRefreshAdapter = false
  //是否需要刷新library
  private var mNeedRefreshLibrary: Boolean = false
  //    //是否从主题颜色选择对话框返回
  //    private boolean mFromColorChoose = false;
  //缓存大小
  private var mCacheSize: Long = 0
  private val mHandler: MsgHandler by lazy {
    MsgHandler(this)
  }
  private val mScanSize = intArrayOf(0, 500 * ByteConstants.KB, ByteConstants.MB, 2 * ByteConstants.MB, 5 * ByteConstants.MB)
  private var mOriginalAlbumChoice: String? = null

  private val mDisposables = ArrayList<Disposable>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(R.layout.activity_setting)
    ButterKnife.bind(this)
    setUpToolbar(getString(R.string.setting))

    //读取重启aitivity之前的数据
    if (savedInstanceState != null) {
      mNeedRecreate = savedInstanceState.getBoolean(EXTRA_RECREATE)
      mNeedRefreshAdapter = savedInstanceState.getBoolean(EXTRA_REFRESH_ADAPTER)
      //            mFromColorChoose = savedInstanceState.getBoolean("fromColorChoose");
    }

    val keyWord = arrayOf(SETTING_KEY.COLOR_NAVIGATION, SETTING_KEY.SHAKE, SETTING_KEY.DESKTOP_LYRIC_SHOW,
        SETTING_KEY.SCREEN_ALWAYS_ON, SETTING_KEY.NOTIFY_STYLE_CLASSIC, SETTING_KEY.IMMERSIVE_MODE,
        SETTING_KEY.PLAY_AT_BREAKPOINT, SETTING_KEY.IGNORE_MEDIA_STORE, SETTING_KEY.SHOW_DISPLAYNAME,
        SETTING_KEY.AUDIO_FOCUS)
    ButterKnife.apply(arrayOf(mNaviSwitch, mShakeSwitch, mFloatLrcSwitch, mScreenSwitch, mNotifyStyleSwitch, mImmersiveSwitch, mBreakpointSwitch, mIgnoreMediastoreSwitch, mShowDisplaynameSwitch, mAudioFocusSwitch)) { view, index ->
      TintHelper.setTintAuto(view, getAccentColor(), false)

      view.isChecked = SPUtil.getValue(mContext, SETTING_KEY.NAME, keyWord[index], false)
      //5.0以上才支持变色导航栏
      if (view.id == R.id.setting_navaigation_switch) {
        view.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
      }
      view.setOnCheckedChangeListener(object : OnCheckedChangeListener {
        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
          SPUtil.putValue(mContext, SETTING_KEY.NAME, keyWord[index], isChecked)
          when (buttonView.id) {
            //变色导航栏
            R.id.setting_navaigation_switch -> {
              mNeedRecreate = true
              ThemeStore.sColoredNavigation = isChecked
              mHandler.sendEmptyMessage(RECREATE)
            }
            //摇一摇
            R.id.setting_shake_switch -> if (isChecked) {
              ShakeDetector.getInstance().beginListen()
            } else {
              ShakeDetector.getInstance().stopListen()
            }
            //桌面歌词
            R.id.setting_lrc_float_switch -> {
              if (isChecked && !FloatWindowManager.getInstance().checkPermission(mContext)) {
                mFloatLrcSwitch.setOnCheckedChangeListener(null)
                mFloatLrcSwitch.isChecked = false
                mFloatLrcSwitch.setOnCheckedChangeListener(this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                  val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                  intent.data = Uri.parse("package:$packageName")
                  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                  Util.startActivitySafely(mContext, intent)
                }
                ToastUtil.show(mContext, R.string.plz_give_float_permission)
                return
              }
              mFloatLrcTip.setText(if (isChecked) R.string.opened_desktop_lrc else R.string.closed_desktop_lrc)
              val intent = MusicUtil.makeCmdIntent(Command.TOGGLE_DESKTOP_LYRIC)
              intent.putExtra(EXTRA_DESKTOP_LYRIC, mFloatLrcSwitch.isChecked)
              sendLocalBroadcast(intent)
            }
            //沉浸式状态栏
            R.id.setting_immersive_switch -> {
              ThemeStore.sImmersiveMode = isChecked
              mNeedRecreate = true
              mHandler.sendEmptyMessage(RECREATE)
            }
            //忽略内嵌
            R.id.setting_ignore_mediastore_switch -> {
              ImageUriRequest.IGNORE_MEDIA_STORE = isChecked
              mNeedRefreshAdapter = true
            }
            //文件名
            R.id.setting_displayname_switch -> {
              Song.SHOW_DISPLAYNAME = isChecked
              mNeedRefreshAdapter = true
            }
          }

        }
      })
    }

    //桌面歌词
    mFloatLrcTip.setText(
        if (mFloatLrcSwitch.isChecked) R.string.opened_desktop_lrc else R.string.closed_desktop_lrc)

    //主题颜色指示器
    (mPrimaryColorSrc.drawable as GradientDrawable)
        .setColor(getMaterialPrimaryColor())
    (mAccentColorSrc.drawable as GradientDrawable).setColor(getAccentColor())

    //初始化箭头颜色
    val accentColor = getAccentColor()
    ButterKnife.apply(mArrows) { view, index -> Theme.tintDrawable(view, view.background, accentColor) }

    //标题颜色
    ButterKnife.apply(mTitles) { view, index -> view.setTextColor(accentColor) }

    //封面
    mOriginalAlbumChoice = SPUtil
        .getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.AUTO_DOWNLOAD_ALBUM_COVER,
            mContext.getString(R.string.always))
    mAlbumCoverText.text = mOriginalAlbumChoice

    //根据系统版本决定是否显示通知栏样式切换
    findViewById<View>(R.id.setting_classic_notify_container).visibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) View.VISIBLE else View.GONE

    //当前主题
    mThemeText.text = getThemeText()

    //锁屏样式
    val lockScreen = SPUtil.getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.LOCKSCREEN, Constants.APLAYER_LOCKSCREEN)
    mLockScreenTip.setText(if (lockScreen == 0)
      R.string.aplayer_lockscreen_tip
    else if (lockScreen == 1) R.string.system_lockscreen_tip else R.string.lockscreen_off_tip)

    //计算缓存大小
    object : Thread() {
      override fun run() {
        mCacheSize = 0
        mCacheSize += Util.getFolderSize(externalCacheDir)
        mCacheSize += Util.getFolderSize(cacheDir)
        mHandler.sendEmptyMessage(CACHE_SIZE)
      }
    }.start()

    if (IS_GOOGLEPLAY) {
      findViewById<View>(R.id.setting_update_container).visibility = View.GONE
    }
  }


  override fun onBackPressed() {
    val intent = intent
    intent.putExtra(EXTRA_RECREATE, mNeedRecreate)
    intent.putExtra(EXTRA_REFRESH_ADAPTER, mNeedRefreshAdapter)
    intent.putExtra(EXTRA_REFRESH_LIBRARY, mNeedRefreshLibrary)
    setResult(Activity.RESULT_OK, intent)
    finish()
  }

  override fun onClickNavigation() {
    onBackPressed()
  }

  override fun onFolderSelection(dialog: FolderChooserDialog, folder: File) {
    var tag = dialog.tag ?: return
    var playListName = ""
    try {
      if (tag.contains("ExportPlayList")) {
        val tagAndName = tag.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        tag = tagAndName[0]
        playListName = tagAndName[1]
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }

    when (tag) {
      //            case "Lrc":
      //                boolean success = SPUtil.putValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCAL_LYRIC_SEARCH_DIR, folder.getAbsolutePath());
      //                ToastUtil.show(this, success ? R.string.setting_success : R.string.setting_error, Toast.LENGTH_SHORT);
      //                mLrcPath.setText(getString(R.string.lrc_tip, SPUtil.getValue(this, SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.LOCAL_LYRIC_SEARCH_DIR, "")));
      //                break;
      "Scan" -> {
        if (folder.exists() && folder.isDirectory && folder.list() != null) {
          SPUtil.putValue(this, SETTING_KEY.NAME, SETTING_KEY.MANUAL_SCAN_FOLDER, folder.absolutePath)
        }

        MediaScanner(mContext).scanFiles(folder)
        mNeedRefreshAdapter = true
      }
      "ExportPlayList" -> {
        if (TextUtils.isEmpty(playListName)) {
          ToastUtil.show(mContext, R.string.export_fail)
          return
        }
        if (folder.exists() && folder.isDirectory && folder.list() != null) {
          SPUtil.putValue(this, SETTING_KEY.NAME, SETTING_KEY.EXPORT_PLAYLIST_FOLDER, folder.absolutePath)
        }
        mDisposables
            .add(exportPlayListToFile(this, playListName, File(folder, "$playListName.m3u")))
      }
    }
  }

  override fun onFileSelection(dialog: FileChooserDialog, file: File) {
    when (dialog.tag) {
      "Import" -> {
        val newPlaylistName = file.name.substring(0, file.name.lastIndexOf("."))

        // 记录下导入的父目录
        val parent = file.parentFile
        if (parent.exists() && parent.isDirectory && parent.list() != null) {
          SPUtil.putValue(this, SETTING_KEY.NAME, SETTING_KEY.IMPORT_PLAYLIST_FOLDER,
              parent.absolutePath)
        }

        DatabaseRepository.getInstance()
            .getAllPlaylist()
            .map<List<String>> { playLists ->
              val allPlayListsName = ArrayList<String>()
              //判断是否存在
              var alreadyExist = false
              for ((_, name) in playLists) {
                allPlayListsName.add(name)
                if (name.equals(newPlaylistName, ignoreCase = true)) {
                  alreadyExist = true
                }
              }
              //不存在则提示新建
              if (!alreadyExist) {
                allPlayListsName
                    .add(0, newPlaylistName + "(" + getString(R.string.new_create) + ")")
              }

              allPlayListsName
            }
            .compose(applySingleScheduler())
            .subscribe { allPlayListsName ->
              getBaseDialog(mContext)
                  .title(R.string.import_playlist_to)
                  .items(allPlayListsName)
                  .itemsCallback { dialog1, itemView, position, text ->
                    val chooseNew = position == 0 && text.toString().endsWith(
                        "(" + getString(R.string.new_create) + ")")
                    mDisposables.add(
                        importM3UFile(this@SettingActivity, file,
                            if (chooseNew) newPlaylistName else text.toString(),
                            chooseNew))
                  }
                  .show()
            }
      }
    }

  }

  override fun onFileChooserDismissed(dialog: FileChooserDialog) {

  }


  override fun onColorSelection(dialog: ColorChooserDialog, selectedColor: Int) {
    when (dialog.title) {
      R.string.primary_color -> saveMaterialPrimaryColor(selectedColor)
      R.string.accent_color -> saveAccentColor(selectedColor)
    }
    mNeedRecreate = true
    recreate()
  }

  @SuppressLint("CheckResult")
  @OnClick(R.id.setting_filter_container, R.id.setting_primary_color_container, R.id.setting_notify_color_container, R.id.setting_feedback_container, R.id.setting_about_container, R.id.setting_update_container, R.id.setting_lockscreen_container, R.id.setting_lrc_priority_container, R.id.setting_lrc_float_container, R.id.setting_navigation_container, R.id.setting_shake_container, R.id.setting_eq_container, R.id.setting_clear_container, R.id.setting_breakpoint_container, R.id.setting_screen_container, R.id.setting_scan_container, R.id.setting_classic_notify_container, R.id.setting_album_cover_container, R.id.setting_library_category_container, R.id.setting_immersive_container, R.id.setting_import_playlist_container, R.id.setting_export_playlist_container, R.id.setting_ignore_mediastore_container, R.id.setting_cover_source_container, R.id.setting_player_bottom_container, R.id.setting_restore_delete_container, R.id.setting_displayname_container, R.id.setting_general_theme_container, R.id.setting_accent_color_container, R.id.setting_language_container, R.id.setting_auto_play_headset_container, R.id.setting_audio_focus_container)
  fun onClick(v: View) {
    when (v.id) {
      //文件过滤
      R.id.setting_filter_container -> configFilterSize()
      //曲库
      R.id.setting_library_category_container -> configLibraryCategory()
      //桌面歌词
      R.id.setting_lrc_float_container -> mFloatLrcSwitch.isChecked = !mFloatLrcSwitch.isChecked
      //歌词搜索优先级
      R.id.setting_lrc_priority_container -> configLyricPriority()
      //屏幕常亮
      R.id.setting_screen_container -> mScreenSwitch.isChecked = !mScreenSwitch.isChecked
      //手动扫描
      R.id.setting_scan_container -> {
        val initialFile = File(
            SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.MANUAL_SCAN_FOLDER, ""))
        val builder = Builder(this)
            .chooseButton(R.string.choose_folder)
            .tag("Scan")
            .allowNewFolder(false, R.string.new_folder)
        if (initialFile.exists() && initialFile.isDirectory && initialFile.list() != null) {
          builder.initialPath(initialFile.absolutePath)
        }
        builder.show()
      }
      //锁屏显示
      R.id.setting_lockscreen_container -> configLockScreen()
      //导航栏变色
      R.id.setting_navigation_container -> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
          ToastUtil.show(this, getString(R.string.only_lollopop))
          return
        }
        mNaviSwitch.isChecked = !mNaviSwitch.isChecked
      }
      //摇一摇
      R.id.setting_shake_container -> mShakeSwitch.isChecked = !mShakeSwitch.isChecked
      //选择主色调
      R.id.setting_primary_color_container -> ColorChooserDialog.Builder(this@SettingActivity, R.string.primary_color)
          .accentMode(false)
          .preselect(getMaterialPrimaryColor())
          .allowUserColorInput(true)
          .allowUserColorInputAlpha(false)
          .show()
      //选择强调色
      R.id.setting_accent_color_container -> ColorChooserDialog.Builder(this@SettingActivity, R.string.accent_color)
          .accentMode(true)
          .preselect(getAccentColor())
          .allowUserColorInput(true)
          .allowUserColorInputAlpha(false)
          .show()
      //通知栏底色
      R.id.setting_notify_color_container -> configNotifyBackgroundColor()
      //音效设置
      R.id.setting_eq_container -> EQHelper.startEqualizer(this)
      //意见与反馈
      R.id.setting_feedback_container -> gotoEmail()
      //关于我们
      R.id.setting_about_container -> startActivity(Intent(this, AboutActivity::class.java))
      //检查更新
      R.id.setting_update_container -> {
        UpdateAgent.forceCheck = true
        UpdateAgent.listener = UpdateListener(mContext)
        UpdateAgent.check(this)
      }
      //清除缓存
      R.id.setting_clear_container -> clearCache()
      //通知栏样式
      R.id.setting_classic_notify_container -> mNotifyStyleSwitch.isChecked = !mNotifyStyleSwitch.isChecked
      //专辑与艺术家封面自动下载
      R.id.setting_album_cover_container -> configCoverDownload()
      //封面下载源
      R.id.setting_cover_source_container -> configCoverDownloadSource()
      //沉浸式状态栏
      R.id.setting_immersive_container -> mImmersiveSwitch.isChecked = !mImmersiveSwitch.isChecked
      //歌单导入
      R.id.setting_import_playlist_container -> importPlayList()
      //歌单导出
      R.id.setting_export_playlist_container -> exportPlayList()
      //断点播放
      R.id.setting_breakpoint_container -> mBreakpointSwitch.isChecked = !mBreakpointSwitch.isChecked
      //忽略内嵌封面
      R.id.setting_ignore_mediastore_container -> mIgnoreMediastoreSwitch.isChecked = !mIgnoreMediastoreSwitch.isChecked
      //播放界面底部
      R.id.setting_player_bottom_container -> changeBottomOfPlayingScreen()
      //恢复移除的歌曲
      R.id.setting_restore_delete_container -> restoreDeleteSong()
      //文件名
      R.id.setting_displayname_container -> mShowDisplaynameSwitch.isChecked = !mShowDisplaynameSwitch.isChecked
      //全局主题
      R.id.setting_general_theme_container -> configGeneralTheme()
      //语言
      R.id.setting_language_container -> changeLanguage()
      //音频焦点
      R.id.setting_audio_focus_container -> mAudioFocusSwitch.isChecked = !mAudioFocusSwitch.isChecked
      //自动播放
      R.id.setting_auto_play_headset_container -> configAutoPlay()
    }
  }

  private fun configAutoPlay() {
    val headset = getString(R.string.auto_play_headset_plug)
    val open = getString(R.string.auto_play_open_software)
    val never = getString(R.string.auto_play_none)

    val choice = SPUtil
        .getValue(this, SETTING_KEY.NAME, SETTING_KEY.AUTO_PLAY, HeadsetPlugReceiver.NEVER)
    getBaseDialog(this)
        .items(*arrayOf(headset, open, never))
        .itemsCallbackSingleChoice(choice) { dialog, itemView, which, text ->
          SPUtil.putValue(this@SettingActivity, SETTING_KEY.NAME, SETTING_KEY.AUTO_PLAY, which)
          true
        }
        .show()
  }

  private fun changeLanguage() {
    val zh = getString(R.string.zh)
    val english = getString(R.string.english)
    val auto = getString(R.string.auto)

    getBaseDialog(this)
        .items(auto, zh, english)
        .itemsCallbackSingleChoice(
            SPUtil.getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.LANGUAGE, AUTO)
        ) { dialog, itemView, which, text ->
          LanguageHelper.saveSelectLanguage(mContext, which)

          val intent = Intent(mContext, MainActivity::class.java)
          intent.action = Intent.ACTION_MAIN
          intent.addCategory(Intent.CATEGORY_LAUNCHER)
          intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
          startActivity(intent)
          true
        }
        .show()
  }

  private fun gotoEmail() {
    getBaseDialog(this)
        .title(getString(R.string.send_log))
        .positiveText(R.string.confirm)
        .negativeText(R.string.cancel)
        .onAny { dialog, which ->
          val pm = packageManager
          val pi = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
          val feedBack = Feedback(
              pi.versionName,
              pi.versionCode.toString(),
              Build.DISPLAY,
              Build.CPU_ABI + "," + Build.CPU_ABI2,
              Build.MANUFACTURER,
              Build.MODEL,
              Build.VERSION.RELEASE,
              Build.VERSION.SDK_INT.toString()
          )
          val emailIntent = Intent()
          emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback))
          emailIntent.putExtra(Intent.EXTRA_TEXT, "\n\n\n" + feedBack)

          tryLaunch(catch = {
            Timber.w(it)
            ToastUtil.show(this, R.string.send_error, it.toString())
          }, block = {
            if (which == DialogAction.POSITIVE) {
              withContext(Dispatchers.IO) {
                try {
                  val zipFile = File("${Environment.getExternalStorageDirectory().absolutePath}/Android/data/$packageName/logs.zip")
                  zipFile.delete()
                  zipFile.createNewFile()
                  zipFile.zipOutputStream()
                      .zipFrom("${Environment.getExternalStorageDirectory().absolutePath}/Android/data/$packageName/logs",
                          "${applicationInfo.dataDir}/shared_prefs")
                  if (zipFile.length() > 0) {
                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                      emailIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                      FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".fileprovider", zipFile)
                    } else {
                      Uri.parse("file://${zipFile.absoluteFile}")
                    }
                    emailIntent.action = Intent.ACTION_SEND
                    emailIntent.type = "application/octet-stream"
                    emailIntent.putExtra(Intent.EXTRA_STREAM, uri)
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf(if (!IS_GOOGLEPLAY) "568920427@qq.com" else "rRemix.me@gmail.com"))
                  }
                } catch (e: Exception) {
                  Timber.w(e)
                }
              }
            } else {
              emailIntent.action = Intent.ACTION_SENDTO
              emailIntent.data = Uri.parse(if (!IS_GOOGLEPLAY) "mailto:568920427@qq.com" else "mailto:rRemix.me@gmail.com")
            }

            if (Util.isIntentAvailable(this, emailIntent)) {
              startActivity(emailIntent)
            } else {
              ToastUtil.show(this, R.string.not_found_email)
            }
//            Intent.createChooser(data,"Email")
          })
        }
        .show()
  }

  /**
   * 配置全局主题
   */
  private fun configGeneralTheme() {
    getBaseDialog(this)
        .items(R.array.general_theme)
        .itemsCallback { dialog, itemView, position, text ->
          val valTheme = getThemeText()
          if (text != valTheme) {
            setGeneralTheme(position)
            mThemeText.text = text
            mNeedRecreate = true
            recreate()
          }
        }.show()
  }


  /**
   * 恢复移除的歌曲
   */
  private fun restoreDeleteSong() {
    SPUtil.deleteValue(this, SETTING_KEY.NAME, SETTING_KEY.BLACKLIST_SONG)
    contentResolver.notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null)
    ToastUtil.show(mContext, R.string.alread_restore_songs)
  }


  /**
   * 播放列表导出
   */
  private fun exportPlayList() {
    mDisposables.add(DatabaseRepository.getInstance()
        .getAllPlaylist()
        .map<List<String>> { playLists ->
          val allplayListNames = ArrayList<String>()
          for ((_, name) in playLists) {
            allplayListNames.add(name)
          }
          allplayListNames
        }
        .compose(applySingleScheduler())
        .subscribe { allPlayListNames ->
          getBaseDialog(mContext)
              .title(R.string.choose_playlist_to_export)
              .negativeText(R.string.cancel)
              .items(allPlayListNames)
              .itemsCallback { dialog, itemView, position, text ->
                val initialFile = File(
                    SPUtil.getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.EXPORT_PLAYLIST_FOLDER, ""))
                val builder = Builder(this@SettingActivity)
                    .chooseButton(R.string.choose_folder)
                    .tag("ExportPlayList-$text")
                    .allowNewFolder(true, R.string.new_folder)
                if (initialFile.exists() && initialFile.isDirectory && initialFile.list() != null) {
                  builder.initialPath(initialFile.absolutePath)
                }
                builder.show()
              }
              .show()
        })
  }

  /**
   * 播放列表导入
   */
  @SuppressLint("CheckResult")
  private fun importPlayList() {
    getBaseDialog(mContext)
        .title(R.string.choose_import_way)
        .negativeText(R.string.cancel)
        .items(getString(R.string.import_from_external_storage), getString(R.string.import_from_others))
        .itemsCallback { dialog, itemView, select, text ->
          if (select == 0) {
            val initialFile = File(
                SPUtil.getValue(this, SETTING_KEY.NAME, SETTING_KEY.IMPORT_PLAYLIST_FOLDER, ""))
            val builder = FileChooserDialog.Builder(
                this@SettingActivity)
                .tag("Import")
                .extensionsFilter(".m3u")
            if (initialFile.exists() && initialFile.isDirectory && initialFile.list() != null) {
              builder.initialPath(initialFile.absolutePath)
            }
            builder.show()
          } else {
            Single
                .fromCallable { DatabaseRepository.getInstance().playlistFromMediaStore }
                .compose(applySingleScheduler())
                .subscribe({ localPlayLists ->
                  if (localPlayLists == null || localPlayLists.isEmpty()) {
                    ToastUtil.show(mContext, R.string.import_fail,
                        getString(R.string.no_playlist_can_import))
                    return@subscribe
                  }
                  val selectedIndices = ArrayList<Int>()
                  for (i in 0 until localPlayLists.size) {
                    selectedIndices.add(i)
                  }
                  getBaseDialog(mContext)
                      .title(R.string.choose_import_playlist)
                      .positiveText(R.string.choose)
                      .items(localPlayLists.keys)
                      .itemsCallbackMultiChoice(
                          selectedIndices.toTypedArray()
                      ) { dialog1, which, allSelects ->
                        mDisposables
                            .add(importLocalPlayList(mContext, localPlayLists, allSelects))
                        true
                      }.show()

                }, { throwable ->
                  ToastUtil
                      .show(mContext, R.string.import_fail, throwable.toString())
                })
          }
        }
        .theme(getMDDialogTheme())
        .show()
  }

  /**
   * 歌词搜索优先级
   */
  private fun configLyricPriority() {
    LyricPriorityDialog.newInstance().show(supportFragmentManager, "configLyricPriority")
  }

  /**
   * 配置封面下载源
   */
  private fun configCoverDownloadSource() {
    val oldChoice = SPUtil
        .getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.ALBUM_COVER_DOWNLOAD_SOURCE,
            DOWNLOAD_LASTFM)
    getBaseDialog(mContext)
        .title(R.string.cover_download_source)
        .items(getString(R.string.lastfm), getString(R.string.netease))
        .itemsCallbackSingleChoice(oldChoice
        ) { dialog, view, which, text ->
          if (oldChoice != which) {
            mNeedRefreshAdapter = true
            ImageUriRequest.DOWNLOAD_SOURCE = which
            SPUtil.putValue(mContext, SETTING_KEY.NAME,
                SETTING_KEY.ALBUM_COVER_DOWNLOAD_SOURCE, which)
          }
          true
        }
        .show()
  }

  /**
   * 配置封面是否下载
   */
  private fun configCoverDownload() {
    val choice = SPUtil
        .getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.AUTO_DOWNLOAD_ALBUM_COVER,
            mContext.getString(R.string.always))
    getBaseDialog(mContext)
        .title(R.string.auto_download_album_artist_cover)
        .items(getString(R.string.always), getString(R.string.wifi_only), getString(R.string.never))
        .itemsCallbackSingleChoice(
            when (choice) {
              mContext.getString(R.string.wifi_only) -> 1
              mContext.getString(R.string.always) -> 0
              else -> 2
            }
        ) { dialog, view, which, text ->
          mAlbumCoverText.text = text
          //仅从从不改变到仅在wifi下或者总是的情况下，才刷新Adapter
          mNeedRefreshAdapter = mNeedRefreshAdapter ||
              (mContext.getString(R.string.wifi_only) == text && mContext.getString(R.string.always) == text && mOriginalAlbumChoice != text)
          clearDownloadCover(text)
          ImageUriRequest.AUTO_DOWNLOAD_ALBUM = text.toString()
          SPUtil.putValue(mContext, SETTING_KEY.NAME,
              SETTING_KEY.AUTO_DOWNLOAD_ALBUM_COVER,
              text.toString())
          true
        }.show()
  }

  /**
   * 当用户选择从不下载时询问是否清除已有封面
   */
  private fun clearDownloadCover(text: CharSequence) {
    if (getString(R.string.never) == text) {
      getBaseDialog(mContext)
          .title(R.string.clear_download_cover)
          .positiveText(R.string.confirm)
          .negativeText(R.string.cancel)
          .onPositive { clearDialog, action ->
            SPUtil.deleteFile(mContext, SPUtil.COVER_KEY.NAME)
            Fresco.getImagePipeline().clearCaches()
            mNeedRefreshAdapter = true
          }.show()
    }
  }

  /**
   * 清除缓存
   */
  private fun clearCache() {
    getBaseDialog(mContext)
        .content(R.string.confirm_clear_cache)
        .positiveText(R.string.confirm)
        .negativeText(R.string.cancel)
        .onPositive { dialog, which ->
          object : Thread() {
            override fun run() {
              //清除歌词，封面等缓存
              //清除配置文件、数据库等缓存
              Util.deleteFilesByDirectory(cacheDir)
              Util.deleteFilesByDirectory(externalCacheDir)
              //                        SPUtil.deleteFile(mContext,SPUtil.SETTING_KEY.NAME);
              //                        deleteDatabase(DBOpenHelper.DBNAME);
              //清除fresco缓存
              Fresco.getImagePipeline().clearCaches()
              mHandler.sendEmptyMessage(CLEAR_FINISH)
              mNeedRefreshAdapter = true
              ImageUriRequest.clearUriCache()
            }
          }.start()
        }.show()
  }

  /**
   * 配置通知栏底色
   */
  private fun configNotifyBackgroundColor() {
    if (!SPUtil.getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.NOTIFY_STYLE_CLASSIC, false)) {
      ToastUtil.show(mContext, R.string.notify_bg_color_warnning)
      return
    }
    getBaseDialog(mContext)
        .title(R.string.notify_bg_color)
        .items(
            getString(R.string.use_system_color), getString(R.string.use_black_color))
        .itemsCallbackSingleChoice(if (SPUtil
                .getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.NOTIFY_SYSTEM_COLOR, true)) 0 else 1)
        { dialog, view, which, text ->
          SPUtil.putValue(mContext, SETTING_KEY.NAME,
              SETTING_KEY.NOTIFY_SYSTEM_COLOR, which == 0)
          true
        }
        .show()
  }

  /**
   * 配置锁屏界面
   */
  private fun configLockScreen() {
    //0:APlayer锁屏 1:系统锁屏 2:关闭
    getBaseDialog(mContext)
        .title(R.string.lockscreen_show)
        .items(getString(R.string.aplayer_lockscreen), getString(R.string.system_lockscreen), getString(R.string.close))
        .itemsCallbackSingleChoice(SPUtil
            .getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.LOCKSCREEN,
                Constants.APLAYER_LOCKSCREEN)
        ) { dialog, view, which, text ->
          SPUtil.putValue(mContext, SETTING_KEY.NAME, SETTING_KEY.LOCKSCREEN,
              which)
          mLockScreenTip.setText(
              when (which) {
                Constants.APLAYER_LOCKSCREEN -> R.string.aplayer_lockscreen_tip
                Constants.SYSTEM_LOCKSCREEN -> R.string.system_lockscreen_tip
                else -> R.string.lockscreen_off_tip
              })
          true
        }.show()
  }

  /**
   * 配置曲库目录
   */
  private fun configLibraryCategory() {
    val categoryJson = SPUtil
        .getValue(mContext, SETTING_KEY.NAME, SETTING_KEY.LIBRARY_CATEGORY, "")

    val oldCategories = Gson()
        .fromJson<List<Category>>(categoryJson, object : TypeToken<List<Category>>() {

        }.type)
    if (oldCategories == null || oldCategories.isEmpty()) {
      ToastUtil.show(mContext, getString(R.string.load_failed))
      return
    }
    val selected = ArrayList<Int>()
    for (temp in oldCategories) {
      selected.add(temp.order)
    }

    val allLibraryStrings = Category.getAllLibraryString(this)
    getBaseDialog(mContext)
        .title(R.string.library_category)
        .positiveText(R.string.confirm)
        .items(allLibraryStrings)
        .itemsCallbackMultiChoice(selected.toTypedArray()
        ) { dialog, which, text ->
          if (text.isEmpty()) {
            ToastUtil.show(mContext, getString(R.string.plz_choose_at_least_one_category))
            return@itemsCallbackMultiChoice true
          }
          val newCategories = ArrayList<Category>()
          for (choose in which) {
            newCategories.add(Category(choose))
          }
          if (newCategories != oldCategories) {
            mNeedRefreshLibrary = true
            intent.putExtra(EXTRA_CATEGORY, newCategories)
            SPUtil.putValue(mContext, SETTING_KEY.NAME,
                SETTING_KEY.LIBRARY_CATEGORY,
                Gson().toJson(newCategories, object : TypeToken<List<Category>>() {}.type))
          }
          true
        }.show()
  }

  /**
   * 配置过滤大小
   */
  private fun configFilterSize() {
    //读取以前设置
    var position = 0
    for (i in mScanSize.indices) {
      position = i
      if (mScanSize[i] == MediaStoreUtil.SCAN_SIZE) {
        break
      }
    }
    getBaseDialog(mContext)
        .title(R.string.set_filter_size)
        .items("0K", "500K", "1MB", "2MB", "5MB")
        .itemsCallbackSingleChoice(position) { dialog, itemView, which, text ->
          SPUtil.putValue(mContext, SETTING_KEY.NAME, SETTING_KEY.SCAN_SIZE,
              mScanSize[which])
          MediaStoreUtil.SCAN_SIZE = mScanSize[which]
          contentResolver.notifyChange(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null)
          true
        }.show()
  }

  private fun changeBottomOfPlayingScreen() {
    val position = SPUtil
        .getValue(mContext, SETTING_KEY.NAME, BOTTOM_OF_NOW_PLAYING_SCREEN,
            PlayerActivity.BOTTOM_SHOW_BOTH)
    getBaseDialog(mContext)
        .title(R.string.show_on_bottom)
        .items(getString(R.string.show_next_song_only), getString(R.string.show_vol_control_only), getString(R.string.tap_to_toggle), getString(R.string.close))
        .itemsCallbackSingleChoice(position) { dialog, itemView, which, text ->
          if (position != which) {
            SPUtil.putValue(mContext, SETTING_KEY.NAME, BOTTOM_OF_NOW_PLAYING_SCREEN, which)
          }
          true
        }.show()
  }

  @OnHandleMessage
  fun handleInternal(msg: Message) {
    if (msg.what == RECREATE) {
      recreate()
    }
    if (msg.what == CACHE_SIZE) {
      mCache.text = getString(R.string.cache_size, mCacheSize.toFloat() / 1024f / 1024f)
    }
    if (msg.what == CLEAR_FINISH) {
      ToastUtil.show(mContext, getString(R.string.clear_success))
      mCache.setText(R.string.zero_size)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(EXTRA_RECREATE, mNeedRecreate)
    //        outState.putBoolean("fromColorChoose", mFromColorChoose);
    outState.putBoolean(EXTRA_REFRESH_ADAPTER, mNeedRefreshAdapter)
  }

  override fun onDestroy() {
    super.onDestroy()
    mHandler.remove()
    for (disposable in mDisposables) {
      if (!disposable.isDisposed) {
        disposable.dispose()
      }
    }

  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == REQUEST_THEME_COLOR) {
      if (data != null) {
        mNeedRecreate = data.getBooleanExtra(EXTRA_RECREATE, false)
        if (mNeedRecreate) {
          mHandler.sendEmptyMessage(RECREATE)
        }
      }
    } else if (requestCode == REQUEST_EQ) {

    }
  }

  companion object {
    private const val RECREATE = 100
    private const val CACHE_SIZE = 101
    private const val CLEAR_FINISH = 102
    private const val REQUEST_THEME_COLOR = 0x10
  }

}
