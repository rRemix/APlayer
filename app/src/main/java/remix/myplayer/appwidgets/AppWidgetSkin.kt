package remix.myplayer.appwidgets

import android.support.annotation.ColorInt
import android.support.annotation.DrawableRes

import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.Constants
import remix.myplayer.util.SPUtil

enum class AppWidgetSkin private constructor(@param:ColorInt var titleColor: Int, @param:ColorInt var artistColor: Int,
                                             @param:ColorInt var progressColor: Int, @param:ColorInt var btnColor: Int, @param:DrawableRes var background: Int,
                                             @param:DrawableRes
                                             //    public Bitmap getTimerBitmap(){
                                             //        return BitmapFactory.decodeResource(App.getContext().getResources(),mTimerRes);
                                             //    }
                                             //
                                             //    public Bitmap getNextBitmap(){
                                             //        return BitmapFactory.decodeResource(App.getContext().getResources(),mNextRes);
                                             //    }
                                             //
                                             //    public Bitmap getPrevBitmap(){
                                             //        return BitmapFactory.decodeResource(App.getContext().getResources(),mPrevRes);
                                             //    }
                                             //
                                             //    public Bitmap getLoveBitmap(){
                                             //        return BitmapFactory.decodeResource(App.getContext().getResources(),mLoveRes);
                                             //    }
                                             //
                                             //    public Bitmap getModeBitmap(){
                                             //        return MusicService.getPlayModel() == Constants.PLAY_LOOP ? getModeNormalBitmap() :
                                             //                MusicService.getPlayModel() == Constants.PLAY_REPEATONE ? getModeRepeatBitmap() : getModeShuffleBitmap();
                                             //    }
                                             //
                                             //    private Bitmap getModeRepeatBitmap(){
                                             //        return BitmapFactory.decodeResource(App.getContext().getResources(),mModeRepeatRes);
                                             //    }
                                             //
                                             //    private Bitmap getModeNormalBitmap(){
                                             //        return BitmapFactory.decodeResource(App.getContext().getResources(),mModeNormalRes);
                                             //    }
                                             //
                                             //    private Bitmap getModeShuffleBitmap(){
                                             //        return BitmapFactory.decodeResource(App.getContext().getResources(),mModeShuffleRes);
                                             //    }
                                             //
                                             //    public Bitmap getPlayPauseBitmap(){
                                             //        return MusicService.isPlaying() ? getPauseBitmap() : getPlayBitmap();
                                             //    }
                                             //
                                             //    private Bitmap getPlayBitmap(){
                                             //        return BitmapFactory.decodeResource(App.getContext().getResources(),mPlayRes);
                                             //    }
                                             //
                                             //    private Bitmap getPauseBitmap(){
                                             //        return BitmapFactory.decodeResource(App.getContext().getResources(),mPauseRes);
                                             //    }
                                             //
                                             //    public Bitmap getLovedBitmap(){
                                             //        return BitmapFactory.decodeResource(App.getContext().getResources(),R.drawable.widget_btn_like_prs);
                                             //    }

                                             val timerRes: Int, @param:DrawableRes val nextRes: Int, @param:DrawableRes val prevRes: Int,
                                             @param:DrawableRes val loveRes: Int, @param:DrawableRes val modeRepeatRes: Int, @param:DrawableRes val modeNormalRes: Int, @param:DrawableRes val modeShuffleRes: Int,
                                             @param:DrawableRes val playRes: Int, @param:DrawableRes val pauseRes: Int) {
  WHITE_1F(ColorUtil.getColor(R.color.appwidget_title_color_white_1f),
      ColorUtil.getColor(R.color.appwidget_artist_color_white_1f),
      ColorUtil.getColor(R.color.appwidget_progress_color_white_1f),
      ColorUtil.getColor(R.color.appwidget_btn_color_white_1f),
      R.drawable.bg_corner_app_widget_white_1f,
      R.drawable.widget_btn_timer, R.drawable.widget_btn_next_normal, R.drawable.widget_btn_previous_normal,
      R.drawable.widget_btn_like_nor, R.drawable.widget_btn_one_normal, R.drawable.widget_btn_loop_normal,
      R.drawable.widget_btn_shuffle_normal, R.drawable.widget_btn_play_normal, R.drawable.widget_btn_stop_normal),
  TRANSPARENT(ColorUtil.getColor(R.color.appwidget_title_color_transparent),
      ColorUtil.getColor(R.color.appwidget_artist_color_transparent),
      ColorUtil.getColor(R.color.appwidget_progress_color_transparent),
      ColorUtil.getColor(R.color.appwidget_btn_color_transparent),
      R.drawable.bg_corner_app_widget_transparent,
      R.drawable.widget_btn_timer_transparent, R.drawable.widget_btn_next_normal_transparent,
      R.drawable.widget_btn_previous_normal_transparent, R.drawable.widget_btn_like_nor_transparent,
      R.drawable.widget_btn_one_normal_transparent, R.drawable.widget_btn_loop_normal_transparent,
      R.drawable.widget_btn_shuffle_normal_transparent, R.drawable.widget_btn_play_normal_transparent,
      R.drawable.widget_btn_stop_normal_transparent);

  val lovedRes: Int
    get() = R.drawable.widget_btn_like_prs

  val modeRes: Int
    get() {
      val playModel = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_MODEL, Constants.PLAY_LOOP)
      return when (playModel) {
        Constants.PLAY_SHUFFLE -> modeShuffleRes
        Constants.PLAY_REPEAT -> modeRepeatRes
        else -> modeNormalRes
      }
    }
}
