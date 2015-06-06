package com.lesgens.minou;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;
import com.lesgens.minou.adapters.ChannelChatAdapter;
import com.lesgens.minou.adapters.ChannelsAdapter;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.controllers.PreferencesController;
import com.lesgens.minou.listeners.EventsListener;
import com.lesgens.minou.models.City;
import com.lesgens.minou.models.Event;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.receivers.NetworkStateReceiver;
import com.lesgens.minou.receivers.NetworkStateReceiver.NetworkStateReceiverListener;
import com.lesgens.minou.utils.Utils;

public class ChannelChatActivity extends Activity implements OnClickListener, EventsListener, OnOpenListener, NetworkStateReceiverListener {
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private Typeface tf;
	private ImageView sendBt;
	private ChannelChatAdapter chatAdapter;
	private StickyListHeadersListView listMessages;
	private EditText editText;
	private ImageView menuPrivate;
	private SlidingMenu slidingMenu;
	private ListView listChannels;
	private ListView listPrivate;
	private ChannelsAdapter channelsAdapter;
	private ChannelsAdapter privatesAdapter;
	private ScheduledExecutorService scheduler;
	private Future<?> future;
	private TextView tvConnectionProblem;
	private NetworkStateReceiver networkStateReceiver;
	private Uri imageUri;
	private String channelName;

	public static void show(final Context context, final String channelName){
		Intent i = new Intent(context, ChannelChatActivity.class);
		i.putExtra("channelName", channelName);
		context.startActivity(i);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.public_chat_container);

		channelName = getIntent().getStringExtra("channelName");
		if(channelName == null) {
			channelName = "";
		} else{
			channelName = "/" + channelName;
		}

		TextView city = (TextView) findViewById(R.id.city_name);

		tvConnectionProblem = (TextView) findViewById(R.id.connection_problem);

		tf = Typeface.createFromAsset(getAssets(), "fonts/Raleway_Thin.otf");
		city.setTypeface(tf);
		if(channelName.isEmpty()){
			city.setText(Controller.getInstance().getCity().getId());
		} else{
			city.setText(channelName.substring(1));
		}
		editText = (EditText) findViewById(R.id.editText);
		editText.clearFocus();

		slidingMenu = (SlidingMenu) findViewById(R.id.slidingmenulayout);
		slidingMenu.setOnOpenListener(this);

		sendBt = (ImageView) findViewById(R.id.send);
		sendBt.setOnClickListener(this);


