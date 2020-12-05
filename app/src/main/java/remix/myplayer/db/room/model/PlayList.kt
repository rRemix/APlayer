package remix.myplayer.db.room.model

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Created by remix on 2019/1/12
 */
@Entity(indices = [Index(value = ["name"], unique = true)])
@TypeConverters(PlayList.Converter::class)
data class PlayList(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
//    val count: Int,
    val audioIds: LinkedHashSet<Int>,
    val date: Long
) {

  class Converter {
    @TypeConverter
    fun toStrList(listStr: String?): LinkedHashSet<Int>? {
      val gson = Gson()
      return gson.fromJson(listStr, object : TypeToken<LinkedHashSet<Int>>() {}.type)
    }

    @TypeConverter
    fun toListStr(list: LinkedHashSet<Int>?): String? {
      return Gson().toJson(list)
    }
  }

  companion object {
    const val TABLE_NAME = "PlayList"
  }
}