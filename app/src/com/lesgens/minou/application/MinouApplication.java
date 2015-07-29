package com.lesgens.minou.application;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

public class MinouApplication extends Application {
	private static final String TAG = "Minou_Application";
	private static Activity currentActivity = null;
	private static MinouApplication instance;

	@Override
	public void onCreate(){
		super.onCreate();
		instance = this;
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
	
	public static MinouApplication getInstance() {
		return instance;
	}
}