package remix.myplayer.ui.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;

import com.afollestad.materialdialogs.MaterialDialog;
import com.umeng.analytics.MobclickAgent;

import butterknife.BindView;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.helper.SortOrder;
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
@SuppressLint("Registered")
public abstract class MultiChoiceActivity extends ToolbarActivity{
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
            if(SPUtil.getValue(App.getContext(),SPUtil.SETTING_KEY.NAME,"IsFirstMulti",true)){
                SPUtil.putValue(App.getContext(),SPUtil.SETTING_KEY.NAME,"IsFirstMulti",false);
                if(mTipPopupWindow == null){
                    mTipPopupWindow = new TipPopupwindow(this);
                    mTipPopupWindow.setOnDismissListener(() -> mTipPopupWindow = null);
                }
                if(!mTipPopupWindow.isShowing() && multiShow){
                    mTipPopupWindow.show(new View(this));
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.action_search){
            startActivity(new Intent(mContext, SearchActivity.class));
            return true;
        } else if(item.getItemId() == R.id.action_timer){
            startActivity(new Intent(mContext, TimerDialog.class));
            return true;
        } else {
            String sortOrder = null;
            switch (item.getItemId()){
                case R.id.action_sort_order_title:
                    sortOrder = SortOrder.SongSortOrder.SONG_A_Z;
                    item.setChecked(true);
                    break;
                case R.id.action_sort_order_title_desc:
                    sortOrder = SortOrder.SongSortOrder.SONG_Z_A;
                    item.setChecked(true);
                    break;
                case R.id.action_sort_order_album:
                    sortOrder = SortOrder.SongSortOrder.SONG_ALBUM_A_Z;
                    item.setChecked(true);
                    break;
                case R.id.action_sort_order_album_desc:
                    sortOrder = SortOrder.SongSortOrder.SONG_ALBUM_Z_A;
                    item.setChecked(true);
                    break;
                case R.id.action_sort_order_artist:
                    sortOrder = SortOrder.SongSortOrder.SONG_ARTIST_A_Z;
                    item.setChecked(true);
                    break;
                case R.id.action_sort_order_artist_desc:
                    sortOrder = SortOrder.SongSortOrder.SONG_ARTIST_Z_A;
                    item.setChecked(true);
                    break;
                case R.id.action_sort_order_date:
                    sortOrder = SortOrder.SongSortOrder.SONG_DATE;
                    item.setChecked(true);
                    break;
                case R.id.action_sort_order_date_desc:
                    sortOrder = SortOrder.SongSortOrder.SONG_DATE_DESC;
                    break;
//                case R.id.action_sort_order_duration:
//                    sortOrder = SortOrder.SongSortOrder.SONG_DURATION;
//                    item.setChecked(true);
//                    break;
//                case R.id.action_sort_order_year:
//                    sortOrder = SortOrder.SongSortOrder.SONG_YEAR;
//                    item.setChecked(true);
//                    break;
                case R.id.action_sort_order_playlist_name:
                    sortOrder = SortOrder.PlayListSortOrder.PLAYLIST_A_Z;
                    item.setChecked(true);
                    break;
                case R.id.action_sort_order_playlist_name_desc:
                    sortOrder = SortOrder.PlayListSortOrder.PLAYLIST_Z_A;
                    item.setChecked(true);
                    break;
                case R.id.action_sort_order_playlist_date:
                    sortOrder = SortOrder.PlayListSortOrder.PLAYLIST_DATE;
                    item.setChecked(true);
                    break;
                case R.id.action_sort_order_custom:
                    sortOrder = SortOrder.PlayListSongSortOrder.PLAYLIST_SONG_CUSTOM;
                    item.setChecked(true);
                    break;
                case R.id.action_sort_order_track_number:
                    sortOrder = SortOrder.ChildHolderSongSortOrder.SONG_TRACK_NUMBER;
                    item.setChecked(true);
                    break;
            }
            if(!TextUtils.isEmpty(sortOrder))
                saveSortOrder(sortOrder);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(getMenuLayoutId(),menu);
        tintMenuIcon(menu);
        return true;
    }

    protected void setUpMenuItem(Menu menu, String sortOrder) {
        SubMenu subMenu = menu.findItem(R.id.action_sort_order).getSubMenu();
        switch (sortOrder){
            case SortOrder.SongSortOrder.SONG_A_Z:
                subMenu.findItem(R.id.action_sort_order_title).setChecked(true);
                break;
            case SortOrder.SongSortOrder.SONG_Z_A:
                subMenu.findItem(R.id.action_sort_order_title_desc).setChecked(true);
                break;
            case SortOrder.SongSortOrder.SONG_ALBUM_A_Z:
                subMenu.findItem(R.id.action_sort_order_album).setChecked(true);
                break;
            case SortOrder.SongSortOrder.SONG_ALBUM_Z_A:
                subMenu.findItem(R.id.action_sort_order_album_desc).setChecked(true);
                break;
            case SortOrder.SongSortOrder.SONG_ARTIST_A_Z:
                subMenu.findItem(R.id.action_sort_order_artist).setChecked(true);
                break;
            case SortOrder.SongSortOrder.SONG_ARTIST_Z_A:
                subMenu.findItem(R.id.action_sort_order_artist_desc).setChecked(true);
                break;
            case SortOrder.SongSortOrder.SONG_DATE:
                subMenu.findItem(R.id.action_sort_order_date).setChecked(true);
                break;
            case SortOrder.SongSortOrder.SONG_DATE_DESC:
                subMenu.findItem(R.id.action_sort_order_date_desc).setChecked(true);
                break;
//            case SortOrder.SongSortOrder.SONG_DURATION:
//                subMenu.findItem(R.id.action_sort_order_duration).setChecked(true);
//                break;
//            case SortOrder.SongSortOrder.SONG_YEAR:
//                subMenu.findItem(R.id.action_sort_order_year).setChecked(true);
//                break;
            case SortOrder.PlayListSortOrder.PLAYLIST_A_Z:
                subMenu.findItem(R.id.action_sort_order_playlist_name).setChecked(true);
                break;
            case SortOrder.PlayListSortOrder.PLAYLIST_Z_A:
                subMenu.findItem(R.id.action_sort_order_playlist_name_desc).setChecked(true);
                break;
            case SortOrder.PlayListSortOrder.PLAYLIST_DATE:
                subMenu.findItem(R.id.action_sort_order_playlist_date).setChecked(true);
                break;
            case SortOrder.ChildHolderSongSortOrder.SONG_TRACK_NUMBER:
                subMenu.findItem(R.id.action_sort_order_track_number).setChecked(true);
                break;
            case SortOrder.PlayListSongSortOrder.PLAYLIST_SONG_CUSTOM:
                subMenu.findItem(R.id.action_sort_order_custom).setChecked(true);
                break;
        }
    }

    protected void tintMenuIcon(Menu menu){
        //主题颜色
        int themeColor = ColorUtil.getColor(ThemeStore.isLightTheme() ? R.color.black : R.color.white);
        for(int i = 0 ; i < menu.size();i++){
            MenuItem menuItem = menu.getItem(i);
            if(menuItem.getIcon() != null)
                menuItem.setIcon(Theme.TintDrawable(menuItem.getIcon(),themeColor));
        }
    }

    public void onMultiBackPress(){
        mMultiChoice.updateOptionMenu(false);
        if(mTipPopupWindow != null && mTipPopupWindow.isShowing()){
            mTipPopupWindow.dismiss();
            mTipPopupWindow = null;
        }
    }

    protected int getMenuLayoutId(){
        return R.menu.menu_main_simple;
    }
    protected void saveSortOrder(String sortOrder){

    }
}
