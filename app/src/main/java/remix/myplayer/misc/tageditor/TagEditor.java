package remix.myplayer.misc.tageditor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.Nullable;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.images.Artwork;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

import io.reactivex.Observable;

/**
 * 标签相关
 */
public class TagEditor {
    private final AudioFile mAudioFile;
    private final String mPath;
    private final AudioHeader mAudioHeader;

    public TagEditor(String path){
        mPath = path;
        mAudioFile = getAudioFile();
        mAudioHeader = mAudioFile.getAudioHeader();
    }

    @NotNull
    public AudioFile getAudioFile(){
        try {
            return AudioFileIO.read(new File(mPath));
        } catch (CannotReadException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TagException e) {
            e.printStackTrace();
        } catch (ReadOnlyFileException e) {
            e.printStackTrace();
        } catch (InvalidAudioFrameException e) {
            e.printStackTrace();
        }
        return new AudioFile();
    }

    public String getFormat(){
        return mAudioHeader.getFormat();
    }

    public String getBitrate(){
        return mAudioHeader.getBitRate();
    }

    public String getSamlingRate(){
        return mAudioHeader.getSampleRate();
    }

    @Nullable
    public String getFiledValue(FieldKey field){
        if(mAudioFile == null)
            return "";
        try {
            return mAudioFile.getTagOrCreateAndSetDefault().getFirst(field);
        }catch (Exception e){
            return null;
        }
    }

    @Nullable
    public String getFiledValue(String path,String field){
        if(mAudioFile == null)
            return "";
        try {
            return mAudioFile.getTagOrCreateAndSetDefault().getFirst(field);
        }catch (Exception e){
            return null;
        }
    }

    @Nullable
    public String getSongTitle(){
        try {
            return getFiledValue(FieldKey.TITLE);
        }catch (Exception e){
            return null;
        }
    }

    @Nullable
    public String getAlbumTitle(){
        try {
            return getFiledValue(FieldKey.ALBUM);
        }catch (Exception e){
            return null;
        }
    }

    @Nullable
    public String getArtistName(){
        try {
            return getFiledValue(FieldKey.ARTIST);
        }catch (Exception e){
            return null;
        }
    }

    @Nullable
    public String getAlbumArtistName(){
        try {
            return getFiledValue(FieldKey.ALBUM_ARTIST);
        }catch (Exception e){
            return null;
        }
    }

    @Nullable
    public String getGenreName(){
        try {
            return getFiledValue(FieldKey.GENRE);
        }catch (Exception e){
            return null;
        }
    }

    @Nullable
    public String getSongYear(){
        try {
            return getFiledValue(FieldKey.YEAR);
        }catch (Exception e){
            return null;
        }
    }

    @Nullable
    public String getTrakNumber(){
        try {
            return getFiledValue(FieldKey.TRACK);
        }catch (Exception e){
            return null;
        }
    }

    @Nullable
    public String getLyric(){
        try {
            return getFiledValue(FieldKey.LYRICS);
        }catch (Exception e){
            return null;
        }
    }

    @Nullable
    public Bitmap getAlbumArt(){
        if(mAudioFile == null)
            return null;
        try {
            Artwork artworkTag = mAudioFile.getTagOrCreateAndSetDefault().getFirstArtwork();
            if (artworkTag != null) {
                byte[] artworkBinaryData = artworkTag.getBinaryData();
                return BitmapFactory.decodeByteArray(artworkBinaryData, 0, artworkBinaryData.length);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    public Observable<Boolean> save(String title, String album, String artist, String year, String genre, String trackNumber, String lyric) {
        return Observable.create(e -> {
            if(mAudioFile == null){
                e.onError(new Throwable("AudioFile null"));
                return;
            }

            Tag tag = mAudioFile.getTagOrCreateAndSetDefault();
            tag.setField(FieldKey.TITLE,title);
            tag.setField(FieldKey.ALBUM,album == null ? "" : album);
            tag.setField(FieldKey.ARTIST,artist == null ? "" : artist);
            tag.setField(FieldKey.YEAR,year == null ? "" : year);
            tag.setField(FieldKey.GENRE,genre == null ? "" : genre);
            tag.setField(FieldKey.TRACK,trackNumber == null ? "" : trackNumber);
//            tag.setField(FieldKey.LYRICS,lyric == null ? "" : lyric);

            mAudioFile.commit();
            e.onNext(true);
            e.onComplete();
        });
    }
}
