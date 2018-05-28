package remix.myplayer.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import butterknife.BindView
import com.facebook.drawee.view.SimpleDraweeView
import remix.myplayer.R
import remix.myplayer.adapter.holder.BaseViewHolder
import remix.myplayer.bean.PurchaseBean

class PurchaseAdapter (context: Context, layoutId: Int) : BaseAdapter<PurchaseBean,PurchaseAdapter.PurchaseHolder>(context,layoutId) {

    @SuppressLint("SetTextI18n")
    override fun convert(holder: PurchaseHolder?, bean: PurchaseBean?, position: Int) {
        if(holder == null || bean == null)
            return
        holder.mTitle.text = bean.title
        when(position){
            0,1,2-> {
                holder.mLogo.setActualImageResource(when(position){
                    0 -> R.drawable.icon_wechat_donate
                    1 -> R.drawable.icon_alipay_donate
                    else -> {
                        R.drawable.icon_paypal_donate
                    }
                })
                holder.mPrice.visibility = View.GONE
                holder.mPrice.text = ""
            }
            else ->{
                if(TextUtils.isEmpty(bean.logo)){
                    holder.mLogo.visibility = View.GONE
                }else{
                    holder.mLogo.visibility = View.VISIBLE
                    holder.mLogo.setImageURI(bean.logo)
                }
                holder.mPrice.visibility = View.VISIBLE
                holder.mPrice.text = bean.price
            }
        }
        holder.mRoot.setOnClickListener {
            mOnItemClickLitener.onItemClick(it,position)
        }
    }

    class PurchaseHolder(itemView: View) : BaseViewHolder(itemView){
        @BindView(R.id.item_price)
        lateinit var mPrice: TextView
        @BindView(R.id.item_logo)
        lateinit var mLogo: SimpleDraweeView
        @BindView(R.id.item_title)
        lateinit var mTitle: TextView
    }

}