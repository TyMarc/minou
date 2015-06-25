package com.lesgens.minou;

import com.lesgens.minou.application.MinouApplication;

import android.app.Activity;

public class MinouActivity extends Activity{
	
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
