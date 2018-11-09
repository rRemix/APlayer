package remix.myplayer.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.lyric.LrcView
import remix.myplayer.lyric.SearchLrc
import remix.myplayer.misc.interfaces.OnInflateFinishListener
import remix.myplayer.ui.fragment.base.BaseMusicFragment
import remix.myplayer.util.LogUtil
import remix.myplayer.util.SPUtil
import remix.myplayer.util.ToastUtil
import java.util.*

/**
 * Created by Remix on 2015/12/2.
 */

/**
 * 歌词界面Fragment
 */
class LyricFragment : BaseMusicFragment(), Runnable {
    private var onFindListener: OnInflateFinishListener? = null
    private var info: Song? = null
    @BindView(R.id.lrcView)
    lateinit var lrcView: LrcView
    @BindView(R.id.offsetContainer)
    lateinit var offsetContainer: View

    private var disposable: Disposable? = null

    fun setOnInflateFinishListener(l: OnInflateFinishListener) {
        onFindListener = l
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPageName = LyricFragment::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_lrc, container, false)
        mUnBinder = ButterKnife.bind(this, rootView)

        onFindListener?.onViewInflateFinish(lrcView)

        return rootView
    }

    override fun onDestroyView() {
        offsetContainer.removeCallbacks(this)
        disposable?.dispose()
        super.onDestroyView()
    }

    @JvmOverloads
    fun updateLrc(song: Song, clearCache: Boolean = false) {
        info = song
        getLrc("", clearCache)
    }

    fun updateLrc(lrcPath: String) {
        getLrc(lrcPath, true)
    }

    private fun getLrc(manualPath: String, clearCache: Boolean) {
        if (!isVisible)
            return
        if (info == null) {
            lrcView.setText(getStringSafely(R.string.no_lrc))
            return
        }
        if (clearCache) {
            //清除offset
            SPUtil.putValue(App.getContext(), SPUtil.LYRIC_OFFSET_KEY.NAME, info?.id.toString() + "", 0)
            lrcView.setOffset(0)
        }
        val id = info?.id
        disposable = SearchLrc(info!!).getLyric(manualPath, clearCache)
                .doOnSubscribe { lrcView.setText(getStringSafely(R.string.searching)) }
                .subscribe(Consumer {
                    if (id == info?.id) {
                        if (it == null || it.isEmpty()) {
                            lrcView.setText(getStringSafely(R.string.no_lrc))
                            return@Consumer
                        }
                        lrcView.setOffset(SPUtil.getValue(mContext, SPUtil.LYRIC_OFFSET_KEY.NAME, info?.id.toString() + "", 0))
                        lrcView.lrcRows = it
                    }
                }, Consumer {
                    LogUtil.e(it)
                    if (id == info?.id) {
                        lrcView.lrcRows = null
                        lrcView.setText(getStringSafely(R.string.no_lrc))
                    }
                })
    }

    @OnClick(R.id.offsetReduce, R.id.offsetAdd, R.id.offsetReset)
    internal fun onClick(view: View) {
        offsetContainer.removeCallbacks(this)
        offsetContainer.postDelayed(this, DELAY_HIDE.toLong())

        val originalOffset = SPUtil.getValue(mContext, SPUtil.LYRIC_OFFSET_KEY.NAME, info?.id.toString() + "", 0)
        var newOffset = originalOffset
        when (view.id) {
            R.id.offsetReset -> {
                newOffset = 0
                ToastUtil.show(mContext, R.string.lyric_offset_reset)
            }
            R.id.offsetAdd -> newOffset += 500
            R.id.offsetReduce -> newOffset -= 500
        }
        if (originalOffset != newOffset) {
            SPUtil.putValue(mContext, SPUtil.LYRIC_OFFSET_KEY.NAME, info?.id.toString() + "", newOffset)
            if (newOffset != 0 && Math.abs(newOffset) <= 60000) {//最大偏移60s
                ToastUtil.show(mContext, if (newOffset > 0) R.string.lyric_advance_x_second else R.string.lyric_delay_x_second,
                        String.format(Locale.getDefault(), "%.1f", newOffset / 1000f))
            }
            lrcView.setOffset(newOffset)
        }

    }

    fun showLyricOffsetView() {
        if (lrcView.lrcRows == null || lrcView.lrcRows.isEmpty()) {
            ToastUtil.show(mContext, R.string.no_lrc)
            return
        }
        offsetContainer.visibility = View.VISIBLE
        offsetContainer.postDelayed(this, DELAY_HIDE.toLong())
    }

    override fun run() {
        offsetContainer.visibility = View.GONE
    }

    companion object {
        private const val DELAY_HIDE = 5000
    }

}
