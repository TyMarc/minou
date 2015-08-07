package com.lesgens.minou;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Future;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;
import android.content.Context;
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
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
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
import com.lesgens.minou.fragments.MessageDialogFragment;
import com.lesgens.minou.listeners.CrossbarConnectionListener;
import com.lesgens.minou.listeners.EventsListener;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.Event;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.models.Topic;
import com.lesgens.minou.models.User;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.receivers.NetworkStateReceiver;
import com.lesgens.minou.receivers.NetworkStateReceiver.NetworkStateReceiverListener;
import com.lesgens.minou.utils.ExpandCollapseAnimation;
import com.lesgens.minou.utils.FileTransferDialogFragment;
import com.lesgens.minou.utils.FileTransferDialogFragment.FileTransferListener;
import com.lesgens.minou.utils.NotificationHelper;
import com.lesgens.minou.utils.Utils;

public class ChatActivity extends MinouFragmentActivity implements OnClickListener, EventsListener, NetworkStateReceiverListener, CrossbarConnectionListener, OnItemClickListener, OnItemLongClickListener, OnScrollListener, FileTransferListener {
	private static final String TAG = "ChannelChatActivity";
	private ChatAdapter chatAdapter;
	private StickyListHeadersListView listMessages;
	private EditText editText;
	private Future<?> future;
	private TextView tvConnectionProblem;
	private TextView channelTextView;
	private NetworkStateReceiver networkStateReceiver;
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
		
		if(isTopic()) {
			((TextView) findViewById(R.id.name)).setText(Utils.capitalizeFirstLetters(((Topic) channel).getDescription()));
			((TextView) findViewById(R.id.users_connected)).setText(((Topic) channel).getCount() + "");
			//animateSlideInDetails(0);
		} else {
			findViewById(R.id.topic_details).setVisibility(View.GONE);
		}
	}
	
	private void animateSlideOutDetails(final int delay){
		ExpandCollapseAnimation anim = new ExpandCollapseAnimation(findViewById(R.id.topic_details), 500, ExpandCollapseAnimation.COLLAPSE);
		anim.setStartOffset(delay);
		findViewById(R.id.topic_details).startAnimation(anim);
	}
	
	private void animateSlideInDetails(final int delay){
		ExpandCollapseAnimation anim = new ExpandCollapseAnimation(findViewById(R.id.topic_details), 500, ExpandCollapseAnimation.EXPAND);
		anim.setStartOffset(delay);
		anim.setAnimationListener(new AnimationListener(){

			@Override
			public void onAnimationEnd(Animation animation) {
				animateSlideOutDetails(2000);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub
				
			}});
		findViewById(R.id.topic_details).startAnimation(anim);
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
		deleteMessage(message);

		if(message.getMsgType() == MessageType.IMAGE){
			try {
				FileTransferDialogFragment.sendPicture(this, Utils.read(new File(message.getDataPath())), channelNamespace);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else if(message.getMsgType() == MessageType.TEXT){
			sendMessage(message.getContent());
		}
	}

	public void deleteMessage(final Message message){
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
		final Message message = chatAdapter.getItem(arg2);
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

	@Override
	public void onDialogClosed(Message message) {
		chatAdapter.addMessage(message);
		chatAdapter.notifyDataSetChanged();
		scrollMyListViewToBottom();
	}
}
