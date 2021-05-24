package remix.myplayer.db.room.model

import android.os.Parcelable
import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.parcel.Parcelize
import remix.myplayer.bean.mp3.APlayerModel

/**
 * Created by remix on 2019/1/12
 */
@Parcelize
@Entity(indices = [Index(value = ["name"], unique = true)])
@TypeConverters(PlayList.Converter::class)
data class PlayList(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
//    val count: Int,
    val audioIds: LinkedHashSet<Long>,
    val date: Long
) : Parcelable, APlayerModel {

  class Converter {
    @TypeConverter
    fun toStrList(listStr: String?): LinkedHashSet<Long>? {
      val gson = Gson()
      return gson.fromJson(listStr, object : TypeToken<LinkedHashSet<Long>>() {}.type)
    }

    @TypeConverter
    fun toListStr(list: LinkedHashSet<Long>?): String? {
      return Gson().toJson(list)
    }
  }

  companion object {
    const val TABLE_NAME = "PlayList"
  }

  override fun getKey(): String {
    return id.toString()
  }

}