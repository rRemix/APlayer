//package remix.myplayer.misc
//
//import android.content.Context
//import android.content.Intent
//import android.graphics.Color
//import io.reactivex.Maybe
//import io.reactivex.Observable
//import remix.myplayer.App
//import remix.myplayer.R
//import remix.myplayer.db.DBOpenHelper
//import remix.myplayer.db.room.DatabaseRepository
//import remix.myplayer.service.MusicService
//import remix.myplayer.theme.ThemeStore
//import remix.myplayer.util.ColorUtil
//import remix.myplayer.util.SPUtil
//import remix.myplayer.util.Util
//import timber.log.Timber
//import kotlin.concurrent.thread
//
//object Migration {
//  /**
//   * 迁移主题
//   */
//  @JvmStatic
//  fun migrationTheme(context: Context) {
//    thread {
//      //已经迁移过
//      if (SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, "migration_theme", false)) {
//        return@thread
//      }
//      SPUtil.putValue(context, SPUtil.SETTING_KEY.NAME, "migration_theme", true)
//
//      //先判断是不是夜间模式
//      if (SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, "ThemeMode", 0) == 1) {
//        ThemeStore.setGeneralTheme(ThemeStore.DARK)
//        ThemeStore.saveAccentColor(ColorUtil.getColor(R.color.md_purple_primary))
//        ThemeStore.saveMaterialPrimaryColor(ColorUtil.getColor(R.color.dark_background_color_main))
//      } else {
//        //读取以前的materialprimary color
//        val oldThemeColor = SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, "ThemeColor", 108)
//        val colorRes = when (oldThemeColor) {
//          100 -> R.color.md_red_primary
//          101 -> R.color.md_brown_primary
//          102 -> R.color.md_navy_primary
//          103 -> R.color.md_green_primary
//          104 -> R.color.md_yellow_primary
//          105 -> R.color.md_purple_primary
//          106 -> R.color.md_indigo_primary
//          107 -> R.color.md_plum_primary
//          108 -> R.color.md_blue_primary
//          109 -> R.color.md_white_primary
//          110 -> R.color.md_pink_primary
//          else -> R.color.md_blue_primary
//        }
//        val color = ColorUtil.getColor(colorRes)
//        ThemeStore.setGeneralTheme(ThemeStore.LIGHT)
//        //白色主题AccentColor是黑色
//        ThemeStore.saveAccentColor(if (colorRes == R.color.md_white_primary) Color.BLACK else color)
//        ThemeStore.saveMaterialPrimaryColor(color)
//      }
//    }
//  }
//
//  /**
//   * 迁移数据库
//   */
//  @JvmStatic
//  fun migrationDatabase(context: Context) {
//    thread(priority = Thread.MAX_PRIORITY) {
//      Timber.v("开始迁移")
//      //已经迁移过
//      if (SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, "migration_database", false)) {
//        Timber.v("已经迁移过")
//        return@thread
//      }
//      SPUtil.putValue(context, SPUtil.SETTING_KEY.NAME, "migration_database", true)
//
//      val db = DBOpenHelper(context).writableDatabase
//      //读取所有播放列表
//      db.query("play_list_song", null, null, null, null, null, null, null).use { cursor ->
//        if (cursor.count == 0) {
//          Timber.v("没有以前的数据")
//          return@thread
//        }
//
//        //读取以前的播放列表
//        val playlistMap = HashMap<String, ArrayList<Int>>()
//
//        while (cursor.moveToNext()) {
//          val playListName = cursor.getString(cursor.getColumnIndex("play_list_name"))
//          val audioId = cursor.getInt(cursor.getColumnIndex("audio_id"))
//          if (!playlistMap.containsKey(playListName)) {
//            playlistMap[playListName] = ArrayList<Int>().also { it.add(audioId) }
//          } else {
//            playlistMap[playListName]?.add(audioId)
//          }
//        }
//
//        //以前的播放队列
//        playlistMap.remove(App.getContext().getString(R.string.play_queue))?.let {
//          //将以前的播放队列插入到新的数据库中
//          DatabaseRepository.getInstance()
//              .insertToPlayQueue(it)
//              .subscribe({
//                Timber.v("插入播放队列: $it")
//              }, {
//                Timber.v(it)
//              })
//        }
//
//        //将以前的播放列表插入到新的数据库中
//        Observable.fromIterable(playlistMap.entries)
//            .flatMapSingle { entry ->
//              DatabaseRepository.getInstance()
//                  .insertPlayList(entry.key)
//                  .filter { id ->
//                    id > 0
//                  }
//                  .onErrorResumeNext(Maybe.just(0))
//                  .toSingle()
//                  .flatMap {
//                    DatabaseRepository.getInstance()
//                        .insertToPlayList(entry.value, entry.key)
//
//                  }
//            }
//            .doFinally {
//              Timber.v("更新界面")
//              Util.sendLocalBroadcast(Intent(MusicService.MEDIA_STORE_CHANGE))
//            }
//            .subscribe({
//              Timber.v("插入播放列表: $it")
//            }, {
//              Timber.v(it)
//            })
//      }
//
//    }
//  }
//
//  /**
//   * 迁移
//   */
//  @JvmStatic
//  fun migrationLibrary(context: Context){
//    thread {
//      //已经迁移过
//      if (SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, "migration_library", false)) {
//        return@thread
//      }
//      //清除以前保存的
//      SPUtil.deleteValue(context,SPUtil.SETTING_KEY.NAME,SPUtil.SETTING_KEY.LIBRARY_CATEGORY)
//      SPUtil.putValue(context, SPUtil.SETTING_KEY.NAME, "migration_library", true)
//    }
//  }
//
//  @JvmStatic
//  fun migrationPlayModel(context: Context){
//    thread {
//      //已经迁移过
//      if (SPUtil.getValue(context, SPUtil.SETTING_KEY.NAME, "migration_play_model", false)) {
//        return@thread
//      }
//      SPUtil.putValue(context, SPUtil.SETTING_KEY.NAME, "migration_play_model", true)
//      //根据以前保存的设置现在的播放模式
//      val oldPlayModel = SPUtil.getValue(context,SPUtil.SETTING_KEY.NAME,SPUtil.SETTING_KEY.PLAY_MODEL,-1)
//      if(oldPlayModel in 50..52){
//        SPUtil.putValue(context,SPUtil.SETTING_KEY.NAME,SPUtil.SETTING_KEY.PLAY_MODEL,oldPlayModel - 49)
//      }
//    }
//  }
//}