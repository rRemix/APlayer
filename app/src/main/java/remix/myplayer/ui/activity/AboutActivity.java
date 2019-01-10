package remix.myplayer.ui.activity;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.App;
import remix.myplayer.R;

/**
 * Created by Remix on 2016/3/26.
 */
public class AboutActivity extends ToolbarActivity {

  @BindView(R.id.about_text)
  TextView mVersion;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_about);

    ButterKnife.bind(this);
    try {
      PackageManager pm = App.getContext().getPackageManager();
      PackageInfo pi = pm
          .getPackageInfo(App.getContext().getPackageName(), PackageManager.GET_ACTIVITIES);
      mVersion.setText("v" + pi.versionName);
    } catch (Exception ignored) {

    }

    setUpToolbar(getString(R.string.about));
  }

}
