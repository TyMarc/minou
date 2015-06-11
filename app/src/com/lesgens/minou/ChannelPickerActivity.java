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
import android.view.View;
import android.view.View.OnClickListener;

import com.lesgens.minou.controllers.Controller;

public class ChannelPickerActivity extends FragmentActivity implements OnClickListener{
	private static final int REQUEST_ADD_CHANNEL = 101;
	private MinouPagerAdapter mMinouPagerAdapter;
	private ViewPager mViewPager;
	private ArrayList<MinouFragment> fragments;

	public static void show(final Context context){
		Intent i = new Intent(context, ChannelPickerActivity.class);
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
		
		findViewById(R.id.add_channel).setOnClickListener(this);
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
		}
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.add_channel){
			AddAChannelActivity.show(this, false, Controller.getInstance().getCurrentChannel().getNamespace(), REQUEST_ADD_CHANNEL);
		}
	}
}