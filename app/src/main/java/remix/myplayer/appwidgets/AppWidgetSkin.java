package remix.myplayer.appwidgets;

import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;

import remix.myplayer.R;
import remix.myplayer.util.ColorUtil;

public enum AppWidgetSkin {
    WHITE_1F(ColorUtil.getColor(R.color.appwidget_title_color_white_1f),
            ColorUtil.getColor(R.color.appwidget_artist_color_white_1f),
            ColorUtil.getColor(R.color.appwidget_progress_color_white_1f),
            ColorUtil.getColor(R.color.appwidget_btn_color_white_1f),
            R.drawable.bg_corner_app_widget_white_1f),
    TRANSPARENT(ColorUtil.getColor(R.color.appwidget_title_color_transparent),
            ColorUtil.getColor(R.color.appwidget_artist_color_transparent),
            ColorUtil.getColor(R.color.appwidget_progress_color_transparent),
            ColorUtil.getColor(R.color.appwidget_btn_color_transparent),
            R.drawable.bg_corner_app_widget_transparent);

    private int mTitleColor;
    private int mArtistColor;
    private int mProgressColor;
    private int mBtnColor;
    private int mBackground;

    AppWidgetSkin(@ColorInt int titleColor, @ColorInt int artistColor,
                  @ColorInt int progressColor, @ColorInt int btnColor, @DrawableRes int background) {
        this.mTitleColor = titleColor;
        this.mArtistColor = artistColor;
        this.mProgressColor = progressColor;
        this.mBtnColor = btnColor;
        this.mBackground = background;
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
