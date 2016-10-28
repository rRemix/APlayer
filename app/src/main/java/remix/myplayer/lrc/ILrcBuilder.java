package remix.myplayer.lrc;

import java.io.BufferedReader;
import java.util.ArrayList;

import remix.myplayer.model.LrcInfo;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/28 09:48
 */

public interface ILrcBuilder {
    ArrayList<LrcInfo> getLrcRows(BufferedReader bufferedReader,boolean needCache,String songName,String artistName);
}
