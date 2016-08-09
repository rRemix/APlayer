package remix.myplayer.ui.dialog;

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.ui.activity.BaseActivity;
import remix.myplayer.ui.activity.PlayListActivity;
import remix.myplayer.util.XmlUtil;

/**
 * Created by taeja on 16-3-17.
 */

/**
 * 添加播放列表的对话框
 */

public class AddPlayListDialog extends BaseActivity {
    @BindView(R.id.playlist_add_edit)
    EditText mEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlist_add);
        ButterKnife.bind(this);

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
    protected void onStart() {
        super.onStart();
        overridePendingTransition(R.anim.popup_in,0);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.popup_out);
    }
}
