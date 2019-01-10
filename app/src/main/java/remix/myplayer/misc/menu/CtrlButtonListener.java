package remix.myplayer.misc.menu;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import remix.myplayer.R;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;
import remix.myplayer.util.Util;

/**
 * Created by Remix on 2015/12/3.
 */

/**
 * 播放控制
 */
public class CtrlButtonListener implements View.OnClickListener {

  private Context context;

  public CtrlButtonListener(Context context) {
    this.context = context;
  }

  @Override
  public void onClick(View v) {
    Intent intent = new Intent(MusicService.ACTION_CMD);
    switch (v.getId()) {
      case R.id.lockscreen_prev:
      case R.id.playbar_prev:
        intent.putExtra("Control", Command.PREV);
        break;
      case R.id.lockscreen_next:
      case R.id.playbar_next:
        intent.putExtra("Control", Command.NEXT);
        break;
      case R.id.lockscreen_play:
      case R.id.playbar_play:
        intent.putExtra("Control", Command.TOGGLE);
        break;
    }
    Util.sendLocalBroadcast(intent);
  }
}
