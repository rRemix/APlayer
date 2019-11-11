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
      startActivity(Intent(mContext, SearchActivity::class.java))
      return true
    } else if (item.itemId == R.id.action_timer) {
      TimerDialog.newInstance()
          .show(supportFragmentManager, TimerDialog::class.java.simpleName)
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
        R.id.action_sort_order_display_title -> {
          sortOrder = SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_A_Z
          item.isChecked = true
        }
        R.id.action_sort_order_display_title_desc -> {
          sortOrder = SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_Z_A
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
        R.id.action_sort_order_date_desc -> {
          sortOrder = SortOrder.SongSortOrder.SONG_DATE_DESC
          item.isChecked = true
        }
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
    super.onCreateOptionsMenu(menu)
    menuInflater.inflate(getMenuLayoutId(), menu)
    tintMenuIcon(menu)
    return true
  }

  protected fun setUpMenuItem(menu: Menu, sortOrder: String) {
    val subMenu = menu.findItem(R.id.action_sort_order).subMenu
    when (sortOrder) {
      SortOrder.SongSortOrder.SONG_A_Z -> subMenu.findItem(R.id.action_sort_order_title).isChecked = true
      SortOrder.SongSortOrder.SONG_Z_A -> subMenu.findItem(R.id.action_sort_order_title_desc).isChecked = true
      SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_A_Z -> subMenu.findItem(R.id.action_sort_order_display_title).isChecked = true
      SortOrder.SongSortOrder.SONG_DISPLAY_TITLE_Z_A -> subMenu.findItem(R.id.action_sort_order_display_title_desc).isChecked = true
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

  private fun tintMenuIcon(menu: Menu) {
    ToolbarContentTintHelper.handleOnCreateOptionsMenu(this, toolbar, menu, getToolbarBackgroundColor(toolbar))
  }

  protected open fun saveSortOrder(sortOrder: String?) {

  }


}
