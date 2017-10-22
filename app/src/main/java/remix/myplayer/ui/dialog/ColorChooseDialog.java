package remix.myplayer.ui.dialog;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.model.mp3.ColorChoose;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;

/**
 * @ClassName ColorChooseDialog
 * @Description 主题颜色选择
 * @Author Xiaoborui
 * @Date 2016/8/26 11:14
 */
public class ColorChooseDialog extends BaseDialogActivity {
    @BindView(R.id.color_container)
    LinearLayout mColorContainer;
    private final int[] mColors = new int[]{R.color.md_blue_primary,R.color.md_red_primary,R.color.md_brown_primary,R.color.md_navy_primary,
            R.color.md_green_primary,R.color.md_yellow_primary,R.color.md_purple_primary,R.color.md_indigo_primary,R.color.md_plum_primary,
            R.color.md_white_primary_dark};
    private final String[] mColorTexts = new String[]{"默认","韓紅色","灰汁色","青碧色","常盤色","藤黄色",
                                                        "桔梗色","竜膽色","红梅色","银白色"};
    private final int[] mThemeColors = new int[]{ThemeStore.THEME_BLUE,ThemeStore.THEME_RED,ThemeStore.THEME_BROWN,ThemeStore.THEME_NAVY,
            ThemeStore.THEME_GREEN,ThemeStore.THEME_YELLOW,ThemeStore.THEME_PURPLE,ThemeStore.THEME_INDIGO,ThemeStore.THEME_PLUM,ThemeStore.THEME_WHITE};

    private ArrayList<ColorChoose> mColorInfoList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MobclickAgent.onEvent(this,"ThemeColor");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_color_choose);
        ButterKnife.bind(this);

        for(int i = 0; i < mColors.length ; i++){
            addColor(mColors[i],mColorTexts[i],mThemeColors[i]);
        }
    }

    /**
     * 添加颜色
     */
    private void addColor(@ColorRes int mdColor, String colorText,int themeColor) {
        View colorItem = LayoutInflater.from(this).inflate(R.layout.item_color_choose,null);
        ImageView src = (ImageView) colorItem.findViewById(R.id.color_choose_item_src);
        GradientDrawable drawable = (GradientDrawable) src.getDrawable();
        drawable.setColor(ColorUtil.getColor(mdColor));

        ImageView check = (ImageView) colorItem.findViewById(R.id.color_choose_item_check);
        check.setVisibility(isColorChoose(themeColor) ? View.VISIBLE : View.GONE);

        TextView colorTextView = (TextView) colorItem.findViewById(R.id.color_choose_item_text);
        colorTextView.setText(colorText);
        colorTextView.setTextColor(ThemeStore.getTextColorPrimary());

        colorItem.setOnClickListener(new ColorLisener(themeColor));
        mColorContainer.addView(colorItem);
        mColorInfoList.add(new ColorChoose(themeColor,colorText,check));
    }

    /**
     * 判断是否是选中的颜色
     * @param color
     * @return
     */
    private boolean isColorChoose(int color){
        return color == ThemeStore.THEME_COLOR;
    }


    class ColorLisener implements View.OnClickListener{
        private int mThemeColor;
        public ColorLisener(int themeColor){
            mThemeColor = themeColor;
        }
        @Override
        public void onClick(View v) {

            if(!ThemeStore.isDay()){
                new MaterialDialog.Builder(ColorChooseDialog.this)
                        .content("当前为夜间模式，是否切换主题颜色?")
                        .buttonRippleColor(ThemeStore.getRippleColor())
                        .backgroundColorAttr(R.attr.background_color_3)
                        .positiveColorAttr(R.attr.text_color_primary)
                        .negativeColorAttr(R.attr.text_color_primary)
                        .contentColorAttr(R.attr.text_color_primary)
                        .positiveText("是")
                        .negativeText("否")
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                changeThemeColor(true);
                            }
                        })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {

                            }
                        }).
                        show();
            } else {
                changeThemeColor(false);
            }
        }

        private void changeThemeColor(boolean isfromNight) {
            Intent intent = new Intent();
            intent.putExtra("needRecreate",true);
            intent.putExtra("fromColorChoose",isfromNight);
            setResult(Activity.RESULT_OK,intent);
            ThemeStore.THEME_MODE = ThemeStore.DAY;
            ThemeStore.THEME_COLOR = mThemeColor;
            ThemeStore.MATERIAL_COLOR_PRIMARY = ThemeStore.getMaterialPrimaryColorRes();
            ThemeStore.MATERIAL_COLOR_PRIMARY_DARK = ThemeStore.getMaterialPrimaryDarkColorRes();
            ThemeStore.saveThemeColor(ThemeStore.THEME_COLOR);
            ThemeStore.saveThemeMode(ThemeStore.THEME_MODE);
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        overridePendingTransition(android.R.anim.fade_in,0);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, android.R.anim.fade_out);
    }
}
