package remix.myplayer.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;
import com.umeng.analytics.MobclickAgent;

import butterknife.BindView;
import remix.myplayer.APlayerApplication;
import remix.myplayer.R;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.customview.TipPopupwindow;
import remix.myplayer.ui.dialog.TimerDialog;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.SPUtil;

import static com.afollestad.materialdialogs.DialogAction.POSITIVE;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/9/29 10:37
 */
public class MultiChoiceActivity extends ToolbarActivity{
    @Nullable
    @BindView(R.id.toolbar)
    Toolbar mToolBar;
    @Nullable
    @BindView(R.id.toolbar_multi)
    ViewGroup mMultiToolBar;
    protected MultiChoice mMultiChoice = null;
    private TipPopupwindow mTipPopupWindow;
    public MultiChoice getMultiChoice(){
        return mMultiChoice;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUpMultiChoice();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpClick();
    }

    protected void setUpClick() {
        View[] views = new View[]{findViewById(R.id.multi_delete),findViewById(R.id.multi_playlist),findViewById(R.id.multi_playqueue)};
        for(View view : views){
            if(view != null)
                view.setOnClickListener(view1 -> {
                    switch (view1.getId()){
                        case R.id.multi_delete:
                            String title = MultiChoice.TYPE == Constants.PLAYLIST ? getString(R.string.confirm_delete_playlist) : MultiChoice.TYPE == Constants.PLAYLISTSONG ?
                                    getString(R.string.confirm_delete_from_playlist) : getString(R.string.confirm_delete_from_library);
                            new MaterialDialog.Builder(mContext)
                                    .content(title)
                                    .buttonRippleColor(ThemeStore.getRippleColor())
                                    .positiveText(R.string.confirm)
                                    .negativeText(R.string.cancel)
                                    .checkBoxPromptRes(R.string.delete_source, false, null)
                                    .onAny((dialog, which) -> {
                                        if(which == POSITIVE){
                                            MobclickAgent.onEvent(mContext,"Delete");
                                            if(mMultiChoice != null)
                                                mMultiChoice.OnDelete(dialog.isPromptCheckBoxChecked());
                                        }
                                    })
                                    .backgroundColorAttr(R.attr.background_color_3)
                                    .positiveColorAttr(R.attr.text_color_primary)
                                    .negativeColorAttr(R.attr.text_color_primary)
                                    .contentColorAttr(R.attr.text_color_primary)
                                    .show();
                            break;
                        case R.id.multi_playqueue:
                            MobclickAgent.onEvent(MultiChoiceActivity.this,"AddtoPlayingList");
                            if(mMultiChoice != null)
                                mMultiChoice.OnAddToPlayQueue();
                            break;
                        case R.id.multi_playlist:
                            MobclickAgent.onEvent(MultiChoiceActivity.this,"AddtoPlayList");
                            if(mMultiChoice != null)
                                mMultiChoice.OnAddToPlayList();
                            break;
                    }
                });
        }
//        ButterKnife.apply(new View[]{findViewById(R.id.multi_delete),findViewById(R.id.multi_playlist),findViewById(R.id.multi_playqueue)}, new ButterKnife.Action<View>() {
//            @Override
//            public void apply(@NonNull View view, int index) {
//                if(view != null)
//                    view.setOnClickListener(MultiChoiceActivity.this);
//            }
//        });
    }

    protected void setUpMultiChoice() {
        mMultiChoice = new MultiChoice(this);
        mMultiChoice.setOnUpdateOptionMenuListener(multiShow -> {
            mMultiChoice.setShowing(multiShow);
            mToolBar.setVisibility(multiShow ? View.GONE : View.VISIBLE);
            mMultiToolBar.setVisibility(multiShow ? View.VISIBLE : View.GONE);
            //清空
            if(!mMultiChoice.isShow()){
                mMultiChoice.clear();
            }
            //只有主界面显示分割线
            mMultiToolBar.findViewById(R.id.multi_divider).setVisibility(MultiChoiceActivity.this instanceof MainActivity ? View.VISIBLE : View.GONE);
            //第一次长按操作显示提示框
            if(SPUtil.getValue(APlayerApplication.getContext(),SPUtil.SETTING_KEY.SETTING_NAME,"IsFirstMulti",true)){
                SPUtil.putValue(APlayerApplication.getContext(),SPUtil.SETTING_KEY.SETTING_NAME,"IsFirstMulti",false);
                if(mTipPopupWindow == null){
                    mTipPopupWindow = new TipPopupwindow(MultiChoiceActivity.this);
                    mTipPopupWindow.setOnDismissListener(() -> mTipPopupWindow = null);
                }
                if(!mTipPopupWindow.isShowing() && multiShow){
                    mTipPopupWindow.show(new View(MultiChoiceActivity.this));
                }
            }
        });
    }

    @Override
    protected void setUpToolbar(Toolbar toolbar, String title) {
        super.setUpToolbar(toolbar,title);
        toolbar.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.toolbar_search:
                    startActivity(new Intent(MultiChoiceActivity.this, SearchActivity.class));
                    break;
                case R.id.toolbar_timer:
                    startActivity(new Intent(MultiChoiceActivity.this, TimerDialog.class));
                    break;
            }
            return true;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.

        getMenuInflater().inflate(R.menu.toolbar_menu,menu);
        //主题颜色
        int themeColor = ColorUtil.getColor(ThemeStore.isLightTheme() ? R.color.black : R.color.white);
        for(int i = 0 ; i < menu.size();i++){
            MenuItem menuItem = menu.getItem(i);
            menuItem.setIcon(Theme.TintDrawable(menuItem.getIcon(),themeColor));
        }
        return true;
    }

    public void onBackPress(){
        mMultiChoice.updateOptionMenu(false);
        if(mTipPopupWindow != null && mTipPopupWindow.isShowing()){
            mTipPopupWindow.dismiss();
            mTipPopupWindow = null;
        }
    }

}
