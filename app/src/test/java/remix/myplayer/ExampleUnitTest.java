package remix.myplayer;

import android.net.Uri;
import android.provider.MediaStore;

import org.junit.Test;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Uri uri1 = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        Uri uri2 = MediaStore.Audio.Artists.getContentUri(MediaStore.Audio.ArtistColumns.NUMBER_OF_ALBUMS);
    }


}