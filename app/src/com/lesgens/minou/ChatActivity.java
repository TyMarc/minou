package com.lesgens.minou;

import java.util.concurrent.Future;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.desmond.squarecamera.CameraActivity;
import com.lesgens.minou.adapters.ChatAdapter;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.controllers.PreferencesController;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.enums.MessageType;
import com.lesgens.minou.enums.SendingStatus;
import com.lesgens.minou.listeners.CrossbarConnectionListener;
import com.lesgens.minou.listeners.EventsListener;
import com.lesgens.minou.models.Event;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.models.User;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.receivers.NetworkStateReceiver;
import com.lesgens.minou.receivers.NetworkStateReceiver.NetworkStateReceiverListener;
import com.lesgens.minou.utils.ExpandCollapseAnimation;
import com.lesgens.minou.utils.NotificationHelper;
import com.lesgens.minou.utils.Utils;

public class ChatActivity extends MinouFragmentActivity implements OnClickListener, EventsListener, NetworkStateReceiverListener, CrossbarConnectionListener, OnItemClickListener, OnItemLongClickListener {
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int PICK_IMAGE_ACTIVITY_REQUEST_CODE = 101;
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
	private TopicsFragment publicChooserFragment;
	private boolean animationOnGoing;

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

		publicChooserFragment = TopicsFragment.createFragmentWithoutBottomBar();

		channelTextView = (TextView) findViewById(R.id.channel_name);

		final String channelName = getIntent().getStringExtra("channelName");
		if(channelName != null){
			Controller.getInstance().setCurrentChannel(channelName);
		}
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
		findViewById(R.id.public_btn).setOnClickListener(this);

		listMessages = (StickyListHeadersListView) findViewById(R.id.list);
		listMessages.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
		listMessages.setOnItemLongClickListener(this);
		listMessages.setOnItemClickListener(this);

		networkStateReceiver = new NetworkStateReceiver(this);

		refreshChannel();
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
			ChannelSettingsActivity.show(this);
		} else if(v.getId() == R.id.public_btn){
			togglePublicChooser();
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
		CharSequence fts[] = new CharSequence[] {getResources().getString(R.string.take_picture), getResources().getString(R.string.pick_picture)};

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.file_transfer);
		builder.setItems(fts, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch(which){
				case 0:
					takePhoto();
					break;
				case 1:
					pickPicture();
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
			sendPicture(message.getData());
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
		Controller.getInstance().setCurrentChannel(user);
		ChatActivity.show(ChatActivity.this);
		finish();
	}

	private void togglePublicChooser() {
		if(!animationOnGoing){
			if(findViewById(R.id.public_chooser).getVisibility() == View.GONE){
				animationOnGoing = true;
				FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
				ft.add(R.id.public_chooser, publicChooserFragment).commit();
				Animation dropDown = new ExpandCollapseAnimation(findViewById(R.id.public_chooser), 200, 0);
				dropDown.setAnimationListener(new AnimationListener(){

					@Override
					public void onAnimationStart(Animation animation) {
						findViewById(R.id.public_chooser_sep).setVisibility(View.VISIBLE);
					}

					@Override
					public void onAnimationEnd(Animation animation) {
						animationOnGoing = false;
					}

					@Override
					public void onAnimationRepeat(Animation animation) {}});
				findViewById(R.id.public_chooser).startAnimation(dropDown);
			} else{
				animationOnGoing = true;
				Animation dropDown = new ExpandCollapseAnimation(findViewById(R.id.public_chooser), 200, 1);
				dropDown.setAnimationListener(new AnimationListener(){

					@Override
					public void onAnimationStart(Animation animation) {}

					@Override
					public void onAnimationEnd(Animation animation) {
						findViewById(R.id.public_chooser_sep).setVisibility(View.GONE);
						FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
						ft.remove(publicChooserFragment).commit();
						animationOnGoing = false;
					}

					@Override
					public void onAnimationRepeat(Animation animation) {}});
				findViewById(R.id.public_chooser).startAnimation(dropDown);
			}
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

	public void pickPicture() {
		Intent i = new Intent(
				Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

		startActivityForResult(i, PICK_IMAGE_ACTIVITY_REQUEST_CODE);
	}

	public void takePhoto() {
		Intent startCustomCameraIntent = new Intent(this, CameraActivity.class);
		startActivityForResult(startCustomCameraIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
	}

	@Override
	public void onBackPressed(){
		HomeActivity.show(this, isPrivate() ? 0 : 2);
		finish();
	}

	private boolean isPrivate(){
		return Controller.getInstance().getCurrentChannel() instanceof User;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == PICK_IMAGE_ACTIVITY_REQUEST_CODE || requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) && resultCode == RESULT_OK) {
			imageUri = data.getData();
			preparePicture();
		}
	}

	private void preparePicture(){
		getContentResolver().notifyChange(imageUri, null);

		new Handler(getMainLooper()).post(new Runnable(){

			@Override
			public void run() {
				try {
					Bitmap bitmap = android.provider.MediaStore.Images.Media
							.getBitmap(getContentResolver(), imageUri);

					final byte[] byteArray = Utils.prepareImageFT(ChatActivity.this, bitmap, imageUri);

					sendPicture(byteArray);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}});
	}

	private void sendPicture(byte[] byteArray){
		String filename = Controller.getInstance().getId() + "_" + System.currentTimeMillis() + ".jpeg";
		Message message = new Message(Controller.getInstance().getMyself(), filename, byteArray, false, SendingStatus.PENDING, MessageType.IMAGE);
		chatAdapter.addMessage(message);
		chatAdapter.notifyDataSetChanged();

		Server.sendPicture(message, channelNamespace);

		DatabaseHelper.getInstance().addMessage(message, Controller.getInstance().getId(), channelNamespace);
		scrollMyListViewToBottom();
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

		if(message != null && message.getMsgType() == MessageType.IMAGE) {
			ImageViewerActivity.show(this, message.getId().toString());
		}
	}

	public void notifyAdapter() {
		runOnUiThread(new Runnable(){

			@Override
			public void run() {
				chatAdapter.notifyDataSetChanged();
			}});
	}
}
