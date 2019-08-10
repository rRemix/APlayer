package remix.myplayer.ui.adapter

import android.annotation.SuppressLint
import android.view.View
import android.widget.TextView
import butterknife.BindView
import com.facebook.drawee.view.SimpleDraweeView
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.misc.PurchaseBean
import remix.myplayer.ui.adapter.holder.BaseViewHolder

class PurchaseAdapter(layoutId: Int) : BaseAdapter<PurchaseBean, PurchaseAdapter.PurchaseHolder>(layoutId) {
  private val LOGOS_OTHERS = listOf(R.drawable.icon_cookie, R.drawable.icon_cake, R.drawable.icon_drink,
      R.drawable.icon_movie, R.drawable.icon_gift)
  private val LOGOS_DONATE = listOf(R.drawable.icon_wechat_donate, R.drawable.icon_alipay_donate, R.drawable.icon_paypal_donate)

  @SuppressLint("SetTextI18n")
  override fun convert(holder: PurchaseHolder?, bean: PurchaseBean?, position: Int) {
    if (holder == null || bean == null)
      return
    holder.mTitle.text = bean.title.replace("(APlayer)", "")
    if (App.IS_GOOGLEPLAY) {
      holder.mLogo.setActualImageResource(LOGOS_OTHERS[position])
      holder.mPrice.text = bean.price
    } else {
      when (position) {
        0, 1, 2 -> {
          holder.mLogo.setActualImageResource(LOGOS_DONATE[position])
          holder.mPrice.text = ""
        }
        3, 4, 5, 6, 7 -> {
          holder.mLogo.setActualImageResource(LOGOS_OTHERS[position - 3])
          holder.mPrice.text = bean.price
        }
      }
    }

    holder.mRoot.setOnClickListener {
      mOnItemClickListener.onItemClick(it, position)
    }
  }

  class PurchaseHolder(itemView: View) : BaseViewHolder(itemView) {
    @BindView(R.id.item_price)
    lateinit var mPrice: TextView
    @BindView(R.id.item_logo)
    lateinit var mLogo: SimpleDraweeView
    @BindView(R.id.item_title)
    lateinit var mTitle: TextView
  }

}