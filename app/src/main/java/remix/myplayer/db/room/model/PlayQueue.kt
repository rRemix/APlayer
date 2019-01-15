package remix.myplayer.db.room.model

import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey

/**
 * Created by remix on 2019/1/12
 */
@Entity
data class PlayQueue(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val audio_id: Int
) {

}