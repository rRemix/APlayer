package com.umeng.example.analytics;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTabHost;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.umeng.analytics.MobclickAgent;
import com.umeng.example.R;

/**
 * <p>
 * The demo shows how to integrate analytic SDK into Application based on
 * 'Fragment'. PageView ( like Fragment or viewgroup) can be tracked with new
 * API.
 * <p/>
 * <p>
 * SDKV4.6.2 之前 页面访问只能统计到 'Activity' 级别，不能统计到每个 'Fragment' .现在 新增的页面统计API，可以用来统计
 * Fragment 这样颗粒度更细的页面。
 * </P>
 *
 * @author ntop
 *
 */
public class FragmentTabs extends FragmentActivity {
    private FragmentTabHost mTabHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.umeng_example_analytics_fragment_tabs);
        mTabHost = (FragmentTabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup(this, getSupportFragmentManager(), R.id.realtabcontent);

        mTabHost.addTab(mTabHost.newTabSpec("simple").setIndicator("Simple"), FragmentSimple.class, null);
        mTabHost.addTab(mTabHost.newTabSpec("contacts").setIndicator("Contacts"), FragmentContacts.class, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    public static class FragmentSimple extends Fragment {
        private final String mPageName = "FragmentSimple";

        static FragmentSimple newInstance(int num) {
            FragmentSimple f = new FragmentSimple();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putInt("num", num);
            f.setArguments(args);

            return f;
        }

        @Override
        public void onPause() {
            // TODO Auto-generated method stub
            super.onPause();
            MobclickAgent.onPageEnd(mPageName);
        }

        @Override
        public void onResume() {
            // TODO Auto-generated method stub
            super.onResume();
            MobclickAgent.onPageStart(mPageName);
        }

        /**
         * The Fragment's UI is just a simple text view showing its instance
         * number.
         */
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            TextView tv = new TextView(getActivity());
            tv.setText("Fragment Simple");
            return tv;
        }
    }

    public static class FragmentContacts extends Fragment {
        private final String mPageName = "FragmentContacts";

        static FragmentSimple newInstance(int num) {
            FragmentSimple f = new FragmentSimple();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putInt("num", num);
            f.setArguments(args);

            return f;
        }

        /**
         * The Fragment's UI is just a simple text view showing its instance
         * number.
         */
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            TextView tv = new TextView(getActivity());
            tv.setText("Fragment Contacts");
            return tv;
        }

        @Override
        public void onPause() {
            // TODO Auto-generated method stub
            super.onPause();
            MobclickAgent.onPageEnd(mPageName);
        }

        @Override
        public void onResume() {
            // TODO Auto-generated method stub
            super.onResume();
            MobclickAgent.onPageStart(mPageName);
        }
    }
}