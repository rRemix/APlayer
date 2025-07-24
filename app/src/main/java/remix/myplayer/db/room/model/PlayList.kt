package remix.myplayer.db.room.model

import androidx.room.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.Serializable
import remix.myplayer.bean.mp3.APlayerModel
import java.io.Serial

/**
 * Created by remix on 2019/1/12
 */
@Serializable
@Entity(indices = [Index(value = ["name"], unique = true)])
@TypeConverters(PlayList.Converter::class)
data class PlayList(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
//    val count: Int,
    val audioIds: ArrayList<Long>,
    val date: Long
) : APlayerModel {

  fun isFavorite() = id == 1L

  override fun getKey(): String {
    return id.toString()
  }

  class Converter {
    @TypeConverter
    fun toStrList(listStr: String?): ArrayList<Long>? {
      val gson = Gson()
      return gson.fromJson(listStr, object : TypeToken<ArrayList<Long>>() {}.type)
    }

    @TypeConverter
    fun toListStr(list: ArrayList<Long>?): String? {
      return Gson().toJson(list)
    }
  }

  companion object {
    @Serial
    private const val serialVersionUID: Long = 7380279450459904510L
    const val TABLE_NAME = "PlayList"
  }
}