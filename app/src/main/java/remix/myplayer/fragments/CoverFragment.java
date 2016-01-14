package remix.myplayer.fragments;

import android.app.ActionBar;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;


import java.io.File;

import remix.myplayer.R;
import remix.myplayer.activities.AudioHolderActivity;
import remix.myplayer.utils.MP3Info;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/12/2.
 */
public class CoverFragment extends Fragment {
    private ImageView mImage;
    private Bitmap mBitmap;
    private MP3Info mInfo;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mInfo = (MP3Info)getArguments().getSerializable("MP3Info");
        View rootView = inflater.inflate(R.layout.cover,container,false);
        mImage = (ImageView)rootView.findViewById(R.id.cover_image);
//        mImage.setBackgroundResource(R.drawable.bg_cover_corners);
//        mImage.setMinimumHeight(AudioHolderActivity.mWidth - 10);
//        mImage.setMinimumWidth(AudioHolderActivity.mWidth - 10);
        if(mInfo != null && (mBitmap = Utility.CheckBitmapBySongId((int)mInfo.getId(),false)) != null)
            mImage.setImageBitmap(mBitmap);

        return rootView;
//        LinearLayout layout = new LinearLayout(getContext());
//        layout.setLayoutParams(new  ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
//        mImage = new ImageView(getContext());
//        mBitmap = BitmapFactory.decodeFile(mInfo.getAlbumArt());
//        if(mBitmap != null)
//            mImage.setImageBitmap(mBitmap);
//        else
//            mImage.setImageResource(R.drawable.no_art_normal);
//        mImage.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT));
//        layout.addView(mImage);
//        return layout;
    }
    public void UpdateCover(Bitmap bitmap)
    {
        if(!isAdded())
            return;
        if (mImage == null)
            return;
        if(bitmap != null)
            mImage.setImageBitmap(bitmap);
        else
            mImage.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.no_art_normal));
    }
}
