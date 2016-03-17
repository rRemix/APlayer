package remix.myplayer.ui.popupwindow;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import remix.myplayer.R;
import remix.myplayer.activities.BaseActivity;
import remix.myplayer.activities.PlayListActivity;
import remix.myplayer.utils.XmlUtil;

/**
 * Created by taeja on 16-3-17.
 */
public class AddPlayListDialog extends BaseActivity {
    private EditText mEdit;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlist_add);

        WindowManager.LayoutParams lp = getWindow().getAttributes();

        getWindow().setAttributes(lp);
        getWindow().setGravity(Gravity.CENTER);

        mEdit = (EditText)findViewById(R.id.playlist_add_edit);
        mEdit.getBackground().setColorFilter(getResources().getColor(R.color.intersperse_color), PorterDuff.Mode.SRC_ATOP);
        mEdit.setText("本地歌单" + PlayListActivity.mPlaylist.size());
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
}
