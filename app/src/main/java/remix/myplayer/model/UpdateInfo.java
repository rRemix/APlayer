package remix.myplayer.model;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by Remix on 2016/9/13.
 */
public class UpdateInfo implements Serializable {
    public ArrayList<String> Logs = new ArrayList<>();
    public String VersionName;
    public String ApkUrl;
    public String Size;
    public String MD5;
}
