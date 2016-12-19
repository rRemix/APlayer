package remix.myplayer.lyric;

import java.io.BufferedReader;
import java.util.List;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/28 09:48
 */

public interface ILrcParser {
    List<LrcRow> getLrcRows(BufferedReader bufferedReader, boolean needCache, String songName, String artistName);
}
