package remix.myplayer.model;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/9/23 09:18
 */
public class MultiPosition {
    public int Position;
    public MultiPosition(int pos){
        Position = pos;
    }


    @Override
    public boolean equals(Object o) {
        return o instanceof MultiPosition && this.Position == ((MultiPosition)o).Position;
    }
}
