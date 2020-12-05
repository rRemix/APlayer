package remix.myplayer.ui.fragment;

import static remix.myplayer.ui.activity.RecordShareActivity.EXTRA_CONTENT;
import static remix.myplayer.ui.activity.RecordShareActivity.EXTRA_SONG;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTextChanged;
import remix.myplayer.R;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.ui.activity.RecordShareActivity;
import remix.myplayer.ui.fragment.base.BaseMusicFragment;

/**
 * Created by Remix on 2015/12/28.
 */

/**
 * 心情记录的Fragment
 */
public class RecordFragment extends BaseMusicFragment {

  @BindView(R.id.edit_record)
  EditText mEdit;

  public static final int REQUEST_SHARE = 1;
  private boolean mShareSuccess;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPageName = RecordFragment.class.getSimpleName();
  }

  @Nullable
  @Override
  public View onCreateView(final LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.fragment_record, container, false);
    mUnBinder = ButterKnife.bind(this, rootView);

    //启动分享心情的Activity
    (rootView.findViewById(R.id.sharebtn)).setOnClickListener(v -> {
//            if (mEdit.getText().toString().equals("")) {
//                ToastUtil.show(mContext,R.string.plz_input_sharecontent);
//                return;
//            }
      Intent intent = new Intent(mContext, RecordShareActivity.class);
      Bundle arg = new Bundle();
      arg.putString(EXTRA_CONTENT, mEdit.getText().toString());
      arg.putParcelable(EXTRA_SONG, MusicServiceRemote.getCurrentSong());
      intent.putExtras(arg);
      startActivityForResult(intent, REQUEST_SHARE);
    });
//        mRecordContainer.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//            @Override
//            public boolean onPreDraw() {
//                mRecordContainer.getViewTreeObserver().removeOnPreDrawListener(this);
//
//                int containerWidth = mRecordContainer.getWidth();
//                int containerHeight = mRecordContainer.getHeight();
//                ViewGroup.LayoutParams lp = mRecordContainer.getLayoutParams();
//                lp.width = lp.height = containerWidth > containerHeight ? mRecordContainer.getHeight() : mRecordContainer.getWidth();
//                mRecordContainer.setLayoutParams(lp);
//                return true;
//            }
//        });

    return rootView;
  }

  @OnTextChanged(value = R.id.edit_record, callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
  void afterExplainChanged(Editable s) {

  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (data != null && requestCode == REQUEST_SHARE && resultCode == Activity.RESULT_OK) {
      mShareSuccess = data.getBooleanExtra("ShareSuccess", false);
    }
  }
}

