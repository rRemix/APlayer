package remix.myplayer.appwidgets;

import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;

import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.SPUtil;

import static remix.myplayer.helper.MusicServiceRemote.isPlaying;

public enum AppWidgetSkin {
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

    private int mTitleColor;
    private int mArtistColor;
    private int mProgressColor;
    private int mBtnColor;
    private int mBackground;
    private int mTimerRes;
    private int mNextRes;
    private int mPrevRes;
    private int mLoveRes;
    private int mModeRepeatRes;
    private int mModeNormalRes;
    private int mModeShuffleRes;
    private int mPlayRes;
    private int mPauseRes;

    AppWidgetSkin(@ColorInt int titleColor, @ColorInt int artistColor,
                  @ColorInt int progressColor, @ColorInt int btnColor, @DrawableRes int background,
                  @DrawableRes int timerRes, @DrawableRes int nextRes, @DrawableRes int prevRes,
                  @DrawableRes int loveRes, @DrawableRes int repeatRes, @DrawableRes int normalRes, @DrawableRes int shuffleRes,
                  @DrawableRes int playRes, @DrawableRes int pauseRes) {
        mTitleColor = titleColor;
        mArtistColor = artistColor;
        mProgressColor = progressColor;
        mBtnColor = btnColor;
        mBackground = background;
        mTimerRes = timerRes;
        mNextRes = nextRes;
        mPrevRes = prevRes;
        mLoveRes = loveRes;
        mModeRepeatRes = repeatRes;
        mModeNormalRes = normalRes;
        mModeShuffleRes = shuffleRes;
        mPlayRes = playRes;
        mPauseRes = pauseRes;
    }

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

    public int getTimerRes() {
        return mTimerRes;
    }

    public int getNextRes() {
        return mNextRes;
    }

    public int getPrevRes() {
        return mPrevRes;
    }

    public int getLoveRes() {
        return mLoveRes;
    }

    public int getLovedRes() {
        return R.drawable.widget_btn_like_prs;
    }

    public int getModeRepeatRes() {
        return mModeRepeatRes;
    }

    public int getModeNormalRes() {
        return mModeNormalRes;
    }

    public int getModeShuffleRes() {
        return mModeShuffleRes;
    }

    public int getModeRes() {
        final int playModel = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.PLAY_MODEL, Constants.PLAY_LOOP);
        return playModel == Constants.PLAY_SHUFFLE ? mModeShuffleRes :
                playModel == Constants.PLAY_REPEATONE ? mModeRepeatRes : mModeNormalRes;
    }

    public int getPlayPauseRes() {
        return isPlaying() ? getPauseRes() : getPlayRes();
    }

    public int getPlayRes() {
        return mPlayRes;
    }

    public int getPauseRes() {
        return mPauseRes;
    }

    public int getTitleColor() {
        return mTitleColor;
    }

    public void setTitleColor(int titleColor) {
        this.mTitleColor = titleColor;
    }

    public int getArtistColor() {
        return mArtistColor;
    }

    public void setArtistColor(int artistColor) {
        this.mArtistColor = artistColor;
    }

    public int getProgressColor() {
        return mProgressColor;
    }

    public void setProgressColor(int progressColor) {
        this.mProgressColor = progressColor;
    }

    public int getBtnColor() {
        return mBtnColor;
    }

    public void setBtnColor(int btnColor) {
        this.mBtnColor = btnColor;
    }

    public int getBackground() {
        return mBackground;
    }

    public void setBackground(int background) {
        this.mBackground = background;
    }
}
