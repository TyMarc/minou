package com.lesgens.minou;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.lesgens.minou.adapters.ChatAdapter;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.controllers.PreferencesController;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.enums.MessageType;
import com.lesgens.minou.enums.SendingStatus;
import com.lesgens.minou.listeners.CrossbarConnectionListener;
import com.lesgens.minou.listeners.EventsListener;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.Event;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.models.User;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.receivers.NetworkStateReceiver;
import com.lesgens.minou.receivers.NetworkStateReceiver.NetworkStateReceiverListener;
import com.lesgens.minou.utils.FileManager;
import com.lesgens.minou.utils.NotificationHelper;
import com.lesgens.minou.utils.Utils;

public class ChatActivity extends MinouFragmentActivity implements OnClickListener, EventsListener, NetworkStateReceiverListener, CrossbarConnectionListener, OnItemClickListener, OnItemLongClickListener, OnScrollListener {
	private static final String TAG = "ChannelChatActivity";
	private ChatAdapter chatAdapter;
	private StickyListHeadersListView listMessages;
	private EditText editText;
	private Future<?> future;
	private TextView tvConnectionProblem;
	private TextView channelTextView;
	private NetworkStateReceiver networkStateReceiver;
	private Uri imageUri;
	private String channelNamespace;
	private Channel channel;

	public static void show(final Context context, final String namespace){
		Intent i = new Intent(context, ChatActivity.class);
		i.putExtra("namespace", namespace);
		context.startActivity(i);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.chat);

		channelTextView = (TextView) findViewById(R.id.channel_name);

		channelNamespace = getIntent().getStringExtra("namespace");
		channel = Controller.getInstance().getChannelsContainer().getChannelByName(channelNamespace);
		
		tvConnectionProblem = (TextView) findViewById(R.id.connection_problem);

		editText = (EditText) findViewById(R.id.editText);
		editText.clearFocus();

		findViewById(R.id.send).setOnClickListener(this);

