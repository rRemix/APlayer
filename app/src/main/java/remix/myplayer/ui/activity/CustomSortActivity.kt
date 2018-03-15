package remix.myplayer.ui.activity

import android.os.Bundle
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import butterknife.BindView
import butterknife.ButterKnife
import remix.myplayer.R
import remix.myplayer.adapter.CustomSortAdapter
import remix.myplayer.bean.mp3.Song
import remix.myplayer.theme.ThemeStore
import remix.myplayer.ui.customview.fastcroll_recyclerview.FastScrollRecyclerView
import remix.myplayer.util.ColorUtil
import remix.myplayer.util.LogUtil
import java.util.*

class CustomSortActivity : ToolbarActivity() {
    @BindView(R.id.custom_sort_recyclerView)
    lateinit private var mRecyclerView: FastScrollRecyclerView
    lateinit private var mAdapter: CustomSortAdapter

    private var mInfoList: List<Song>? = null
    private var mPlayListID: Int = 0
    private var mPlayListName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_sort)
        ButterKnife.bind(this@CustomSortActivity)

        mPlayListID = intent.getIntExtra("id",-1)
        mPlayListName = intent.getStringExtra("name")
        mInfoList = intent.getSerializableExtra("list") as List<Song>

        setUpToolbar(findViewById(R.id.toolbar),mPlayListName)

        mAdapter = CustomSortAdapter(mContext,R.layout.activity_custom_sort)
        mAdapter.setHasStableIds(true)
        mAdapter.setData(mInfoList)

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val dragFlag = ItemTouchHelper.LEFT or ItemTouchHelper.DOWN or ItemTouchHelper.UP or ItemTouchHelper.RIGHT
                return makeMovementFlags(dragFlag, 0)
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                LogUtil.d("ChildHolderAdapter", "from: " + viewHolder.adapterPosition + " to: " + target.adapterPosition)
                Collections.swap(mAdapter.datas, viewHolder.adapterPosition - 1, viewHolder.adapterPosition - 1)
                mAdapter.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

            }
        })
        itemTouchHelper.attachToRecyclerView(mRecyclerView)

        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mRecyclerView.itemAnimator = DefaultItemAnimator()
        mRecyclerView.adapter = mAdapter
        mRecyclerView.setBubbleTextColor(if (ThemeStore.isLightTheme())
            ColorUtil.getColor(R.color.white)
        else
            ThemeStore.getTextColorPrimary())

    }


}

