package com.lesgens.minou;

import java.util.ArrayList;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.View.OnClickListener;

import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.views.FloatingActionButton;

public class ChannelPickerActivity extends FragmentActivity implements OnClickListener, OnPageChangeListener{
	private static final int REQUEST_ADD_CHANNEL = 101;
	private MinouPagerAdapter mMinouPagerAdapter;
	private ViewPager mViewPager;
	private ArrayList<MinouFragment> fragments;
	private FloatingActionButton floatingActionButton;
	private boolean hiddenFAB;
	private int selectedPosition;

	public static void show(final Context context){
		Intent i = new Intent(context, ChannelPickerActivity.class);
		context.startActivity(i);
	}

	public static void show(final Context context, boolean isPrivate){
		Intent i = new Intent(context, ChannelPickerActivity.class);
		i.putExtra("isPrivate", isPrivate);
		context.startActivity(i);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.channel_chooser);

		fragments = new ArrayList<MinouFragment>();
		MinouFragment fragment = new PrivateChannelChooserFragment();
		fragments.add(fragment);
		fragment = new PublicChannelChooserFragment();
		fragments.add(fragment);

		mMinouPagerAdapter =
				new MinouPagerAdapter(
						getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mMinouPagerAdapter);
		mViewPager.setOnPageChangeListener(this);

		final boolean isPrivate = getIntent().getBooleanExtra("isPrivate", true);
		selectedPosition = isPrivate ? 0 : 1;
		mViewPager.setCurrentItem(selectedPosition);
		initFAB();
	}

	@Override
	public void onBackPressed(){
		if(selectedPosition != 0){
			mViewPager.setCurrentItem(0);
		} else{
			super.onBackPressed();
		}
	}

	public void initFAB(){
		floatingActionButton = (FloatingActionButton) findViewById(R.id.add_channel);
		floatingActionButton.setOnClickListener(this);
		if(selectedPosition == 0){
			floatingActionButton.animate().translationY(500).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					super.onAnimationEnd(animation);
					floatingActionButton.setVisibility(View.VISIBLE);
					hiddenFAB = true;
				}
			});;
		} else{
			floatingActionButton.setVisibility(View.VISIBLE);
		}
	}


	private class MinouPagerAdapter extends FragmentStatePagerAdapter {
		public MinouPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			return fragments.get(i);
		}

		@Override
		public int getCount() {
			return fragments.size();
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return fragments.get(position).getTitle();
		}
	}




	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == REQUEST_ADD_CHANNEL && resultCode == RESULT_OK){
			ChatActivity.show(this);
			finish();
		}
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.add_channel){
			AddAChannelActivity.show(this, false, Controller.getInstance().getCurrentChannel().getNamespace(), REQUEST_ADD_CHANNEL);
		}
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
	}

	@Override
	public void onPageSelected(int position) {
		selectedPosition = position;
		if(position == 0){
			floatingActionButton.animate().translationY(500);
		} else if(hiddenFAB){
			floatingActionButton.animate().translationY(0);
		}
	}
}