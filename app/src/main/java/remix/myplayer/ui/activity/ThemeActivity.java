package remix.myplayer.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.SharedPrefsUtil;

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
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.day:
                ThemeStore.THEME_MODE = ThemeStore.DAY;
                ThemeStore.THEME_COLOR = SharedPrefsUtil.getValue(ThemeActivity.this,"Theme","ThemeColor",ThemeStore.THEME_PINK);
                ThemeStore.STATUS_BAR_COLOR = ThemeStore.getThemeStatusBarColor(ThemeStore.THEME_COLOR);
                ThemeStore.TOOLBAR_COLOR = ThemeStore.getThemeToolBarColor(ThemeStore.THEME_COLOR);
                SharedPrefsUtil.putValue(ThemeActivity.this,"Setting","ThemeMode",ThemeStore.THEME_MODE);

                break;
            case R.id.night:
                ThemeStore.THEME_MODE = ThemeStore.NIGHT;
                ThemeStore.THEME_COLOR = SharedPrefsUtil.getValue(ThemeActivity.this,"Theme","ThemeColor",ThemeStore.THEME_PINK);
                ThemeStore.STATUS_BAR_COLOR = R.color.night_background_color_3;
                ThemeStore.TOOLBAR_COLOR = R.color.night_background_color_3;
                SharedPrefsUtil.putValue(ThemeActivity.this,"Setting","ThemeMode",ThemeStore.THEME_MODE);
                break;
            case R.id.change:

                ThemeStore.THEME_MODE =ThemeStore.DAY;

                ThemeStore.THEME_COLOR = SharedPrefsUtil.getValue(ThemeActivity.this,"Theme","ThemeColor",ThemeStore.THEME_PINK);

                ThemeStore.STATUS_BAR_COLOR = ThemeStore.getThemeStatusBarColor(ThemeStore.THEME_COLOR);
                ThemeStore.TOOLBAR_COLOR = ThemeStore.getThemeToolBarColor(ThemeStore.THEME_COLOR);

                if(++ThemeStore.THEME_COLOR > ThemeStore.THEME_INDIGO)
                    ThemeStore.THEME_COLOR = ThemeStore.THEME_PURPLE;

                SharedPrefsUtil.putValue(ThemeActivity.this,"Setting","ThemeMode",ThemeStore.THEME_MODE);
                SharedPrefsUtil.putValue(ThemeActivity.this,"Setting","ThemeColor",ThemeStore.THEME_COLOR);
                break;
            case R.id.exit:
                Intent intent = new Intent();
                intent.putExtra("needRefresh",true);
                setResult(Activity.RESULT_OK,intent);
                finish();
                break;
        }

        Intent intent = getIntent();
        overridePendingTransition(0, 0);
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
    }
}
