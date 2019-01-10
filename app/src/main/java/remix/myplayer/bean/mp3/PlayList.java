package remix.myplayer.bean.mp3;

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
  public int Date;

  public PlayList() {
  }

  public PlayList(String name, int count) {
    Name = name;
    Count = count;
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  public int getId() {
    return _Id;
  }

  public void setId(int _Id) {
    this._Id = _Id;
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

  public int getDate() {
    return Date;
  }

  public void setDate(int date) {
    Date = date;
  }

  @Override
  public String toString() {
    return "PlayList{" +
        "_Id=" + _Id +
        ", Name='" + Name + '\'' +
        ", Count=" + Count +
        ", Date=" + Date +
        '}';
  }
}
