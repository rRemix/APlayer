package remix.myplayer.data.db.room.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import remix.myplayer.data.model.audio.APlayerModel
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
      return Json.decodeFromString(listStr ?: return ArrayList())
    }

    @TypeConverter
    fun toListStr(list: ArrayList<Long>?): String? {
      return Json.encodeToString(list)
    }
  }

  companion object {
    @Serial
    private const val serialVersionUID: Long = 7380279450459904510L
    const val TABLE_NAME = "PlayList"
  }
}