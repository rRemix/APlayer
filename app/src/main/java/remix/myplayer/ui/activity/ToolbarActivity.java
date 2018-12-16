package remix.myplayer.ui.activity;

import android.annotation.SuppressLint;
import android.support.v7.widget.Toolbar;

import remix.myplayer.R;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.base.BaseMusicActivity;
import remix.myplayer.util.ColorUtil;

import static remix.myplayer.theme.ThemeStore.getMaterialPrimaryColor;
import static remix.myplayer.theme.ThemeStore.getMaterialPrimaryColorReverse;
import static remix.myplayer.theme.ThemeStore.isMDColorLight;


/**
 * Created by taeja on 16-3-15.
 */
@SuppressLint("Registered")
public class ToolbarActivity extends BaseMusicActivity {
    protected void setUpToolbar(Toolbar toolbar, String title) {
        toolbar.setTitle(title);

        setSupportActionBar(toolbar);
        //主题颜色
        int reverseColor = getMaterialPrimaryColorReverse();
        toolbar.setBackgroundColor(getMaterialPrimaryColor());
        toolbar.setTitleTextColor(reverseColor);
//        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black_24dp);
//        toolbar.setNavigationIcon(Theme.TintDrawable(R.drawable.ic_arrow_back_black_24dp,reverseColor));
        toolbar.setNavigationIcon(Theme.TintDrawable(R.drawable.common_btn_back,reverseColor));
        toolbar.setNavigationOnClickListener(v -> onClickNavigation());

    }

    protected void onClickNavigation() {
        finish();
    }

}
