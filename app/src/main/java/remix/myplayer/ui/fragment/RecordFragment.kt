package remix.myplayer.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_record.*
import remix.myplayer.R
import remix.myplayer.helper.MusicServiceRemote
import remix.myplayer.ui.activity.RecordShareActivity
import remix.myplayer.ui.fragment.base.BaseMusicFragment

/**
 * Created by Remix on 2015/12/28.
 */
/**
 * 心情记录的Fragment
 */
class RecordFragment : BaseMusicFragment() {
  //  @BindView(R.id.edit_record)
//  var mEdit: EditText? = null
  private var shareSuccess = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    pageName = RecordFragment::class.java.simpleName
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_record, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    sharebtn.setOnClickListener { v: View? ->
//            if (mEdit.getText().toString().equals("")) {
//                ToastUtil.show(mContext,R.string.plz_input_sharecontent);
//                return;
//            }
      val intent = Intent(requireContext(), RecordShareActivity::class.java)
      val arg = Bundle()
      arg.putString(RecordShareActivity.EXTRA_CONTENT, edit_record.text.toString())
      arg.putSerializable(RecordShareActivity.EXTRA_SONG, MusicServiceRemote.getCurrentSong())
      intent.putExtras(arg)
      startActivityForResult(intent, REQUEST_SHARE)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (data != null && requestCode == REQUEST_SHARE && resultCode == Activity.RESULT_OK) {
      shareSuccess = data.getBooleanExtra("ShareSuccess", false)
    }
  }

  companion object {
    const val REQUEST_SHARE = 1
  }
}