package remix.myplayer.ui.dialog;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import remix.myplayer.R;
import remix.myplayer.ui.activity.BaseActivity;
import remix.myplayer.ui.activity.PlayListActivity;
import remix.myplayer.inject.ViewInject;
import remix.myplayer.util.XmlUtil;

/**
 * Created by taeja on 16-3-17.
 */

/**
 * 添加播放列表的对话框
 */

public class AddPlayListDialog extends BaseActivity {
    @ViewInject(R.id.playlist_add_edit)
    private EditText mEdit;

    @Override
    public int getLayoutId() {
        return R.layout.playlist_add;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowManager.LayoutParams lp = getWindow().getAttributes();

        getWindow().setAttributes(lp);
        getWindow().setGravity(Gravity.CENTER);

        mEdit.getBackground().setColorFilter(getResources().getColor(R.color.intersperse_color), PorterDuff.Mode.SRC_ATOP);
        mEdit.setText("本地歌单" + PlayListActivity.getPlayList().size());
    }

    public void onCancel(View v){
        finish();
    }

    public void onAdd(View v){
        String name = ((EditText)findViewById(R.id.playlist_add_edit)).getText().toString();
        if (name != null && !name.equals("")) {
            XmlUtil.addPlaylist(name);
            PlayListActivity.mInstance.getAdapter().notifyDataSetChanged();
        }
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.popup_out);
    }
}
