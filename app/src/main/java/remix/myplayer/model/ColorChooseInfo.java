package remix.myplayer.model;

import android.widget.ImageView;

/**
 * @ClassName ColorChooseInfo
 * @Description 颜色选择信息
 * @Author Xiaoborui
 * @Date 2016/8/26 14:43
 */
public class ColorChooseInfo {
    /** 主题颜色*/
    public int mThemeColor;

    /** 颜色文本 */
    public String mColorText;

    /** 是否选中 */
    public ImageView mCheck;

    public ColorChooseInfo(int ThemeColor, String ColorText,ImageView Check) {
        this.mThemeColor = ThemeColor;
        this.mColorText = ColorText;
        this.mCheck = Check;
    }
}
