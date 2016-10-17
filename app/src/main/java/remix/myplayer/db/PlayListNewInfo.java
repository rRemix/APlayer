package remix.myplayer.db;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/13 11:22
 */
public class PlayListNewInfo {
    public int _Id;
    public String Name;
    public int Count;
    public PlayListNewInfo(){}
    public PlayListNewInfo(String name, int count) {
        Name = name;
        Count = count;
    }
}
