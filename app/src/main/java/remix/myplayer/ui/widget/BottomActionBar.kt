package remix.myplayer.ui.widget

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.app.ActivityOptionsCompat
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.bottom_actionbar.view.*
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.misc.AnimationUrl
import remix.myplayer.bean.mp3.Song
import remix.myplayer.request.LibraryUriRequest
import remix.myplayer.request.RequestConfig
import remix.myplayer.service.MusicService
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.activity.PlayerActivity
import remix.myplayer.ui.activity.base.BaseActivity
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.DensityUtil
import remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType
import remix.myplayer.util.LogUtil
import remix.myplayer.util.ToastUtil
import java.lang.ref.WeakReference

class BottomActionBar @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(context, attrs, defStyleAttr) {
    //保存封面位置信息
    private var mCoverRect: Rect? = null
    //图片路径
    private val mAnimUrl = AnimationUrl()
    //手势检测
    private var mGestureDetector: GestureDetector

    init {
        View.inflate(context, R.layout.bottom_actionbar,this)
        //设置整个背景着色
        Theme.TintDrawable(bottom_action_content,
                R.drawable.commom_playercontrols_bg,
                ColorUtil.getColor(if (ThemeStore.THEME_MODE == ThemeStore.DAY) R.color.day_background_color_3 else R.color.night_background_color_3))
        Theme.TintDrawable(playbar_next,
                R.drawable.bf_btn_next,
                ColorUtil.getColor(if (ThemeStore.THEME_MODE == ThemeStore.DAY) R.color.black_323335 else R.color.white))
        //手势检测
        mGestureDetector = GestureDetector(context, GestureListener(this))
        bottom_action_content.setOnTouchListener { v, event -> mGestureDetector.onTouchEvent(event) }

        //获取封面位置信息
        bottom_action_bar_cover.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                bottom_action_bar_cover.viewTreeObserver.removeOnPreDrawListener(this)
                //数据失效重新获取位置信息
                if (mCoverRect == null || bottom_action_bar_cover.width <= 0 || bottom_action_bar_cover.height <= 0) {
                    mCoverRect = Rect()
                    bottom_action_bar_cover.getGlobalVisibleRect(mCoverRect)
                }
                return true
            }
        })
    }

    //更新界面
    fun updateBottomStatus(song: Song?, isPlaying: Boolean) {
        if (song == null)
            return
        //歌曲名 艺术家
        if (bottom_title != null)
            bottom_title.text = song.title
        if (bottom_artist != null)
            bottom_artist.text = song.artist
        //封面
        if (bottom_action_bar_cover != null)
            object : LibraryUriRequest(bottom_action_bar_cover,
                    getSearchRequestWithAlbumType(song),
                    RequestConfig.Builder(SMALL_IMAGE_SIZE, SMALL_IMAGE_SIZE).build()) {
                override fun onSuccess(result: String?) {
                    super.onSuccess(result)
                    mAnimUrl.albumId = song.albumId
                    mAnimUrl.url = result
                }

                override fun onError(errMsg: String) {
                    super.onError(errMsg)
                    mAnimUrl.albumId = -1
                    mAnimUrl.url = ""
                }
            }.load()
        //设置按钮着色
        if (playbar_play == null)
            return
        if (isPlaying) {
            Theme.TintDrawable(playbar_play,
                    R.drawable.bf_btn_stop,
                    ColorUtil.getColor(if (ThemeStore.THEME_MODE == ThemeStore.DAY) R.color.black_323335 else R.color.white))
        } else {
            Theme.TintDrawable(playbar_play,
                    R.drawable.bf_btn_play,
                    ColorUtil.getColor(if (ThemeStore.THEME_MODE == ThemeStore.DAY) R.color.black_323335 else R.color.white))
        }
    }

    fun startPlayerActivity() {
        if (MusicService.getCurrentMP3() == null)
            return
        val intent = Intent(context, PlayerActivity::class.java)
        val bundle = Bundle()
        intent.putExtras(bundle)
        intent.putExtra("FromActivity", true)
        intent.putExtra("Rect", mCoverRect)
        intent.putExtra("AnimUrl", mAnimUrl)

        try {
            val activity = context as Activity
            if (!(activity as BaseActivity).isDestroyed) {
                val options = ActivityOptionsCompat
                        .makeSceneTransitionAnimation(activity, bottom_action_bar_cover, "image")
                ActivityCompat.startActivity(context, intent, options.toBundle())
            }
        }catch (e : ClassCastException){
             ToastUtil.show(context, "跳转失败: $e")
        }

    }

    internal class GestureListener(bottomActionBar: BottomActionBar) : GestureDetector.SimpleOnGestureListener() {
        private val mReference: WeakReference<BottomActionBar> = WeakReference(bottomActionBar)

        override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            LogUtil.e(TAG, "onFling  " + "Y1: " + e1.y + " Y2: " + e2.y)
            if (velocityY < 0 && e1.y - e2.y > Y_THRESHOLD)
                mReference.get()?.startPlayerActivity()
            return true
        }

        companion object {
            private val TAG = "GestureListener"
            private val Y_THRESHOLD = DensityUtil.dip2px(App.getContext(), 10f)
        }
    }
}
