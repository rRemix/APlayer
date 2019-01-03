package remix.myplayer.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.View
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.theme.TintHelper
import remix.myplayer.ui.adapter.CustomSortAdapter
import remix.myplayer.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView
import remix.myplayer.util.ColorUtil.isColorLight
import remix.myplayer.util.PlayListUtil
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util
import java.util.*
import kotlin.collections.ArrayList

class CustomSortActivity : ToolbarActivity() {
    @BindView(R.id.custom_sort_recyclerView)
    lateinit var mRecyclerView: FastScrollRecyclerView

    lateinit var mAdapter: CustomSortAdapter
    lateinit var mMDDialog: MaterialDialog

    private var mInfoList: List<Song>? = null
    private var mPlayListID: Int = 0
    private var mPlayListName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_sort)
        ButterKnife.bind(this@CustomSortActivity)

        mPlayListID = intent.getIntExtra(EXTRA_ID, -1)
        mPlayListName = intent.getStringExtra(EXTRA_NAME)
        mInfoList = intent.getSerializableExtra(EXTRA_LIST) as List<Song>

        setUpToolbar(mPlayListName)

        mAdapter = CustomSortAdapter(mContext, R.layout.item_custom_sort)
        mAdapter.setData(mInfoList)
        mAdapter.setOnItemClickListener(object : OnItemClickListener {
            override fun onItemLongClick(view: View?, position: Int) {
                Util.vibrate(mContext, 100)
            }

            override fun onItemClick(view: View?, position: Int) {

            }

        })

        ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val dragFlag = ItemTouchHelper.LEFT or ItemTouchHelper.DOWN or ItemTouchHelper.UP or ItemTouchHelper.RIGHT
                return makeMovementFlags(dragFlag, 0)
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                Collections.swap(mAdapter.datas, if (viewHolder.adapterPosition >= 0) viewHolder.adapterPosition else 0,
                        if (target.adapterPosition >= 0) target.adapterPosition else 0)
                mAdapter.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

            }


        }).attachToRecyclerView(mRecyclerView)

        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mRecyclerView.itemAnimator = DefaultItemAnimator()
        mRecyclerView.adapter = mAdapter


        val accentColor = ThemeStore.getAccentColor()
        mRecyclerView.setBubbleColor(accentColor)
        mRecyclerView.setHandleColor(accentColor)
        mRecyclerView.setBubbleTextColor(resources.getColor(if (isColorLight(accentColor))
            R.color.light_text_color_primary
        else
            R.color.dark_text_color_primary))

        mMDDialog = Theme.getBaseDialog(mContext)
                .title(R.string.saveing)
                .content(R.string.please_wait)
                .progress(true, 0)
                .progressIndeterminateStyle(false).build()

        TintHelper.setTintAuto(findViewById<FloatingActionButton>(R.id.custom_sort_save), accentColor, true)
    }


    @OnClick(R.id.custom_sort_save)
    fun onClick() {
        doAsync {
            uiThread {
                mMDDialog.show()
            }
            Thread.sleep(1000)
            val result = PlayListUtil.clearTable(mPlayListName) + PlayListUtil.addMultiSongs(mInfoList?.map { it.Id }, mPlayListName, mPlayListID)
            uiThread {
                ToastUtil.show(mContext, if (result > 0) R.string.save_success else R.string.save_error)
                mMDDialog.dismiss()
                finish()
            }
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context, id: Int, name: String, list: List<Song>) {
            context.startActivity(Intent(context, CustomSortActivity::class.java)
                    .putExtra(EXTRA_ID, id)
                    .putExtra(EXTRA_NAME, name)
                    .putExtra(EXTRA_LIST, ArrayList(list)))
        }

        private const val EXTRA_ID = "id"
        private const val EXTRA_NAME = "name"
        private const val EXTRA_LIST = "list"
    }

}