		if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) == false){
			findViewById(R.id.send_picture).setVisibility(View.GONE);
		} else{
			findViewById(R.id.send_picture).setOnClickListener(this);
		}
		
		findViewById(R.id.add_channel).setOnClickListener(this);
		findViewById(R.id.add_private).setOnClickListener(this);

		menuPrivate = (ImageView) findViewById(R.id.menu_private);
		menuPrivate.setOnClickListener(this);

		chatAdapter = new ChannelChatAdapter(this, new ArrayList<Message>());
		listMessages = (StickyListHeadersListView) findViewById(R.id.list);
		listMessages.setAdapter(chatAdapter);
		listMessages.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);

		channelsAdapter = new ChannelsAdapter(this, PreferencesController.getChannels(this));
		listChannels = (ListView) findViewById(R.id.list_channels);
		listChannels.setAdapter(channelsAdapter);
		listChannels.setOnItemClickListener(new OnItemClickListenerChannel());

		privatesAdapter = new ChannelsAdapter(this, PreferencesController.getPrivates(this));
		listPrivate = (ListView) findViewById(R.id.list_private);
		listPrivate.setAdapter(privatesAdapter);
		listPrivate.setOnItemClickListener(new OnItemClickListenerPrivate());

		networkStateReceiver = new NetworkStateReceiver(this);


		Server.addEventsListener(this);

		scheduler = Executors.newSingleThreadScheduledExecutor();

	}

	@Override
	public void onResume(){
		super.onResume();
		if(scheduler != null){
			future = scheduler.scheduleAtFixedRate
					(new Runnable() {
						public void run() {
							Server.getEvents(channelName);
						}
					}, 0, 5, TimeUnit.SECONDS);
		}

		networkStateReceiver.addListener(this);
		this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
	}

	@Override
	public void onPause(){
		super.onPause();
		if(future != null){
			future.cancel(true);
		}

		networkStateReceiver.removeListener(this);
		this.unregisterReceiver(networkStateReceiver);
	}

	@Override
	public void onDestroy(){
		super.onDestroy();
		if(scheduler != null){
			scheduler.shutdownNow();
		}
		Server.removeEventsListener(this);
	}

	@Override
	public void onBackPressed(){
		if(slidingMenu.isMenuShowing()){
			slidingMenu.toggle(true);
		} else{
			super.onBackPressed();
		}
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.send){
			final String text = editText.getText().toString();
			if(!text.isEmpty()){
				Message message = new Message(Controller.getInstance().getMyself(), text, false);
				chatAdapter.addMessage(message);
				chatAdapter.notifyDataSetChanged();
				Server.sendPublicMessage(Controller.getInstance().getCity(), message.getMessage());
				Server.sendMessage(message.getMessage());
				editText.setText("");
				scrollMyListViewToBottom();
			}
		} else if(v.getId() == R.id.menu_private){
			hideKeyboard();
			new Handler(getMainLooper()).postDelayed(new Runnable(){

				@Override
				public void run() {
					slidingMenu.toggle(true);
				}}, 200);

		} else if(v.getId() == R.id.send_picture){
			takePhoto();
		} else if(v.getId() == R.id.add_channel){
			Toast.makeText(this, "Add a channel conversation", Toast.LENGTH_SHORT).show();
		} else if(v.getId() == R.id.add_private){
			Toast.makeText(this, "Add a private conversation", Toast.LENGTH_SHORT).show();
		}
	}

	private void scrollMyListViewToBottom() {
		listMessages.post(new Runnable() {
			@Override
			public void run() {
				// Select the last row so it will scroll into view...
				listMessages.setSelection(chatAdapter.getCount() - 1);
			}
		});
	}

	public void takePhoto() {
		Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
		File photo = new File(Environment.getExternalStorageDirectory(),  "Pic.jpg");
		intent.putExtra(MediaStore.EXTRA_OUTPUT,
				Uri.fromFile(photo));
		imageUri = Uri.fromFile(photo);
		startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
			if (resultCode == Activity.RESULT_OK) {
				getContentResolver().notifyChange(imageUri, null);

				new Handler(getMainLooper()).post(new Runnable(){

					@Override
					public void run() {
						try {
							Bitmap bitmap = android.provider.MediaStore.Images.Media
									.getBitmap(getContentResolver(), imageUri);
							ByteArrayOutputStream stream = new ByteArrayOutputStream();

							final Bitmap bitmapScaled = Utils.scaleDown(bitmap, Utils.dpInPixels(ChannelChatActivity.this, 250), true);
							bitmap.recycle();
							bitmapScaled.compress(Bitmap.CompressFormat.JPEG, 100, stream);
							byte[] byteArray = stream.toByteArray();

							String encoded = Utils.MINOU_IMAGE_BASE + Base64.encodeToString(byteArray, Base64.DEFAULT);
							Message message = new Message(Controller.getInstance().getMyself(), encoded, false);
							chatAdapter.addMessage(message);
							chatAdapter.notifyDataSetChanged();
							Server.sendPublicMessage(Controller.getInstance().getCity(), message.getMessage());
							Server.sendMessage(message.getMessage());
							scrollMyListViewToBottom();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}});
			}
		}   
	}


	private void hideKeyboard(){
		InputMethodManager imm = (InputMethodManager)getSystemService(
				Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
	}

	@Override
	public void onEventsReceived(final List<Event> events) {
		runOnUiThread(new Runnable(){

			@Override
			public void run() {
				for(Event e : events){
					if(e instanceof Message && e.getDestination() instanceof City){
						if(!Controller.getInstance().getBlockedPeople(ChannelChatActivity.this).contains(((Message) e).getUser().getId())){
							chatAdapter.addMessage((Message) e);
							chatAdapter.notifyDataSetChanged();
							scrollMyListViewToBottom();
						}
					}
				}
			}});
	}

	private class OnItemClickListenerPrivate implements OnItemClickListener{
		@Override
		public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
			String user = ((String) adapter.getItemAtPosition(position));
			PrivateChatActivity.show(ChannelChatActivity.this, user	, user);
			slidingMenu.toggle(true);
		}
	}

	private class OnItemClickListenerChannel implements OnItemClickListener{
		@Override
		public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
			String channel = ((String) adapter.getItemAtPosition(position));
			if(!channelName.endsWith(channel)){
				ChannelChatActivity.show(ChannelChatActivity.this, channel);
				slidingMenu.toggle(true);
			}
		}
	}

	@Override
	public void onUserHistoryReceived(List<Event> events) {
	}

	@Override
	public void onOpen() {
		hideKeyboard();
	}

	@Override
	public void onNetworkAvailable() {
		tvConnectionProblem.setVisibility(View.GONE);
		sendBt.setEnabled(true);
	}

	@Override
	public void onNetworkUnavailable() {
		tvConnectionProblem.setVisibility(View.VISIBLE);
		sendBt.setEnabled(false);
	}
}
