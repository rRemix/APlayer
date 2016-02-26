package remix.myplayer.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

/**
 * Created by taeja on 16-2-26.
 */
public class QQApi {
    public static String Test(String song,String artist){
        URL url = null;
        HttpURLConnection httpURLConnection = null;
        InputStreamReader in = null;
        BufferedReader br = null;
        String s = new String();
        StringBuilder sb = new StringBuilder();
        try {
            String tmp = "http://shopcgi.qqmusic.qq.com/fcgi-bin/shopsearch.fcg?value=" + URLEncoder.encode(song,"gb2312") + "&artist=" +  URLEncoder.encode(artist,"gb2312") + "&type=qry_song&out=json&page_no=1&page_record_num=5";
            String tmp1 = "http://shopcgi.qqmusic.qq.com/fcgi-bin/shopsearch.fcg?value=" + song + "&artist=" +  artist + "&type=qry_song&out=json&page_no=1&page_record_num=5";
            url = new URL(tmp);
            httpURLConnection = (HttpURLConnection)url.openConnection();
            httpURLConnection.connect();
            in = new InputStreamReader(httpURLConnection.getInputStream());
            br = new BufferedReader(in);
            if(br == null) return null;
            while((s = br.readLine()) != null){
                sb.append(s + "/r/n");
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                if (br != null)
                    br.close();
                if(in != null)
                    in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sb.toString();
    }
}
