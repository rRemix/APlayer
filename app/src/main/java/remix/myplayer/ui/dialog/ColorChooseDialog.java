package remix.myplayer.ui.dialog;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.model.ColorChooseInfo;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.BaseActivity;
import remix.myplayer.util.ColorUtil;

/**
 * @ClassName ColorChooseDialog
 * @Description 主题颜色选择
 * @Author Xiaoborui
 * @Date 2016/8/26 11:14
 */
public class ColorChooseDialog extends BaseActivity{
    @BindView(R.id.color_container)
    LinearLayout mColorContainer;
    private final int[] mColors = new int[]{R.color.md_purple_primary,R.color.md_red_primary,
            R.color.md_pink_primary,R.color.md_brown_primary,R.color.md_indigo_primary};
    private final String[] mColorTexts = new String[]{"紫色","红色","粉色","棕色","蓝色"};
    private final int[] mThemeColors = new int[]{ThemeStore.THEME_PURPLE,ThemeStore.THEME_RED,ThemeStore.THEME_PINK,
                                                ThemeStore.THEME_BROWN,ThemeStore.THEME_INDIGO};

    private ArrayList<ColorChooseInfo> mColorInfoList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        View colorItem = LayoutInflater.from(this).inflate(R.layout.color_choose_item,null);
        ImageView src = (ImageView) colorItem.findViewById(R.id.color_choose_item_src);
        GradientDrawable drawable = (GradientDrawable) src.getDrawable();
        drawable.setColor(ColorUtil.getColor(mdColor));

        ImageView check = (ImageView) colorItem.findViewById(R.id.color_choose_item_check);
        check.setVisibility(isColorChoose(mdColor) ? View.VISIBLE : View.GONE);
        TextView colorTextView = (TextView) colorItem.findViewById(R.id.color_choose_item_text);
        colorTextView.setText(colorText);

        colorItem.setOnClickListener(new ColorLisener(themeColor));
        mColorContainer.addView(colorItem);
        mColorInfoList.add(new ColorChooseInfo(themeColor,colorText,check));
    }

    /**
     * 判断是否是选中的颜色
     * @param colorRes
     * @return
     */
    private boolean isColorChoose(@ColorRes int colorRes){
        return colorRes == ThemeStore.MATERIAL_COLOR_PRIMARY;
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
                        .backgroundColor(ThemeStore.getBackgroundColor3())
                        .positiveColor(ThemeStore.getTextColorPrimary())
                        .negativeColor(ThemeStore.getTextColorPrimary())
                        .contentColor(ThemeStore.getTextColorPrimary())
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
                        }).show();
            } else {
                changeThemeColor(false);
            }
        }

        private void changeThemeColor(boolean isfromNight) {
            Intent intent = new Intent();
            intent.putExtra("needRefresh",true);
            intent.putExtra("fromColorChoose",isfromNight);
            setResult(Activity.RESULT_OK,intent);
            ThemeStore.THEME_MODE = ThemeStore.DAY;
            ThemeStore.THEME_COLOR = mThemeColor;
            ThemeStore.MATERIAL_COLOR_PRIMARY = ThemeStore.getMaterialPrimaryColor();
            ThemeStore.MATERIAL_COLOR_PRIMARY_DARK = ThemeStore.getMaterialPrimaryDarkColor();
            ThemeStore.saveThemeColor(ThemeStore.THEME_COLOR);
            ThemeStore.saveThemeMode(ThemeStore.THEME_MODE);
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        overridePendingTransition(R.anim.popup_in,0);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.popup_out);
    }
}
