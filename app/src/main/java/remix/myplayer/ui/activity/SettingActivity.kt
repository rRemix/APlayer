package remix.myplayer.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_ALLOW_MULTIPLE
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.afollestad.materialdialogs.DialogAction
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.soundcloud.android.crop.Crop
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.App.Companion.IS_GOOGLEPLAY
import remix.myplayer.BuildConfig
import remix.myplayer.R
import remix.myplayer.bean.misc.Library
import remix.myplayer.bean.misc.Library.Companion.getAllLibraryString
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.ActivitySettingBinding
import remix.myplayer.db.room.DatabaseRepository
import remix.myplayer.glide.UriFetcher
import remix.myplayer.glide.UriFetcher.DOWNLOAD_LASTFM
import remix.myplayer.helper.EQHelper
import remix.myplayer.helper.LanguageHelper
import remix.myplayer.helper.LanguageHelper.AUTO
import remix.myplayer.helper.M3UHelper.exportPlayListToFile
import remix.myplayer.helper.M3UHelper.importLocalPlayList
import remix.myplayer.helper.M3UHelper.importM3UFile
import remix.myplayer.helper.ShakeDetector
import remix.myplayer.misc.AppInfo
import remix.myplayer.misc.MediaScanner
import remix.myplayer.misc.SystemInfo
import remix.myplayer.misc.cache.DiskCache
import remix.myplayer.misc.floatpermission.FloatWindowManager
import remix.myplayer.misc.handler.MsgHandler
import remix.myplayer.misc.handler.OnHandleMessage
import remix.myplayer.misc.receiver.HeadsetPlugReceiver
import remix.myplayer.misc.tryLaunch
import remix.myplayer.misc.update.UpdateAgent
import remix.myplayer.misc.update.UpdateListener
import remix.myplayer.misc.zipFrom
import remix.myplayer.misc.zipOutputStream
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.service.MusicService.Companion.EXTRA_DESKTOP_LYRICS
import remix.myplayer.theme.Theme
import remix.myplayer.theme.Theme.getBaseDialog
import remix.myplayer.theme.ThemeStore
import remix.myplayer.theme.TintHelper
import remix.myplayer.ui.ViewCommon
import remix.myplayer.ui.activity.MainActivity.Companion.EXTRA_LIBRARY
import remix.myplayer.ui.activity.MainActivity.Companion.EXTRA_RECREATE
import remix.myplayer.ui.activity.MainActivity.Companion.EXTRA_REFRESH_ADAPTER
import remix.myplayer.ui.activity.MainActivity.Companion.EXTRA_REFRESH_LIBRARY
import remix.myplayer.ui.activity.PlayerActivity.Companion.BACKGROUND_ADAPTIVE_COLOR
import remix.myplayer.ui.activity.PlayerActivity.Companion.BACKGROUND_CUSTOM_IMAGE
import remix.myplayer.ui.activity.PlayerActivity.Companion.BACKGROUND_THEME
import remix.myplayer.ui.dialog.LyricsOrderDialog
import remix.myplayer.ui.dialog.color.ColorChooserDialog
import remix.myplayer.ui.misc.FolderChooser
import remix.myplayer.util.*
import remix.myplayer.util.Constants.KB
import remix.myplayer.util.Constants.MB
import remix.myplayer.util.RxUtil.applySingleScheduler
import remix.myplayer.util.SPUtil.SETTING_KEY
import remix.myplayer.util.SPUtil.SETTING_KEY.BOTTOM_OF_NOW_PLAYING_SCREEN
import remix.myplayer.util.Util.isSupportStatusBarLyric
import remix.myplayer.util.Util.sendLocalBroadcast
import timber.log.Timber
import java.io.File

/**
 * @ClassName SettingActivity
 * @Description 设置界面
 * @Author Xiaoborui
 * @Date 2016/8/23 13:51
 */
