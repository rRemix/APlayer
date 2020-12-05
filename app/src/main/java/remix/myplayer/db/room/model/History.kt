package remix.myplayer.db.room.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Created by remix on 2019/1/12
 */
@Entity
data class History(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val audio_id: Int,
    val play_count: Int,
    val last_play: Long
) {
}