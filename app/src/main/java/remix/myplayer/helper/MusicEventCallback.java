package remix.myplayer.helper;

import remix.myplayer.service.MusicService;

public interface MusicEventCallback {
    void onMediaStoreChanged();
    void onPermissionChanged(boolean has);
    void onPlayListChanged();
    void onServiceConnected(MusicService service);
    void onMetaChanged();
    void onPlayStateChange();
    void onServiceDisConnected();
}
