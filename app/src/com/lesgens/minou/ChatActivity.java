package com.lesgens.minou;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Future;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import se.emilsjolander.stickylistheaders.WrapperViewList.OnRefreshListener;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.lesgens.minou.adapters.ChatAdapter;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.controllers.PreferencesController;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.enums.MessageType;
import com.lesgens.minou.enums.SendingStatus;
import com.lesgens.minou.fragments.FileTransferDialogFragment;
import com.lesgens.minou.fragments.FileTransferDialogFragment.FileTransferListener;
import com.lesgens.minou.fragments.ListenAudioFragment;
import com.lesgens.minou.fragments.MessageDialogFragment;
import com.lesgens.minou.listeners.CrossbarConnectionListener;
import com.lesgens.minou.listeners.EventsListener;
import com.lesgens.minou.listeners.FetchMoreMessagesListener;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.Event;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.models.Topic;
import com.lesgens.minou.models.User;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.receivers.NetworkStateReceiver;
import com.lesgens.minou.receivers.NetworkStateReceiver.NetworkStateReceiverListener;
import com.lesgens.minou.utils.NotificationHelper;
import com.lesgens.minou.utils.Utils;

public class ChatActivity extends MinouFragmentActivity implements OnClickListener, EventsListener, 
			NetworkStateReceiverListener, CrossbarConnectionListener, OnItemClickListener, 
			OnItemLongClickListener, OnScrollListener, FileTransferListener, FetchMoreMessagesListener,
			OnRefreshListener {
	private static final String TAG = "ChatActivity";
	private ChatAdapter chatAdapter;
	private StickyListHeadersListView listMessages;
	private EditText editText;
	private Future<?> future;
	private TextView tvConnectionProblem;
	private TextView channelTextView;
	private NetworkStateReceiver networkStateReceiver;
	private String channelNamespace;
	private Channel channel;
	private String mFilename;
	private MediaRecorder mRecorder = null;
	private ImageView audioBtn;
	private Handler handler;
	private ProgressBar progressAudio;

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

		handler = new Handler(getMainLooper());

		channelTextView = (TextView) findViewById(R.id.channel_name);

		channelNamespace = getIntent().getStringExtra("namespace");
		channel = Controller.getInstance().getChannelsContainer().getChannelByName(channelNamespace);

		tvConnectionProblem = (TextView) findViewById(R.id.connection_problem);

		editText = (EditText) findViewById(R.id.editText);
		editText.clearFocus();

		audioBtn = (ImageView) findViewById(R.id.audio_btn);
		audioBtn.setOnClickListener(this);
		
		progressAudio = (ProgressBar) findViewById(R.id.audio_progress);

		findViewById(R.id.send).setOnClickListener(this);

		if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) == false){
			findViewById(R.id.send_ft).setVisibility(View.GONE);
		} else{
			findViewById(R.id.send_ft).setOnClickListener(this);
		}
		
		findViewById(R.id.take_picture_ft).setOnClickListener(this);
		findViewById(R.id.pick_picture_ft).setOnClickListener(this);
		findViewById(R.id.pick_video_ft).setOnClickListener(this);

		findViewById(R.id.back_btn).setOnClickListener(this);
		findViewById(R.id.settings_btn).setOnClickListener(this);

		listMessages = (StickyListHeadersListView) findViewById(R.id.list);
		listMessages.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);
		listMessages.setOnItemLongClickListener(this);
		listMessages.setOnItemClickListener(this);
		listMessages.setOnScrollListener(this);
		
		listMessages.setOnRefreshListener(this);


		networkStateReceiver = new NetworkStateReceiver(this);

//		if(isTopic()) {
//			((TextView) findViewById(R.id.name)).setText(Utils.capitalizeFirstLetters(((Topic) channel).getDescription()));
//			((TextView) findViewById(R.id.users_connected)).setText(((Topic) channel).getCount() + "");
//			//animateSlideInDetails(0);
//		} else {
			findViewById(R.id.topic_details).setVisibility(View.GONE);
