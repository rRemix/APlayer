package remix.myplayer.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.PurchaseInfo
import com.anjlab.android.iab.v3.SkuDetails
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_support_develop.*
import kotlinx.coroutines.launch
import remix.myplayer.App
import remix.myplayer.BuildConfig
import remix.myplayer.R
import remix.myplayer.bean.misc.Purchase
import remix.myplayer.databinding.ActivitySupportDevelopBinding
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.theme.Theme
import remix.myplayer.ui.adapter.PurchaseAdapter
import remix.myplayer.util.AlipayUtil
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util
import timber.log.Timber

class SupportActivity : ToolbarActivity(), BillingProcessor.IBillingHandler {
  private lateinit var binding: ActivitySupportDevelopBinding

  private val adapter: PurchaseAdapter by lazy {
    PurchaseAdapter(R.layout.item_support)
  }

  val SKU_IDS = arrayListOf("price_3", "price_8", "price_15", "price_25", "price_40")

  private var billingProcessor: BillingProcessor? = null
  private var disposable: Disposable? = null
  private val googlePlay = App.IS_GOOGLEPLAY

//  private lateinit var loading: MaterialDialog

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivitySupportDevelopBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setUpToolbar(getString(R.string.support_develop))

    val beans = ArrayList<Purchase>()
    if (!googlePlay) {
      beans.add(Purchase("wechat", "icon_wechat_donate", getString(R.string.wechat), ""))
      beans.add(Purchase("alipay", "icon_alipay_donate", getString(R.string.alipay), ""))
      beans.add(Purchase("paypal", "icon_paypal_donate", getString(R.string.paypal), ""))
    }

    adapter.setDataList(beans)
    adapter.onItemClickListener = object : OnItemClickListener {
      override fun onItemLongClick(view: View?, position: Int) {
      }

      override fun onItemClick(view: View?, position: Int) {
        if (googlePlay) {
          billingProcessor?.purchase(this@SupportActivity, SKU_IDS[position])
        } else {
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
            else -> {
              billingProcessor?.purchase(this@SupportActivity, SKU_IDS[position - 3])
            }
          }
        }

      }
    }

    recyclerView.layoutManager = GridLayoutManager(this, 2)
    recyclerView.adapter = adapter

//    loading = Theme.getBaseDialog(this)
//      .title(R.string.loading)
//      .content(R.string.please_wait)
//      .canceledOnTouchOutside(false)
//      .progress(true, 0)
//      .progressIndeterminateStyle(false).build()

    billingProcessor = BillingProcessor(this, BuildConfig.GOOGLE_PLAY_LICENSE_KEY, this)

    if (!googlePlay) {
      ad.visibility = View.VISIBLE
      ad_content.text = """
        如果你的手机并未下载过"快手极速版"，并且是新用户，可通过以下步骤支持开发者：
        一.在安卓应用商店或AppStore下载"快手极速版"
        二.注册并登陆账号(未登录过的微信\QQ\手机号码任意一种方式均可)
        三.点左上角"三"或者放大镜扫描下方二维码(可长按保存)
        四.半小时之内观看至少一分钟的视频即可帮助开发者获得奖励。另外，如果用户在第二天和连续七天观看一分钟的视频，开发者都可以获得奖励
      """.trimIndent()
      ad_qrcode.setOnLongClickListener {
        launch {
          Util.saveToAlbum(this@SupportActivity, R.drawable.ad_qrcode, "a_ad_qrCode.png")
        }
        return@setOnLongClickListener true
      }
    } else {
      ad.visibility = View.GONE
    }
  }

  private fun loadSkuDetails() {
    if (adapter.dataList.size > 3)
      return
//    if (hasWindowFocus()) {
//      loading.show()
//    }

    billingProcessor?.getPurchaseListingDetailsAsync(
      SKU_IDS,
      object : BillingProcessor.ISkuDetailsResponseListener {
        override fun onSkuDetailsResponse(products: MutableList<SkuDetails>?) {
//          loading.dismiss()
          if (products.isNullOrEmpty()) {
            return
          }
          val beans = ArrayList<Purchase>()
          products.sortWith { o1, o2 ->
            o1.priceValue.compareTo(o2.priceValue)
          }
          products.forEach {
            beans.add(Purchase(it.productId, "", it.title, it.priceText))
          }
          adapter.dataList.addAll(beans)
          adapter.notifyDataSetChanged()
        }

        override fun onSkuDetailsError(error: String?) {
          if (googlePlay) {
            ToastUtil.show(this@SupportActivity, R.string.error_occur, error)
          }
//          loading.dismiss()
        }
      })
  }

  override fun onBillingInitialized() {
    Timber.v("loadSkuDetails")
    loadSkuDetails()
  }

  override fun onPurchaseHistoryRestored() {
    Timber.v("onPurchaseHistoryRestored")
//    Toast.makeText(this, R.string.restored_previous_purchases, Toast.LENGTH_SHORT).show()
  }

  @SuppressLint("CheckResult")
  override fun onProductPurchased(productId: String, details: PurchaseInfo?) {
    Timber.v("onProductPurchased")
    billingProcessor?.consumePurchaseAsync(productId,
      object : BillingProcessor.IPurchasesResponseListener {
        override fun onPurchasesSuccess() {
          Toast.makeText(this@SupportActivity, R.string.thank_you, Toast.LENGTH_SHORT).show()
        }

        override fun onPurchasesError() {
          Toast.makeText(this@SupportActivity, R.string.payment_failure, Toast.LENGTH_SHORT).show()
        }

      })
  }

  override fun onBillingError(errorCode: Int, error: Throwable?) {
    Timber.v("onBillingError")
    if (googlePlay) {
      ToastUtil.show(this, R.string.error_occur, "code = $errorCode err =  $error")
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    billingProcessor?.release()
    disposable?.dispose()
//    if (loading.isShowing)
//      loading.dismiss()
  }
}