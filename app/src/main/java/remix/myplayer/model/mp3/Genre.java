package remix.myplayer.model.mp3;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/9/7 09:37
 */
public class Genre {
    public int GenreID;
    public String GenreName = "";
    public Genre(int id,String name){
        GenreID = id;
        GenreName = name;
    }
    public Genre(){}

    @Override
    public String toString() {
        return "Genre{" +
                "GenreID=" + GenreID +
                ", GenreName='" + GenreName + '\'' +
                '}';
    }
}
