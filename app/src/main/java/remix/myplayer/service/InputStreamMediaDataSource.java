package remix.myplayer.service;

import android.media.MediaDataSource;
import android.os.Build;
import androidx.annotation.RequiresApi;
import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;
import java.io.IOException;
import java.io.InputStream;
import remix.myplayer.db.room.model.WebDav;
import timber.log.Timber;

@RequiresApi(api = Build.VERSION_CODES.M)
public class InputStreamMediaDataSource extends MediaDataSource {

  private InputStream is;
  private long streamLength, lastReadEndPosition;
  private final Sardine sardine = new OkHttpSardine();
  private final String url;

  public InputStreamMediaDataSource(WebDav webDav, String url, long length) throws IOException {
    sardine.setCredentials(webDav.getAccount(), webDav.getPwd());
    is = sardine.get(url);
    this.url = url;
    this.streamLength = length;
    if (streamLength <= 0) {
      try {
        this.streamLength = is.available();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    Timber.w("length: %s", streamLength);
  }
//  public InputStreamMediaDataSource(InputStream is, long streamLength) {
//    this.is = is;
//    this.streamLength = streamLength;
//    if (streamLength <= 0){
//      try {
//        this.streamLength = is.available(); //Correct value of InputStream#available() method not always supported by InputStream implementation!
//      } catch (IOException e) {
//        e.printStackTrace();
//      }
//    }
//  }

  @Override
  public synchronized void close() throws IOException {
    is.close();
  }

  @Override
  public synchronized int readAt(long position, byte[] buffer, int offset, int size)
      throws IOException {
    if (position >= streamLength) {
      return -1;
    }

    if (position + size > streamLength) {
      size -= (position + size) - streamLength;
    }

    if (position < lastReadEndPosition) {
      Timber.w("position < lastReadEndPosition, pos: " + position + " last: " + lastReadEndPosition);
      is.close();
      lastReadEndPosition = 0;
      is = sardine.get(url);
    }

    long skipped = is.skip(position - lastReadEndPosition);
    if (skipped == position - lastReadEndPosition) {
      int bytesRead = is.read(buffer, offset, size);
      lastReadEndPosition = position + bytesRead;
      return bytesRead;
    } else {
      return -1;
    }
  }

  @Override
  public synchronized long getSize() throws IOException {
    return streamLength;
  }
}
