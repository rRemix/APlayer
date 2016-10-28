package remix.myplayer.lrc;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import remix.myplayer.model.LrcInfo;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.DiskCache;
import remix.myplayer.util.DiskLruCache;

import static remix.myplayer.util.CommonUtil.getMill;

/**
 * @ClassName
 * @Description 解析歌词实现类
 * @Author Xiaoborui
 * @Date 2016/10/28 09:50
 */

public class LrcBuilderImpl implements ILrcBuilder {
    @Override
    public ArrayList<LrcInfo> getLrcRows(BufferedReader bufferedReader, boolean needCache,String songName,String artistName) {
        //解析歌词
        TreeMap<Integer,String> lrcMap = new TreeMap<>();
        lrcMap.clear();
        String s = "";
        DiskLruCache.Editor editor = null;
        OutputStream lrcCachaStream = null;
        if (bufferedReader == null)
            return null;

        try {
            if (needCache) {
                editor = DiskCache.getLrcDiskCache().edit(CommonUtil.hashKeyForDisk(songName + "/" + artistName));
                lrcCachaStream = editor.newOutputStream(0);
            }
            while ((s = bufferedReader.readLine()) != null) {

                if (needCache)
                    lrcCachaStream.write((s + "\r\n").getBytes());
                //判断是否是歌词内容
                if (s.startsWith("[ti") || s.startsWith("[ar") || s.startsWith("[al") ||
                        s.startsWith("[by") || s.startsWith("[off"))
                    continue;
                int startIndex = -1;
                while ((startIndex = s.indexOf("[", startIndex + 1)) != -1) {
                    int endIndex = s.indexOf("]", startIndex);
                    if (endIndex < 0)
                        continue;
                    Integer time = getMill(s.substring(startIndex, endIndex));
                    String lrc = s.substring(s.lastIndexOf(']') + 1, s.length());
                    if (time != -1 && !lrc.equals(""))
                        lrcMap.put(time, lrc +"\r\n");
                }
            }
            if (needCache) {
                lrcCachaStream.flush();
                editor.commit();
                DiskCache.getLrcDiskCache().flush();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null)
                    bufferedReader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //将解析后的歌词封装
        ArrayList<LrcInfo> list = new ArrayList<>();
        Iterator it = lrcMap.keySet().iterator();
        while (it.hasNext()) {
            int startime = (int)it.next();
            String sentence = lrcMap.get(startime);
            list.add(new LrcInfo(sentence,startime));
        }
        //设置每句歌词开始与结束时间
        for(int i = 0 ; i < list.size() - 1 ;i++) {
            LrcInfo cur = list.get(i);
            LrcInfo nxt = list.get(i + 1);
            list.get(i).setEndTime(nxt.getStartTime());
            list.get(i).setDuration(cur.getEndTime() - cur.getStartTime());
        }

        return list;
    }
}
