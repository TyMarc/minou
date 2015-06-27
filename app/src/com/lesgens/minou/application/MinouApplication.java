package com.lesgens.minou.application;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import com.lesgens.minou.MinouActivity;

public class MinouApplication extends Application {
	private static final String TAG = "Minou_Application";
	private static Activity currentActivity = null;

	@Override
	public void onCreate(){
		super.onCreate();
	}

	public static boolean isActivityVisible() {
		return activityVisible;
	}  

	public static void activityResumed() {
		Log.i(TAG, "activity resumed");
		activityVisible = true;
	}

	public static void activityPaused() {
		Log.i(TAG, "activity paused");
		activityVisible = false;
	}

	private static boolean activityVisible;

	public static void setCurrentActivity(Activity activity) {
		currentActivity = activity;
	}
	
	public static Activity getCurrentActivity() {
		return currentActivity;
	}
}