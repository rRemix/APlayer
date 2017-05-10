package remix.myplayer.model.mp3;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2017/1/11 09:39
 */

public class LrcRequest {
    public int ID;
    public String AccessKey;
    public LrcRequest(){}
    public LrcRequest(int ID, String accessKey) {
        this.ID = ID;
        AccessKey = accessKey;
    }
}
