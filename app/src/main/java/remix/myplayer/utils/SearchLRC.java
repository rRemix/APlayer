package remix.myplayer.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;

import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import remix.myplayer.application.Application;
import remix.myplayer.infos.LrcInfo;

/**
 * Created by Remix on 2015/12/7.
 */

/**
 * 根据歌曲名和歌手名 搜索歌词并解析成固定格式
 */
public class SearchLRC {
    private static final String TAG = "SearchLRC";
    private static final String DEFAULT_LOCAL = "GB2312";
    private String mSongName;
    private String mArtistName;
    private static RequestQueue mQueue;
    private boolean mIsFind = false;

    public SearchLRC(String musicName, String singerName) {
        //创建RequestQueue对象
        if(mQueue == null)
            mQueue = Volley.newRequestQueue(Application.getContext());
        
        //传进来的如果是汉字，那么就要进行编码转化
        try {
            mSongName = URLEncoder.encode(musicName, "utf-8");
            mArtistName = URLEncoder.encode(singerName, "utf-8");

        } catch (UnsupportedEncodingException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }
        
    }

    /**
     * 根据歌手与歌手名,获得歌词id
     * @return 歌词id
     */
    public int getLrcId(){
        URL lrcIdUrl = null;
        try {
            lrcIdUrl = new URL("http://box.zhangmen.baidu.com/x?op=12&count=1&title=" +
                    mSongName + "$$" + mArtistName +"$$$$");
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        BufferedReader br = null;
        String s = new String();
        StringBuffer strBuffer = new StringBuffer();
        try {
            HttpURLConnection httpConn = (HttpURLConnection) lrcIdUrl.openConnection();
            httpConn.connect();
            InputStreamReader inReader = new InputStreamReader(httpConn.getInputStream());
            br = new BufferedReader(inReader);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
            if(br == null)
                return 0;
            while ((s = br.readLine()) != null) {
                strBuffer.append(s + "/r/n");

                int begin = 0, end = 0, number = 0;// number=0表示暂无歌词
                String strid = "";
                begin = strBuffer.indexOf("<lrcid>");
                if (begin != -1) {
                    end = strBuffer.indexOf("</lrcid>", begin);
                    strid = strBuffer.substring(begin + 7, end);
                    number = Integer.parseInt(strid);
                }
                return number;
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(br != null) {
                    Log.d(TAG,"StringBuffer:" + strBuffer);
                    br.close();
                }
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }


    /**
     * 根据歌词id,发送请求并解析歌词
     * @return 歌词信息list
     */
    public LinkedList<LrcInfo> getLrc() {
        int lrcId = getLrcId();
        if(lrcId <= 0)
            return null;
        
        //拼接url
        String geciURL = "http://box.zhangmen.baidu.com/bdlrc/"
                + lrcId / 100 + "/" + lrcId + ".lrc";
        SetFindLRC(lrcId);
       
        URL url = null;
        try {
            url = new URL(geciURL);
        } catch (MalformedURLException e2) {
            e2.printStackTrace();
        }
        //获得输入流
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(url.openStream(), "GB2312"));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        
        //解析歌词
        TreeMap<Integer,String> lrcMap = new TreeMap<>();
        lrcMap.clear();
        String s = new String();
        if (br == null) {
            Log.d(TAG,"stream is null");
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

        //将解析后的歌词封装
        LinkedList<LrcInfo> list = new LinkedList();
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


    /**
     * 根据字符串形式的时间，得到毫秒值
     * @param strTime 时间字符串
     * @return
     */
    public int getMill(String strTime) {
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


    private void SetFindLRC(int number) {
        if(number == 0)
            mIsFind = false;
        else
            mIsFind = true;
    }
    public boolean GetFindLRC(){
        return mIsFind;
    }
}
