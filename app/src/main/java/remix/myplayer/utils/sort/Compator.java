package remix.myplayer.utils.sort;

import java.util.Comparator;

import remix.myplayer.infos.MP3Info;

/**
 * Created by taeja on 16-6-28.
 */
public class Compator implements Comparator<MP3Info> {
    private static CharacterParser mParser = new CharacterParser();
    @Override
    public int compare(MP3Info lhs, MP3Info rhs) {
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
        String letter = pinyin.substring(0,1).toUpperCase();
        if(!letter.matches("[A-Z]"))
            letter = "Z";
        return letter;
    }
}