//		}
	}

	//	private void animateSlideOutDetails(final int delay){
	//		ExpandCollapseAnimation anim = new ExpandCollapseAnimation(findViewById(R.id.topic_details), 500, ExpandCollapseAnimation.COLLAPSE);
	//		anim.setStartOffset(delay);
	//		findViewById(R.id.topic_details).startAnimation(anim);
	//	}

	//	private void animateSlideInDetails(final int delay){
	//		ExpandCollapseAnimation anim = new ExpandCollapseAnimation(findViewById(R.id.topic_details), 500, ExpandCollapseAnimation.EXPAND);
	//		anim.setStartOffset(delay);
	//		anim.setAnimationListener(new AnimationListener(){
	//
	//			@Override
	//			public void onAnimationEnd(Animation animation) {
	//				animateSlideOutDetails(2000);
	//			}
	//
	//			@Override
	//			public void onAnimationRepeat(Animation animation) {
	//
	//			}
	//
	//			@Override
	//			public void onAnimationStart(Animation animation) {
	//
	//			}});
	//		findViewById(R.id.topic_details).startAnimation(anim);
	//	}

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
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Log.i(TAG, "onActivityResult");
		if ((requestCode == FileTransferDialogFragment.PICK_IMAGE_ACTIVITY_REQUEST_CODE || requestCode == FileTransferDialogFragment.CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) && resultCode == Activity.RESULT_OK) {
			Uri uri = data != null ? data.getData() : null;
			if(uri == null) {
				uri = Uri.parse(FileTransferDialogFragment.tempFilename);
			}
			Message message = FileTransferDialogFragment.prepareAndSendPicture(this, uri, channelNamespace);
			chatAdapter.addMessage(message);
			chatAdapter.notifyDataSetChanged();
			scrollMyListViewToBottom();
		} else if ((requestCode == FileTransferDialogFragment.PICK_VIDEO_ACTIVITY_REQUEST_CODE || requestCode == FileTransferDialogFragment.CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) && resultCode == Activity.RESULT_OK) {
			Uri uri = data.getData();
			Message message = FileTransferDialogFragment.prepareAndSendVideo(this, uri, channelNamespace);
			chatAdapter.addMessage(message);
			chatAdapter.notifyDataSetChanged();
			scrollMyListViewToBottom();
		}
	}

	@Override
	public void onResume(){
		super.onResume();

		refreshChannel();

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

		if (mRecorder != null) {
			mRecorder.release();
			mRecorder = null;
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
			new FileTransferDialogFragment(this, channelNamespace).show(getSupportFragmentManager(), "file_share_dialog");
		} else if(v.getId() == R.id.settings_btn){
			ChannelSettingsActivity.show(this, channelNamespace);
		} else if(v.getId() == R.id.audio_btn){
			toggleAudioRecording();
		} else if(v.getId() == R.id.take_picture_ft) {
			takePhoto();
		} else if(v.getId() == R.id.pick_picture_ft) {
			pickPicture();
		} else if(v.getId() == R.id.pick_video_ft){
			pickVideo();
		}
	}

	
	private void toggleAudioRecording(){
		if(progressAudio.getVisibility() == View.GONE) {
			try{
				onRecord(true);
				progressAudio.setVisibility(View.VISIBLE);
			} catch(IllegalStateException ise) {
				ise.printStackTrace();
				Toast.makeText(this, R.string.audio_already_recording, Toast.LENGTH_SHORT).show();
			}
		} else{
			progressAudio.setVisibility(View.GONE);
			if(mRecorder != null) {
				handler.postDelayed(new Runnable(){

					@Override
					public void run() {
						onRecord(false);
						Message message = FileTransferDialogFragment.sendAudio(ChatActivity.this, mFilename, channelNamespace);
						chatAdapter.addMessage(message);
						chatAdapter.notifyDataSetChanged();
						scrollMyListViewToBottom();
					}}, 300);
			} else {
				Toast.makeText(this, R.string.audio_error_recording, Toast.LENGTH_SHORT).show();
			}
		}
	}

	private void sendMessage(final String text){
		Message message = new Message(Controller.getInstance().getMyself(), text, false, SendingStatus.PENDING, MessageType.TEXT);
		chatAdapter.addMessage(message);
		chatAdapter.notifyDataSetChanged();
		Server.sendMessage(message, channelNamespace);
		DatabaseHelper.getInstance().addMessage(message, Controller.getInstance().getId(), channelNamespace, true);
		editText.setText("");
		scrollMyListViewToBottom();
	}

	public void retryMessage(final Message message){
		deleteMessage(message, false);

		if(message.getMsgType() == MessageType.IMAGE){
			try {
				FileTransferDialogFragment.sendPicture(this, Utils.read(new File(message.getDataPath())), channelNamespace);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if(message.getMsgType() == MessageType.TEXT){
			sendMessage(message.getContent());
		} else if(message.getMsgType() == MessageType.VIDEO){
			try {
				FileTransferDialogFragment.sendVideo(this, Utils.read(new File(message.getDataPath())), channelNamespace);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if(message.getMsgType() == MessageType.AUDIO){
			FileTransferDialogFragment.sendAudio(this, message.getDataPath(), channelNamespace);
		}
	}

	public void deleteMessage(final Message message, final boolean deleteFile){
		if(deleteFile && message.getDataPath() != null) {
			File file = new File(message.getDataPath());
			file.delete();
		}
		DatabaseHelper.getInstance().removeMessage(message);
		chatAdapter.remove(message);
		chatAdapter.notifyDataSetChanged();
	}

	public void addContact(final User user){
		Server.subscribeToConversation(this, user);
		DatabaseHelper.getInstance().setUserAsContact(user);
	}

	public void pmUser(final User user){
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
		HomeActivity.show(this, isPrivate() ? 1 : 0);
		finish();
	}

	private boolean isPrivate(){
		return channel instanceof User;
	}

	private boolean isTopic(){
		return channel instanceof Topic;
	}

	@Override
	public void onNewEvent(final Event event) {
		Log.i(TAG, "channel received=" + event.getChannelNamespace()+ " this channel=" + channelNamespace);
		if(!event.getChannelNamespace().equals(channelNamespace)){
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
		final Message message = chatAdapter.getItem(arg2 - 1);
		MessageDialogFragment.newInstance(this, message.getId().toString()).show(getSupportFragmentManager(), "message_dialog");

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
	public void onDisconnected() {
		tvConnectionProblem.setVisibility(View.VISIBLE);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Message message = chatAdapter.getItem(position - 1);

		if(message != null){ 
			if(message.getMsgType() == MessageType.IMAGE) {
				ImageViewerActivity.show(this, message.getId().toString());
			} else if(message.getMsgType() == MessageType.VIDEO) {
				PlayVideoActivity.show(this, message.getDataPath());
			} else if(message.getMsgType() == MessageType.AUDIO) {
				new ListenAudioFragment(message.getDataPath()).show(getSupportFragmentManager(), ListenAudioFragment.class.getName());
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

	@Override
	public void onDialogClosed(Message message) {
		chatAdapter.addMessage(message);
		chatAdapter.notifyDataSetChanged();
		scrollMyListViewToBottom();
	}


	private void onRecord(boolean start) throws IllegalStateException{
		if (start) {
			startRecording();
		} else {
			stopRecording();
		}
	}

	private void startRecording() throws IllegalStateException {
		mRecorder = new MediaRecorder();
		mRecorder.reset();
		mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
		File cacheDir = new File(getCacheDir().getAbsoluteFile().getAbsolutePath());
		cacheDir.mkdirs();
		mFilename = getCacheDir().getAbsolutePath() + "/" + Controller.getInstance().getId() + "_" + System.currentTimeMillis() + ".3gp";
		mRecorder.setOutputFile(mFilename);
		mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

		try {
			mRecorder.prepare();
		} catch (IOException e) {
			Log.e(TAG, "prepare() failed");
		}

		mRecorder.start();
	}

	private void stopRecording() {
		mRecorder.stop();
		mRecorder.release();
		mRecorder = null;
	}

	@Override
	public void onMessagesFetch(final ArrayList<Message> messages) {
		runOnUiThread(new Runnable(){

			@Override
			public void run() {
				for(int i = messages.size() - 1 ; i >= 0; i--) {
					chatAdapter.addMessage(messages.get(i), 0);
				}
				listMessages.onRefreshComplete();
			}});
	}

	@Override
	public void onRefresh() {
		Server.getMoreMessages(channel, ChatActivity.this);
	}
	
	public void pickPicture() {
		Intent i = new Intent(
				Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

		startActivityForResult(i, FileTransferDialogFragment.PICK_IMAGE_ACTIVITY_REQUEST_CODE);
	}

	public void pickVideo() {
		Intent i = new Intent(
				Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);

		startActivityForResult(i, FileTransferDialogFragment.PICK_VIDEO_ACTIVITY_REQUEST_CODE);
	}

	public void takePhoto() {
		Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.parse(FileTransferDialogFragment.tempFilename));
		startActivityForResult(i, FileTransferDialogFragment.CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
	}

	public void takeVideo() {
		Intent i = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
		//MMS video quality for smaller transfers (400ko for 5 seconds video instead of 10mo)
		i.putExtra(android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 0);
		startActivityForResult(i, FileTransferDialogFragment.CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
	}
}
