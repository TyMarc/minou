package com.lesgens.minou;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
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
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.lesgens.minou.adapters.PrivateChatAdapter;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.listeners.EventsListener;
import com.lesgens.minou.models.Event;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.models.User;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.receivers.NetworkStateReceiver;
import com.lesgens.minou.receivers.NetworkStateReceiver.NetworkStateReceiverListener;
import com.lesgens.minou.utils.Utils;

public class PrivateChatActivity extends MinouActivity implements OnClickListener, EventsListener, NetworkStateReceiverListener {

	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private Typeface tf;
	private ImageView sendBt;
	private PrivateChatAdapter chatAdapter;
	private StickyListHeadersListView listMessages;
	private EditText editText;
	private ScheduledExecutorService scheduler;
	private Future<?> future;
	private User remoteUser;
	private TextView tvConnectionProblem;
	private NetworkStateReceiver networkStateReceiver;
	private Uri imageUri;
	private boolean isComingBackFromTakingPhoto;

	public static void show(Context context, String tokenId, String realName){
		Intent i = new Intent(context, PrivateChatActivity.class);
		i.putExtra("tokenId", tokenId);
		i.putExtra("realName", realName);
		context.startActivity(i);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.private_chat);

		isComingBackFromTakingPhoto = false;

		remoteUser = Controller.getInstance().getUser(getIntent().getStringExtra("tokenId"));
		TextView fbName = (TextView) findViewById(R.id.fbName);

		tf = Typeface.createFromAsset(getAssets(), "fonts/Raleway_Thin.otf");
		fbName.setTypeface(tf);
		fbName.setText(getIntent().getStringExtra("realName"));

		((ImageView) findViewById(R.id.avatar)).setImageBitmap(remoteUser.getAvatar());
		findViewById(R.id.back).setOnClickListener(this);

		if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) == false){
			findViewById(R.id.send_picture).setVisibility(View.GONE);
		} else{
			findViewById(R.id.send_picture).setOnClickListener(this);
		}

		editText = (EditText) findViewById(R.id.editText);
		editText.clearFocus();

		sendBt = (ImageView) findViewById(R.id.send);
		sendBt.setOnClickListener(this);

		tvConnectionProblem = (TextView) findViewById(R.id.connection_problem);

		chatAdapter = new PrivateChatAdapter(this, DatabaseHelper.getInstance().getPrivateMessages(remoteUser));
		listMessages = (StickyListHeadersListView) findViewById(R.id.list);
		listMessages.setAdapter(chatAdapter);
		listMessages.setTranscriptMode(ListView.TRANSCRIPT_MODE_NORMAL);

		Server.setEventsListener(this);

		scheduler = Executors.newSingleThreadScheduledExecutor();

		networkStateReceiver = new NetworkStateReceiver(this);
	}

	@Override
	public void onResume(){
		super.onResume();
		
		if(!isComingBackFromTakingPhoto){
			Server.getUserEvents(remoteUser);
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

							final Bitmap bitmapScaled = Utils.scaleDown(bitmap, Utils.dpInPixels(PrivateChatActivity.this, 250), true);
							bitmap.recycle();
							bitmapScaled.compress(Bitmap.CompressFormat.JPEG, 100, stream);
							byte[] byteArray = stream.toByteArray();

							String encoded = Utils.MINOU_IMAGE_BASE + Base64.encodeToString(byteArray, Base64.DEFAULT);
							Message message = new Message(Controller.getInstance().getMyself(), encoded, false);
							chatAdapter.addMessage(message);
							chatAdapter.notifyDataSetChanged();
							Server.sendChannelMessage(remoteUser, message.getMessage());
							editText.setText("");
							scrollMyListViewToTheBottomNowWeHere();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}});




			}
		}   
	}

	public void takePhoto() {
		Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
		File photo = new File(Environment.getExternalStorageDirectory(),  "Pic.jpg");
		intent.putExtra(MediaStore.EXTRA_OUTPUT,
				Uri.fromFile(photo));
		imageUri = Uri.fromFile(photo);
		isComingBackFromTakingPhoto = true;
		startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.send){
			final String text = editText.getText().toString();
			if(!text.isEmpty()){
				Message message = new Message(Controller.getInstance().getMyself(), text, false);
				chatAdapter.addMessage(message);
				chatAdapter.notifyDataSetChanged();
				Server.sendChannelMessage(remoteUser, message.getMessage());
				DatabaseHelper.getInstance().addMessage(message, remoteUser.getId());
				editText.setText("");
				scrollMyListViewToTheBottomNowWeHere();
			}
		} else if(v.getId() == R.id.back){
			onBackPressed();
		} else if(v.getId() == R.id.send_picture){
			takePhoto();
		}
	}

	private void scrollMyListViewToTheBottomNowWeHere() {
		listMessages.post(new Runnable() {
			@Override
			public void run() {
				// Select the last row so it will scroll into view...
				listMessages.setSelection(chatAdapter.getCount() - 1);
			}
		});
	}

	@Override
	public boolean onEventsReceived(List<Event> events, final String channel) {
		if(channel.equals(remoteUser.getId())){
			return false;
		}
		for(Event e : events){
			android.util.Log.i("Minou", "New event=" + e);
			if(e instanceof Message && e.getDestination() instanceof User){
				if(((User) e.getDestination()).getId().equals(Controller.getInstance().getMyId())){
					if(chatAdapter.addMessage((Message) e)){
						DatabaseHelper.getInstance().addMessage((Message) e, remoteUser.getId());
					}
					chatAdapter.notifyDataSetChanged();
					scrollMyListViewToTheBottomNowWeHere();
				}
			}
		}
		return true;
	}

	@Override
	public void onUserHistoryReceived(List<Event> events) {
		Collections.sort(events, dateComparator);
		for(Event e : events){
			if(e instanceof Message && e.getDestination() instanceof User){
				if((e.getUser()).getId().equals(Controller.getInstance().getMyId())){
					((Message) e).setIsIncoming(false);
				}
				if(chatAdapter.addMessage((Message) e)){
					Log.i("ChannelChatActivity", "adding message to db");
					DatabaseHelper.getInstance().addMessage((Message) e, remoteUser.getId());
				}
				chatAdapter.notifyDataSetChanged();
				scrollMyListViewToTheBottomNowWeHere();
			}
		}
	}

	Comparator<Event> dateComparator = new Comparator<Event>()
			{
		@Override
		public int compare(Event lhs, Event rhs)
		{
			try
			{
				return lhs.getTimestamp().compareTo(rhs.getTimestamp());
			}
			catch (Exception e)
			{
				return 0;
			}
		}
			};

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
