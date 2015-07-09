package com.lesgens.minou;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;

import com.lesgens.minou.listeners.EventsListener;
import com.lesgens.minou.models.Event;
import com.lesgens.minou.models.User;
import com.lesgens.minou.network.Server;

public class ChannelPickerActivity extends FragmentActivity implements OnPageChangeListener, EventsListener{
	private MinouPagerAdapter mMinouPagerAdapter;
	private ViewPager mViewPager;
	private ArrayList<MinouFragment> fragments;
	private int selectedPosition;
	private PrivateChannelChooserFragment privateChannelChooserFragment;
	private PublicChannelChooserFragment publicChannelChooserFragment;
	private ProfileFragment profileFragment;

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
		privateChannelChooserFragment = new PrivateChannelChooserFragment();
		fragments.add(privateChannelChooserFragment);
		publicChannelChooserFragment = new PublicChannelChooserFragment();
		fragments.add(publicChannelChooserFragment);
		profileFragment = new ProfileFragment();
		fragments.add(profileFragment);

		mMinouPagerAdapter =
				new MinouPagerAdapter(
						getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mMinouPagerAdapter);
		mViewPager.addOnPageChangeListener(this);

		final boolean isPrivate = getIntent().getBooleanExtra("isPrivate", true);
		selectedPosition = isPrivate ? 0 : 1;
		mViewPager.setCurrentItem(selectedPosition);
	}
	
	public void onResume(){
		super.onResume();
		Server.addEventsListener(this);
	}

	@Override
	public void onPause(){
		super.onPause();
		Server.removeEventsListener(this);
	}

	@Override
	public void onBackPressed(){
		if(selectedPosition != 0){
			mViewPager.setCurrentItem(0);
		} else{
			super.onBackPressed();
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
			return fragments.get(position).getTitle(ChannelPickerActivity.this);
		}
	}

	@Override
	public void onPageScrollStateChanged(int arg0) {
	}

	@Override
	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
	}

	@Override
	public void onPageSelected(int position) {
		selectedPosition = position;
	}

	@Override
	public void onNewEvent(Event event) {
		if(event.getChannel() instanceof User){
			runOnUiThread(new Runnable(){

				@Override
				public void run() {
					privateChannelChooserFragment.refreshList();
				}});
		}
	}
}