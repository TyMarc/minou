package com.lesgens.minou;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

public class ChatActivity extends MinouActivity implements OnClickListener, EventsListener, NetworkStateReceiverListener {
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final String TAG = "ChannelChatActivity";
	private ImageView sendBt;
	private ChatAdapter chatAdapter;
	private StickyListHeadersListView listMessages;
	private EditText editText;
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

		channelTextView = (TextView) findViewById(R.id.channel_name);

		final String channelName = getIntent().getStringExtra("channelName");
		if(channelName != null){
			Controller.getInstance().setCurrentChannel(channelName);
		}
		tvConnectionProblem = (TextView) findViewById(R.id.connection_problem);

		editText = (EditText) findViewById(R.id.editText);
		editText.clearFocus();

		if(!isPrivate()){
			findViewById(R.id.topBar).setBackgroundColor(getResources().getColor(R.color.dark_main_color));
		}

		sendBt = (ImageView) findViewById(R.id.send);
		sendBt.setOnClickListener(this);

		if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) == false){
			findViewById(R.id.send_picture).setVisibility(View.GONE);
		} else{
			findViewById(R.id.send_picture).setOnClickListener(this);
		}

		findViewById(R.id.back_btn).setOnClickListener(this);
		findViewById(R.id.settings_btn).setOnClickListener(this);

		listMessages = (StickyListHeadersListView) findViewById(R.id.list);
		listMessages.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
		listMessages.setOnItemLongClickListener(new OnItemLongClickListenerUser());

		networkStateReceiver = new NetworkStateReceiver(this);

		refreshChannel();


		Server.addEventsListener(this);

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
		} else if(v.getId() == R.id.back_btn){
			onBackPressed();
		} else if(v.getId() == R.id.send_picture){
			takePhoto();
		} else if(v.getId() == R.id.settings_btn){
			ChannelSettingsActivity.show(this);
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
	public void onBackPressed(){
		ChannelPickerActivity.show(this, isPrivate());
		finish();
	}

	private boolean isPrivate(){
		return Controller.getInstance().getCurrentChannel() instanceof User;
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
	public void onNewEvent(final Event event, final String channel) {
		Log.i(TAG, "channel received=" + channel + " this channel=" + channelNamespace);
		if(!channel.equals(channelNamespace)){
			return ;
		}
		runOnUiThread(new Runnable(){

			@Override
			public void run() {
				if(!Controller.getInstance().getBlockedPeople(ChatActivity.this).contains(((Message) event).getUser().getId())){
					chatAdapter.addMessage((Message) event);
					chatAdapter.notifyDataSetChanged();
					scrollMyListViewToBottom();
				}				
			}});

	}

	private class OnItemLongClickListenerUser implements OnItemLongClickListener{

		@Override
		public boolean onItemLongClick(final AdapterView<?> arg0, final View arg1,
				final int arg2, final long arg3) {
			final Message message = chatAdapter.getItem(arg2);
			if(message.getUser().getId().equals(Controller.getInstance().getAuthId()) || isPrivate()){
				return false;
			}
			
			new AlertDialog.Builder(ChatActivity.this).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					if(DatabaseHelper.getInstance().getPrivateChannels().contains(message.getUser())){
						Controller.getInstance().setCurrentChannel(message.getUser());
					} else{
						DatabaseHelper.getInstance().addPrivateChannel(message.getUser().getUsername(), message.getUser().getId());
						Server.subscribeToPrivateChannel(ChatActivity.this, message.getUser());
					}
					ChatActivity.show(ChatActivity.this);
					finish();
				}})
				.setNegativeButton(R.string.no, null)
				.setTitle(R.string.pm)
				.setMessage(R.string.add_channel)
				.show();
			
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
