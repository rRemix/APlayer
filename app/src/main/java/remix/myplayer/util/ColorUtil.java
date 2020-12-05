package remix.myplayer.util;

import android.graphics.Color;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.FloatRange;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;
import java.util.Collections;
import java.util.Comparator;
import remix.myplayer.App;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/7/28 10:53
 */
public class ColorUtil {

  private ColorUtil() {
  }

  @ColorInt
  public static int getColor(@ColorRes int colorRes) {
    return ContextCompat.getColor(App.getContext(), colorRes);
  }

  /**
   * 调整颜色透明度
   */
  @ColorInt
  public static int adjustAlpha(@ColorInt int paramInt,
      @FloatRange(from = 0.0D, to = 1.0D) float paramFloat) {
    return Color.argb(Math.round(Color.alpha(paramInt) * paramFloat), Color.red(paramInt),
        Color.green(paramInt), Color.blue(paramInt));
  }

  public static int blendColors(int paramInt1, int paramInt2,
      @FloatRange(from = 0.0D, to = 1.0D) float paramFloat) {
    float f1 = 1.0F - paramFloat;
    float f2 = Color.alpha(paramInt1);
    float f3 = Color.alpha(paramInt2);
    float f4 = Color.red(paramInt1);
    float f5 = Color.red(paramInt2);
    float f6 = Color.green(paramInt1);
    float f7 = Color.green(paramInt2);
    float f8 = Color.blue(paramInt1);
    float f9 = Color.blue(paramInt2);
    return Color.argb((int) (f2 * f1 + f3 * paramFloat), (int) (f4 * f1 + f5 * paramFloat),
        (int) (f6 * f1 + f7 * paramFloat), (int) (f1 * f8 + f9 * paramFloat));
  }

  /**
   * 让颜色更加深
   */
  @ColorInt
  public static int darkenColor(@ColorInt int paramInt) {
    return shiftColor(paramInt, 0.9F);
  }

  /**
   * 反转颜色
   */
  @ColorInt
  public static int invertColor(@ColorInt int paramInt) {
    int i = Color.red(paramInt);
    int j = Color.green(paramInt);
    int k = Color.blue(paramInt);
    return Color.argb(Color.alpha(paramInt), 255 - i, 255 - j, 255 - k);
  }

  /**
   * 是否是亮色
   */
  public static boolean isColorLight(@ColorInt int color) {
    double darkness = 1.0D -
        (0.299D * (double) Color.red(color) + 0.587D * (double) Color.green(color)
            + 0.114D * (double) Color.blue(color)) / 255.0D;
    return darkness < 0.4D;
  }

  /**
   * 让颜色更加明亮
   */
  @ColorInt
  public static int lightenColor(@ColorInt int paramInt) {
    return shiftColor(paramInt, 1.1F);
  }

  /**
   * 转换颜色
   */
  @ColorInt
  public static int shiftColor(@ColorInt int paramInt,
      @FloatRange(from = 0.0D, to = 2.0D) float paramFloat) {
    if (paramFloat == 1.0F) {
      return paramInt;
    }
    int i = Color.alpha(paramInt);
    float[] arrayOfFloat = new float[3];
    Color.colorToHSV(paramInt, arrayOfFloat);
    arrayOfFloat[2] *= paramFloat;
    return (i << 24) + (Color.HSVToColor(arrayOfFloat) & 0xFFFFFF);
  }

  public static int stripAlpha(@ColorInt int paramInt) {
    return 0xFF000000 | paramInt;
  }

  /**
   * 改变颜色透明度
   */
  @ColorInt
  public static int withAlpha(@ColorInt int paramInt,
      @FloatRange(from = 0.0D, to = 1.0D) float paramFloat) {
    return (Math.min(255, Math.max(0, (int) (255.0F * paramFloat))) << 24) + (0xFFFFFF & paramInt);
  }

  @ColorInt
  public static int getColor(@Nullable Palette palette, int fallback) {
    if (palette != null) {
      if (palette.getVibrantSwatch() != null) {
        return palette.getVibrantSwatch().getRgb();
      } else if (palette.getMutedSwatch() != null) {
        return palette.getMutedSwatch().getRgb();
      } else if (palette.getDarkVibrantSwatch() != null) {
        return palette.getDarkVibrantSwatch().getRgb();
      } else if (palette.getDarkMutedSwatch() != null) {
        return palette.getDarkMutedSwatch().getRgb();
      } else if (palette.getLightVibrantSwatch() != null) {
        return palette.getLightVibrantSwatch().getRgb();
      } else if (palette.getLightMutedSwatch() != null) {
        return palette.getLightMutedSwatch().getRgb();
      } else if (!palette.getSwatches().isEmpty()) {
        return Collections.max(palette.getSwatches(), SwatchComparator.getInstance()).getRgb();
      }
    }
    return fallback;
  }

  public static Palette.Swatch getSwatch(Palette palette) {
    if (palette != null) {
      if (palette.getVibrantSwatch() != null) {
        return palette.getVibrantSwatch();
      } else if (palette.getMutedSwatch() != null) {
        return palette.getMutedSwatch();
      } else if (palette.getDarkVibrantSwatch() != null) {
        return palette.getDarkVibrantSwatch();
      } else if (palette.getDarkMutedSwatch() != null) {
        return palette.getDarkMutedSwatch();
      } else if (palette.getLightVibrantSwatch() != null) {
        return palette.getLightVibrantSwatch();
      } else if (palette.getLightMutedSwatch() != null) {
        return palette.getLightMutedSwatch();
      } else if (!palette.getSwatches().isEmpty()) {
        return Collections.max(palette.getSwatches(), SwatchComparator.getInstance());
      }
    }
    return new Palette.Swatch(Color.GRAY, 100);
  }

  public static boolean isColorCloseToWhite(@ColorInt int color) {
    return StatusBarUtil.MeizuStatusbar.toGrey(color) >= 254;
  }

  public static boolean isColorCloseToBlack(@ColorInt int color) {
    return StatusBarUtil.MeizuStatusbar.toGrey(color) <= 1;
  }

  private static class SwatchComparator implements Comparator<Palette.Swatch> {

    private static SwatchComparator sInstance;

    static SwatchComparator getInstance() {
      if (sInstance == null) {
        sInstance = new SwatchComparator();
      }
      return sInstance;
    }

    @Override
    public int compare(Palette.Swatch lhs, Palette.Swatch rhs) {
      return lhs.getPopulation() - rhs.getPopulation();
    }
  }
}
