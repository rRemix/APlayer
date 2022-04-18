package remix.myplayer.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.PurchaseInfo
import com.anjlab.android.iab.v3.SkuDetails
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import kotlinx.android.synthetic.main.activity_support_develop.*
import remix.myplayer.App
import remix.myplayer.BuildConfig
import remix.myplayer.R
import remix.myplayer.bean.misc.Purchase
import remix.myplayer.databinding.ActivitySupportDevelopBinding
import remix.myplayer.misc.cache.DiskCache
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.theme.Theme
import remix.myplayer.ui.adapter.PurchaseAdapter
import remix.myplayer.util.AlipayUtil
import remix.myplayer.util.RxUtil
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util
import timber.log.Timber
import java.io.File
import java.io.OutputStream
import java.util.*

class SupportActivity : ToolbarActivity(), BillingProcessor.IBillingHandler {
  private lateinit var binding: ActivitySupportDevelopBinding

  private val adapter: PurchaseAdapter by lazy {
    PurchaseAdapter(R.layout.item_support)
  }

  val SKU_IDS = arrayListOf("price_3", "price_8", "price_15", "price_25", "price_40")

  private var billingProcessor: BillingProcessor? = null
  private var disposable: Disposable? = null

  private lateinit var loading: MaterialDialog

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivitySupportDevelopBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setUpToolbar(getString(R.string.support_develop))

    val beans = ArrayList<Purchase>()
    if (!App.IS_GOOGLEPLAY) {
      beans.add(Purchase("wechat", "icon_wechat_donate", getString(R.string.wechat), ""))
      beans.add(Purchase("alipay", "icon_alipay_donate", getString(R.string.alipay), ""))
      beans.add(Purchase("paypal", "icon_paypal_donate", getString(R.string.paypal), ""))
    }

    adapter.setDataList(beans)
    adapter.onItemClickListener = object : OnItemClickListener {
      override fun onItemLongClick(view: View?, position: Int) {
      }

      override fun onItemClick(view: View?, position: Int) {
        if (App.IS_GOOGLEPLAY) {
          billingProcessor?.purchase(this@SupportActivity, SKU_IDS[position])
        } else {
          when (position) {
            0 -> {
              var outputStream: OutputStream? = null
              //保存微信图片
              Observable.just(BitmapFactory.decodeResource(resources, R.drawable.icon_wechat_qrcode))
                  .flatMap(Function {
                    return@Function ObservableSource<File> {
                      val weChatBitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_wechat_qrcode)
                      if (weChatBitmap == null || weChatBitmap.isRecycled) {
                        it.onError(Throwable("Invalid Bitmap"))
                        return@ObservableSource
                      }
                      val dir = DiskCache.getDiskCacheDir(this@SupportActivity, "qrCode")
                      if (!dir.exists())
                        dir.mkdirs()
                      val qrCodeFile = File(dir, "qrCode.png")

                      //删除旧文件
                      contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                          null, MediaStore.Images.Media.DATA + "=?", arrayOf(qrCodeFile.absolutePath), null)
                          ?.use { cursor ->
                            if (cursor.count > 0) {
                              contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + "=?", arrayOf(qrCodeFile.absolutePath))
                            }
                            if (qrCodeFile.exists()) {
                              qrCodeFile.delete()
                            }
                            qrCodeFile.createNewFile()

                            // 保存到系统MediaStore
                            val values = ContentValues()
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                              values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                            } else {
                              values.put(MediaStore.MediaColumns.DATA, qrCodeFile.absolutePath)
                            }
                            values.put(MediaStore.Images.ImageColumns.TITLE, "qrCode")
                            values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, "qrCode")
                            values.put(MediaStore.Images.ImageColumns.DATE_ADDED, System.currentTimeMillis() / 1000)
                            values.put(MediaStore.Images.ImageColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                            values.put(MediaStore.Images.ImageColumns.MIME_TYPE, "image/png")
                            values.put(MediaStore.Images.ImageColumns.WIDTH, weChatBitmap.width)
                            values.put(MediaStore.Images.ImageColumns.HEIGHT, weChatBitmap.height)
                            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                values)
                            if (uri == null) {
                              it.onError(Throwable("Uri Empty"))
                              return@ObservableSource
                            }
                            outputStream = contentResolver.openOutputStream(uri)
                            weChatBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                            values.clear()
                            values.put(MediaStore.Images.ImageColumns.SIZE, qrCodeFile.length())
                            contentResolver.update(uri, values, null, null)
                            weChatBitmap.recycle()
                            it.onNext(qrCodeFile)
                            it.onComplete()
                          }

                    }
                  })
                  .compose(RxUtil.applyScheduler())
                  .doFinally {
                    outputStream?.close()
                  }
                  .subscribe({
                    ToastUtil.show(this@SupportActivity, R.string.save_wechat_qrcode_success, it.absolutePath)
                  }, {
                    ToastUtil.show(this@SupportActivity, R.string.save_error)
                  })
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

    loading = Theme.getBaseDialog(this)
        .title(R.string.loading)
        .content(R.string.please_wait)
        .canceledOnTouchOutside(false)
        .progress(true, 0)
        .progressIndeterminateStyle(false).build()

    billingProcessor = BillingProcessor(this, BuildConfig.GOOGLE_PLAY_LICENSE_KEY, this)
  }

  private fun loadSkuDetails() {
    if (adapter.dataList.size > 3)
      return
    if (hasWindowFocus()) {
      loading.show()
    }

    billingProcessor?.getPurchaseListingDetailsAsync(SKU_IDS, object : BillingProcessor.ISkuDetailsResponseListener {
      override fun onSkuDetailsResponse(products: MutableList<SkuDetails>?) {
        loading.dismiss()
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
        ToastUtil.show(this@SupportActivity, R.string.error_occur, error)
        loading.dismiss()
      }
    })
  }

  override fun onBillingInitialized() {
    Timber.v("loadSkuDetails")
    loadSkuDetails()
  }

  override fun onPurchaseHistoryRestored() {
    Timber.v("onPurchaseHistoryRestored")
    Toast.makeText(this, R.string.restored_previous_purchases, Toast.LENGTH_SHORT).show()
  }

  @SuppressLint("CheckResult")
  override fun onProductPurchased(productId: String, details: PurchaseInfo?) {
    Timber.v("onProductPurchased")
    billingProcessor?.consumePurchaseAsync(productId,object : BillingProcessor.IPurchasesResponseListener{
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
    ToastUtil.show(this, R.string.error_occur, "code = $errorCode err =  $error")
  }

  override fun onDestroy() {
    super.onDestroy()
    billingProcessor?.release()
    disposable?.dispose()
    if (loading.isShowing)
      loading.dismiss()
  }
}