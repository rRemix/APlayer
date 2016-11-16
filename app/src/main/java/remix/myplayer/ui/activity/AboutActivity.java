package remix.myplayer.ui.activity;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringConfig;
import com.facebook.rebound.SpringSystem;
import com.umeng.analytics.MobclickAgent;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.util.DensityUtil;

/**
 * Created by Remix on 2016/3/26.
 */
public class AboutActivity extends ToolbarActivity {
    @BindView(R.id.toolbar)
    Toolbar mToolBar;
    @BindView(R.id.view)
    View view;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ButterKnife.bind(this);
        initToolbar(mToolBar, getString(R.string.about));

        final double startValue = (1080 - DensityUtil.dip2px(this,240)) / 2;
        final double endValue = 1080;
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Spring start = SpringSystem.create().createSpring();
                start.addListener(new SimpleSpringListener(){
                    @Override
                    public void onSpringUpdate(Spring spring) {
                        view.setX((float) spring.getCurrentValue());
//                        if(spring.getCurrentValue() - endValue < 10)
//                            spring.setAtRest();
                    }

                    @Override
                    public void onSpringAtRest(Spring spring) {
                        view.setX((float) startValue);
                        final Spring end = SpringSystem.create().createSpring();
                        end.addListener(new SimpleSpringListener(){
                            @Override
                            public void onSpringUpdate(Spring spring) {
                                view.setScaleX((float) spring.getCurrentValue());
                                view.setScaleY((float) spring.getCurrentValue());
                            }
                        });
                        end.setSpringConfig(new SpringConfig(200,23));
                        end.setCurrentValue(0.6);
                        end.setEndValue(1);
                    }
                });
                start.setSpringConfig(new SpringConfig(200,23));
                start.setRestDisplacementThreshold(40);
                start.setRestSpeedThreshold(40);
                start.setCurrentValue(startValue);
                start.setEndValue(endValue);
            }
        });

    }

    public void onResume() {
        MobclickAgent.onPageStart(AboutActivity.class.getSimpleName());
        super.onResume();
    }
    public void onPause() {
        MobclickAgent.onPageEnd(AboutActivity.class.getSimpleName());
        super.onPause();
    }

}
