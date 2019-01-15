package remix.myplayer.db.room.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Created by remix on 2019/1/12
 */
@Entity
data class PlayListSong(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val audio_id: Long,
    val playlist_id: Int,
    val playlist_name: String,
    //自定义排序
    val order: Int
) {

}