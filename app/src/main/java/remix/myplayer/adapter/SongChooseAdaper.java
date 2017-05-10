package remix.myplayer.adapter;

import android.content.Context;
import android.net.Uri;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.asynctask.AsynLoadImage;
import remix.myplayer.interfaces.OnSongChooseListener;
import remix.myplayer.model.mp3.MP3Item;
import remix.myplayer.ui.activity.SongChooseActivity;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/21 10:02
 */

public class SongChooseAdaper extends BaseAdapter<SongChooseAdaper.SongChooseHolder> {
    private OnSongChooseListener mCheckListener;
    private ArrayList<Integer> mCheckSongIdList = new ArrayList<>();

    public SongChooseAdaper(Context context,OnSongChooseListener l) {
        super(context);
        mCheckListener = l;
    }

    public ArrayList<Integer> getCheckedSong(){
        return mCheckSongIdList;
    }

    @Override
    public SongChooseHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SongChooseHolder(LayoutInflater.from(mContext).inflate(R.layout.item_song_choose,parent,false));
    }

    @Override
    public void onBindViewHolder(final SongChooseHolder holder, int position) {
        if(mCursor.moveToPosition(position)){
            final MP3Item temp = new MP3Item(mCursor.getInt(SongChooseActivity.mSongIdIndex),
                    mCursor.getString(SongChooseActivity.mDisPlayNameIndex),
                    mCursor.getString(SongChooseActivity.mTitleIndex),
                    "",
                    mCursor.getInt(SongChooseActivity.mAlbumIdIndex),
                    mCursor.getString(SongChooseActivity.mArtistIndex),0,"","",0,"","",0);

            //歌曲名
            String name = CommonUtil.processInfo(temp.getTitle(),CommonUtil.SONGTYPE);
            holder.mSong.setText(name);
            //艺术家
            String artist = CommonUtil.processInfo(temp.getArtist(),CommonUtil.ARTISTTYPE);
            holder.mArtist.setText(artist);
            //封面
            holder.mImage.setImageURI(Uri.EMPTY);

            new AsynLoadImage(holder.mImage).execute(temp.getAlbumId(), Constants.URL_ALBUM);
            //选中歌曲
            holder.mRoot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.mCheck.setChecked(!holder.mCheck.isChecked());
                    mCheckListener.OnSongChoose(mCheckSongIdList != null && mCheckSongIdList.size() > 0);
                }
            });

            final int audioId = temp.getId();
            holder.mCheck.setOnCheckedChangeListener(null);
            holder.mCheck.setChecked(mCheckSongIdList != null && mCheckSongIdList.contains(audioId));
            holder.mCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked && !mCheckSongIdList.contains(audioId)){
                        mCheckSongIdList.add(audioId);
                    } else if (!isChecked){
                        mCheckSongIdList.remove(Integer.valueOf(audioId));
                    }
                    mCheckListener.OnSongChoose(mCheckSongIdList != null && mCheckSongIdList.size() > 0);
                }
            });
        }
    }

    class SongChooseHolder extends BaseViewHolder{
        @BindView(R.id.checkbox)
        AppCompatCheckBox mCheck;
        @BindView(R.id.item_img)
        SimpleDraweeView mImage;
        @BindView(R.id.item_song)
        TextView mSong;
        @BindView(R.id.item_album)
        TextView mArtist;
        @BindView(R.id.item_root)
        RelativeLayout mRoot;
        SongChooseHolder(View itemView) {
            super(itemView);
        }
    }
}
