package remix.myplayer.ui.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.QueryProductDetailsParams.Product
import kotlinx.coroutines.launch
import remix.myplayer.App
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

class SupportActivity : ToolbarActivity(), BillingClientStateListener, PurchasesUpdatedListener,
  ProductDetailsResponseListener {
  private lateinit var binding: ActivitySupportDevelopBinding

  private val adapter: PurchaseAdapter by lazy {
    PurchaseAdapter(R.layout.item_support)
  }

  private val SKU_IDS = arrayListOf("price_3", "price_8", "price_15", "price_25", "price_40")

  private val billingClient by lazy {
    BillingClient.newBuilder(this)
      .enablePendingPurchases()
      .setListener(this)
      .build()
  }
  private val productDetails = ArrayList<ProductDetails>()
  private val googlePlay = App.IS_GOOGLEPLAY

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
          launchBillingFlow(productDetails.getOrNull(position) ?: return)
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
              launchBillingFlow(productDetails.getOrNull(position - 3) ?: return)
            }
          }
        }

      }
    }

    binding.recyclerView.layoutManager = GridLayoutManager(this, 2)
    binding.recyclerView.adapter = adapter

//    loading = Theme.getBaseDialog(this)
//      .title(R.string.loading)
//      .content(R.string.please_wait)
//      .canceledOnTouchOutside(false)
//      .progress(true, 0)
//      .progressIndeterminateStyle(false).build()

    billingClient.startConnection(this)

    if (!googlePlay) {
      binding.ad.visibility = View.VISIBLE
      binding.adContent.text = """
        如果你的手机并未下载过"快手极速版"，并且是新用户，可通过以下步骤支持开发者：
        一.在安卓应用商店或AppStore下载"快手极速版"
        二.注册并登陆账号(未登录过的微信\QQ\手机号码任意一种方式均可)
        三.点左上角"三"或者放大镜扫描下方二维码(可长按保存)
        四.半小时之内观看至少一分钟的视频即可帮助开发者获得奖励。另外，如果用户在第二天和连续七天观看一分钟的视频，开发者都可以获得奖励
      """.trimIndent()
      binding.adQrcode.setOnLongClickListener {
        launch {
          Util.saveToAlbum(this@SupportActivity, R.drawable.ad_qrcode, "a_ad_qrCode.png")
        }
        return@setOnLongClickListener true
      }
    } else {
      binding.ad.visibility = View.GONE
    }
  }

  private fun launchBillingFlow(productDetails: ProductDetails) {
    billingClient.launchBillingFlow(
      this, BillingFlowParams.newBuilder()
        .setProductDetailsParamsList(
          listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
              .setProductDetails(productDetails)
              .build()
          )
        )
        .build()
    )
  }

  override fun onBillingSetupFinished(billingResult: BillingResult) {
    Timber.v("onBillingSetupFinished: $billingResult")
    if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
      return
    }

    // query product details
    val params = QueryProductDetailsParams.newBuilder()
      .setProductList(SKU_IDS.map {
        Product.newBuilder()
          .setProductId(it)
          .setProductType(ProductType.INAPP)
          .build()
      })
      .build()
    billingClient.queryProductDetailsAsync(params, this)

    // query purchases
//    billingClient.queryPurchasesAsync(
//      QueryPurchasesParams.newBuilder().build()
//    ) { billingResult1, purchases ->
//      if (billingResult1.responseCode == BillingClient.BillingResponseCode.OK && purchases.isNotEmpty()) {
//        purchases.forEach {
//          handlePurchase(it)
//        }
//      }
//    }
  }

  override fun onBillingServiceDisconnected() {
    Timber.v("onBillingServiceDisconnected")
  }

  override fun onProductDetailsResponse(
    result: BillingResult,
    details: MutableList<ProductDetails>
  ) {
    if (details.isEmpty()) {
      return
    }
    val beans = ArrayList<Purchase>()
    details.sortWith { o1, o2 ->
      o1!!.oneTimePurchaseOfferDetails!!.priceAmountMicros.compareTo(o2!!.oneTimePurchaseOfferDetails!!.priceAmountMicros)
    }
    productDetails.addAll(details)
    details.forEach {
      beans.add(
        Purchase(
          it.productId,
          "",
          it.title,
          it.oneTimePurchaseOfferDetails!!.formattedPrice
        )
      )
    }
    adapter.dataList.addAll(beans)
    binding.recyclerView.post {
      adapter.notifyDataSetChanged()
    }
  }

  override fun onPurchasesUpdated(
    billingResult: BillingResult,
    purchases: MutableList<com.android.billingclient.api.Purchase>?
  ) {
    Timber.v("onPurchasesUpdated: $billingResult")
    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
      for (purchase in purchases) {
        handlePurchase(purchase)
      }
    } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
      Timber.v("user cancel")
    } else {
      Timber.v("other error")
//      if (googlePlay) {
//        ToastUtil.show(
//          this,
//          R.string.error_occur,
//          "code = ${billingResult.responseCode} msg = ${billingResult.debugMessage}"
//        )
//      }
    }
  }

  private fun handlePurchase(purchase: com.android.billingclient.api.Purchase) {
    billingClient.consumeAsync(
      ConsumeParams.newBuilder()
        .setPurchaseToken(purchase.purchaseToken)
        .build()
    ) { result, purchaseToken ->
      Timber.v("handlePurchase, result: $result")
      if (result.responseCode == BillingClient.BillingResponseCode.OK) {
        ToastUtil.show(this@SupportActivity, R.string.thank_you)
      } else {
        ToastUtil.show(this@SupportActivity, R.string.payment_failure)
      }
    }
  }
}