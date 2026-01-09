package remix.myplayer.service

interface Command {
  companion object {

    /**
     * 播放相关的操作才允许前台服务
     */
    fun isAllowForForegroundService(cmd: Int?) : Boolean{
      return cmd == PLAY_AT || cmd == SKIP_TO_PREVIOUS || cmd == SKIP_TO_NEXT || cmd == PLAY_PAUSE || cmd == PLAY_TEMP
    }

    // 控制命令
    const val PLAY_AT: Int = 0
    const val SKIP_TO_PREVIOUS: Int = 1
    const val PLAY_PAUSE: Int = 2
    const val SKIP_TO_NEXT: Int = 3
    const val PAUSE: Int = 4
    const val PLAY: Int = 5
    const val CHANGE_MODEL: Int = 6
    const val LOVE: Int = 7
    const val PLAY_TEMP: Int = 8
    const val UNLOCK_DESKTOP_LYRIC: Int = 9
    const val CLOSE_NOTIFY: Int = 10
    const val ADD_TO_NEXT_SONG: Int = 11
    const val TOGGLE_TIMER: Int = 12
    const val TOGGLE_DESKTOP_LYRIC: Int = 13
    const val TOGGLE_STATUS_BAR_LRC: Int = 14
    const val SEEK_TO: Int = 15
  }
}
