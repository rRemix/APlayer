package remix.myplayer.lyric;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.DiskCache;
import remix.myplayer.util.DiskLruCache;

/**
 * @ClassName
 * @Description 解析歌词实现类
 * @Author Xiaoborui
 * @Date 2016/10/28 09:50
 */

public class DefaultLrcParser implements ILrcParser {
    @Override
    public List<LrcRow> getLrcRows(BufferedReader bufferedReader, boolean needCache,String songName,String artistName) {
        if(bufferedReader == null)
            return null;
        //解析歌词
        TreeMap<Integer,String> lrcMap = new TreeMap<>();
        lrcMap.clear();
        String s = "";
        DiskLruCache.Editor editor = null;
        OutputStream lrcCacheStream = null;
        if (bufferedReader == null)
            return null;

        List<LrcRow> lrcRows = new ArrayList<>();
        try {
            if (needCache) {
                DiskLruCache lrcDiskCache = DiskCache.getLrcDiskCache();
                if(lrcDiskCache != null)
                    editor = lrcDiskCache.edit(CommonUtil.hashKeyForDisk(songName + "/" + artistName));
                if(editor != null)
                    lrcCacheStream = editor.newOutputStream(0);
            }

            while ((s = bufferedReader.readLine()) != null) {
                //缓存
                if (needCache && lrcCacheStream != null)
                    lrcCacheStream.write((s + "\n").getBytes());
                //解析每一行歌词
                List<LrcRow> rows = LrcRow.createRows(s);
                if(rows != null && rows.size() > 0)
                    lrcRows.addAll(rows);
            }
            //为歌词排序
            Collections.sort(lrcRows);

            for (int i = 0; i < lrcRows.size() - 1; i++) {
                lrcRows.get(i).setTotalTime(lrcRows.get(i + 1).getTime() - lrcRows.get(i).getTime());
            }
            lrcRows.get(lrcRows.size() - 1).setTotalTime(5000);

            if (needCache) {
                lrcCacheStream.flush();
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
            try {
                if(lrcCacheStream != null)
                    lrcCacheStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return lrcRows;
    }
}
