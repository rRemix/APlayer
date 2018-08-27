package remix.myplayer.util;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import remix.myplayer.App;
import remix.myplayer.service.Command;
import remix.myplayer.service.MusicService;

public class MusicUtil {
    private static final String TAG = MusicUtil.class.getSimpleName();
    private MusicUtil(){}

    public static void playFromUri(Uri uri) {
        List<Integer> songs = null;
        if (uri.getScheme() != null && uri.getAuthority() != null) {
            if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {//content://xxx
                String songId = null;
                if (uri.getAuthority().equals("com.android.providers.media.documents")) {
                    songId = getSongIdFromMediaProvider(uri);
                } else if (uri.getAuthority().equals("media")) {
                    songId = uri.getLastPathSegment();
                }
                if (songId != null && TextUtils.isDigitsOnly(songId)) {
                    songs = new ArrayList<>();
                    songs.add(Integer.valueOf(songId));
                }
            }
        }
        if (songs == null) {
            File songFile = null;
            if (uri.getAuthority() != null && uri.getAuthority().equals("com.android.externalstorage.documents")) {
                songFile = new File(Environment.getExternalStorageDirectory(), uri.getPath().split(":", 2)[1]);
            }
            if (songFile == null) {
                String path = getFilePathFromUri(App.getContext(), uri);
                if (path != null)
                    songFile = new File(path);
            }
            if (songFile == null && uri.getPath() != null) {
                songFile = new File(uri.getPath());
            }
            if (songFile != null) {
                int id = MediaStoreUtil.getSongIdByUrl(songFile.getAbsolutePath());
                if (id > 0) {
                    songs = new ArrayList<>();
                    songs.add(id);
                }
            }
        }
        if (songs != null && !songs.isEmpty()) {
            Intent intent = new Intent(MusicService.ACTION_CMD);
            Bundle arg = new Bundle();
            arg.putInt("Control", Command.PLAYSELECTEDSONG);
            arg.putInt("Position", 0);
            intent.putExtras(arg);
            Global.setPlayQueue(songs, App.getContext(), intent);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String getSongIdFromMediaProvider(Uri uri) {
        return DocumentsContract.getDocumentId(uri).split(":")[1];
    }

    @Nullable
    private static String getFilePathFromUri(Context context, Uri uri) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, null, null,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
}
