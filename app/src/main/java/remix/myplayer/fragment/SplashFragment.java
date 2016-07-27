package remix.myplayer.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.ui.activity.MainActivity;

/**
 * Created by taeja on 16-6-8.
 */
public class SplashFragment extends BaseFragment {
    private static final int[] mImgIds = new int[]{R.drawable.splash1,R.drawable.splash2,R.drawable.splash3,R.drawable.splash4};
    @BindView(R.id.splash_img)
    ImageView mBackground;
    @BindView(R.id.splash_btn)
    Button mStart;
    private int mIndex;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mIndex = getArguments().getInt("Index");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_splash,null);
        mUnBinder = ButterKnife.bind(this,root);

        mStart.setVisibility(mIndex == mImgIds.length - 1 ? View.VISIBLE : View.GONE);
        mStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getContext(), MainActivity.class));
            }
        });

        if(mIndex < mImgIds.length){
            mBackground.setImageResource(mImgIds[mIndex]);
        }
        return root;
    }
}