//todo 重构整个界面
class SettingActivity : ToolbarActivity(), ColorChooserDialog.ColorCallback,
    SharedPreferences.OnSharedPreferenceChangeListener {
  private lateinit var binding: ActivitySettingBinding

  private lateinit var checkedChangedListener: OnCheckedChangeListener

  //是否需要重建activity
  private var needRecreate = false

  //是否需要刷新adapter
  private var needRefreshAdapter = false

  //是否需要刷新library
  private var needRefreshLibrary: Boolean = false
  
  //缓存大小
  private var cacheSize: Long = 0
  private val handler: MsgHandler by lazy {
    MsgHandler(this)
  }
  
  private val scanSize = intArrayOf(0, 500 * KB, MB, 2 * MB, 5 * MB)
  private var originalAlbumChoice: String? = null

  private val disposables = ArrayList<Disposable>()

  private var pendingExportPlaylist: String? = null

  private var blackList: Set<String> = emptySet()
  
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivitySettingBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setUpToolbar(getString(R.string.setting))

    //读取重启aitivity之前的数据
    if (savedInstanceState != null) {
      needRecreate = savedInstanceState.getBoolean(EXTRA_RECREATE)
      needRefreshAdapter = savedInstanceState.getBoolean(EXTRA_REFRESH_ADAPTER)
      //            mFromColorChoose = savedInstanceState.getBoolean("fromColorChoose");
    }

    val keyWord = arrayOf(
        SETTING_KEY.COLOR_NAVIGATION,
        SETTING_KEY.SHAKE,
        SETTING_KEY.DESKTOP_LYRIC_SHOW,
        SETTING_KEY.STATUSBAR_LYRIC_SHOW,
        SETTING_KEY.SCREEN_ALWAYS_ON,
        SETTING_KEY.NOTIFY_STYLE_CLASSIC,
        SETTING_KEY.IMMERSIVE_MODE,
        SETTING_KEY.PLAY_AT_BREAKPOINT,
        SETTING_KEY.IGNORE_MEDIA_STORE,
        SETTING_KEY.SHOW_DISPLAYNAME,
        SETTING_KEY.FORCE_SORT,
        SETTING_KEY.BLACK_THEME,
        SETTING_KEY.AUDIO_FOCUS
    )
    arrayOf(
        binding.settingNavaigationSwitch,
        binding.settingShakeSwitch,
        binding.settingLrcFloatSwitch,
        binding.settingStatusbarLrcSwitch,
        binding.settingScreenSwitch,
        binding.settingNotifySwitch,
        binding.settingImmersiveSwitch,
        binding.settingBreakpointSwitch,
        binding.settingIgnoreMediastoreSwitch,
        binding.settingDisplaynameSwitch,
        binding.settingForceSortSwitch,
        binding.settingBlackThemeSwitch,
        binding.settingAudioFocusSwitch
    ).forEachIndexed { index, view ->
      TintHelper.setTintAuto(view, ThemeStore.accentColor, false)

      view.isChecked = SPUtil.getValue(
          this, SETTING_KEY.NAME, keyWord[index], false
      )
      //5.0以上才支持变色导航栏
      if (view.id == R.id.setting_navaigation_switch) {
        view.isEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
      }
      checkedChangedListener = object : OnCheckedChangeListener {
        override fun onCheckedChanged(
            buttonView: CompoundButton, isChecked: Boolean
        ) {
          SPUtil.putValue(this@SettingActivity, SETTING_KEY.NAME, keyWord[index], isChecked)
          when (buttonView.id) {
            //变色导航栏
            R.id.setting_navaigation_switch -> {
              needRecreate = true
              ThemeStore.sColoredNavigation = isChecked
              handler.sendEmptyMessage(RECREATE)
            }
            //摇一摇
            R.id.setting_shake_switch -> if (isChecked) {
              ShakeDetector.getInstance().beginListen()
            } else {
              ShakeDetector.getInstance().stopListen()
            }
            //桌面歌词
            R.id.setting_lrc_float_switch -> {
              if (isChecked && !FloatWindowManager.getInstance().checkPermission(this@SettingActivity)) {
                binding.settingLrcFloatSwitch.setOnCheckedChangeListener(null)
                binding.settingLrcFloatSwitch.isChecked = false
                binding.settingLrcFloatSwitch.setOnCheckedChangeListener(this)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                  val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                  intent.data = Uri.parse("package:$packageName")
                  intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                  Util.startActivitySafely(this@SettingActivity, intent)
                }
                ToastUtil.show(this@SettingActivity, R.string.plz_give_float_permission)
                return
              }
              binding.settingLrcFloatTip.setText(if (isChecked) R.string.opened_desktop_lrc else R.string.closed_desktop_lrc)
              val intent = MusicUtil.makeCmdIntent(Command.TOGGLE_DESKTOP_LYRIC)
              intent.putExtra(
                  EXTRA_DESKTOP_LYRICS, binding.settingLrcFloatSwitch.isChecked
              )
              sendLocalBroadcast(intent)
            }
            //状态栏歌词
            R.id.setting_statusbar_lrc_switch -> {
              val intent =
                  MusicUtil.makeCmdIntent(Command.TOGGLE_STATUS_BAR_LRC)
              sendLocalBroadcast(intent)
            }
            //沉浸式状态栏
            R.id.setting_immersive_switch -> {
              ThemeStore.sImmersiveMode = isChecked
              needRecreate = true
              handler.sendEmptyMessage(RECREATE)
            }
            //忽略内嵌
            R.id.setting_ignore_mediastore_switch -> {
              needRefreshAdapter = true
            }
            //文件名
            R.id.setting_displayname_switch -> {
              Song.SHOW_DISPLAYNAME = isChecked
              needRefreshAdapter = true
            }
            R.id.setting_force_sort_switch -> {
              sendLocalBroadcast(Intent(MusicService.MEDIA_STORE_CHANGE))
            }
            //黑色主题
            R.id.setting_black_theme_switch -> {
              if (!ThemeStore.isLightTheme) {
                needRecreate = true
                recreate()
              }
            }
            //经典通知栏
            R.id.setting_notify_switch -> {
              if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                RxPermissions(this@SettingActivity)
                  .request(Manifest.permission.POST_NOTIFICATIONS)
                  .subscribe { has ->
                    if (!has){
                      ToastUtil.show(this@SettingActivity, R.string.need_permission)
                      SPUtil.putValue(this@SettingActivity, SETTING_KEY.NAME, keyWord[index], false)
                      buttonView.setOnCheckedChangeListener(null)
                      buttonView.isChecked = false
                      buttonView.setOnCheckedChangeListener(checkedChangedListener)
                    }
                  }
              }
            }
          }

        }
      }
      view.setOnCheckedChangeListener(checkedChangedListener)
    }

    //桌面歌词
    binding.settingLrcFloatTip.setText(
        if (binding.settingLrcFloatSwitch.isChecked) R.string.opened_desktop_lrc else R.string.closed_desktop_lrc
    )

    if (!isSupportStatusBarLyric(this)) {
      binding.settingStatusbarLrcContainer.visibility = View.GONE
    }

    //主题颜色指示器
    (binding.settingColorPrimaryIndicator.drawable as GradientDrawable).setColor(
        ThemeStore.materialPrimaryColor
    )
    (binding.settingColorAccentIndicator.drawable as GradientDrawable).setColor(
        ThemeStore.accentColor
    )

    // 初始化箭头颜色和标题颜色
    val accentColor = ThemeStore.accentColor
    arrayOf(
        binding.settingEqArrow,
        binding.settingFeedbackArrow,
        binding.settingAboutArrow,
        binding.settingUpdateArrow
    ).forEach {
      Theme.tintDrawable(it, it.drawable, accentColor)
    }
    arrayOf(
        binding.settingCommonTitle,
        binding.settingColorTitle,
        binding.settingCoverTitle,
        binding.settingLibraryTitle,
        binding.settingLrcTitle,
        binding.settingNotifyTitle,
        binding.settingOtherTitle,
        binding.settingPlayerTitle,
        binding.settingPlayTitle
    ).forEach {
      it.setTextColor(accentColor)
    }

    //封面
    originalAlbumChoice = SPUtil.getValue(
        this,
        SETTING_KEY.NAME,
        SETTING_KEY.AUTO_DOWNLOAD_ALBUM_COVER,
        getString(R.string.always)
    )
    binding.settingCoverSourceText.text = originalAlbumChoice

    // 封面下载源
    val coverSource = SPUtil.getValue(
        this, SETTING_KEY.NAME, SETTING_KEY.ALBUM_COVER_DOWNLOAD_SOURCE, 0
    )
    binding.settingCoverSourceText.setText(
        if (coverSource == 0) R.string.cover_download_from_lastfm else R.string.cover_download_from_netease
    )

    //根据系统版本决定是否显示通知栏样式切换
    binding.settingClassicNotifyContainer.visibility = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) View.VISIBLE else View.GONE

    // 深色主题
    val darkTheme = SPUtil.getValue(
        this,
        SETTING_KEY.NAME,
        SETTING_KEY.DARK_THEME,
        ThemeStore.FOLLOW_SYSTEM
    )
    binding.settingDarkThemeText.text = getString(
        when (darkTheme) {
          ThemeStore.ALWAYS_OFF -> R.string.always_off
          ThemeStore.ALWAYS_ON -> R.string.always_on
          ThemeStore.FOLLOW_SYSTEM -> R.string.follow_system
          else -> R.string.follow_system
        }
    )

    //锁屏样式
    val lockScreen = SPUtil.getValue(
        this,
        SETTING_KEY.NAME,
        SETTING_KEY.LOCKSCREEN,
        Constants.APLAYER_LOCKSCREEN
    )
    binding.settingLockscreenText.setText(
        when (lockScreen) {
          0 -> R.string.aplayer_lockscreen_tip
          1 -> R.string.system_lockscreen_tip
          else -> R.string.lockscreen_off_tip
        }
    )

    // 是否自动下载封面
    binding.settingAlbumCoverText.text = SPUtil.getValue(this,
        SETTING_KEY.NAME,
        SETTING_KEY.AUTO_DOWNLOAD_ALBUM_COVER,
        getString(R.string.always)
    )

    updatePlayerBackgroundText()

    //计算缓存大小
    object : Thread() {
      override fun run() {
        cacheSize = 0
//        cacheSize += Util.getFolderSize(externalCacheDir)
        cacheSize += Util.getFolderSize(cacheDir)
        handler.sendEmptyMessage(CACHE_SIZE)
      }
    }.start()

    if (!BuildConfig.ENABLE_UPDATER) {
      binding.settingUpdateContainer.visibility = View.GONE
    }

    getSharedPreferences(
        SETTING_KEY.NAME, Context.MODE_PRIVATE
    ).registerOnSharedPreferenceChangeListener(
        this
    )

    // 点击事件处理
    arrayOf(
        binding.settingBlacklistContainer,
        binding.settingPrimaryColorContainer,
        binding.settingNotifyColorContainer,
        binding.settingFeedbackContainer,
        binding.settingAboutContainer,
        binding.settingUpdateContainer,
        binding.settingLockscreenContainer,
        binding.settingLrcPriorityContainer,
        binding.settingLrcFloatContainer,
        binding.settingNavigationContainer,
        binding.settingShakeContainer,
        binding.settingEqContainer,
        binding.settingClearContainer,
        binding.settingBreakpointContainer,
        binding.settingScreenContainer,
        binding.settingScanContainer,
        binding.settingClassicNotifyContainer,
        binding.settingAlbumCoverContainer,
        binding.settingLibraryCategoryContainer,
        binding.settingImmersiveContainer,
        binding.settingImportPlaylistContainer,
        binding.settingExportPlaylistContainer,
        binding.settingIgnoreMediastoreContainer,
        binding.settingCoverSourceContainer,
        binding.settingPlayerBottomContainer,
        binding.settingDisplaynameContainer,
        binding.settingForceSortContainer,
        binding.settingDarkThemeContainer,
        binding.settingBlackThemeContainer,
        binding.settingAccentColorContainer,
        binding.settingLanguageContainer,
        binding.settingAutoPlayHeadsetContainer,
        binding.settingAudioFocusContainer,
        binding.settingRestoreDeleteContainer,
        binding.settingFilterContainer,
        binding.settingPlayerBackground
    ).forEach {
      it.setOnClickListener(object : View.OnClickListener {
        override fun onClick(v: View?) {
          when (v?.id) {
            //大小过滤
            R.id.setting_filter_container -> configFilterSize()
            //黑名单
            R.id.setting_blacklist_container -> configBlackList()
            //曲库
            R.id.setting_library_category_container -> configLibrary()
            //桌面歌词
            R.id.setting_lrc_float_container -> binding.settingLrcFloatSwitch.isChecked =
                !binding.settingLrcFloatSwitch.isChecked
            //歌词搜索优先级
            R.id.setting_lrc_priority_container -> configLyricPriority()
            //屏幕常亮
            R.id.setting_screen_container -> binding.settingScreenSwitch.isChecked =
                !binding.settingScreenSwitch.isChecked
            //手动扫描
            R.id.setting_scan_container ->
              FolderChooser(
                  this@SettingActivity,
                  TAG_SCAN,
                  null,
                  SETTING_KEY.NAME,
                  SETTING_KEY.MANUAL_SCAN_FOLDER,
                  object : FolderChooser.FolderCallback {
                    override fun onFolderSelection(chooser: FolderChooser, folder: File) {
                      MediaScanner(this@SettingActivity).scanFiles(folder)
                      needRefreshAdapter = true
                    }
                  }).show()
            //锁屏显示
            R.id.setting_lockscreen_container -> configLockScreen()
            //导航栏变色
            R.id.setting_navigation_container -> {
              if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                ToastUtil.show(
                    this@SettingActivity, getString(R.string.only_lollopop)
                )
                return
              }
              binding.settingNavaigationSwitch.isChecked =
                  !binding.settingNavaigationSwitch.isChecked
            }
            //摇一摇
            R.id.setting_shake_container -> binding.settingShakeSwitch.isChecked =
                !binding.settingShakeSwitch.isChecked
            //选择主色调
            R.id.setting_primary_color_container -> ColorChooserDialog.Builder(
                this@SettingActivity, R.string.primary_color
            ).accentMode(false).preselect(ThemeStore.materialPrimaryColor)
                .allowUserColorInput(true).allowUserColorInputAlpha(false).show()
            //选择强调色
            R.id.setting_accent_color_container -> ColorChooserDialog.Builder(
                this@SettingActivity, R.string.accent_color
            ).accentMode(true).preselect(ThemeStore.accentColor)
                .allowUserColorInput(true).allowUserColorInputAlpha(false).show()
            //通知栏底色
            R.id.setting_notify_color_container -> configNotifyBackgroundColor()
            //音效设置
            R.id.setting_eq_container -> EQHelper.startEqualizer(this@SettingActivity)
            //意见与反馈
            R.id.setting_feedback_container -> gotoEmail()
            //关于我们
            R.id.setting_about_container -> startActivity(
                Intent(
                    this@SettingActivity, AboutActivity::class.java
                )
            )
            //检查更新
            R.id.setting_update_container -> {
              UpdateAgent.forceCheck = true
              UpdateAgent.listener = UpdateListener(this@SettingActivity)
              UpdateAgent.check(this@SettingActivity)
            }
            //清除缓存
            R.id.setting_clear_container -> clearCache()
            //通知栏样式
            R.id.setting_classic_notify_container -> binding.settingNotifySwitch.isChecked =
                !binding.settingNotifySwitch.isChecked
            //专辑与艺术家封面自动下载
            R.id.setting_album_cover_container -> configCoverDownload()
            //封面下载源
            R.id.setting_cover_source_container -> configCoverDownloadSource()
            //沉浸式状态栏
            R.id.setting_immersive_container -> binding.settingImmersiveSwitch.isChecked =
                !binding.settingImmersiveSwitch.isChecked
            //歌单导入
            R.id.setting_import_playlist_container -> importPlayList()
            //歌单导出
            R.id.setting_export_playlist_container -> exportPlayList()
            //断点播放
            R.id.setting_breakpoint_container -> binding.settingBreakpointSwitch.isChecked =
                !binding.settingBreakpointSwitch.isChecked
            //忽略内嵌封面
            R.id.setting_ignore_mediastore_container -> binding.settingIgnoreMediastoreSwitch.isChecked =
                !binding.settingIgnoreMediastoreSwitch.isChecked
            //播放界面底部
            R.id.setting_player_bottom_container -> changeBottomOfPlayingScreen()
            //恢复移除的歌曲
            R.id.setting_restore_delete_container -> restoreDeleteSong()
            //文件名
            R.id.setting_displayname_container -> binding.settingDisplaynameSwitch.isChecked =
                !binding.settingDisplaynameSwitch.isChecked
            //强制排序
            R.id.setting_force_sort_container -> binding.settingForceSortSwitch.isChecked =
                !binding.settingForceSortSwitch.isChecked
            //深色主题
            R.id.setting_dark_theme_container -> configDarkTheme()
            //黑色主题
            R.id.setting_black_theme_container -> binding.settingBlackThemeSwitch.isChecked =
                !binding.settingBlackThemeSwitch.isChecked
            //语言
            R.id.setting_language_container -> changeLanguage()
            //音频焦点
            R.id.setting_audio_focus_container -> binding.settingAudioFocusSwitch.isChecked =
                !binding.settingAudioFocusSwitch.isChecked
            //自动播放
            R.id.setting_auto_play_headset_container -> configAutoPlay()
            //自定义播放界面背景
            R.id.setting_player_background -> configPlayerBackgroundConfig()
          }
        }
      })
    }
  }

  private fun updatePlayerBackgroundText() {
    //播放界面背景
    val nowPlayingScreen = SPUtil.getValue(
        this,
        SETTING_KEY.NAME,
        SETTING_KEY.PLAYER_BACKGROUND,
        BACKGROUND_ADAPTIVE_COLOR
    )
    binding.settingNowPlayingScreenText.setText(
        when (nowPlayingScreen) {
          BACKGROUND_THEME -> R.string.now_playing_screen_theme
          BACKGROUND_ADAPTIVE_COLOR -> R.string.now_playing_screen_cover
          BACKGROUND_CUSTOM_IMAGE -> R.string.now_playing_screen_custom
          else -> R.string.now_playing_screen_theme
        }
    )
  }


  override fun onBackPressed() {
    val intent = intent
    intent.putExtra(EXTRA_RECREATE, needRecreate)
    intent.putExtra(EXTRA_REFRESH_ADAPTER, needRefreshAdapter)
    intent.putExtra(EXTRA_REFRESH_LIBRARY, needRefreshLibrary)
    setResult(Activity.RESULT_OK, intent)
    finish()
  }

  override fun onClickNavigation() {
    onBackPressed()
  }


  override fun onColorSelection(dialog: ColorChooserDialog, selectedColor: Int) {
    when (dialog.title) {
      R.string.primary_color -> ThemeStore.materialPrimaryColor = selectedColor
      R.string.accent_color -> ThemeStore.accentColor = selectedColor
    }
    needRecreate = true
    recreate()
  }

  private fun configPlayerBackgroundConfig() {
    val current = SPUtil.getValue(
        this,
        SETTING_KEY.NAME,
        SETTING_KEY.PLAYER_BACKGROUND,
        BACKGROUND_ADAPTIVE_COLOR
    )

    getBaseDialog(this).items(R.array.player_background)
        .itemsCallback { dialog, itemView, position, text ->
          if (current == position && position != BACKGROUND_CUSTOM_IMAGE) {
            return@itemsCallback
          }

          SPUtil.putValue(
              this, SETTING_KEY.NAME, SETTING_KEY.PLAYER_BACKGROUND, position
          )
          updatePlayerBackgroundText()

          if (position == BACKGROUND_CUSTOM_IMAGE) {
            Crop.pickImage(this, Crop.REQUEST_PICK)
          }
        }
        .show()
  }

  /**
   * 配置过滤大小
   */
  private fun configFilterSize() {
    //读取以前设置
    var position = 0
    for (i in scanSize.indices) {
      position = i
      if (scanSize[i] == MediaStoreUtil.SCAN_SIZE) {
        break
      }
    }
    getBaseDialog(this)
        .title(R.string.set_filter_size)
        .items("0K", "500K", "1MB", "2MB", "5MB")
        .itemsCallbackSingleChoice(position) { dialog, itemView, which, text ->
          SPUtil.putValue(
              this, SETTING_KEY.NAME, SETTING_KEY.SCAN_SIZE, scanSize[which]
          )
          MediaStoreUtil.SCAN_SIZE = scanSize[which]
          contentResolver.notifyChange(
              MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null
          )
          true
        }.show()
  }

  private fun configAutoPlay() {
    val headset = getString(R.string.auto_play_headset_plug)
    val open = getString(R.string.auto_play_open_software)
    val never = getString(R.string.auto_play_none)

    val choice = SPUtil.getValue(
        this, SETTING_KEY.NAME, SETTING_KEY.AUTO_PLAY, HeadsetPlugReceiver.NEVER
    )
    getBaseDialog(this)
        .items(*arrayOf(headset, open, never))
        .itemsCallbackSingleChoice(choice) { dialog, itemView, which, text ->
          SPUtil.putValue(
              this@SettingActivity, SETTING_KEY.NAME, SETTING_KEY.AUTO_PLAY, which
          )
          true
        }
        .show()
  }

  private fun changeLanguage() {
    val zhSimple = getString(R.string.zh_simple)
    val zhTraditional = getString(R.string.zh_traditional)
    val english = getString(R.string.english)
    val japanese = getString(R.string.japanese)
    val auto = getString(R.string.auto)

    getBaseDialog(this)
        .items(auto, zhSimple, zhTraditional, english, japanese)
        .itemsCallbackSingleChoice(
            SPUtil.getValue(
                this, SETTING_KEY.NAME, SETTING_KEY.LANGUAGE, AUTO
            )
        ) { dialog, itemView, which, text ->
          LanguageHelper.saveSelectLanguage(this, which)

          val intent = Intent(this, MainActivity::class.java)
          intent.action = Intent.ACTION_MAIN
          intent.addCategory(Intent.CATEGORY_LAUNCHER)
          intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
          startActivity(intent)
          true
        }
        .show()
  }

  private fun gotoEmail() {
    fun send(sendLog: Boolean) {
      val emailIntent = Intent()
      emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback))
      emailIntent.putExtra(
          Intent.EXTRA_TEXT,
          "\n\n\nApp info:\n${AppInfo.prettyPrinted}\n\nSystem info:\n${SystemInfo.prettyPrinted}"
      )

      tryLaunch(catch = {
        Timber.w(it)
        ToastUtil.show(this, R.string.send_error, it.toString())
      }, block = {
        if (sendLog) {
          withContext(Dispatchers.IO) {
            try {
              val zipFile =
                  File("${Environment.getExternalStorageDirectory().absolutePath}/Android/data/$packageName/logs.zip")
              zipFile.delete()
              zipFile.createNewFile()
              zipFile.zipOutputStream().zipFrom(
                  "${Environment.getExternalStorageDirectory().absolutePath}/Android/data/$packageName/logs",
                  "${applicationInfo.dataDir}/shared_prefs"
              )
              if (zipFile.length() > 0) {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                  emailIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                  FileProvider.getUriForFile(
                      this@SettingActivity,
                      BuildConfig.APPLICATION_ID + ".fileprovider",
                      zipFile
                  )
                } else {
                  Uri.parse("file://${zipFile.absoluteFile}")
                }
                emailIntent.action = Intent.ACTION_SEND
                emailIntent.type = "application/octet-stream"
                emailIntent.putExtra(Intent.EXTRA_STREAM, uri)
                emailIntent.putExtra(
                    Intent.EXTRA_EMAIL,
                    arrayOf(if (!IS_GOOGLEPLAY) "568920427@qq.com" else "rRemix.me@gmail.com")
                )
              }
            } catch (e: Exception) {
              Timber.w(e)
            }
          }
        } else {
          emailIntent.action = Intent.ACTION_SENDTO
          emailIntent.data =
              Uri.parse(if (!IS_GOOGLEPLAY) "mailto:568920427@qq.com" else "mailto:rRemix.me@gmail.com")
        }

        Util.startActivitySafely(this, emailIntent)
      })
    }
    getBaseDialog(this)
        .title(getString(R.string.send_log))
        .positiveText(R.string.yes)
        .negativeText(R.string.no)
        .neutralText(R.string.cancel)
        .onPositive { dialog, which -> send(true) }
        .onNegative { dialog, which -> send(false) }
        .onNeutral { dialog, which -> dialog.cancel() }
        .show()
  }

  /**
   * 设置深色主题
   */
  private fun configDarkTheme() {
    val currentSetting = when (SPUtil.getValue(
        this, SETTING_KEY.NAME, SETTING_KEY.DARK_THEME, ThemeStore.FOLLOW_SYSTEM
    )) {
      ThemeStore.ALWAYS_OFF -> 0
      ThemeStore.ALWAYS_ON -> 1
      ThemeStore.FOLLOW_SYSTEM -> 2
      else -> 2
    }
    getBaseDialog(this).items(R.array.dark_theme)
        .itemsCallbackSingleChoice(currentSetting) { dialog, itemView, which, text ->
          if (which != currentSetting) {
            SPUtil.putValue(
                this, SETTING_KEY.NAME, SETTING_KEY.DARK_THEME, when (which) {
              0 -> ThemeStore.ALWAYS_OFF
              1 -> ThemeStore.ALWAYS_ON
              2 -> ThemeStore.FOLLOW_SYSTEM
              else -> ThemeStore.FOLLOW_SYSTEM
            }
            )
            needRecreate = true
            recreate()
          }
          true
        }.show()
  }

  /**
   * 恢复移除的歌曲
   */
  private fun restoreDeleteSong() {
    SPUtil.deleteValue(this, SETTING_KEY.NAME, SETTING_KEY.BLACKLIST_SONG)
    contentResolver.notifyChange(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null
    )
    ToastUtil.show(this, R.string.alread_restore_songs)
  }

  /**
   * 播放列表导出
   */
  private fun exportPlayList() {
    disposables.add(DatabaseRepository.getInstance().getAllPlaylist()
        .map<List<String>> { playLists ->
          val allplayListNames = ArrayList<String>()
          for ((_, name) in playLists) {
            allplayListNames.add(name)
          }
          allplayListNames
        }.compose(applySingleScheduler()).subscribe { allPlayListNames ->
          getBaseDialog(this).title(R.string.choose_playlist_to_export)
              .negativeText(R.string.cancel).items(allPlayListNames)
              .itemsCallback { _, _, _, text ->
                pendingExportPlaylist = text.toString()
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                  type = MimeTypeMap.getSingleton().getMimeTypeFromExtension("m3u")
                  addCategory(Intent.CATEGORY_OPENABLE)
                  putExtra(Intent.EXTRA_TITLE, "$text.m3u")
                }
                Util.startActivityForResultSafely(this, intent, REQUEST_EXPORT_PLAYLIST)
              }.show()
        })
  }

  /**
   * 播放列表导入
   */
  @SuppressLint("CheckResult")
  private fun importPlayList() {
    getBaseDialog(this).title(R.string.choose_import_way)
        .negativeText(R.string.cancel).items(
            getString(R.string.import_from_external_storage),
            getString(R.string.import_from_others)
        )
        .itemsCallback { _, _, select, _ ->
          if (select == 0) {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
              putExtra(EXTRA_ALLOW_MULTIPLE, true)
              type = MimeTypeMap.getSingleton().getMimeTypeFromExtension("m3u")
              addCategory(Intent.CATEGORY_OPENABLE)
            }
            Util.startActivityForResultSafely(this, intent, REQUEST_IMPORT_PLAYLIST)
          } else {
            Single
                .fromCallable { DatabaseRepository.getInstance().playlistFromMediaStore }
                .compose(applySingleScheduler())
                .subscribe({ localPlayLists ->
                  if (localPlayLists == null || localPlayLists.isEmpty()) {
                    ToastUtil.show(
                        this,
                        R.string.import_fail,
                        getString(R.string.no_playlist_can_import)
                    )
                    return@subscribe
                  }
                  val selectedIndices = ArrayList<Int>()
                  for (i in 0 until localPlayLists.size) {
                    selectedIndices.add(i)
                  }
                  getBaseDialog(this).title(R.string.choose_import_playlist)
                      .positiveText(R.string.choose).items(localPlayLists.keys)
                      .itemsCallbackMultiChoice(
                          selectedIndices.toTypedArray()
                      ) { dialog1, which, allSelects ->
                        disposables.add(
                            importLocalPlayList(
                                this, localPlayLists, allSelects
                            )
                        )
                        true
                      }.show()

                }, { throwable ->
                  ToastUtil.show(
                      this, R.string.import_fail, throwable.toString()
                  )
                })
          }
        }
        .theme(ThemeStore.mDDialogTheme)
        .show()
  }

  /**
   * 歌词搜索优先级
   */
  private fun configLyricPriority() {
    ViewCommon.showLocalLyricTip(this) {
      LyricsOrderDialog().show(supportFragmentManager, "configLyricPriority")
    }
  }

  /**
   * 配置封面下载源
   */
  private fun configCoverDownloadSource() {
    val oldChoice = SPUtil.getValue(
        this,
        SETTING_KEY.NAME,
        SETTING_KEY.ALBUM_COVER_DOWNLOAD_SOURCE,
        DOWNLOAD_LASTFM
    )
    getBaseDialog(this).title(R.string.cover_download_source)
        .items(getString(R.string.lastfm), getString(R.string.netease))
        .itemsCallbackSingleChoice(
            oldChoice
        ) { dialog, view, which, text ->
          if (oldChoice != which) {
            needRefreshAdapter = true
            SPUtil.putValue(
                this,
                SETTING_KEY.NAME,
                SETTING_KEY.ALBUM_COVER_DOWNLOAD_SOURCE,
                which
            )
            binding.settingCoverSourceText.setText(
                if (which == 0) R.string.cover_download_from_lastfm else R.string.cover_download_from_netease
            )
          }
          true
        }
        .show()
  }

  /**
   * 配置封面是否下载
   */
  private fun configCoverDownload() {
    val choice = SPUtil.getValue(
        this,
        SETTING_KEY.NAME,
        SETTING_KEY.AUTO_DOWNLOAD_ALBUM_COVER,
        getString(R.string.always)
    )
    getBaseDialog(this).title(R.string.auto_download_album_artist_cover)
        .items(
            getString(R.string.always), getString(R.string.wifi_only), getString(
            R.string.never
        )
        )
        .itemsCallbackSingleChoice(
            when (choice) {
              getString(R.string.wifi_only) -> 1
              getString(R.string.always) -> 0
              else -> 2
            }
        ) { dialog, view, which, text ->
          binding.settingAlbumCoverText.text = text
          //仅从从不改变到仅在wifi下或者总是的情况下，才刷新Adapter
          needRefreshAdapter =
              needRefreshAdapter || (getString(R.string.wifi_only) == text && getString(R.string.always) == text && originalAlbumChoice != text)
          clearDownloadCover(text)
          SPUtil.putValue(
              this,
              SETTING_KEY.NAME,
              SETTING_KEY.AUTO_DOWNLOAD_ALBUM_COVER,
              text.toString()
          )
          true
        }.show()
  }

  /**
   * 当用户选择从不下载时询问是否清除已有封面
   */
  private fun clearDownloadCover(text: CharSequence) {
    if (getString(R.string.never) == text) {
      getBaseDialog(this)
          .title(R.string.clear_download_cover)
          .positiveText(R.string.confirm)
          .negativeText(R.string.cancel)
          .onPositive { clearDialog, action ->
            SPUtil.deleteFile(this, SPUtil.COVER_KEY.NAME)
//            Fresco.getImagePipeline().clearCaches()
            Glide.get(this).clearMemory()
            Completable
                .fromAction {
                  Glide.get(this).clearDiskCache()
                }
                .subscribeOn(Schedulers.io())
                .subscribe()
            needRefreshAdapter = true
          }.show()
    }
  }

  /**
   * 清除缓存
   */
  private fun clearCache() {
    getBaseDialog(this)
        .content(R.string.confirm_clear_cache)
        .positiveText(R.string.confirm)
        .negativeText(R.string.cancel)
        .onPositive { dialog, which ->
          Glide.get(this@SettingActivity).clearMemory()
          Completable
              .fromAction {
                //清除歌词，封面等缓存
                //清除配置文件、数据库等缓存
                Util.deleteFilesByDirectory(cacheDir)
//                Util.deleteFilesByDirectory(externalCacheDir)
                //清除glide缓存
                Glide.get(this@SettingActivity).clearDiskCache()
                UriFetcher.clearAllCache()
                needRefreshAdapter = true
                handler.sendEmptyMessage(CLEAR_FINISH)
              }
              .subscribeOn(Schedulers.io())
              .subscribe()
        }.show()
  }

  /**
   * 配置通知栏底色
   */
  private fun configNotifyBackgroundColor() {
    if (!SPUtil.getValue(
            this, SETTING_KEY.NAME, SETTING_KEY.NOTIFY_STYLE_CLASSIC, false
        )
    ) {
      ToastUtil.show(this, R.string.notify_bg_color_warnning)
      return
    }
    getBaseDialog(this).title(R.string.notify_bg_color).items(
        getString(R.string.use_system_color), getString(R.string.use_black_color)
    ).itemsCallbackSingleChoice(
        if (SPUtil.getValue(
                this, SETTING_KEY.NAME, SETTING_KEY.NOTIFY_SYSTEM_COLOR, true
            )
        ) 0 else 1
    ) { dialog, view, which, text ->
      SPUtil.putValue(
          this,
          SETTING_KEY.NAME,
          SETTING_KEY.NOTIFY_SYSTEM_COLOR,
          which == 0
      )
      true
    }
        .show()
  }

  /**
   * 配置锁屏界面
   */
  private fun configLockScreen() {
    //0:APlayer锁屏 1:系统锁屏 2:关闭
    getBaseDialog(this).title(R.string.lockscreen_show).items(
        getString(R.string.aplayer_lockscreen),
        getString(R.string.system_lockscreen),
        getString(
            R.string.close
        )
    ).itemsCallbackSingleChoice(
        SPUtil.getValue(
            this,
            SETTING_KEY.NAME,
            SETTING_KEY.LOCKSCREEN,
            Constants.APLAYER_LOCKSCREEN
        )
    ) { dialog, view, which, text ->
      SPUtil.putValue(
          this, SETTING_KEY.NAME, SETTING_KEY.LOCKSCREEN, which
      )
      binding.settingLockscreenText.setText(
          when (which) {
            Constants.APLAYER_LOCKSCREEN -> R.string.aplayer_lockscreen_tip
            Constants.SYSTEM_LOCKSCREEN -> R.string.system_lockscreen_tip
            else -> R.string.lockscreen_off_tip
          }
      )
      true
    }.show()
  }

  /**
   * 配置曲库目录
   */
  private fun configLibrary() {
    val libraryJson = SPUtil
        .getValue(this, SETTING_KEY.NAME, SETTING_KEY.LIBRARY, "")

    val oldLibraries = Gson().fromJson<List<Library>>(
        libraryJson, object : TypeToken<List<Library>>() {}.type
    )
    if (oldLibraries == null || oldLibraries.isEmpty()) {
      ToastUtil.show(this, getString(R.string.load_failed))
      return
    }
    val selected = ArrayList<Int>()
    for (temp in oldLibraries) {
      selected.add(temp.order)
    }

    val allLibraryStrings = getAllLibraryString(this)
    getBaseDialog(this).title(R.string.library_category)
        .positiveText(R.string.confirm).items(allLibraryStrings)
        .itemsCallbackMultiChoice(
            selected.toTypedArray()
        ) { dialog, which, text ->
          if (text.isEmpty()) {
            ToastUtil.show(
                this, getString(R.string.plz_choose_at_least_one_category)
            )
            return@itemsCallbackMultiChoice true
          }
          val newLibraries = ArrayList<Library>()
          for (choose in which) {
            newLibraries.add(Library(choose))
          }
          if (newLibraries != oldLibraries) {
            needRefreshLibrary = true
            intent.putExtra(EXTRA_LIBRARY, newLibraries)
            SPUtil.putValue(
                this, SETTING_KEY.NAME, SETTING_KEY.LIBRARY, Gson().toJson(
                newLibraries, object : TypeToken<List<Library>>() {}.type
            )
            )
          }
          true
        }.show()
  }

  /**
   * 设置黑名单
   */
  private fun configBlackList() {
    blackList = SPUtil.getStringSet(this, SETTING_KEY.NAME, SETTING_KEY.BLACKLIST)
    val items = ArrayList<String>(blackList)
    items.sortWith(Comparator { left, right ->
      File(left).name.compareTo(File(right).name)
    })

    getBaseDialog(this).items(items)
        .itemsCallback { dialog, itemView, position, text ->
          getBaseDialog(this).title(R.string.remove_from_blacklist).content(
              getString(
                  R.string.do_you_want_remove_from_blacklist, text
              )
          )
              .onPositive { dialog, which ->
                val mutableSet = LinkedHashSet<String>(blackList)
                val it = mutableSet.iterator()
                while (it.hasNext()) {
                  if (it.next().contentEquals(text)) {
                    it.remove()
                    break
                  }
                }
                SPUtil.putStringSet(
                    this, SETTING_KEY.NAME, SETTING_KEY.BLACKLIST, mutableSet
                )
                contentResolver.notifyChange(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null
                )
              }
              .positiveText(R.string.confirm)
              .negativeText(R.string.cancel)
              .show()
        }
        .title(R.string.blacklist)
        .neutralText(R.string.clear)
        .positiveText(R.string.add)
        .onAny { dialog, which ->
          when (which) {
            DialogAction.NEUTRAL -> {
              //clear
              getBaseDialog(this).title(R.string.clear_blacklist_title)
                  .content(R.string.clear_blacklist_content)
                  .negativeText(R.string.cancel).positiveText(R.string.confirm)
                  .onPositive { dialog, which ->
                    SPUtil.putStringSet(
                        this,
                        SETTING_KEY.NAME,
                        SETTING_KEY.BLACKLIST,
                        LinkedHashSet<String>()
                    )
                    contentResolver.notifyChange(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null
                    )
                  }.show()
            }
            DialogAction.POSITIVE -> {
              //add
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // 启动获取目录的Intent
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                  addCategory(Intent.CATEGORY_DEFAULT)
                }
                startActivityForResult(intent, REQUEST_CODE_ADD_BLACKLIST)
              } else {
                FolderChooser(
                  this,
                  TAG_BLACKLIST,
                  null,
                  null,
                  null,
                  object : FolderChooser.FolderCallback {
                    override fun onFolderSelection(chooser: FolderChooser, folder: File) {
                      if (folder.isDirectory) {
                        val newBlacklist = LinkedHashSet<String>(blackList)
                        newBlacklist.add(folder.absolutePath)
                        SPUtil.putStringSet(
                          this@SettingActivity,
                          SETTING_KEY.NAME,
                          SETTING_KEY.BLACKLIST,
                          newBlacklist
                        )
                        contentResolver.notifyChange(
                          MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                          null
                        )
                      }
                      configBlackList()
                    }
                  }).show()
              }
            }
            DialogAction.NEGATIVE -> {}
          }
        }
        .show()
  }

  private fun changeBottomOfPlayingScreen() {
    val position = SPUtil.getValue(
        this,
        SETTING_KEY.NAME,
        BOTTOM_OF_NOW_PLAYING_SCREEN,
        PlayerActivity.BOTTOM_SHOW_BOTH
    )
    getBaseDialog(this).title(R.string.show_on_bottom).items(
        getString(R.string.show_next_song_only),
        getString(R.string.show_vol_control_only),
        getString(
            R.string.tap_to_toggle
        ),
        getString(R.string.close)
    )
        .itemsCallbackSingleChoice(position) { dialog, itemView, which, text ->
          if (position != which) {
            SPUtil.putValue(
                this, SETTING_KEY.NAME, BOTTOM_OF_NOW_PLAYING_SCREEN, which
            )
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
      binding.settingClearText.text = getString(
          R.string.cache_size, cacheSize.toFloat() / 1024f / 1024f
      )
    }
    if (msg.what == CLEAR_FINISH) {
      ToastUtil.show(this, getString(R.string.clear_success))
      binding.settingClearText.setText(R.string.zero_size)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(EXTRA_RECREATE, needRecreate)
    outState.putBoolean(EXTRA_REFRESH_ADAPTER, needRefreshAdapter)
  }

  override fun onDestroy() {
    super.onDestroy()
    handler.remove()
    for (disposable in disposables) {
      if (!disposable.isDisposed) {
        disposable.dispose()
      }
    }
  }

  override fun onSharedPreferenceChanged(
      sharedPreferences: SharedPreferences?, key: String?
  ) {
    if (key == SETTING_KEY.DESKTOP_LYRIC_SHOW) {
      binding.settingLrcFloatSwitch.setOnCheckedChangeListener(null)
      binding.settingLrcFloatSwitch.isChecked = SPUtil.getValue(
          this, SETTING_KEY.NAME, SETTING_KEY.DESKTOP_LYRIC_SHOW, false
      )
      binding.settingLrcFloatSwitch.setOnCheckedChangeListener(checkedChangedListener)
    }
  }

  override fun onActivityResult(
      requestCode: Int, resultCode: Int, data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      REQUEST_THEME_COLOR ->
        if (data != null) {
          needRecreate = data.getBooleanExtra(EXTRA_RECREATE, false)
          if (needRecreate) {
            handler.sendEmptyMessage(RECREATE)
          }
        }
      REQUEST_IMPORT_PLAYLIST -> {
        if (resultCode == Activity.RESULT_OK) {
          if (data?.data != null) {
            val uri = data.data ?: return
            DatabaseRepository
              .getInstance()
              .getAllPlaylist()
              .compose(applySingleScheduler())
              .subscribe { playlists ->
                getBaseDialog(this)
                  .title(R.string.add_to_playlist)
                  .items(playlists.map { it.name })
                  .itemsCallback { _, _, _, text ->
                    disposables.add(importM3UFile(this, uri, text.toString(), false))
                  }
                  .neutralText(R.string.create_playlist)
                  .onNeutral { _, _ ->
                    val m3uFile = DocumentFile.fromSingleUri(this, uri)
                    getBaseDialog(this)
                      .title(R.string.new_playlist)
                      .positiveText(R.string.create)
                      .negativeText(R.string.cancel)
                      .content(R.string.input_playlist_name)
                      .input(null, m3uFile?.name?.removeSuffix(".m3u")) { _, input ->
                        if (playlists.map { it.name }.contains(input.toString())) {
                          ToastUtil.show(this, R.string.playlist_already_exist)
                        } else if (!input.isNullOrBlank()){
                          disposables.add(importM3UFile(this, uri, input.toString(), true))
                        }
                      }
                      .show()
                  }
                  .show()
              }
          } else {
            // 多个按顺序直接导入
            data?.clipData?.let { clipData ->
              val pairs = ArrayList<Pair<Uri, DocumentFile>>()
              for (i in 0 until clipData.itemCount) {
                val uri = clipData.getItemAt(i).uri
                pairs.add(Pair(uri, DocumentFile.fromSingleUri(this, uri) ?: continue))
              }
              pairs.forEach {
                importM3UFile(this, it.first, it.second.name ?: "unknown", true)
              }
            }
          }
        }
      }
      REQUEST_EXPORT_PLAYLIST -> {
        if (resultCode == Activity.RESULT_OK) {
          pendingExportPlaylist?.let { pendingExportPlaylist ->
            data?.data?.let { uri ->
              disposables.add(exportPlayListToFile(this, pendingExportPlaylist, uri))
            }
          }
          pendingExportPlaylist = null
        }
      }
      REQUEST_CODE_ADD_BLACKLIST ->{
        if (resultCode == Activity.RESULT_OK) {
          data?.data?.let { uri ->
            val folder = DocumentFile.fromTreeUri(this, uri)
            if (folder?.isDirectory == true) {
              val folderPath = parseDocument(folder)
              val newBlacklist = LinkedHashSet(blackList)
              if (folderPath != null) {
                newBlacklist.add("${Environment.getExternalStorageDirectory()}/$folderPath")
                SPUtil.putStringSet(
                        this@SettingActivity,
                        SETTING_KEY.NAME,
                        SETTING_KEY.BLACKLIST,
                        newBlacklist
                )
                contentResolver.notifyChange(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        null
                )
              }
            }
            configBlackList()
          }
        }
      }
        Crop.REQUEST_PICK -> {
        //选择图片
        val cacheDir = DiskCache.getDiskCacheDir(
            this, "thumbnail/player"
        )
        if (data == null || !cacheDir.exists() && !cacheDir.mkdirs()) {
          ToastUtil.show(this, R.string.setting_error)
          return
        }

        val oldFile = File(cacheDir, "player.jpg")
        if (oldFile.exists()) {
          oldFile.delete()
        }
        val destination = Uri.fromFile(oldFile)
        Crop.of(data.data, destination).withAspect(
            resources.displayMetrics.widthPixels,
            resources.displayMetrics.heightPixels
        )
            .start(this)
      }
      Crop.REQUEST_CROP ->
        if (data == null || Crop.getOutput(data) == null) {
          ToastUtil.show(this, R.string.setting_error)
          return
        }
    }
  }

  private fun parseDocument(folder: DocumentFile): String? {
    val decodedUri = Uri.decode(folder.uri.toString())
    val folderPath = if (decodedUri.lastIndexOf("document/primary:") != -1) {
      decodedUri.split("document/primary:")[1]
    } else if (decodedUri.lastIndexOf("document/home:") != -1) {
      "Documents/" + decodedUri.split("document/home:")[1]
    } else {
      null
    }
    return folderPath
  }

  companion object {
    private const val RECREATE = 100
    private const val CACHE_SIZE = 101
    private const val CLEAR_FINISH = 102
    private const val REQUEST_THEME_COLOR = 0x10
    private const val REQUEST_IMPORT_PLAYLIST = 0x102
    private const val REQUEST_EXPORT_PLAYLIST = 0x103
    private const val REQUEST_CODE_ADD_BLACKLIST = 0x104

    private const val TAG_SCAN = "Scan"
    private const val TAG_IMPORT_PLAYLIST = "ImportPlaylist"
    private const val TAG_EXPORT_PLAYLIST = "ExportPlaylist"
    private const val TAG_BLACKLIST = "Blacklist"
  }

}
