package remix.myplayer.db.room.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import remix.myplayer.db.room.model.PlayListSong

/**
 * Created by remix on 2019/1/12
 */
@Dao
interface PlayListSongDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertPlayListSong(playListSongs: List<PlayListSong>): LongArray

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertPlayListSong(playListSong: PlayListSong): Long

}