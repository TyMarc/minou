package com.lesgens.minou.application;

import android.app.Application;
import android.util.Log;

public class MinouApplication extends Application {
	private static final String TAG = "Minou_Application";
	
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
}