package remix.myplayer.activities;

import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import remix.myplayer.R;
import remix.myplayer.ui.dialog.TimerDialog;

/**
 * Created by taeja on 16-3-15.
 */
public class ToolbarActivity extends BaseAppCompatActivity {

    protected void initToolbar(Toolbar toolbar,String title){
        toolbar.setTitle(title);
        toolbar.setTitleTextColor(Color.parseColor("#ffffffff"));
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.common_btn_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.toolbar_search:
                        startActivity(new Intent(ToolbarActivity.this, SearchActivity.class));
                        break;
                    case R.id.toolbar_timer:
                        startActivity(new Intent(ToolbarActivity.this, TimerDialog.class));
                        break;
                }
                return true;
            }
        });
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
