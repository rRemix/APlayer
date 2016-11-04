package remix.myplayer.model;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/13 11:22
 */
public class PlayListInfo {
    public int _Id;
    public String Name;
    public int Count;
    public PlayListInfo(){}
    public PlayListInfo(String name, int count) {
        Name = name;
        Count = count;
    }
}
