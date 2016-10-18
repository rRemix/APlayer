package remix.myplayer.ui.dialog;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import remix.myplayer.R;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Global;
import remix.myplayer.util.PlayListUtil;

/**
 * Created by taeja on 16-3-17.
 */

/**
 * 添加播放列表的对话框
 */

public class AddPlayListDialog extends BaseDialogActivity {
    @BindView(R.id.playlist_add_edit)
    EditText mEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_playlist_add);
        ButterKnife.bind(this);

        WindowManager.LayoutParams lp = getWindow().getAttributes();

        getWindow().setAttributes(lp);
        getWindow().setGravity(Gravity.CENTER);

        //修改下划线颜色
        //修改光标颜色
        Theme.setTinit(mEdit,ColorUtil.getColor(ThemeStore.MATERIAL_COLOR_PRIMARY),true);
        mEdit.setText("本地歌单" + Global.mPlayList.size());

    }

    @OnClick({R.id.playlist_continue,R.id.playlist_cancel})
    public void onClikc(View v){
        switch (v.getId()){
            case R.id.playlist_continue:
                String name = ((EditText)findViewById(R.id.playlist_add_edit)).getText().toString();
                if (!TextUtils.isEmpty(name)) {
                    PlayListUtil.addPlayList(name);
                    if(getIntent().getBooleanExtra("FromPlayListActivity",false)){
                        setResult(Activity.RESULT_OK);
                    } else {
                        Intent intent = new Intent();
                        intent.putExtra("PlayListName",name);
                        setResult(Activity.RESULT_OK,intent);
                    }
                } else {
                    Toast.makeText(AddPlayListDialog.this,R.string.add_playlist_error,Toast.LENGTH_SHORT).show();
                }
                finish();
                break;
            case R.id.playlist_cancel:
                finish();
                break;
        }
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
