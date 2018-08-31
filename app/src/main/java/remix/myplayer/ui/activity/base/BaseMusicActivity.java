package remix.myplayer.ui.activity.base;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import remix.myplayer.helper.MusicEventHelper;
import remix.myplayer.helper.MusicServiceRemote;

@SuppressLint("Registered")
public class BaseMusicActivity extends BaseActivity implements MusicEventHelper.MusicEventCallback {
    private MusicServiceRemote.ServiceToken mServiceToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mServiceToken = MusicServiceRemote.bindToService(this, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                BaseMusicActivity.this.onServiceConnected();
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                BaseMusicActivity.this.onServiceDisConnected();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MusicServiceRemote.unbindFromService(mServiceToken);
    }

    @Override
    public void onMediaStoreChanged() {

    }

    @Override
    public void onPermissionChanged(boolean has) {

    }

    @Override
    public void onPlayListChanged() {

    }

    @Override
    public void onServiceConnected() {
        MusicEventHelper.addCallback(BaseMusicActivity.this);
    }

    @Override
    public void onServiceDisConnected() {
        MusicEventHelper.removeCallback(BaseMusicActivity.this);
    }
}
