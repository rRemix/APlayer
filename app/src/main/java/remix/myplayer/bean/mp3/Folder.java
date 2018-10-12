package remix.myplayer.bean.mp3;

/**
 * Created by Remix on 2018/1/9.
 */

public class Folder {
    private String Name;
    private int Count;
    private String Path;

    public Folder(String name, int count, String path) {
        Name = name;
        Count = count;
        Path = path;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public int getCount() {
        return Count;
    }

    public void setCount(int count) {
        Count = count;
    }

    public String getPath() {
        return Path;
    }

    public void setPath(String path) {
        Path = path;
    }
}
