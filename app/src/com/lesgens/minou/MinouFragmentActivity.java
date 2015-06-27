package com.lesgens.minou;

import com.lesgens.minou.application.MinouApplication;

import android.support.v4.app.FragmentActivity;

public class MinouFragmentActivity extends FragmentActivity {
	@Override
	public void onPause(){
		super.onPause();
		MinouApplication.activityPaused();
		MinouApplication.setCurrentActivity(null);
	}
	
	@Override
	public void onResume(){
		super.onResume();
		MinouApplication.activityResumed();
		MinouApplication.setCurrentActivity(this);
	}
}
