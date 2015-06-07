package com.lesgens.minou;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

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
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu.OnOpenListener;
import com.lesgens.minou.adapters.ChannelChatAdapter;
import com.lesgens.minou.adapters.ChannelsAdapter;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.controllers.PreferencesController;
import com.lesgens.minou.listeners.EventsListener;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.Event;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.receivers.NetworkStateReceiver;
import com.lesgens.minou.receivers.NetworkStateReceiver.NetworkStateReceiverListener;
import com.lesgens.minou.utils.Utils;
import com.lesgens.minou.views.CustomYesNoDialog;

public class ChannelChatActivity extends MinouActivity implements OnClickListener, EventsListener, OnOpenListener, NetworkStateReceiverListener {
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final String TAG = "ChannelChatActivity";
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
	private TextView channelTextView;
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
		
		channelTextView = (TextView) findViewById(R.id.city_name);

		tvConnectionProblem = (TextView) findViewById(R.id.connection_problem);

		tf = Typeface.createFromAsset(getAssets(), "fonts/Raleway_Thin.otf");
		channelTextView.setTypeface(tf);

		setChannelName(getIntent().getStringExtra("channelName"));
		
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

		chatAdapter = new ChannelChatAdapter(this, Controller.getInstance().getMessages(channelName));
		listMessages = (StickyListHeadersListView) findViewById(R.id.list);
		listMessages.setAdapter(chatAdapter);
		listMessages.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);

		ArrayList<String> channels = PreferencesController.getChannels(this);
		channels.add(0, Controller.getInstance().getCity().getName());
		channels.add(0, Controller.getInstance().getCity().getState());
		channels.add(0, Controller.getInstance().getCity().getCountry());
		channelsAdapter = new ChannelsAdapter(this, channels);
		listChannels = (ListView) findViewById(R.id.list_channels);
		listChannels.setAdapter(channelsAdapter);
		listChannels.setOnItemClickListener(new OnItemClickListenerChannel());
		listChannels.setOnItemLongClickListener(new OnItemLongClickListenerChannel());

		privatesAdapter = new ChannelsAdapter(this, PreferencesController.getPrivateChannels(this));
		listPrivate = (ListView) findViewById(R.id.list_private);
		listPrivate.setAdapter(privatesAdapter);
		listPrivate.setOnItemClickListener(new OnItemClickListenerPrivate());
		listPrivate.setOnItemLongClickListener(new OnItemLongClickListenerPrivateChannel());

		networkStateReceiver = new NetworkStateReceiver(this);


		Server.setEventsListener(this);

		scheduler = Executors.newSingleThreadScheduledExecutor();

	}
	
	public void setChannelName(final String newChannelName){
		channelName = newChannelName;
		if(channelName == null) {
			channelName = "";
		}

		if(channelName.isEmpty()){
			channelTextView.setText(Controller.getInstance().getCity().getName());
		} else{
			channelTextView.setText(channelName);
		}
		
		Server.setEventsListener(this);
	}

	@Override
	public void onResume(){
		super.onResume();

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
				Log.i(TAG, "Sending message to channel=" + channelName);
				Message message = new Message(Controller.getInstance().getMyself(), text, false);
				chatAdapter.addMessage(message);
				chatAdapter.notifyDataSetChanged();
				Server.sendMessage(message.getMessage(), channelName);
				Controller.getInstance().addMessage(channelName, message);
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
			ConnectToChannelActivity.show(this, false);
		} else if(v.getId() == R.id.add_private){
			ConnectToChannelActivity.show(this, true);
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

							Message message = new Message(Controller.getInstance().getMyself(), byteArray, false);
							chatAdapter.addMessage(message);
							chatAdapter.notifyDataSetChanged();
							Server.sendMessage(byteArray, channelName);
							Controller.getInstance().addMessage(channelName, message);
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
	public boolean onEventsReceived(final List<Event> events, final String channel) {
		if(!channel.equals(channelName)){
			return false;
		}
		runOnUiThread(new Runnable(){

			@Override
			public void run() {
				for(Event e : events){
					if(e instanceof Message && e.getDestination() instanceof Channel){
						if(!Controller.getInstance().getBlockedPeople(ChannelChatActivity.this).contains(((Message) e).getUser().getId())){
							chatAdapter.addMessage((Message) e);
							chatAdapter.notifyDataSetChanged();
							scrollMyListViewToBottom();
						}
					}
				}
			}});
		
		return true;
	}

	private class OnItemClickListenerPrivate implements OnItemClickListener{
		@Override
		public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
			String user = ((String) adapter.getItemAtPosition(position));
			PrivateChatActivity.show(ChannelChatActivity.this, user	, Controller.getInstance().getUser(user).getName());
			finish();
		}
	}

	private class OnItemClickListenerChannel implements OnItemClickListener{
		@Override
		public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
			String channel = ((String) adapter.getItemAtPosition(position));
			if(!channelName.endsWith(channel)){
				Log.i(TAG, "New channel name=" + channel);
				setChannelName(channel);
				chatAdapter.clear();
				chatAdapter.addAll(Controller.getInstance().getMessages(channel));
				slidingMenu.toggle(true);
			}
		}
	}
	
	private class OnItemLongClickListenerChannel implements OnItemLongClickListener{


		@Override
		public boolean onItemLongClick(final AdapterView<?> arg0, final View arg1,
				final int arg2, final long arg3) {
			if(arg2 == 0){
				return false;
			}
			CustomYesNoDialog dialog = new CustomYesNoDialog(ChannelChatActivity.this){

				@Override
				public void onPositiveClick() {
					super.onPositiveClick();
					final String channel = ((TextView) arg1.findViewById(R.id.name)).getText().toString();
					PreferencesController.removeChannel(ChannelChatActivity.this, channel);
					channelsAdapter.remove(channel);
				}

			};

			dialog.show();
			dialog.setDialogText(R.string.delete_channel);			
			return true;
		}
		
	}
	
	private class OnItemLongClickListenerPrivateChannel implements OnItemLongClickListener{

		@Override
		public boolean onItemLongClick(final AdapterView<?> arg0, final View arg1,
				final int arg2, final long arg3) {
			if(arg2 == 0){
				return false;
			}
			CustomYesNoDialog dialog = new CustomYesNoDialog(ChannelChatActivity.this){

				@Override
				public void onPositiveClick() {
					super.onPositiveClick();
					final String channel = ((TextView) arg1.findViewById(R.id.name)).getText().toString();
					PreferencesController.removePrivateChannel(ChannelChatActivity.this, channel);
					privatesAdapter.remove(channel);
				}

			};

			dialog.show();
			dialog.setDialogText(R.string.delete_channel);			
			return true;
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
