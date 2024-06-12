package com.zune.customtv;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.base.base.BaseActivity;
import com.zune.customtv.fragment.AiQingFragment;
import com.zune.customtv.fragment.HomeFragment;
import com.zune.customtv.fragment.TouPingFragment;
import com.zune.customtv.fragment.VideoFragment;
import com.zune.customtv.fragment.VoiceFragment;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    public static void start(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
    }

    private String[] titles = new String[]{"首页", "影片", "音乐和录音", "爱情保卫战","投屏"};
    private List<Fragment> mFragments = new ArrayList<>();

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView() {
        setTitle("影音资料");
        mFragments.add(new HomeFragment());
        mFragments.add(new VideoFragment());
        mFragments.add(new VoiceFragment());
        mFragments.add(new AiQingFragment());
        mFragments.add(new TouPingFragment());
        LinearLayout tabLayout = findViewById(R.id.tab_layout);
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                TextView childAt = (TextView) tabLayout.getChildAt(position);
                childAt.requestFocus();
            }
        });
        viewPager.setOffscreenPageLimit(4);
        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @NonNull
            @Override
            public Fragment getItem(int position) {
                return mFragments.get(position);
            }

            @Nullable
            @Override
            public CharSequence getPageTitle(int position) {
                return titles[position];
            }

            @Override
            public int getCount() {
                return titles.length;
            }
        });
        for (int i = 0; i < titles.length; i++) {
            TextView childAt = (TextView) tabLayout.getChildAt(i);
            childAt.setTag(i);
            childAt.setText(titles[i]);
            childAt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    v.setBackgroundResource(hasFocus ? R.drawable.bg_select : R.drawable.bg_normal);
                    ((TextView)v).setTextColor(ContextCompat.getColor(MainActivity.this, hasFocus ? R.color.white : R.color.half_white));
                }
            });
            childAt.setOnClickListener(v -> {
                int position = (int) v.getTag();
                viewPager.setCurrentItem(position);
            });
        }
    }
}
