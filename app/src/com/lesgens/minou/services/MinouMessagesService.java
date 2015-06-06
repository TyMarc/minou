package com.lesgens.minou.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MinouMessagesService extends Service {
	   
	  private static final String TAG = MinouMessagesService.class.getSimpleName();
	   
	  @Override
	  public IBinder onBind(Intent intent) {
	    // TODO Auto-generated method stub
	    return null;
	  }
	 
	  @Override
	  public void onCreate() {
	    super.onCreate();
	    Log.i(TAG, "Service creating");
	     
	  }
	 
	  @Override
	  public void onDestroy() {
	    super.onDestroy();
	    Log.i(TAG, "Service destroying");
	     
	  }
	}