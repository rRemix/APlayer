package remix.myplayer.bean.kugou;

import java.io.Serializable;

/**
 * Created by Remix on 2017/11/21.
 */

public class KLrcResponse implements Serializable {
    private static final long serialVersionUID = -5095962158319526120L;
    public String charset;
    public String content;
    public String fmt;
    public String info;
    public int status;
}
