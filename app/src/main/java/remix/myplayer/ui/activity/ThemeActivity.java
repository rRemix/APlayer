package remix.myplayer.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.theme.ThemeStore;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/8/3 16:59
 */
public class ThemeActivity extends BaseAppCompatActivity implements View.OnClickListener{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_theme);
        ButterKnife.bind(this);
        findView(R.id.day).setOnClickListener(this);
        findView(R.id.night).setOnClickListener(this);
        findView(R.id.change).setOnClickListener(this);
        findView(R.id.exit).setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.day:
                ThemeStore.THEME_MODE = ThemeStore.DAY;
                ThemeStore.THEME_COLOR = ThemeStore.loadThemeColor();
                ThemeStore.MATERIAL_COLOR_PRIMARY = ThemeStore.getMaterialPrimaryColor(ThemeStore.THEME_COLOR);
                ThemeStore.MATERIAL_COLOR_PRIMARY_DARK = ThemeStore.getMaterialPrimaryDarkColor(ThemeStore.THEME_COLOR);
                ThemeStore.saveThemeMode(ThemeStore.THEME_MODE);
                break;
            case R.id.night:
                ThemeStore.THEME_MODE = ThemeStore.NIGHT;
                ThemeStore.THEME_COLOR = ThemeStore.loadThemeColor();
                ThemeStore.MATERIAL_COLOR_PRIMARY = R.color.night_background_color_3;
                ThemeStore.MATERIAL_COLOR_PRIMARY_DARK = R.color.night_background_color_3;
                ThemeStore.saveThemeMode(ThemeStore.THEME_MODE);
                break;
            case R.id.change:
                ThemeStore.THEME_MODE =ThemeStore.DAY;

                ThemeStore.THEME_COLOR = ThemeStore.loadThemeColor();
                if(++ThemeStore.THEME_COLOR > ThemeStore.THEME_INDIGO)
                    ThemeStore.THEME_COLOR = ThemeStore.THEME_PURPLE;

                ThemeStore.MATERIAL_COLOR_PRIMARY = ThemeStore.getMaterialPrimaryColor(ThemeStore.THEME_COLOR);
                ThemeStore.MATERIAL_COLOR_PRIMARY_DARK = ThemeStore.getMaterialPrimaryDarkColor(ThemeStore.THEME_COLOR);

                ThemeStore.saveThemeColor(ThemeStore.THEME_COLOR);
                ThemeStore.saveThemeMode(ThemeStore.THEME_MODE);

                break;
            case R.id.exit:
                Intent intent = new Intent();
                intent.putExtra("needRefresh",true);
                setResult(Activity.RESULT_OK,intent);
                finish();
                break;
        }
        if(view.getId() != R.id.exit){
//            MainActivity.mInstance.finish();
            recreate();
        }
    }
}
