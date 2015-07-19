package com.lesgens.minou;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;

import com.lesgens.minou.adapters.MinouPagerAdapter;
import com.lesgens.minou.listeners.CrossbarConnectionListener;
import com.lesgens.minou.listeners.EventsListener;
import com.lesgens.minou.models.Event;
import com.lesgens.minou.models.User;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.receivers.NetworkStateReceiver;
import com.lesgens.minou.receivers.NetworkStateReceiver.NetworkStateReceiverListener;
import com.lesgens.minou.views.SlidingTabLayout;

public class HomeActivity extends FragmentActivity implements OnPageChangeListener, EventsListener, NetworkStateReceiverListener, CrossbarConnectionListener{
	private MinouPagerAdapter mMinouPagerAdapter;
	private ViewPager mViewPager;
	private ArrayList<MinouFragment> fragments;
	private int selectedPosition;
	private ConversationsFragment privateChannelChooserFragment;
	private TopicsFragment publicChannelChooserFragment;
	private ProfileFragment profileFragment;
	private ContactsFragment contactsFragment;
	private SlidingTabLayout tabs;
	private NetworkStateReceiver networkStateReceiver;
	
	public static void show(final Context context){
		Intent i = new Intent(context, HomeActivity.class);
		context.startActivity(i);
	}

	public static void show(final Context context, int position){
		Intent i = new Intent(context, HomeActivity.class);
		i.putExtra("position", position);
		context.startActivity(i);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);

		fragments = new ArrayList<MinouFragment>();
		privateChannelChooserFragment = new ConversationsFragment();
		fragments.add(privateChannelChooserFragment);
		contactsFragment = new ContactsFragment();
		fragments.add(contactsFragment);
		publicChannelChooserFragment = new TopicsFragment();
		fragments.add(publicChannelChooserFragment);
		profileFragment = new ProfileFragment();
		fragments.add(profileFragment);

		mMinouPagerAdapter =
				new MinouPagerAdapterImpl(
						getSupportFragmentManager());
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mMinouPagerAdapter);
		mViewPager.addOnPageChangeListener(this);

		selectedPosition = getIntent().getIntExtra("position", 0);
		mViewPager.setCurrentItem(selectedPosition);
		
		 // Assiging the Sliding Tab Layout View
        tabs = (SlidingTabLayout) findViewById(R.id.tabs);
 
        // Setting Custom Color for the Scroll bar indicator of the Tab View
        tabs.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {


			@Override
			public int getDividerColor(int position) {
				return getResources().getColor(R.color.dark_main_color_semi_trans);
			}

			@Override
			public int getIndicatorColor(int position) {
				return getResources().getColor(R.color.light_main_color);
			}
        });
 
        // Setting the ViewPager For the SlidingTabsLayout
        tabs.setViewPager(mViewPager);

        networkStateReceiver = new NetworkStateReceiver(this);
	}
	
	private class MinouPagerAdapterImpl extends MinouPagerAdapter{

		public MinouPagerAdapterImpl(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int position) {
			return fragments.get(position);
		}

		@Override
		public int getDrawableId(int position) {
			return fragments.get(position).getTitleDrawableId();
		}

		@Override
		public int getCount() {
			return fragments.size();
		}
		
	}
	
	public void onResume(){
		super.onResume();
		Server.addEventsListener(this);
		Server.addCrossbarConnectionListener(this);
		networkStateReceiver.addListener(this);
		this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
	}

	@Override
	public void onPause(){
		super.onPause();
		Server.removeEventsListener(this);
		Server.removeCrossbarConnectionListener(this);
		networkStateReceiver.removeListener(this);
		this.unregisterReceiver(networkStateReceiver);
	}

	@Override
	public void onBackPressed(){
		if(selectedPosition != 0){
			mViewPager.setCurrentItem(0);
		} else if (privateChannelChooserFragment.isPickerOpen()) {
			privateChannelChooserFragment.closePicker();
		} else{
			super.onBackPressed();
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
	
	@Override
	public void onNetworkAvailable() {
		findViewById(R.id.connection_problem).setVisibility(View.GONE);
	}

	@Override
	public void onNetworkUnavailable() {
		findViewById(R.id.connection_problem).setVisibility(View.VISIBLE);
	}

	@Override
	public void onConnected() {
		findViewById(R.id.connection_problem).setVisibility(View.GONE);
	}

	@Override
	public void onConnecting() {
	}

	@Override
	public void onDisonnected() {
		findViewById(R.id.connection_problem).setVisibility(View.VISIBLE);
	}
}