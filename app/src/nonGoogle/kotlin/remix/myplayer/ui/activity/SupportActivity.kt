package remix.myplayer.ui.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import kotlinx.coroutines.launch
import remix.myplayer.R
import remix.myplayer.bean.misc.Purchase
import remix.myplayer.databinding.ActivitySupportDevelopBinding
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.theme.Theme
import remix.myplayer.ui.adapter.PurchaseAdapter
import remix.myplayer.util.AlipayUtil
import remix.myplayer.util.Util

class SupportActivity : ToolbarActivity() {
    private lateinit var binding: ActivitySupportDevelopBinding

    private val adapter: PurchaseAdapter by lazy {
        PurchaseAdapter(R.layout.item_support)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySupportDevelopBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setUpToolbar(getString(R.string.support_develop))

        val beans = ArrayList<Purchase>()
        beans.add(Purchase("wechat", "icon_wechat_donate", getString(R.string.wechat), ""))
        beans.add(Purchase("alipay", "icon_alipay_donate", getString(R.string.alipay), ""))
        beans.add(Purchase("paypal", "icon_paypal_donate", getString(R.string.paypal), ""))

        adapter.setDataList(beans)
        adapter.onItemClickListener = object : OnItemClickListener {
            override fun onItemClick(view: View?, position: Int) {
                when (position) {
                    0 -> {
                        //保存微信图片
                        launch {
                            Util.saveToAlbum(
                                this@SupportActivity,
                                R.drawable.icon_wechat_qrcode,
                                "wechat_qrCode.png"
                            )
                        }
                    }

                    1 -> {
                        Theme.getBaseDialog(this@SupportActivity)
                            .title(R.string.support_develop)
                            .positiveText(R.string.jump_alipay_account)
                            .negativeText(R.string.cancel)
                            .content(R.string.donate_tip)
                            .onPositive { _, _ -> AlipayUtil.startAlipayClient(this@SupportActivity as Activity) }
                            .show()
                    }

                    2 -> {
                        val intent = Intent("android.intent.action.VIEW")
                        intent.data = Uri.parse("https://www.paypal.me/rRemix")
                        Util.startActivitySafely(this@SupportActivity, intent)
                    }
                }
            }

            override fun onItemLongClick(view: View?, position: Int) {
            }
        }

        binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
        binding.recyclerView.adapter = adapter

//        binding.ad.visibility = View.VISIBLE
//        binding.adContent.text = """
//        如果你的手机并未下载过"快手极速版"，并且是新用户，可通过以下步骤支持开发者：
//        一.在安卓应用商店或AppStore下载"快手极速版"
//        二.注册并登陆账号(未登录过的微信\QQ\手机号码任意一种方式均可)
//        三.点左上角"三"或者放大镜扫描下方二维码(可长按保存)
//        四.半小时之内观看至少一分钟的视频即可帮助开发者获得奖励。另外，如果用户在第二天和连续七天观看一分钟的视频，开发者都可以获得奖励
//        """.trimIndent()
//        binding.adQrcode.setOnLongClickListener {
//            launch {
//                Util.saveToAlbum(this@SupportActivity, R.drawable.ad_qrcode, "a_ad_qrCode.png")
//            }
//            return@setOnLongClickListener true
//        }
    }
}
