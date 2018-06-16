package remix.myplayer.ui.activity

import android.app.Activity
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.View
import butterknife.BindView
import butterknife.ButterKnife
import com.afollestad.materialdialogs.MaterialDialog
import io.reactivex.Observable
import io.reactivex.ObservableSource
import io.reactivex.functions.Function
import remix.myplayer.R
import remix.myplayer.adapter.PurchaseAdapter
import remix.myplayer.bean.PurchaseBean
import remix.myplayer.interfaces.OnItemClickListener
import remix.myplayer.misc.cache.DiskCache
import remix.myplayer.request.network.RxUtil
import remix.myplayer.util.AlipayUtil
import remix.myplayer.util.ToastUtil
import java.io.File
import java.io.OutputStream
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
        for (i in 0..1) {
            when (i) {
                0 -> beans.add(PurchaseBean(i.toString(),"icon_wechat_donate",getString(R.string.wechat),""))
                1 -> beans.add(PurchaseBean(i.toString(),"icon_alipay_donate",getString(R.string.alipay),""))
            }
        }

        mAdapter = PurchaseAdapter(mContext,R.layout.item_support)
        mAdapter.setData(beans)
        mAdapter.setOnItemClickListener(object : OnItemClickListener{
            override fun onItemLongClick(view: View?, position: Int) {
            }
            override fun onItemClick(view: View?, position: Int) {
                when(position){
                    0 -> {
                        var outputStream: OutputStream? = null
                        var cursor: Cursor? = null
                        //保存微信图片
                        Observable.just(BitmapFactory.decodeResource(resources,R.drawable.icon_wechat_qrcode))
                                .flatMap(Function<Bitmap, ObservableSource<File>> {
                                    return@Function ObservableSource<File> {
                                        val weChatBitmap = BitmapFactory.decodeResource(resources,R.drawable.icon_wechat_qrcode)
                                        if(weChatBitmap == null || weChatBitmap.isRecycled){
                                            it.onError(Throwable("Invalid Bitmap"))
                                            return@ObservableSource
                                        }
                                        val dir = DiskCache.getDiskCacheDir(mContext, "qrCode")
                                        if(!dir.exists())
                                            dir.mkdirs()
                                        val qrCodeFile = File(dir,"qrCode.png")

                                        //删除旧文件
                                        cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                                null,MediaStore.Images.Media.DATA + "=?", arrayOf(qrCodeFile.absolutePath),null)
                                        if(cursor != null && cursor!!.count > 0){
                                            contentResolver.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,MediaStore.Images.Media.DATA + "=?",arrayOf(qrCodeFile.absolutePath))
                                        }
                                        if(qrCodeFile.exists()){
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
                                        if(uri == null){
                                            it.onError(Throwable("Uri Empty"))
                                            return@ObservableSource
                                        }
                                        outputStream = contentResolver.openOutputStream(uri)
                                        weChatBitmap.compress(Bitmap.CompressFormat.PNG,100,outputStream)
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
                                    ToastUtil.showLong(mContext,R.string.save_wechat_qrcode_success,it.absolutePath)
                                }, {
                                    ToastUtil.show(mContext,R.string.save_error)
                                })
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

        })

        mRecyclerView.layoutManager = GridLayoutManager(mContext,2)
        mRecyclerView.adapter = mAdapter
    }

}