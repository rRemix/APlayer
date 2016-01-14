package remix.myplayer.utils;

/**
 * Created by Remix on 2015/12/7.
 */
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeMap;

import android.support.v4.app.INotificationSideChannel;
import android.util.Log;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class SearchLRC {
    public static TreeMap<Integer,String> lrcMap = new TreeMap<>();
    private URL url;
    public static final String DEFAULT_LOCAL = "GB2312";
    StringBuffer sb = new StringBuffer();

    private boolean findNumber = false;
    public SearchLRC(){};
    /*
     * 初始化，根据参数取得lrc的地址
     */
    public SearchLRC(String musicName, String singerName) {

        //传进来的如果是汉字，那么就要进行编码转化
        try {
            musicName = URLEncoder.encode(musicName, "utf-8");
            singerName = URLEncoder.encode(singerName, "utf-8");
        } catch (UnsupportedEncodingException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        String strUrl = "http://box.zhangmen.baidu.com/x?op=12&count=1&title=" +
                musicName + "$$" + singerName +"$$$$";
        try
        {
            url = new URL(strUrl);
        }
        catch (Exception e1)
        {
            e1.printStackTrace();
        }
        BufferedReader br = null;
        String s = new String();
        try
        {
            HttpURLConnection httpConn = (HttpURLConnection)url.openConnection();
            httpConn.connect();
            InputStreamReader inReader = new InputStreamReader(httpConn.getInputStream());
            br = new BufferedReader(inReader);
        }
        catch (IOException e1)
        {
            e1.printStackTrace();
        }
        try
        {
            if(br == null)
                return;
            while ((s = br.readLine()) != null)
            {
                sb.append(s + "/r/n");
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                if(br != null)
                    br.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
    /*
     * 根据lrc的地址，读取lrc文件流
     * 生成歌词的ArryList
     * 每句歌词是一个String
     */
    public LinkedList<LrcInfo> fetchLyric()
    {

        int begin = 0, end = 0, number = 0;// number=0表示暂无歌词
        String strid = "";
        begin = sb.indexOf("<lrcid>");
        Log.d("test", "sb = " + sb);
        if (begin != -1) {
            end = sb.indexOf("</lrcid>", begin);
            strid = sb.substring(begin + 7, end);
            number = Integer.parseInt(strid);
        }
        if(number <= 0)
            return null;
        String geciURL = "http://box.zhangmen.baidu.com/bdlrc/"
                + number / 100 + "/" + number + ".lrc";
        SetFindLRC(number);
        Log.d("test", "geciURL = " + geciURL);
        //ArrayList gcContent =new ArrayList();
        String s = new String();
        try {
            url = new URL(geciURL);
        } catch (MalformedURLException e2) {
            e2.printStackTrace();
        }

        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(url.openStream(), "GB2312"));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        lrcMap.clear();
        if (br == null) {
            System.out.print("stream is null");
        } else {
            try {

                while ((s = br.readLine()) != null) {
                    //判断是否是歌词内容
                    if(s.startsWith("[ti") || s.startsWith("[ar") || s.startsWith("[al") ||
                            s.startsWith("[by") || s.startsWith("[off"))
                        continue;
                    int startIndex = -1;
                    while((startIndex = s.indexOf("[", startIndex + 1)) != -1)
                    {
                        int endIndex = s.indexOf("]", startIndex);
                        Integer time = getMill(s.substring(startIndex, endIndex));
                        String lrc = s.substring(s.lastIndexOf(']') + 1,s.length());
                        if(time != -1 && !lrc.equals(""))
                            lrcMap.put(time,lrc);
                    }
                }
                br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        LinkedList<LrcInfo> list = new LinkedList();
        Iterator it = lrcMap.keySet().iterator();


        while (it.hasNext())
        {
            int startime = (int)it.next();
            String sentence = lrcMap.get(startime);
            list.add(new LrcInfo(sentence,startime));
        }
        for(int i = 0 ; i < list.size() - 1 ;i++)
        {
            LrcInfo cur = list.get(i);
            LrcInfo nxt = list.get(i + 1);
            list.get(i).setEndTime(nxt.getStartTime());
            list.get(i).setDuration(cur.getEndTime() - cur.getStartTime());
        }

        return list;
    }

    public int getMill(String strTime)
    {
        int min;
        int sec;
        int mill;
        if(strTime.substring(1,3).matches("[0-9]*"))
            min = Integer.parseInt(strTime.substring(1, 3));
        else
            return -1;
        if(strTime.substring(4,6).matches("[0-9]*"))
            sec = Integer.parseInt(strTime.substring(4, 6));
        else
            return -1;
        if(strTime.substring(7,9).matches("[0-9]*"))
            mill = Integer.parseInt(strTime.substring(7,9));
        else
            return -1;
        return min * 60000 + sec * 1000 + mill;
    }

    public boolean ParseLrc()
    {
        String test = "[00:01:02]";
        String test1 = "[00:01:02][00:01:02]";

        String reg = "//[/d+2./d+2./d+2//]";
        boolean b1 = test.matches(reg);
        boolean b2 = test.matches(reg);
        System.out.println(b1);
        System.out.println(b2);
        return true;
    }

    private void SetFindLRC(int number) {
        if(number == 0)
            findNumber = false;
        else
            findNumber = true;
    }
    public boolean GetFindLRC(){
        return findNumber;
    }
}
