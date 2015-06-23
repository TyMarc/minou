package com.lesgens.minou;

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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.lesgens.minou.adapters.ChatAdapter;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.listeners.EventsListener;
import com.lesgens.minou.models.Event;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.models.User;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.receivers.NetworkStateReceiver;
import com.lesgens.minou.receivers.NetworkStateReceiver.NetworkStateReceiverListener;
import com.lesgens.minou.utils.NotificationHelper;
import com.lesgens.minou.utils.Utils;
import com.lesgens.minou.views.CustomYesNoDialog;

public class ChatActivity extends MinouActivity implements OnClickListener, EventsListener, NetworkStateReceiverListener {
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final String TAG = "ChannelChatActivity";
	private ImageView sendBt;
	private ChatAdapter chatAdapter;
	private StickyListHeadersListView listMessages;
	private EditText editText;
	private ImageView menuPrivate;
	private ScheduledExecutorService scheduler;
	private Future<?> future;
	private TextView tvConnectionProblem;
	private TextView channelTextView;
	private NetworkStateReceiver networkStateReceiver;
	private Uri imageUri;
	private String channelNamespace;

	public static void show(final Context context){
		Intent i = new Intent(context, ChatActivity.class);
		context.startActivity(i);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.chat);

		channelTextView = (TextView) findViewById(R.id.city_name);

		final String channelName = getIntent().getStringExtra("channelName");
		if(channelName != null){
			Controller.getInstance().setCurrentChannel(channelName);
		}
		tvConnectionProblem = (TextView) findViewById(R.id.connection_problem);

		editText = (EditText) findViewById(R.id.editText);
		editText.clearFocus();


		sendBt = (ImageView) findViewById(R.id.send);
		sendBt.setOnClickListener(this);

		if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) == false){
			findViewById(R.id.send_picture).setVisibility(View.GONE);
		} else{
			findViewById(R.id.send_picture).setOnClickListener(this);
		}

		menuPrivate = (ImageView) findViewById(R.id.menu_private);
		menuPrivate.setOnClickListener(this);

		listMessages = (StickyListHeadersListView) findViewById(R.id.list);
		listMessages.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
		listMessages.setOnItemLongClickListener(new OnItemLongClickListenerUser());

		networkStateReceiver = new NetworkStateReceiver(this);

		refreshChannel();


		Server.setEventsListener(this);

		scheduler = Executors.newSingleThreadScheduledExecutor();

	}


	public void refreshChannel(){
		channelNamespace = Controller.getInstance().getCurrentChannel().getNamespace();
		
		if(Controller.getInstance().getCurrentChannel() instanceof User){
			channelTextView.setText(Utils.capitalizeFirstLetters(((User) Controller.getInstance().getCurrentChannel()).getUsername()));
		} else{
			channelTextView.setText(Utils.capitalizeFirstLetters(Controller.getInstance().getCurrentChannel().getName()));	
		}

		NotificationHelper.cancelAll(this);

		chatAdapter = new ChatAdapter(this, DatabaseHelper.getInstance().getMessages(Controller.getInstance().getCurrentChannel()), Controller.getInstance().getCurrentChannel() instanceof User);
		listMessages.setAdapter(chatAdapter);
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
	public void onClick(View v) {
		if(v.getId() == R.id.send){
			final String text = editText.getText().toString();
			if(!text.isEmpty()){
				Message message = new Message(Controller.getInstance().getMyself(), text, false);
				chatAdapter.addMessage(message);
				chatAdapter.notifyDataSetChanged();
				Server.sendMessage(message.getMessage());
				DatabaseHelper.getInstance().addMessage(message, Controller.getInstance().getAuthId(), channelNamespace);
				editText.setText("");
				scrollMyListViewToBottom();
			}
		} else if(v.getId() == R.id.menu_private){
			onBackPressed();
		} else if(v.getId() == R.id.send_picture){
			takePhoto();
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
							
							final byte[] byteArray = Utils.prepareImageFT(ChatActivity.this, bitmap);

							Message message = new Message(Controller.getInstance().getMyself(), byteArray, false);
							chatAdapter.addMessage(message);
							chatAdapter.notifyDataSetChanged();

							Server.sendMessage(byteArray);

							DatabaseHelper.getInstance().addMessage(message, Controller.getInstance().getAuthId(), channelNamespace);
							scrollMyListViewToBottom();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}});
			}
		}
	}

	@Override
	public boolean onEventsReceived(final List<Event> events, final String channel) {
		Log.i(TAG, "channel received=" + channel + " this channel=" + channelNamespace);
		if(!channel.equals(channelNamespace)){
			return false;
		}
		runOnUiThread(new Runnable(){

			@Override
			public void run() {
				for(Event e : events){
					if(!Controller.getInstance().getBlockedPeople(ChatActivity.this).contains(((Message) e).getUser().getId())){
						chatAdapter.addMessage((Message) e);
						chatAdapter.notifyDataSetChanged();
						scrollMyListViewToBottom();
					}
				}
			}});

		return true;
	}

	private class OnItemLongClickListenerUser implements OnItemLongClickListener{

		@Override
		public boolean onItemLongClick(final AdapterView<?> arg0, final View arg1,
				final int arg2, final long arg3) {

			final Message message = chatAdapter.getItem(arg2);
			if(message.getUser().getId().equals(Controller.getInstance().getAuthId()) || 
					DatabaseHelper.getInstance().getPrivateChannels().contains(message.getUser())){
				return true;
			}
			CustomYesNoDialog dialog = new CustomYesNoDialog(ChatActivity.this){

				@Override
				public void onPositiveClick() {
					super.onPositiveClick();
					DatabaseHelper.getInstance().addPrivateChannel(message.getUser().getUsername(), message.getUser().getId());
					Server.subscribeToPrivateChannel(ChatActivity.this, message.getUser());
					refreshChannel();
				}

			};

			dialog.show();
			dialog.setDialogText(R.string.add_channel);			
			return true;
		}

	}

	

	@Override
	public void onUserHistoryReceived(List<Event> events) {
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
