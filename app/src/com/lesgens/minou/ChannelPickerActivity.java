package com.lesgens.minou;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;

import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.listeners.EventsListener;
import com.lesgens.minou.models.Event;
import com.lesgens.minou.models.User;
import com.lesgens.minou.network.Server;

public class ChannelPickerActivity extends FragmentActivity implements OnClickListener, OnPageChangeListener, EventsListener{
	private static final int REQUEST_ADD_CHANNEL = 101;
	private MinouPagerAdapter mMinouPagerAdapter;
	private ViewPager mViewPager;
	private ArrayList<MinouFragment> fragments;
	private FloatingActionButton floatingActionButton;
	private boolean hiddenFAB;
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
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
		    Window window = getWindow();
		    window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
		    window.setStatusBarColor(getResources().getColor(R.color.dark_main_color));
		}

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

		Server.addEventsListener(this);
		
		final boolean isPrivate = getIntent().getBooleanExtra("isPrivate", true);
		selectedPosition = isPrivate ? 0 : 1;
		mViewPager.setCurrentItem(selectedPosition);
		initFAB();
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
		
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

	public void initFAB(){
		Log.i("ChannelPickerActivity", "initFAB");
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
			findViewById(R.id.pager_title_strip).setBackgroundColor(getResources().getColor(R.color.dark_main_color));
			hiddenFAB = false;
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
		final int currentColor = ((ColorDrawable) findViewById(R.id.pager_title_strip).getBackground()).getColor();
		if(position != 1){
			ObjectAnimator colorFade = ObjectAnimator.ofObject(findViewById(R.id.pager_title_strip), "backgroundColor", new ArgbEvaluator(), currentColor, getResources().getColor(R.color.main_color));
			colorFade.setDuration(300);
			colorFade.start();
			floatingActionButton.animate().translationY(500);
			hiddenFAB = true;
		} else if(hiddenFAB) {
			ObjectAnimator colorFade = ObjectAnimator.ofObject(findViewById(R.id.pager_title_strip), "backgroundColor", new ArgbEvaluator(), currentColor, getResources().getColor(R.color.dark_main_color));
			colorFade.setDuration(300);
			colorFade.start();
			floatingActionButton.animate().translationY(0);
			hiddenFAB = false;
		}
	}

	@Override
	public void onNewEvent(Event event, String channel) {
		if(event.getChannel() instanceof User){
			runOnUiThread(new Runnable(){

				@Override
				public void run() {
					privateChannelChooserFragment.getAdapter().notifyDataSetChanged();
				}});
		}
	}

	@Override
	public void onUserHistoryReceived(List<Event> events) {
		// TODO Auto-generated method stub
		
	}
}