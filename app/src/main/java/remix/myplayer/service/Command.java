package remix.myplayer.service;

public interface Command {

  //控制命令
  int PLAYSELECTEDSONG = 0;
  int PREV = 1;
  int TOGGLE = 2;
  int NEXT = 3;
  int PAUSE = 4;
  int START = 5;
  int CHANGE_MODEL = 6;
  int LOVE = 7;
  int TOGGLE_MEDIASESSION = 8;
  int PLAY_TEMP = 9;
  int TOGGLE_NOTIFY = 10;
  int UNLOCK_DESKTOP_LYRIC = 11;
  int CLOSE_NOTIFY = 12;
  int ADD_TO_NEXT_SONG = 13;
  int CHANGE_LYRIC = 14;
  int PLAY_AT_BREAKPOINT = 15;
  int TOGGLE_TIMER = 16;
  int TOGGLE_DESKTOP_LYRIC = 17;
  int HEADSET_CHANGE = 18;
  int LOCK_DESKTOP_LYRIC = 19;
  int TOGGLE_STATUS_BAR_LRC = 20;
}
