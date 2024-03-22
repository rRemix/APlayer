package remix.myplayer.ui.adapter

import androidx.recyclerview.widget.RecyclerView
import remix.myplayer.misc.interfaces.OnItemClickListener
import remix.myplayer.ui.adapter.holder.BaseViewHolder
import java.lang.reflect.Constructor

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/19 11:31
 */
abstract class BaseAdapter<Data, ViewHolder : BaseViewHolder>(val layoutId: Int) : RecyclerView.Adapter<ViewHolder>() {
  var onItemClickListener: OnItemClickListener? = null
  val dataList: ArrayList<Data> = ArrayList()
  private var constructor: Constructor<*>? = null

  fun setDataList(dataList: List<Data>?) {
    this.dataList.clear()
    if (dataList != null) {
      this.dataList.addAll(dataList)
    }
    notifyDataSetChanged()
  }

  fun getDataList(): List<Data> {
    return dataList
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    convert(holder, getItem(position), position)
  }

  protected open fun getItem(position: Int): Data? {
    return dataList[position]
  }

  protected abstract fun convert(holder: ViewHolder, data: Data?, position: Int)

//  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
//    return try {
//      val itemView = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
//      constructor?.newInstance(itemView) as ViewHolder
//    } catch (e: Exception) {
//      throw IllegalArgumentException(e.toString())
//    }
//  }

//  fun setOnItemClickListener(l: OnItemClickListener?) {
//    onItemClickListener = l
//  }

  override fun getItemCount(): Int {
    return dataList.size
  }

//  private val genericClass: Class<ViewHolder>
//    get() {
//      val genType = javaClass.genericSuperclass
//      val params = (genType as ParameterizedType).actualTypeArguments
//      return if (params != null && params.size > 1 && params[1] is Class<*>) {
//        params[1] as Class<ViewHolder>
//      } else {
//        throw IllegalArgumentException("泛型错误")
//      }
//    }
//
//  init {
//    try {
//      constructor = genericClass.getDeclaredConstructor(View::class.java)
//      constructor?.isAccessible = true
//    } catch (e: Exception) {
//      throw IllegalArgumentException(e.toString())
//    }
//  }
}