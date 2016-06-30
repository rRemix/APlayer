package remix.myplayer.util.sort;

import android.text.TextUtils;
import android.util.Log;

import com.github.promeg.pinyinhelper.Pinyin;
import com.sina.weibo.sdk.register.mobile.PinyinUtils;

import java.util.Comparator;

import remix.myplayer.application.Application;
import remix.myplayer.model.MP3Item;

/**
 * Created by taeja on 16-6-28.
 */
public class Compator implements Comparator<MP3Item> {
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

        for(int i = 0 ; i < ori.length();i++){
            char c = ori.charAt(i);
            if((c >= 65 && c <= 90) || (c >= 97 && c <= 122) || Pinyin.isChinese(c)){
                return Pinyin.toPinyin(c).substring(0,1).toUpperCase();
            }
        }
        return "Z";

    }

    private static String getPinyin(String ori) {
        if(TextUtils.isEmpty(ori)) {
            return "";
        } else {
            StringBuilder sb = new StringBuilder();
            int len = ori.length();

            for(int i = 0; i < len; ++i) {
                char c = ori.charAt(i);
                sb.append(Pinyin.toPinyin(c));
            }
            return sb.toString();
        }

    }
}
