package remix.myplayer.ui.customviews;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;

import remix.myplayer.activities.AudioHolderActivity;
import remix.myplayer.services.MusicService;
import remix.myplayer.infos.MP3Info;

/**
 * Created by Remix on 2015/12/1.
 */
public class BottomActionBar extends RelativeLayout implements View.OnClickListener{

    public BottomActionBar(Context context) {
        super(context);
    }

    public BottomActionBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
    }

    public BottomActionBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(v.getContext(), AudioHolderActivity.class);
        Bundle bundle = new Bundle();
        MP3Info temp = MusicService.getCurrentMP3();
        bundle.putSerializable("MP3Info",MusicService.getCurrentMP3());
        intent.putExtras(bundle);
        intent.putExtra("Isplay",MusicService.getIsplay());
        getContext().startActivity(intent);
    }
}
