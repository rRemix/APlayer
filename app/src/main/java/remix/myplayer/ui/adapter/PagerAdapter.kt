package remix.myplayer.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import java.util.ArrayList

/**
 * Created by Remix on 2015/11/29.
 */
/**
 * ViewPager的适配器
 */
class PagerAdapter(fm: FragmentManager?) : FragmentPagerAdapter(fm!!) {
  private val fragmentList: MutableList<Fragment> = ArrayList()
  private var titles: MutableList<String> = ArrayList()

  fun addFragment(fragment: Fragment) {
    fragmentList.add(fragment)
  }

  fun setTitles(titles: MutableList<String>) {
    this.titles = titles
  }

  fun addTitle(title: String) {
    titles.add(title)
  }

  override fun getPageTitle(position: Int): CharSequence? {
    return titles[position]
  }

  override fun getItem(position: Int): Fragment {
    return fragmentList[position]
  }

  override fun getCount(): Int {
    return fragmentList.size
  }
}