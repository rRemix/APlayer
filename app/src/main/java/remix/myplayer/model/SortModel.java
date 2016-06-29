package remix.myplayer.model;

/**
 * Created by taeja on 16-6-29.
 */
public class SortModel {
    private String mSortLetter;

    public SortModel(String sortLetter){
        this.mSortLetter = sortLetter;
    }

    public void setSortLetter(String sortLetter){
        this.mSortLetter = sortLetter;
    }
    public String getSortLetter(){
        return mSortLetter;
    }
}
