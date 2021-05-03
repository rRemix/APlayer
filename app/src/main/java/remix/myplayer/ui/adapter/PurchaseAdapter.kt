package remix.myplayer.ui.adapter

import android.annotation.SuppressLint
import android.view.View
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.misc.Purchase
import remix.myplayer.databinding.ItemSupportBinding
import remix.myplayer.ui.adapter.holder.BaseViewHolder

class PurchaseAdapter(layoutId: Int) : BaseAdapter<Purchase, PurchaseAdapter.PurchaseHolder>(layoutId) {
  private val LOGOS_OTHERS = listOf(R.drawable.icon_cookie, R.drawable.icon_cake, R.drawable.icon_drink,
      R.drawable.icon_movie, R.drawable.icon_gift)
  private val LOGOS_DONATE = listOf(R.drawable.icon_wechat_donate, R.drawable.icon_alipay_donate, R.drawable.icon_paypal_donate)

  @SuppressLint("SetTextI18n")
  override fun convert(holder: PurchaseHolder, purchase: Purchase?, position: Int) {
    if(purchase == null){
      return
    }
    holder.binding.itemTitle.text = purchase.title.replace("(APlayer)", "")
    if (App.IS_GOOGLEPLAY) {
      holder.binding.iv.setImageResource(LOGOS_OTHERS[position])
      holder.binding.itemPrice.text = purchase.price
    } else {
      when (position) {
        0, 1, 2 -> {
          holder.binding.iv.setImageResource(LOGOS_DONATE[position])
          holder.binding.itemPrice.text = ""
        }
        3, 4, 5, 6, 7 -> {
          holder.binding.iv.setImageResource(LOGOS_OTHERS[position - 3])
          holder.binding.itemPrice.text = purchase.price
        }
      }
    }

    holder.binding.root.setOnClickListener {
      onItemClickListener?.onItemClick(it, position)
    }
  }

  class PurchaseHolder(view: View) : BaseViewHolder(view) {
    val binding = ItemSupportBinding.bind(view)
  }
}