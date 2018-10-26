package remix.myplayer.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.support.annotation.Nullable
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import butterknife.BindView
import com.afollestad.materialdialogs.DialogAction.POSITIVE
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.helper.SortOrder
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.dialog.TimerDialog
import remix.myplayer.ui.multiple.Controller
import remix.myplayer.ui.multiple.MultipleChoice
import remix.myplayer.ui.widget.TipPopupwindow
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.Constants
import remix.myplayer.util.SPUtil

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/9/29 10:37
 */
@SuppressLint("Registered")
abstract class MultiChoiceActivity : ToolbarActivity() {

//    var choice: MultipleChoice<*>? = null
//    private var tipPopupWindow: TipPopupwindow? = null


    open fun getMenuLayoutId(): Int {
        return R.menu.menu_main_simple
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        setUpClick()
    }

    protected open fun setUpClick() {
//        val views = arrayOf(findViewById(R.id.multi_delete), findViewById(R.id.multi_playlist), findViewById<View>(R.id.multi_playqueue))
//        for (view in views) {
//            view?.setOnClickListener { view1 ->
//                when (view1.id) {
//                    R.id.multi_delete -> {
//                        val title = if (choice?.type == Constants.PLAYLIST)
//                            getString(R.string.confirm_delete_playlist)
//                        else if (choice?.type == Constants.PLAYLISTSONG)
//                            getString(R.string.confirm_delete_from_playlist)
//                        else
//                            getString(R.string.confirm_delete_from_library)
//                        Theme.getBaseDialog(mContext)
//                                .content(title)
//                                .positiveText(R.string.confirm)
//                                .negativeText(R.string.cancel)
//                                .checkBoxPromptRes(R.string.delete_source, SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME, SPUtil.SETTING_KEY.DELETE_SOURCE, false), null)
//                                .onAny { dialog, which ->
//                                    if (which == POSITIVE) {
//                                        choice?.delete(dialog.isPromptCheckBoxChecked)
//                                    }
//                                }
//                                .show()
//                    }
//                    R.id.multi_playqueue ->
//                        choice?.addToPlayQueue()
//                    R.id.multi_playlist ->
//                        choice?.addToPlayList()
//                }
//            }
//        }
        //        ButterKnife.apply(new View[]{findViewById(R.id.multi_delete),findViewById(R.id.multi_playlist),findViewById(R.id.multi_playqueue)}, new ButterKnife.Action<View>() {
        //            @Override
        //            public void apply(@NonNull View view, int index) {
        //                if(view != null)
        //                    view.setOnClickListener(MultiChoiceActivity.this);
        //            }
        //        });
    }


//    protected open fun setUpMultiChoice() {
//        multiChoice = MultiChoice(this)
//        multiChoice!!.setOnUpdateOptionMenuListener { multiShow ->
//            multiChoice!!.setShowing(multiShow)
//            toolbar!!.visibility = if (multiShow) View.GONE else View.VISIBLE
//            multiToolbar!!.visibility = if (multiShow) View.VISIBLE else View.GONE
//            //清空
//            if (!multiChoice!!.isShow) {
//                multiChoice!!.clear()
//            }
//            //只有主界面显示分割线
//            multiToolbar!!.findViewById<View>(R.id.multi_divider).visibility = if (this@MultiChoiceActivity is MainActivity) View.VISIBLE else View.GONE
//            //第一次长按操作显示提示框
//            if (SPUtil.getValue(App.getActivity(), SPUtil.SETTING_KEY.NAME, "IsFirstMulti", true)) {
//                SPUtil.putValue(App.getActivity(), SPUtil.SETTING_KEY.NAME, "IsFirstMulti", false)
//                if (tipPopupWindow == null) {
//                    tipPopupWindow = TipPopupwindow(this)
//                    tipPopupWindow!!.setOnDismissListener { tipPopupWindow = null }
//                }
//                if (!tipPopupWindow!!.isShowing && multiShow) {
//                    tipPopupWindow!!.show(View(this))
//                }
//            }
//        }
//    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_search) {
            startActivity(Intent(mContext, SearchActivity::class.java))
            return true
        } else if (item.itemId == R.id.action_timer) {
            startActivity(Intent(mContext, TimerDialog::class.java))
            return true
        } else {
            var sortOrder: String? = null
            when (item.itemId) {
                R.id.action_sort_order_title -> {
                    sortOrder = SortOrder.SongSortOrder.SONG_A_Z
                    item.isChecked = true
                }
                R.id.action_sort_order_title_desc -> {
                    sortOrder = SortOrder.SongSortOrder.SONG_Z_A
                    item.isChecked = true
                }
                R.id.action_sort_order_album -> {
                    sortOrder = SortOrder.SongSortOrder.SONG_ALBUM_A_Z
                    item.isChecked = true
                }
                R.id.action_sort_order_album_desc -> {
                    sortOrder = SortOrder.SongSortOrder.SONG_ALBUM_Z_A
                    item.isChecked = true
                }
                R.id.action_sort_order_artist -> {
                    sortOrder = SortOrder.SongSortOrder.SONG_ARTIST_A_Z
                    item.isChecked = true
                }
                R.id.action_sort_order_artist_desc -> {
                    sortOrder = SortOrder.SongSortOrder.SONG_ARTIST_Z_A
                    item.isChecked = true
                }
                R.id.action_sort_order_date -> {
                    sortOrder = SortOrder.SongSortOrder.SONG_DATE
                    item.isChecked = true
                }
                R.id.action_sort_order_date_desc -> sortOrder = SortOrder.SongSortOrder.SONG_DATE_DESC
                //                case R.id.action_sort_order_duration:
                //                    sortOrder = SortOrder.SongSortOrder.SONG_DURATION;
                //                    item.setChecked(true);
                //                    break;
                //                case R.id.action_sort_order_year:
                //                    sortOrder = SortOrder.SongSortOrder.SONG_YEAR;
                //                    item.setChecked(true);
                //                    break;
                R.id.action_sort_order_playlist_name -> {
                    sortOrder = SortOrder.PlayListSortOrder.PLAYLIST_A_Z
                    item.isChecked = true
                }
                R.id.action_sort_order_playlist_name_desc -> {
                    sortOrder = SortOrder.PlayListSortOrder.PLAYLIST_Z_A
                    item.isChecked = true
                }
                R.id.action_sort_order_playlist_date -> {
                    sortOrder = SortOrder.PlayListSortOrder.PLAYLIST_DATE
                    item.isChecked = true
                }
                R.id.action_sort_order_custom -> {
                    sortOrder = SortOrder.PlayListSongSortOrder.PLAYLIST_SONG_CUSTOM
                    item.isChecked = true
                }
                R.id.action_sort_order_track_number -> {
                    sortOrder = SortOrder.ChildHolderSongSortOrder.SONG_TRACK_NUMBER
                    item.isChecked = true
                }
            }
            if (!TextUtils.isEmpty(sortOrder))
                saveSortOrder(sortOrder)
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(getMenuLayoutId(), menu)
        tintMenuIcon(menu)
        return true
    }

    protected fun setUpMenuItem(menu: Menu, sortOrder: String) {
        val subMenu = menu.findItem(R.id.action_sort_order).subMenu
        when (sortOrder) {
            SortOrder.SongSortOrder.SONG_A_Z -> subMenu.findItem(R.id.action_sort_order_title).isChecked = true
            SortOrder.SongSortOrder.SONG_Z_A -> subMenu.findItem(R.id.action_sort_order_title_desc).isChecked = true
            SortOrder.SongSortOrder.SONG_ALBUM_A_Z -> subMenu.findItem(R.id.action_sort_order_album).isChecked = true
            SortOrder.SongSortOrder.SONG_ALBUM_Z_A -> subMenu.findItem(R.id.action_sort_order_album_desc).isChecked = true
            SortOrder.SongSortOrder.SONG_ARTIST_A_Z -> subMenu.findItem(R.id.action_sort_order_artist).isChecked = true
            SortOrder.SongSortOrder.SONG_ARTIST_Z_A -> subMenu.findItem(R.id.action_sort_order_artist_desc).isChecked = true
            SortOrder.SongSortOrder.SONG_DATE -> subMenu.findItem(R.id.action_sort_order_date).isChecked = true
            SortOrder.SongSortOrder.SONG_DATE_DESC -> subMenu.findItem(R.id.action_sort_order_date_desc).isChecked = true
            //            case SortOrder.SongSortOrder.SONG_DURATION:
            //                subMenu.findItem(R.id.action_sort_order_duration).setChecked(true);
            //                break;
            //            case SortOrder.SongSortOrder.SONG_YEAR:
            //                subMenu.findItem(R.id.action_sort_order_year).setChecked(true);
            //                break;
            SortOrder.PlayListSortOrder.PLAYLIST_A_Z -> subMenu.findItem(R.id.action_sort_order_playlist_name).isChecked = true
            SortOrder.PlayListSortOrder.PLAYLIST_Z_A -> subMenu.findItem(R.id.action_sort_order_playlist_name_desc).isChecked = true
            SortOrder.PlayListSortOrder.PLAYLIST_DATE -> subMenu.findItem(R.id.action_sort_order_playlist_date).isChecked = true
            SortOrder.ChildHolderSongSortOrder.SONG_TRACK_NUMBER -> subMenu.findItem(R.id.action_sort_order_track_number).isChecked = true
            SortOrder.PlayListSongSortOrder.PLAYLIST_SONG_CUSTOM -> subMenu.findItem(R.id.action_sort_order_custom).isChecked = true
        }
    }

    protected fun tintMenuIcon(menu: Menu) {
        //主题颜色
        val themeColor = ColorUtil.getColor(if (ThemeStore.isLightTheme()) R.color.black else R.color.white)
        for (i in 0 until menu.size()) {
            val menuItem = menu.getItem(i)
            if (menuItem.icon != null)
                menuItem.icon = Theme.TintDrawable(menuItem.icon, themeColor)
        }
    }

    protected open fun saveSortOrder(sortOrder: String?) {

    }


}
