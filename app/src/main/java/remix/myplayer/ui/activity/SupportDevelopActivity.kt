package remix.myplayer.ui.activity

import android.app.Activity
import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.View
import butterknife.BindView
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import remix.myplayer.R
import remix.myplayer.adapter.PurchaseAdapter
import remix.myplayer.bean.PurchaseBean
import remix.myplayer.interfaces.OnItemClickListener
import remix.myplayer.util.AlipayUtil
import java.util.*

class SupportDevelopActivity : ToolbarActivity() {
    @BindView(R.id.toolbar)
    lateinit var mToolBar: Toolbar
    @BindView(R.id.activity_support_recyclerView)
    lateinit var mRecyclerView: RecyclerView

    lateinit var mAdapter: PurchaseAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_support_develop)

        ButterKnife.bind(this)

        setUpToolbar(mToolBar,getString(R.string.support_develop))

        val beans = ArrayList<PurchaseBean>()
        for (i in 0..9) {
            when (i) {
                0 -> beans.add(PurchaseBean(i.toString(),"icon_wechat_donate",getString(R.string.wechat),"p"))
                1 -> beans.add(PurchaseBean(i.toString(),"icon_alipay_donate",getString(R.string.alipay),"p"))
                else -> beans.add(PurchaseBean(i.toString(),"http://i4.hexunimg.cn/2012-07-28/144092266.jpg","title","p"))
            }
        }

        mAdapter = PurchaseAdapter(mContext,R.layout.item_support)
        mAdapter.setData(beans)
        mAdapter.setOnItemClickListener(object : OnItemClickListener{
            override fun onItemLongClick(view: View?, position: Int) {
                when(position){
                    0 -> {
                        //保存微信图片
                    }
                    1 -> {
                        MaterialDialog.Builder(mContext)
                                .title(R.string.support_develop)
                                .titleColorAttr(R.attr.text_color_primary)
                                .positiveText(R.string.jump_alipay_account)
                                .negativeText(R.string.cancel)
                                .content(R.string.donate_tip)
                                .onPositive { _, _ -> AlipayUtil.startAlipayClient(mContext as Activity, "FKX01908X8ECOECIQZIL43") }
                                .backgroundColorAttr(R.attr.background_color_3)
                                .positiveColorAttr(R.attr.text_color_primary)
                                .negativeColorAttr(R.attr.text_color_primary)
                                .contentColorAttr(R.attr.text_color_primary)
                                .show()
                    }
                    else ->{

                    }
                }
            }

            override fun onItemClick(view: View?, position: Int) {

            }

        })

        mRecyclerView.layoutManager = GridLayoutManager(mContext,2)
        mRecyclerView.adapter = mAdapter
    }

}