package remix.myplayer.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.databinding.ItemCloudBinding
import remix.myplayer.ui.fragment.base.BaseFragment

class RemoteFragment : BaseFragment() {
  private val items = listOf(
      Cloud(R.drawable.icon_webdav, App.context.getString(R.string.webdav), OnClickListener {
        startActivity(Intent(requireContext(), WebDavActivity::class.java))
      }),
  )

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View {
    return RecyclerView(requireContext()).apply {
      layoutManager = GridLayoutManager(context, 3)
      adapter = CloudAdapter()
    }
  }

  private inner class CloudAdapter : RecyclerView.Adapter<CloudHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CloudHolder {
      return CloudHolder(ItemCloudBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun getItemCount(): Int {
      return items.size
    }

    override fun onBindViewHolder(holder: CloudHolder, position: Int) {
      holder.bind(items[position])
    }
  }

  private inner class CloudHolder(private val binding: ItemCloudBinding) :
      RecyclerView.ViewHolder(binding.root) {

    fun bind(cloud: Cloud) {
      binding.ivIcon.setImageResource(cloud.icon)
      binding.tvDesc.text = cloud.desc
      itemView.setOnClickListener(cloud.onClickListener)
    }
  }

  data class Cloud(val icon: Int, val desc: String, val onClickListener: OnClickListener)
}