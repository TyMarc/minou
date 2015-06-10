package com.lesgens.minou;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
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
import com.lesgens.minou.adapters.PrivateChannelsAdapter;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.controllers.PreferencesController;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.listeners.EventsListener;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.Event;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.models.User;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.receivers.NetworkStateReceiver;
import com.lesgens.minou.receivers.NetworkStateReceiver.NetworkStateReceiverListener;
import com.lesgens.minou.utils.NotificationHelper;
import com.lesgens.minou.utils.Utils;
import com.lesgens.minou.views.CustomYesNoDialog;

public class ChannelChatActivity extends MinouActivity implements OnClickListener, EventsListener, OnOpenListener, NetworkStateReceiverListener {
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int CHANNEL_PICKER_REQUEST_CODE = 101;
	private static final int PRIVATE_PICKER_REQUEST_CODE = 102;
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
	private PrivateChannelsAdapter privatesAdapter;
	private ScheduledExecutorService scheduler;
	private Future<?> future;
	private TextView tvConnectionProblem;
	private TextView channelTextView;
	private NetworkStateReceiver networkStateReceiver;
	private Uri imageUri;
	private String channelNamespace;

	public static void show(final Context context, final String channelNamespace){
		Intent i = new Intent(context, ChannelChatActivity.class);
		i.putExtra("channelNamespace", channelNamespace);
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

		editText = (EditText) findViewById(R.id.editText);
		editText.clearFocus();

		slidingMenu = (SlidingMenu) findViewById(R.id.slidingmenulayout);
		slidingMenu.setOnOpenListener(this);

		sendBt = (ImageView) findViewById(R.id.send);
		sendBt.setOnClickListener(this);

		initChannelName(getIntent().getStringExtra("channelNamespace"));

		if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) == false){
			findViewById(R.id.send_picture).setVisibility(View.GONE);
		} else{
			findViewById(R.id.send_picture).setOnClickListener(this);
		}

		findViewById(R.id.add_channel).setOnClickListener(this);
		findViewById(R.id.add_private).setOnClickListener(this);

		menuPrivate = (ImageView) findViewById(R.id.menu_private);
		menuPrivate.setOnClickListener(this);

		chatAdapter = new ChannelChatAdapter(this, DatabaseHelper.getInstance().getMessages(channelNamespace), false); //TODO
		listMessages = (StickyListHeadersListView) findViewById(R.id.list);
		listMessages.setAdapter(chatAdapter);
		listMessages.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
		listMessages.setOnItemLongClickListener(new OnItemLongClickListenerUser());

		channelsAdapter = new ChannelsAdapter(this, Controller.getInstance().getCurrentChannel().getChannels());
		listChannels = (ListView) findViewById(R.id.list_channels);
		listChannels.setAdapter(channelsAdapter);
		listChannels.setOnItemClickListener(new OnItemClickListenerChannel());
		listChannels.setOnItemLongClickListener(new OnItemLongClickListenerChannel());

		privatesAdapter = new PrivateChannelsAdapter(this, DatabaseHelper.getInstance().getPrivateChannels());
		listPrivate = (ListView) findViewById(R.id.list_private);
		listPrivate.setAdapter(privatesAdapter);
		listPrivate.setOnItemClickListener(new OnItemClickListenerPrivate());
		listPrivate.setOnItemLongClickListener(new OnItemLongClickListenerPrivateChannel());

		networkStateReceiver = new NetworkStateReceiver(this);


		Server.setEventsListener(this);

		scheduler = Executors.newSingleThreadScheduledExecutor();

	}
	
	public void initChannelName(final String channelNamespace){
		this.channelNamespace = channelNamespace;
		
		channelTextView.setText(channelNamespace.substring(channelNamespace.lastIndexOf(".")));
		
		NotificationHelper.cancelAll(this);
	}



	public void setChannelName(final String channelName){
		initChannelName(channelName);
		
		chatAdapter.clear();
		chatAdapter.addAll(DatabaseHelper.getInstance().getMessages(channelName));
		slidingMenu.toggle(true);
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
				Message message = new Message(Controller.getInstance().getMyself(), text, false);
				chatAdapter.addMessage(message);
				chatAdapter.notifyDataSetChanged();
				Log.i(TAG, "Sending message to channel=" + channelNamespace);
				Server.sendMessage(message.getMessage());
				DatabaseHelper.getInstance().addMessage(message, Controller.getInstance().getMyId(), channelNamespace);
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
			ConnectToChannelActivity.show(this, false, channelNamespace, CHANNEL_PICKER_REQUEST_CODE);
		} else if(v.getId() == R.id.add_private){
			ConnectToChannelActivity.show(this, true, channelNamespace, PRIVATE_PICKER_REQUEST_CODE);
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
			if (resultCode == RESULT_OK) {
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

							Server.sendMessage(byteArray, channelNamespace);

							DatabaseHelper.getInstance().addMessage(message, Controller.getInstance().getMyId(), channelNamespace);
							scrollMyListViewToBottom();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}});
			}
		} else if(requestCode == CHANNEL_PICKER_REQUEST_CODE){
			if(resultCode == RESULT_OK){
//				channelsAdapter.add(data.getAction());
//				setChannelName(data.getAction(), null);
			}
		} else if(requestCode == PRIVATE_PICKER_REQUEST_CODE){
			if(resultCode == RESULT_OK){
//				User user = Controller.getInstance().getUser(data.getAction());
//				privatesAdapter.add(user);
//				setChannelName(user.getName(), user.getId());
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
		if(!channel.equals(channelNamespace)){
			return false;
		}
		runOnUiThread(new Runnable(){

			@Override
			public void run() {
				for(Event e : events){
					if(!Controller.getInstance().getBlockedPeople(ChannelChatActivity.this).contains(((Message) e).getUser().getId())){
						chatAdapter.addMessage((Message) e);
						chatAdapter.notifyDataSetChanged();
						scrollMyListViewToBottom();
					}
				}
			}});

		return true;
	}

	private class OnItemClickListenerPrivate implements OnItemClickListener{
		@Override
		public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
			final User user = privatesAdapter.getItem(position);
			if(!channelNamespace.endsWith(user.getName())){
				Log.i(TAG, "New channel name=" + user.getName());
				setChannelName(user.getNamespace());
			}
		}
	}

	private class OnItemClickListenerChannel implements OnItemClickListener{
		@Override
		public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
			Channel channel = channelsAdapter.getItem(position);
			if(!channelNamespace.equals(channel.getNamespace())){
				Log.i(TAG, "New channel name=" + channel);
				Controller.getInstance().setCurrentChannel(channel);
				setChannelName(channel.getNamespace());
			}
		}
	}

	private class OnItemLongClickListenerChannel implements OnItemLongClickListener{


		@Override
		public boolean onItemLongClick(final AdapterView<?> arg0, final View arg1,
				final int arg2, final long arg3) {
			CustomYesNoDialog dialog = new CustomYesNoDialog(ChannelChatActivity.this){

				@Override
				public void onPositiveClick() {
					super.onPositiveClick();
					final Channel channel = channelsAdapter.getItem(arg2);
					DatabaseHelper.getInstance().removePublicChannel(channel.getNamespace());
					DatabaseHelper.getInstance().deleteAllMessages(channel.getNamespace());
					if(channel.getNamespace().equals(channelNamespace)){
						String defaultChannel = PreferencesController.getDefaultChannel(ChannelChatActivity.this);
						Controller.getInstance().setCurrentChannel(defaultChannel);
						setChannelName(defaultChannel);
					} else{
						slidingMenu.toggle(true);
					}
					channelsAdapter.remove(channel);
				}

			};

			dialog.show();
			dialog.setDialogText(R.string.delete_channel);			
			return true;
		}

	}

	private class OnItemLongClickListenerUser implements OnItemLongClickListener{

		@Override
		public boolean onItemLongClick(final AdapterView<?> arg0, final View arg1,
				final int arg2, final long arg3) {

			final Message message = chatAdapter.getItem(arg2);
			if(message.getUser().getId().equals(Controller.getInstance().getMyId()) || 
					DatabaseHelper.getInstance().getPrivateChannels().contains(message.getUser())){
				return true;
			}
			CustomYesNoDialog dialog = new CustomYesNoDialog(ChannelChatActivity.this){

				@Override
				public void onPositiveClick() {
					super.onPositiveClick();
					DatabaseHelper.getInstance().addPrivateChannel(message.getUser().getName(), message.getUser().getId());
					Server.subscribeToChannel(ChannelChatActivity.this, message.getUser().getId());
					setChannelName(message.getChannel().getNamespace());
				}

			};

			dialog.show();
			dialog.setDialogText(R.string.add_channel);			
			return true;
		}

	}

	private class OnItemLongClickListenerPrivateChannel implements OnItemLongClickListener{

		@Override
		public boolean onItemLongClick(final AdapterView<?> arg0, final View arg1,
				final int arg2, final long arg3) {

			final User user = privatesAdapter.getItem(arg2);
			CustomYesNoDialog dialog = new CustomYesNoDialog(ChannelChatActivity.this){

				@Override
				public void onPositiveClick() {
					super.onPositiveClick();
					DatabaseHelper.getInstance().removePrivateChannel(user.getId());
					DatabaseHelper.getInstance().deleteAllMessages(user.getId());
					if(user.getName().toLowerCase().equals(channelNamespace.toLowerCase())){
						String defaultChannel = PreferencesController.getDefaultChannel(ChannelChatActivity.this);
						Controller.getInstance().setCurrentChannel(defaultChannel);
						setChannelName(defaultChannel);
					} else{
						slidingMenu.toggle(true);
					}
					privatesAdapter.remove(user);
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
