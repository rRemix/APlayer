package remix.myplayer.util.sort;

import android.util.Log;

import java.util.Comparator;

import remix.myplayer.model.MP3Item;

/**
 * Created by taeja on 16-6-28.
 */
public class Compator implements Comparator<MP3Item> {
    private static CharacterParser mParser = new CharacterParser();
    @Override
    public int compare(MP3Item lhs, MP3Item rhs) {
        String LSortChar = getFirstLetter(lhs.getTitle());

        String RSortChar = getFirstLetter(rhs.getTitle());

        if (RSortChar.equals("#")) {
            return -1;
        } else if (LSortChar.equals("#")) {
            return 1;
        } else {
            return LSortChar.compareTo(RSortChar);
        }
    }

    public static String getFirstLetter(String ori){
        String pinyin = mParser.getSelling(ori);
        for(int i = 0 ; i < pinyin.length() - 1 ;i++){
            String letter = pinyin.substring(i,i + 1).toUpperCase();
            if(letter.matches("[A-Z]"))
                return letter;
        }
        return "Z";
    }
}
