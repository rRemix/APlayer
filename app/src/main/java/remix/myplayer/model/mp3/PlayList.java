package remix.myplayer.model.mp3;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/13 11:22
 */
public class PlayList {
    public int _Id;
    public String Name;
    public int Count;
    public PlayList(){}
    public PlayList(String name, int count) {
        Name = name;
        Count = count;
    }

}
