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
import java.util.EnumMap;
import java.util.Map;

import io.reactivex.Observable;
import remix.myplayer.util.FileUtil;
import remix.myplayer.util.LogUtil;

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
            return "";
        }
    }

    @Nullable
    public String getFiledValue(String path,String field){
        if(mAudioFile == null)
            return "";
        try {
            return mAudioFile.getTagOrCreateAndSetDefault().getFirst(field);
        }catch (Exception e){
            return "";
        }
    }

    @Nullable
    public String getSongTitle(){
        return getFiledValue(FieldKey.TITLE);
    }

    @Nullable
    public String getAlbumTitle(){
        return getFiledValue(FieldKey.ALBUM);
    }

    @Nullable
    public String getArtistName(){
        return getFiledValue(FieldKey.ARTIST);
    }

    @Nullable
    public String getAlbumArtistName(){
        return getFiledValue(FieldKey.ALBUM_ARTIST);
    }

    @Nullable
    public String getGenreName(){
        return getFiledValue(FieldKey.GENRE);
    }

    @Nullable
    public String getSongYear(){
        return getFiledValue(FieldKey.YEAR);
    }

    @Nullable
    public String getTrackNumber(){
        return getFiledValue(FieldKey.TRACK);
    }

    @Nullable
    public String getLyric(){
        return getFiledValue(FieldKey.LYRICS);
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

            Map<FieldKey, String> fieldKeyValueMap = new EnumMap<>(FieldKey.class);
            fieldKeyValueMap.put(FieldKey.ALBUM,album);
            fieldKeyValueMap.put(FieldKey.TITLE,title);
            fieldKeyValueMap.put(FieldKey.YEAR,year);
            fieldKeyValueMap.put(FieldKey.GENRE,genre);
            fieldKeyValueMap.put(FieldKey.ARTIST,artist);
            fieldKeyValueMap.put(FieldKey.TRACK,trackNumber);
//            fieldKeyValueMap.put(FieldKey.LYRICS,lyric);

            Tag tag = mAudioFile.getTagOrCreateAndSetDefault();
            for (Map.Entry<FieldKey, String> entry : fieldKeyValueMap.entrySet()) {
                try {
                    tag.setField(entry.getKey(), entry.getValue() == null ? "" : entry.getValue());
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }

//            tag.setField(FieldKey.ALBUM,album == null ? "" : album);
//            tag.setField(FieldKey.ARTIST,artist == null ? "" : artist);
//            tag.setField(FieldKey.YEAR,year == null ? "" : year);
//            tag.setField(FieldKey.GENRE,genre == null ? "" : genre);
//            tag.setField(FieldKey.TRACK,trackNumber == null ? "" : trackNumber);
//            tag.setField(FieldKey.LYRICS,lyric == null ? "" : lyric);

            mAudioFile.commit();
            e.onNext(true);
            e.onComplete();
        });
    }
}
