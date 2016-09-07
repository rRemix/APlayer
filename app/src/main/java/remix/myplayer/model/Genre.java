package remix.myplayer.model;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/9/7 09:37
 */
public class Genre {
    public long GenreID;
    public String GenreName;
    public Genre(int id,String name){
        GenreID = id;
        GenreName = name;
    }
    public Genre(){};
}
