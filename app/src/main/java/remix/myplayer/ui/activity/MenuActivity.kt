package remix.myplayer.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import remix.myplayer.R
import remix.myplayer.helper.SortOrder
import remix.myplayer.theme.ToolbarContentTintHelper
import remix.myplayer.ui.dialog.TimerDialog

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/9/29 10:37
 */
@SuppressLint("Registered")
abstract class MenuActivity : ToolbarActivity() {
  open fun getMenuLayoutId(): Int {
    return R.menu.menu_main_simple
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    if (item.itemId == R.id.action_search) {
      startActivity(Intent(this, SearchActivity::class.java))
      return true
    } else if (item.itemId == R.id.action_timer) {
      TimerDialog.newInstance()
          .show(supportFragmentManager, TimerDialog::class.java.simpleName)
      return true
    } else {
      var sortOrder = ""
      when (item.itemId) {
        R.id.action_sort_order_title -> {
          sortOrder = SortOrder.SONG_A_Z
          item.isChecked = true
        }
        R.id.action_sort_order_title_desc -> {
          sortOrder = SortOrder.SONG_Z_A
          item.isChecked = true
        }
        R.id.action_sort_order_display_title -> {
          sortOrder = SortOrder.DISPLAY_NAME_A_Z
          item.isChecked = true
        }
        R.id.action_sort_order_display_title_desc -> {
          sortOrder = SortOrder.DISPLAY_NAME_Z_A
          item.isChecked = true
        }
        R.id.action_sort_order_album -> {
          sortOrder = SortOrder.ALBUM_A_Z
          item.isChecked = true
        }
        R.id.action_sort_order_album_desc -> {
          sortOrder = SortOrder.ALBUM_Z_A
          item.isChecked = true
        }
        R.id.action_sort_order_artist -> {
          sortOrder = SortOrder.ARTIST_A_Z
          item.isChecked = true
        }
        R.id.action_sort_order_artist_desc -> {
          sortOrder = SortOrder.ARTIST_Z_A
          item.isChecked = true
        }
        R.id.action_sort_order_date -> {
          sortOrder = SortOrder.DATE
          item.isChecked = true
        }
        R.id.action_sort_order_date_desc -> {
          sortOrder = SortOrder.DATE_DESC
          item.isChecked = true
        }
        //                case R.id.action_sort_order_duration:
        //                    sortOrder = SortOrder.DURATION;
        //                    item.setChecked(true);
        //                    break;
        //                case R.id.action_sort_order_year:
        //                    sortOrder = SortOrder.YEAR;
        //                    item.setChecked(true);
        //                    break;
        R.id.action_sort_order_playlist_name -> {
          sortOrder = SortOrder.PLAYLIST_A_Z
          item.isChecked = true
        }
        R.id.action_sort_order_playlist_name_desc -> {
          sortOrder = SortOrder.PLAYLIST_Z_A
          item.isChecked = true
        }
        R.id.action_sort_order_playlist_date -> {
          sortOrder = SortOrder.PLAYLIST_DATE
          item.isChecked = true
        }
        R.id.action_sort_order_custom -> {
          sortOrder = SortOrder.PLAYLIST_SONG_CUSTOM
          item.isChecked = true
        }
        R.id.action_sort_order_track_number -> {
          sortOrder = SortOrder.TRACK_NUMBER
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
    super.onCreateOptionsMenu(menu)
    menuInflater.inflate(getMenuLayoutId(), menu)
    tintMenuIcon(menu)
    return true
  }

  protected fun setUpMenuItem(menu: Menu, sortOrder: String) {
    val subMenu = menu.findItem(R.id.action_sort_order).subMenu ?: return
    when (sortOrder) {
      SortOrder.SONG_A_Z -> subMenu.findItem(R.id.action_sort_order_title).isChecked = true
      SortOrder.SONG_Z_A -> subMenu.findItem(R.id.action_sort_order_title_desc).isChecked = true
      SortOrder.DISPLAY_NAME_A_Z -> subMenu.findItem(R.id.action_sort_order_display_title).isChecked = true
      SortOrder.DISPLAY_NAME_Z_A -> subMenu.findItem(R.id.action_sort_order_display_title_desc).isChecked = true
      SortOrder.ALBUM_A_Z -> subMenu.findItem(R.id.action_sort_order_album).isChecked = true
      SortOrder.ALBUM_Z_A -> subMenu.findItem(R.id.action_sort_order_album_desc).isChecked = true
      SortOrder.ARTIST_A_Z -> subMenu.findItem(R.id.action_sort_order_artist).isChecked = true
      SortOrder.ARTIST_Z_A -> subMenu.findItem(R.id.action_sort_order_artist_desc).isChecked = true
      SortOrder.DATE -> subMenu.findItem(R.id.action_sort_order_date).isChecked = true
      SortOrder.DATE_DESC -> subMenu.findItem(R.id.action_sort_order_date_desc).isChecked = true
      //            case SortOrder.DURATION:
      //                subMenu.findItem(R.id.action_sort_order_duration).setChecked(true);
      //                break;
      //            case SortOrder.YEAR:
      //                subMenu.findItem(R.id.action_sort_order_year).setChecked(true);
      //                break;
      SortOrder.PLAYLIST_A_Z -> subMenu.findItem(R.id.action_sort_order_playlist_name).isChecked = true
      SortOrder.PLAYLIST_Z_A -> subMenu.findItem(R.id.action_sort_order_playlist_name_desc).isChecked = true
      SortOrder.PLAYLIST_DATE -> subMenu.findItem(R.id.action_sort_order_playlist_date).isChecked = true
      SortOrder.TRACK_NUMBER -> subMenu.findItem(R.id.action_sort_order_track_number).isChecked = true
      SortOrder.PLAYLIST_SONG_CUSTOM -> subMenu.findItem(R.id.action_sort_order_custom).isChecked = true
    }
  }

  private fun tintMenuIcon(menu: Menu) {
    ToolbarContentTintHelper.handleOnCreateOptionsMenu(this, toolbar, menu, getToolbarBackgroundColor(toolbar))
  }

  protected open fun saveSortOrder(sortOrder: String) {

  }


}
