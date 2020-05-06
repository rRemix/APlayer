package remix.myplayer.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.Toast
import butterknife.BindView
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.TransactionDetails
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Function
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.schedulers.Schedulers
import remix.myplayer.App
import remix.myplayer.BuildConfig
import remix.myplayer.R
import remix.myplayer.bean.misc.PurchaseBean
import remix.myplayer.misc.cache.DiskCache
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.request.network.RxUtil
import remix.myplayer.theme.Theme
import remix.myplayer.ui.adapter.PurchaseAdapter
import remix.myplayer.util.AlipayUtil
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util
import timber.log.Timber
import java.io.File
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.util.*

class SupportDevelopActivity : ToolbarActivity(), BillingProcessor.IBillingHandler {
  @BindView(R.id.toolbar)
  lateinit var mToolBar: Toolbar
  @BindView(R.id.activity_support_recyclerView)
  lateinit var mRecyclerView: RecyclerView

  lateinit var mAdapter: PurchaseAdapter

  val SKU_IDS = arrayListOf("price_3", "price_8", "price_15", "price_25", "price_40")

  private var mBillingProcessor: BillingProcessor? = null
  private var mDisposable: Disposable? = null

  private lateinit var mLoading: MaterialDialog

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_support_develop)
    ButterKnife.bind(this)
    setUpToolbar(getString(R.string.support_develop))

    mAdapter = PurchaseAdapter(R.layout.item_support)

    val beans = ArrayList<PurchaseBean>()
    if (!App.IS_GOOGLEPLAY) {
      beans.add(PurchaseBean("wechat", "icon_wechat_donate", getString(R.string.wechat), ""))
      beans.add(PurchaseBean("alipay", "icon_alipay_donate", getString(R.string.alipay), ""))
      beans.add(PurchaseBean("paypal", "icon_paypal_donate", getString(R.string.paypal), ""))
    }

    mAdapter.setData(beans)
    mAdapter.setOnItemClickListener(object : OnItemClickListener {
      override fun onItemLongClick(view: View?, position: Int) {
      }

      override fun onItemClick(view: View?, position: Int) {
        if (App.IS_GOOGLEPLAY) {
          mBillingProcessor?.purchase(this@SupportDevelopActivity, SKU_IDS[position])
        } else {
          when (position) {
            0 -> {
              var outputStream: OutputStream? = null
              var cursor: Cursor? = null
              //保存微信图片
              Observable.just(BitmapFactory.decodeResource(resources, R.drawable.icon_wechat_qrcode))
                  .flatMap(Function<Bitmap, ObservableSource<File>> {
                    return@Function ObservableSource<File> {
                      val weChatBitmap = BitmapFactory.decodeResource(resources, R.drawable.icon_wechat_qrcode)
                      if (weChatBitmap == null || weChatBitmap.isRecycled) {
                        it.onError(Throwable("Invalid Bitmap"))
                        return@ObservableSource
                      }
                      val dir = DiskCache.getDiskCacheDir(mContext, "qrCode")
                      if (!dir.exists())
                        dir.mkdirs()
                      val qrCodeFile = File(dir, "qrCode.png")

                      //删除旧文件
                      cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                          null, MediaStore.Images.Media.DATA + "=?", arrayOf(qrCodeFile.absolutePath), null)
                      if (cursor != null && cursor!!.count > 0) {
                        contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Images.Media.DATA + "=?", arrayOf(qrCodeFile.absolutePath))
                      }
                      if (qrCodeFile.exists()) {
                        qrCodeFile.delete()
                      }
                      qrCodeFile.createNewFile()

                      // 保存到系统MediaStore
                      val values = ContentValues()
                      values.put(MediaStore.Images.ImageColumns.DATA, qrCodeFile.absolutePath)
                      values.put(MediaStore.Images.ImageColumns.TITLE, "qrCode")
                      values.put(MediaStore.Images.ImageColumns.DISPLAY_NAME, "qrCode")
                      values.put(MediaStore.Images.ImageColumns.DATE_TAKEN, System.currentTimeMillis())
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
                  })
                  .compose(RxUtil.applyScheduler())
                  .doFinally {
                    cursor?.close()
                    outputStream?.close()
                  }
                  .subscribe({
                    ToastUtil.show(mContext, R.string.save_wechat_qrcode_success, it.absolutePath)
                  }, {
                    ToastUtil.show(mContext, R.string.save_error)
                  })
            }
            1 -> {
              Theme.getBaseDialog(mContext)
                  .title(R.string.support_develop)
                  .positiveText(R.string.jump_alipay_account)
                  .negativeText(R.string.cancel)
                  .content(R.string.donate_tip)
                  .onPositive { _, _ -> AlipayUtil.startAlipayClient(mContext as Activity) }
                  .show()
            }
            2 -> {
              val intent = Intent("android.intent.action.VIEW")
              intent.data = Uri.parse("https://www.paypal.me/rRemix")
              Util.startActivitySafely(this@SupportDevelopActivity, intent)
            }
            else -> {
              mBillingProcessor?.purchase(this@SupportDevelopActivity, SKU_IDS[position - 3])
            }
          }
        }

      }
    })

    mRecyclerView.layoutManager = GridLayoutManager(mContext, 2)
    mRecyclerView.adapter = mAdapter

    mLoading = Theme.getBaseDialog(mContext)
        .title(R.string.loading)
        .content(R.string.please_wait)
        .canceledOnTouchOutside(false)
        .progress(true, 0)
        .progressIndeterminateStyle(false).build()

    mBillingProcessor = BillingProcessor(this, BuildConfig.GOOGLE_PLAY_LICENSE_KEY, BillingHandler(this))
  }

  private fun loadSkuDetails() {
    if (mAdapter.datas.size > 3)
      return
    mDisposable = Single.fromCallable { mBillingProcessor?.getPurchaseListingDetails(SKU_IDS) }
        .map {
          val beans = ArrayList<PurchaseBean>()
          it.sortedWith(kotlin.Comparator { o1, o2 ->
            o1.priceValue.compareTo(o2.priceValue)
          }).forEach {
            beans.add(PurchaseBean(it.productId, "", it.title, it.priceText))
          }
          beans
        }
        .doFinally { mLoading.dismiss() }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeWith(object : DisposableSingleObserver<List<PurchaseBean>>() {
          override fun onSuccess(datas: List<PurchaseBean>) {
            mAdapter.datas.addAll(datas)
            mAdapter.notifyDataSetChanged()
          }

          override fun onError(e: Throwable) {
            ToastUtil.show(mContext, R.string.error_occur, e)
          }

          override fun onStart() {
            if (hasWindowFocus()) {
              mLoading.show()
            }
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
  override fun onProductPurchased(productId: String, details: TransactionDetails?) {
    Timber.v("onProductPurchased")
    Single
        .fromCallable {
          mBillingProcessor?.consumePurchase(productId)
        }.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({
          if (it == true) {
            Toast.makeText(this, R.string.thank_you, Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(this, R.string.payment_failure, Toast.LENGTH_SHORT).show()
          }
        }, {
          Timber.w(it)
          Toast.makeText(this, R.string.payment_failure, Toast.LENGTH_SHORT).show()
        })

  }

  override fun onBillingError(errorCode: Int, error: Throwable?) {
    Timber.v("onBillingError")
    ToastUtil.show(mContext, R.string.error_occur, "code = $errorCode err =  $error")
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    mBillingProcessor?.let {
      if (!it.handleActivityResult(requestCode, resultCode, data))
        super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    mBillingProcessor?.release()
    mDisposable?.dispose()
    if (mLoading.isShowing)
      mLoading.dismiss()
  }

  private class BillingHandler(handler: BillingProcessor.IBillingHandler) : BillingProcessor.IBillingHandler {
    private val ref = WeakReference<BillingProcessor.IBillingHandler>(handler)
    override fun onBillingInitialized() {
      ref.get()?.onBillingInitialized()
    }

    override fun onPurchaseHistoryRestored() {
      ref.get()?.onPurchaseHistoryRestored()
    }

    override fun onProductPurchased(productId: String, details: TransactionDetails?) {
      ref.get()?.onProductPurchased(productId, details)
    }

    override fun onBillingError(errorCode: Int, error: Throwable?) {
      ref.get()?.onBillingError(errorCode, error)
    }

  }
}