		if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) == false){
			findViewById(R.id.send_ft).setVisibility(View.GONE);
		} else{
			findViewById(R.id.send_ft).setOnClickListener(this);
		}

		findViewById(R.id.back_btn).setOnClickListener(this);
		findViewById(R.id.settings_btn).setOnClickListener(this);

		listMessages = (StickyListHeadersListView) findViewById(R.id.list);
		listMessages.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
		listMessages.setOnItemLongClickListener(this);
		listMessages.setOnItemClickListener(this);
		listMessages.setOnScrollListener(this);

		networkStateReceiver = new NetworkStateReceiver(this);

		refreshChannel();
	}

	public void refreshChannel(){
		if(channel instanceof User){
			channelTextView.setText(Utils.capitalizeFirstLetters(((User) channel).getUsername()));
		} else{
			channelTextView.setText(Utils.capitalizeFirstLetters(channel.getName()));	
		}

		NotificationHelper.cancelAll(this);

		chatAdapter = new ChatAdapter(this, DatabaseHelper.getInstance().getMessages(channel), channel instanceof User);
		listMessages.setAdapter(chatAdapter);
	}

	@Override
	public void onResume(){
		super.onResume();

		Server.addEventsListener(this);
		Server.addCrossbarConnectionListener(this);
		networkStateReceiver.addListener(this);
		this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
	}

	@Override
	public void onPause(){
		super.onPause();
		if(future != null){
			future.cancel(true);
		}

		Server.removeEventsListener(this);
		Server.removeCrossbarConnectionListener(this);
		networkStateReceiver.removeListener(this);
		this.unregisterReceiver(networkStateReceiver);
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.send){
			final String text = editText.getText().toString();
			if(!text.isEmpty()){
				sendMessage(text);
			}
		} else if(v.getId() == R.id.back_btn){
			onBackPressed();
		} else if(v.getId() == R.id.send_ft){
			showMenuFT();
		} else if(v.getId() == R.id.settings_btn){
			ChannelSettingsActivity.show(this, channelNamespace);
		}
	}

	private void sendMessage(final String text){
		Message message = new Message(Controller.getInstance().getMyself(), text, false, SendingStatus.PENDING, MessageType.TEXT);
		chatAdapter.addMessage(message);
		chatAdapter.notifyDataSetChanged();
		Server.sendMessage(message, channelNamespace);
		DatabaseHelper.getInstance().addMessage(message, Controller.getInstance().getId(), channelNamespace);
		editText.setText("");
		scrollMyListViewToBottom();
	}

	private void showMenuFT(){
		CharSequence fts[] = new CharSequence[] {getResources().getString(R.string.take_picture), 
				getResources().getString(R.string.pick_picture), getResources().getString(R.string.take_video), 
				getResources().getString(R.string.pick_video)};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.file_transfer);
		builder.setItems(fts, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch(which){
				case 0:
					FileManager.takePhoto(ChatActivity.this);
					break;
				case 1:
					FileManager.pickPicture(ChatActivity.this);
					break;
				case 2:
					FileManager.takeVideo(ChatActivity.this);
					break;
				case 3:
					FileManager.pickVideo(ChatActivity.this);
					break;
				}
			}
		});
		builder.show();
	}

	private void showLongClickBubble(final Message message){
		CharSequence fts[];

		final boolean isContact = DatabaseHelper.getInstance().isContact(message.getUser().getId());
		if(!isContact){
			fts = new CharSequence[] {getResources().getString(R.string.dialog_add_contact), 
					getResources().getString(R.string.dialog_add_contact_pm), getResources().getString(R.string.dialog_delete_message)};
		} else {
			fts = new CharSequence[] {getResources().getString(R.string.dialog_pm)
					, getResources().getString(R.string.dialog_delete_message)};
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.options);
		builder.setItems(fts, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(!isContact){
					switch(which){
					case 0:
						addContact(message.getUser());
						break;
					case 1:
						addContact(message.getUser());
						pmUser(message.getUser());
						break;
					case 2:
						deleteMessage(message);
						break;
					}
				} else{
					switch(which){
					case 0:
						pmUser(message.getUser());
						break;
					case 1:
						deleteMessage(message);
						break;
					}
				}
			}
		});
		builder.show();
	}

	private void showLongClickBubblePrivateOrOwn(final Message message){
		CharSequence fts[] = null;
		Log.i(TAG, "My status is=" + message.getStatus());

		if(message.getStatus() != SendingStatus.FAILED){
			fts = new CharSequence[] {getResources().getString(R.string.dialog_delete_message)};
		} else {
			fts = new CharSequence[] {getResources().getString(R.string.dialog_delete_message), getResources().getString(R.string.dialog_retry_message)};
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.options);
		builder.setItems(fts, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {

				switch(which){
				case 0:
					deleteMessage(message);
					break;
				case 1:
					retryMessage(message);
					break;
				}
			}
		});
		builder.show();
	}

	private void retryMessage(final Message message){
		deleteMessage(message);

		if(message.getMsgType() == MessageType.IMAGE){
			try {
				FileManager.sendPicture(this, Utils.read(new File(message.getDataPath())), channelNamespace);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if(message.getMsgType() == MessageType.TEXT){
			sendMessage(message.getContent());
		}
	}

	private void deleteMessage(final Message message){
		DatabaseHelper.getInstance().removeMessage(message);
		chatAdapter.remove(message);
		chatAdapter.notifyDataSetChanged();
	}

	private void addContact(final User user){
		DatabaseHelper.getInstance().setUserAsContact(user);
	}

	private void pmUser(final User user){
		ChatActivity.show(ChatActivity.this, user.getNamespace());
		finish();
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

	@Override
	public void onBackPressed(){
		HomeActivity.show(this, isPrivate() ? 0 : 2);
		finish();
	}

	private boolean isPrivate(){
		return channel instanceof User;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == FileManager.PICK_IMAGE_ACTIVITY_REQUEST_CODE || requestCode == FileManager.CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) && resultCode == RESULT_OK) {
			imageUri = data.getData();
			if(imageUri == null) {
				File photo = new File(getCacheDir(),  "sending.jpg");
				imageUri = Uri.fromFile(photo);
			}
			Message message = FileManager.preparePicture(this, imageUri, channelNamespace);
			chatAdapter.addMessage(message);
			chatAdapter.notifyDataSetChanged();
		} else if ((requestCode == FileManager.PICK_VIDEO_ACTIVITY_REQUEST_CODE || requestCode == FileManager.CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) && resultCode == RESULT_OK) {
			imageUri = data.getData();
			Message message = FileManager.prepareVideo(this, imageUri, channelNamespace);
			chatAdapter.addMessage(message);
			chatAdapter.notifyDataSetChanged();
			scrollMyListViewToBottom();
		}
	}

	@Override
	public void onNewEvent(final Event event) {
		Log.i(TAG, "channel received=" + event.getChannel().getName() + " this channel=" + channelNamespace);
		if(!event.getChannel().getNamespace().equals(channelNamespace)){
			return ;
		}
		runOnUiThread(new Runnable(){

			@Override
			public void run() {
				if(!PreferencesController.getBlockedPeople(ChatActivity.this).contains(((Message) event).getUser().getId())){
					chatAdapter.addMessage((Message) event);
					chatAdapter.notifyDataSetChanged();
					scrollMyListViewToBottom();
				}				
			}});

	}

	@Override
	public boolean onItemLongClick(final AdapterView<?> arg0, final View arg1,
			final int arg2, final long arg3) {
		final Message message = chatAdapter.getItem(arg2);
		if(message.getUser().getId().equals(Controller.getInstance().getId()) || isPrivate()){
			showLongClickBubblePrivateOrOwn(message);
		} else{
			showLongClickBubble(message);
		}

		return true;
	}




	@Override
	public void onNetworkAvailable() {
		tvConnectionProblem.setVisibility(View.GONE);
		for(int i=0;i<((ViewGroup) findViewById(R.id.bottomBar)).getChildCount();i++){
			View child=((ViewGroup) findViewById(R.id.bottomBar)).getChildAt(i);
			child.setEnabled(true);
		}
		findViewById(R.id.bottomBar).setBackgroundColor(Color.WHITE);
	}

	@Override
	public void onNetworkUnavailable() {
		tvConnectionProblem.setVisibility(View.VISIBLE);
		for(int i=0;i<((ViewGroup) findViewById(R.id.bottomBar)).getChildCount();i++){
			View child=((ViewGroup) findViewById(R.id.bottomBar)).getChildAt(i);
			child.setEnabled(false);
		}
		findViewById(R.id.bottomBar).setBackgroundColor(getResources().getColor(R.color.grey));
	}

	@Override
	public void onConnected() {
		tvConnectionProblem.setVisibility(View.GONE);
	}

	@Override
	public void onConnecting() {		
	}

	@Override
	public void onDisonnected() {
		tvConnectionProblem.setVisibility(View.VISIBLE);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Message message = chatAdapter.getItem(position);

		if(message != null){ 
			if(message.getMsgType() == MessageType.IMAGE) {
				ImageViewerActivity.show(this, message.getId().toString());
			} else if(message.getMsgType() == MessageType.VIDEO) {
				PlayVideoActivity.show(this, message.getDataPath());
			}
		}
	}

	public void notifyAdapter() {
		runOnUiThread(new Runnable(){

			@Override
			public void run() {
				chatAdapter.notifyDataSetChanged();
			}});
	}

	public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) 
	{

	}

	public void onScrollStateChanged(AbsListView view, int scrollState) 
	{
		if(scrollState == OnScrollListener.SCROLL_STATE_IDLE) {
			chatAdapter.setLoadImages(true);
			chatAdapter.notifyDataSetChanged();
		} else{
			chatAdapter.setLoadImages(false);
		}
	}

	public String getNamespace() {
		return channelNamespace;
	}
}
