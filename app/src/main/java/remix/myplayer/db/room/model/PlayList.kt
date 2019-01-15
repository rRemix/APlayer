package remix.myplayer.db.room.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey

/**
 * Created by remix on 2019/1/12
 */
@Entity(indices = [Index(value = ["name"], unique = true)])
data class PlayList(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name: String,
    val count: Int,
    val date: Long
) {

}