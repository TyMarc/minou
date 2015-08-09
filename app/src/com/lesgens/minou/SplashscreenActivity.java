package com.lesgens.minou;

import java.io.File;
import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;
import com.lesgens.minou.application.MinouApplication;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.listeners.CrossbarConnectionListener;
import com.lesgens.minou.listeners.UserAuthenticatedListener;
import com.lesgens.minou.models.Geolocation;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.utils.Utils;
import com.todddavies.components.progressbar.ProgressWheel;

public class SplashscreenActivity extends MinouActivity implements
UserAuthenticatedListener, CrossbarConnectionListener, LocationListener {
	private Location mLastLocation;
	private static final String[] PERMISSIONS = {"public_profile"};
	private boolean mConnected = false;
	private static boolean authenticated = false;
	private static boolean geolocated = false;

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
		if(Server.isConnected()){
			goToHome();
			return;
		}
		Controller.getInstance().setDimensionAvatar(this);
		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.splashscreen);

		DatabaseHelper.init(this);

		TextView tv = (TextView) findViewById(R.id.splash_text);

		Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/Raleway_Thin.otf");
		tv.setTypeface(tf);

		if(!isNetworkAvailable() && !isFinishing()){
			new AlertDialog.Builder(this).setPositiveButton(R.string.ok, new OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}})
				.setTitle(R.string.network)
				.setMessage(R.string.no_network)
				.show();
		} else{
			LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
			if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
				buildAlertMessageNoGps();
			}
			locationManager.requestSingleUpdate(LocationManager.NETWORK_PROVIDER, this, getMainLooper());
		}

		Server.addUserAuthenticatedListener(this);
		Server.addCrossbarConnectionListener(this);
		LoginButton authButton = (LoginButton)findViewById(R.id.authButton);
		authButton.setReadPermissions(PERMISSIONS);
		uiHelper = new UiLifecycleHelper(this, callback);
		uiHelper.onCreate(savedInstanceState);
	}

	private void buildAlertMessageNoGps() {
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.location_disabled)
		.setCancelable(false)
		.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int id) {
				startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			}
		})
		.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			public void onClick(final DialogInterface dialog, final int id) {
				dialog.cancel();
			}
		});
		final AlertDialog alert = builder.create();
		alert.show();
	}

	public void onDestroy(){
		super.onDestroy();
		if(uiHelper != null){
			uiHelper.onDestroy();
		}
		Server.removeCrossbarConnectionListener(this);
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

	public void goToHome(){
		Server.removeCrossbarConnectionListener(this);
		HomeActivity.show(this);
		finish();
	}

	@Override
	public void onUserNetworkErrorAuthentication() {
		new AlertDialog.Builder(this).setPositiveButton(R.string.ok, new OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				finish();
			}})
			.setTitle(R.string.network)
			.setMessage(R.string.no_network)
			.show();
	}

	@Override
	public void onUserServerErrorAuthentication() {
		new AlertDialog.Builder(this).setPositiveButton(R.string.ok, new OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {

			}})
			.setTitle(R.string.network)
			.setMessage(R.string.server_error)
			.show();
	}

	@Override
	public void onConnected() {
		goToHome();
	}

	@Override
	public void onLocationChanged(Location location) {
		mLastLocation = location;
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
			} catch (Exception e) {
				e.printStackTrace();
				executeGeocoderFallback(mLastLocation.getLatitude(), mLastLocation.getLongitude(), this);
			}
		}
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnecting() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDisonnected() {
		// TODO Auto-generated method stub

	}

	public static void executeGeocoderFallback(double lat, double lng, final Context context){
		AsyncTask<Double, Void, Address> request = new AsyncTask<Double, Void, Address>() {

			@Override
			protected Address doInBackground(Double... arg0) {
				return Utils.getFromLocation(arg0[0], arg0[1]);
			}

			@Override
			protected void onPostExecute(Address address) {
				super.onPostExecute(address);
				if(address != null){
					final String city = address.getLocality();
					final String country = address.getCountryName();
					final String state = address.getAdminArea();
					android.util.Log.i("Minou", "Fallback City name=" + city);
					Controller.getInstance().setCity(new Geolocation(city, state, country));
					geolocated = true;
					if(authenticated){
						Server.connectToCrossbar(context);
					}
				} else{
					new AlertDialog.Builder(context).setPositiveButton(R.string.retry, new OnClickListener(){

						@Override
						public void onClick(DialogInterface dialog, int which) {
							SplashscreenActivity.show(context);
							if(MinouApplication.getCurrentActivity() != null) {
								MinouApplication.getCurrentActivity().finish();
							}
						}})
						.setNegativeButton(R.string.no, null)
						.setTitle(R.string.location)
						.setMessage(R.string.location_not_found)
						.show();

				}

			}


		};
		request.execute(lat, lng);
	}

}
