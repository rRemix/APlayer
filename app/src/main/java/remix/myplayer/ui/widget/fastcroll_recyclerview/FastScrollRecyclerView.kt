/*
 * Copyright 2016 L4 Digital LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package remix.myplayer.ui.widget.fastcroll_recyclerview

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView

open class FastScrollRecyclerView : RecyclerView {
  private lateinit var fastScroller: FastScroller

  constructor(context: Context) : super(context) {
    layout(context, null)
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
  }

  @JvmOverloads
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) : super(context, attrs, defStyleAttr) {
    layout(context, attrs)
  }

  private fun layout(context: Context, attrs: AttributeSet?) {
    fastScroller = FastScroller(context, attrs)
  }

  override fun setAdapter(adapter: Adapter<*>?) {
    super.setAdapter(adapter)
    if (adapter is FastScroller.SectionIndexer) {
      fastScroller.setSectionIndexer(adapter as FastScroller.SectionIndexer?)
    } else {
      fastScroller.setSectionIndexer(null)
    }
  }

  /**
   * Set the enabled state of fast scrolling.
   *
   * @param enabled True to enable fast scrolling, false otherwise
   */
  fun setFastScrollEnabled(enabled: Boolean) {
    fastScroller.isEnabled = enabled
  }

  /**
   * Hide the scrollbar when not scrolling.
   *
   * @param hideScrollbar True to hide the scrollbar, false to show
   */
  fun setHideScrollbar(hideScrollbar: Boolean) {
    fastScroller.setHideScrollbar(hideScrollbar)
  }

  /**
   * Display a scroll track while scrolling.
   *
   * @param visible True to show scroll track, false to hide
   */
  fun setTrackVisible(visible: Boolean) {
    fastScroller.setTrackVisible(visible)
  }

  /**
   * Set the color of the scroll track.
   *
   * @param color The color for the scroll track
   */
  fun setTrackColor(@ColorInt color: Int) {
    fastScroller.setTrackColor(color)
  }

  /**
   * Set the color for the scroll handle.
   *
   * @param color The color for the scroll handle
   */
  fun setHandleColor(@ColorInt color: Int) {
    fastScroller.setHandleColor(color)
  }

  /**
   * Set the background color of the index bubble.
   *
   * @param color The background color for the index bubble
   */
  fun setBubbleColor(@ColorInt color: Int) {
    fastScroller.setBubbleColor(color)
  }

  /**
   * Set the text color of the index bubble.
   *
   * @param color The text color for the index bubble
   */
  fun setBubbleTextColor(@ColorInt color: Int) {
    fastScroller.setBubbleTextColor(color)
  }

  /**
   * Set the fast scroll state change listener.
   *
   * @param fastScrollStateChangeListener The interface that will listen to fastscroll state change events
   */
  fun setFastScrollStateChangeListener(
      fastScrollStateChangeListener: FastScrollStateChangeListener?) {
    fastScroller.setFastScrollStateChangeListener(fastScrollStateChangeListener)
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    fastScroller.attachRecyclerView(this)
    val parent = parent
    if (parent is ViewGroup) {
      parent.removeView(fastScroller)
      parent.addView(fastScroller)
      fastScroller.setLayoutParams(parent)
    }
  }

  override fun onDetachedFromWindow() {
    fastScroller.detachRecyclerView()
    super.onDetachedFromWindow()
  }
}