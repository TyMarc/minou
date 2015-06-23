package com.lesgens.minou;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationServices;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.listeners.CrossbarConnectionListener;
import com.lesgens.minou.listeners.UserAuthenticatedListener;
import com.lesgens.minou.models.Geolocation;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.views.CustomYesNoDialog;
import com.todddavies.components.progressbar.ProgressWheel;

public class SplashscreenActivity extends MinouActivity implements
UserAuthenticatedListener, ConnectionCallbacks, OnConnectionFailedListener, CrossbarConnectionListener {
	private GoogleApiClient mGoogleApiClient;
	private Location mLastLocation;
	private static final String[] PERMISSIONS = {"public_profile"};
	private boolean mConnected = false;
	private boolean authenticated = false;
	private boolean geolocated = false;

	private UiLifecycleHelper uiHelper;

	private Session.StatusCallback callback = new Session.StatusCallback() {
		public void call(Session session, SessionState state, Exception exception) {
			onSessionStateChange(session, state, exception);
		}
	}; 

	public static void show(Context context){
		Intent i = new Intent(context, SplashscreenActivity.class);
		context.startActivity(i);
	}

	private void onSessionStateChange(Session session, SessionState state, Exception exception) {
		Log.i("SplashscreenActivity", "onSessionStateChange");
		Controller.getInstance().setSession(session);
		if(state.isOpened() && !mConnected){
			mConnected = true;
			session.refreshPermissions();
			List<String> permissions = session.getPermissions();
			Log.i("FACEBOOK_CONNECTION", "Logged in..." + permissions.toString());
			findViewById(R.id.authButton).setVisibility(View.GONE);
			findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
			((ProgressWheel) findViewById(R.id.progressBar)).spin();
			Server.connect(session.getAccessToken());
		} else if(state.isClosed()) {
			mConnected = false;
			Log.i("FACEBOOK_CONNECTION", "Logged out...");
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Controller.getInstance().setDimensionAvatar(this);
		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.splashscreen);

		DatabaseHelper.init(this);

		TextView tv = (TextView) findViewById(R.id.splash_text);

		Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/Raleway_Thin.otf");
		tv.setTypeface(tf);

		if(!isNetworkAvailable()){
			CustomYesNoDialog dialog = new CustomYesNoDialog(this){

				@Override
				public void onPositiveClick() {
					super.onPositiveClick();
					finish();
				}

			};

			dialog.show();
			dialog.transformAsOkDialog();
			dialog.setDialogText(R.string.no_network);
		} else{

			new Handler(getMainLooper()).postDelayed(new Runnable(){

				@Override
				public void run() {
					buildGoogleApiClient();

					mGoogleApiClient.connect();
				}

			}, 300);
		}

		Server.addUserAuthenticatedListener(this);
		Server.setCrossbarConnectionListener(this);
		LoginButton authButton = (LoginButton)findViewById(R.id.authButton);
		authButton.setReadPermissions(PERMISSIONS);
		uiHelper = new UiLifecycleHelper(this, callback);
		uiHelper.onCreate(savedInstanceState);
	}

	public void onDestroy(){
		super.onDestroy();
		uiHelper.onDestroy();

		if(mGoogleApiClient != null){
			if(mGoogleApiClient.isConnected() || mGoogleApiClient.isConnecting()){
				mGoogleApiClient.disconnect();
			}
		}

		Server.removeUserAuthenticatedListener(this);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.i("SplashscreenActivity", "onActivityResult");
		uiHelper.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.i("SplashscreenActivity", "onPause");
		uiHelper.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();
		Log.i("SplashscreenActivity", "onResume");
		Session session = Session.getActiveSession();
		if (session != null &&
				(session.isOpened() || session.isClosed()) ) {
			onSessionStateChange(session, session.getState(), null);
		}
		uiHelper.onResume();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		Log.i("SplashscreenActivity", "onSaveInstanceState");
		uiHelper.onSaveInstanceState(outState);
	}

	@Override
	public void onUserAuthenticated() {
		Log.i("SplashscreenActivity", "onUserAuthenticated");
		authenticated = true;
		if(geolocated){
			Server.connectToCrossbar(this);
		}
	}

	private boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager 
		= (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
		return activeNetworkInfo != null && activeNetworkInfo.isConnected();
	}

	public void goToPublicChat(){
		Server.setCrossbarConnectionListener(null);
		ChannelPickerActivity.show(this);
		finish();
	}
	
	protected synchronized void buildGoogleApiClient() {
		mGoogleApiClient = new GoogleApiClient.Builder(this)
		.addConnectionCallbacks(this)
		.addOnConnectionFailedListener(this)
		.addApi(LocationServices.API)
		.build();
	}
	
	@Override
	public void onConnected(Bundle connectionHint) {
		mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
				mGoogleApiClient);
		if (mLastLocation != null) {
			Geocoder geoCoder = new Geocoder(this, Locale.CANADA);
			try {
				List<Address> address = geoCoder.getFromLocation(mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);
				final String city = address.get(0).getLocality();
				final String country = address.get(0).getCountryName();
				final String state = address.get(0).getAdminArea();
				android.util.Log.i("Minou", "City name=" + city);
				Controller.getInstance().setCity(new Geolocation(city, state, country));
				geolocated = true;
				if(authenticated){
					Server.connectToCrossbar(this);
				}
			} catch (IOException e) {
				e.printStackTrace();
				CustomYesNoDialog dialog = new CustomYesNoDialog(this){
					
					@Override
					public void onPositiveClick() {
						super.onPositiveClick();
						SplashscreenActivity.show(SplashscreenActivity.this);
						finish();
					}
					
					@Override
					public void onNegativeClick() {
						super.onNegativeClick();
						finish();
					}

				};

				dialog.show();
				dialog.setYesText(R.string.retry);
				dialog.setDialogText(getString(R.string.location_not_found));
			}
			catch (NullPointerException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onUserNetworkErrorAuthentication() {
		CustomYesNoDialog dialog = new CustomYesNoDialog(this){

			@Override
			public void onPositiveClick() {
				super.onPositiveClick();
				finish();
			}

		};

		dialog.show();
		dialog.transformAsOkDialog();
		dialog.setDialogText(R.string.no_network);
	}

	@Override
	public void onUserServerErrorAuthentication() {
		CustomYesNoDialog dialog = new CustomYesNoDialog(this){

			@Override
			public void onPositiveClick() {
				super.onPositiveClick();
				finish();
			}

		};

		dialog.show();
		dialog.transformAsOkDialog();
		dialog.setDialogText(R.string.server_error);
	}

	@Override
	public void onConnectionSuspended(int cause) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnection() {
		Server.setEventsListener(null);
		goToPublicChat();
	}

}
