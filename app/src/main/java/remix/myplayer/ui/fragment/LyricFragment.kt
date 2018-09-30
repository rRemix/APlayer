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
    private var mOnFindListener: OnInflateFinishListener? = null
    private var mInfo: Song? = null
    @BindView(R.id.lrcView)
    lateinit var mLrcView: LrcView
    @BindView(R.id.offsetContainer)
    lateinit var mOffsetContainer: View

    private var mDisposable: Disposable? = null

    fun setOnInflateFinishListener(l: OnInflateFinishListener) {
        mOnFindListener = l
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPageName = LyricFragment::class.java.simpleName
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater!!.inflate(R.layout.fragment_lrc, container, false)
        mUnBinder = ButterKnife.bind(this, rootView)

        mOnFindListener?.onViewInflateFinish(mLrcView)

        return rootView
    }

    override fun onDestroyView() {
        mOffsetContainer.removeCallbacks(this)
        mDisposable?.dispose()
        super.onDestroyView()
    }

    @JvmOverloads
    fun updateLrc(song: Song, clearCache: Boolean = false) {
        mInfo = song
        getLrc("", clearCache)
    }

    fun updateLrc(lrcPath: String) {
        getLrc(lrcPath, true)
    }

    private fun getLrc(manualPath: String, clearCache: Boolean) {
        if (!isVisible)
            return
        if (mInfo == null) {
            mLrcView.setText(getStringSafely(R.string.no_lrc))
            return
        }
        if (clearCache) {
            //清除offset
            SPUtil.putValue(App.getContext(), SPUtil.LYRIC_OFFSET_KEY.NAME, mInfo?.id.toString() + "", 0)
            mLrcView.setOffset(0)
        }
        val id = mInfo?.id
        mDisposable = SearchLrc(mInfo).getLyric(manualPath, clearCache)
                .doOnSubscribe { mLrcView.setText(getStringSafely(R.string.searching)) }
                .subscribe(Consumer {
                    if (id == mInfo?.id) {
                        if (it == null || it.size == 0) {
                            mLrcView.setText(getStringSafely(R.string.no_lrc))
                            return@Consumer
                        }
                        mLrcView.setOffset(SPUtil.getValue(mContext, SPUtil.LYRIC_OFFSET_KEY.NAME, mInfo?.id.toString() + "", 0))
                        mLrcView.lrcRows = it
                    }
                }, Consumer {
                    LogUtil.e(it)
                    if (id == mInfo?.id) {
                        mLrcView.lrcRows = null
                        mLrcView.setText(getStringSafely(R.string.no_lrc))
                    }
                })
    }

    @OnClick(R.id.offsetReduce, R.id.offsetAdd, R.id.offsetReset)
    internal fun onClick(view: View) {
        mOffsetContainer.removeCallbacks(this)
        mOffsetContainer.postDelayed(this, DELAY_HIDE.toLong())

        var original = SPUtil.getValue(mContext, SPUtil.LYRIC_OFFSET_KEY.NAME, mInfo?.id.toString() + "", 0)
        when (view.id) {
            R.id.offsetReset -> {
                original = 0
                ToastUtil.show(mContext, R.string.lyric_offset_reset)
            }
            R.id.offsetAdd -> original += 500
            R.id.offsetReduce -> original -= 500
        }
        SPUtil.putValue(mContext, SPUtil.LYRIC_OFFSET_KEY.NAME, mInfo?.id.toString() + "", original)
        if (original != 0 && Math.abs(original) <= 60000) {//最大偏移60s
            ToastUtil.show(mContext, if (original > 0) R.string.lyric_advance_x_second else R.string.lyric_delay_x_second,
                    String.format(Locale.getDefault(), "%.1f", original / 1000f))
        }

        mLrcView.setOffset(original)
    }

    fun showLyricOffsetView() {
        if (mLrcView.lrcRows == null || mLrcView.lrcRows.isEmpty()) {
            ToastUtil.show(mContext, R.string.no_lrc)
            return
        }
        mOffsetContainer.visibility = View.VISIBLE
        mOffsetContainer.postDelayed(this, DELAY_HIDE.toLong())
    }

    override fun run() {
        mOffsetContainer.visibility = View.GONE
    }

    companion object {
        private const val DELAY_HIDE = 5000
    }

}
