package remix.myplayer.bean;

import java.io.Serializable;

import remix.myplayer.lyric.bean.LrcRow;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/5/10 13:37
 */

public class FloatLrcContent implements Serializable {
    public LrcRow Line1 = new LrcRow();
    public LrcRow Line2 = new LrcRow();
}
