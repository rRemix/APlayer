package remix.myplayer.service;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/12/29 09:12
 */

public interface Playback {
    void playSelectSong(int position);
    void toggle();
    void playNext();
    void playPrevious();
    void play();
    void pause();
}
