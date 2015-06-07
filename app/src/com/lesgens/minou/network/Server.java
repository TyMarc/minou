package com.lesgens.minou.network;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import ws.wamp.jawampa.PubSubData;
import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.WampClientBuilder;
import ws.wamp.jawampa.WampError;
import android.content.Context;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

import com.checkin.avatargenerator.AvatarGenerator;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lesgens.minou.application.MinouApplication;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.controllers.PreferencesController;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.listeners.EventsListener;
import com.lesgens.minou.listeners.UserAuthenticatedListener;
import com.lesgens.minou.models.Event;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.models.User;
import com.lesgens.minou.network.HTTPRequest.RequestType;
import com.lesgens.minou.utils.NotificationBuilder;

public class Server {

	private static List<UserAuthenticatedListener> userAuthenticatedListeners = new ArrayList<UserAuthenticatedListener>();
	private static EventsListener eventsListeners = null;
	private static String address = "https://blindr-backend.herokuapp.com/";
	private static String TAG = "Server";
	private static WampClient client;
	private static HashMap<String, Observable<PubSubData>> subscribedChannels = new HashMap<String, Observable<PubSubData>>();

	public static void connect(String authenticationToken) {
		AsyncTask<String, Void, String> request = new AsyncTask<String, Void, String>() {

			@Override
			protected String doInBackground(String... arg0) {
				String finalAddress = address + "auth";
				Log.i(TAG, "auth address: " + finalAddress);
				List<NameValuePair> data = new ArrayList<NameValuePair>();
				data.add(new BasicNameValuePair("fb_token", arg0[0]));
				Log.i(TAG, "fb_token: " + arg0[0]);
				HTTPRequest request = new HTTPRequest(finalAddress, RequestType.POST, data);
				return request.getOutput();
			}

			@Override
			protected void onPostExecute(String result) {
				super.onPostExecute(result);
				String userId = "";
				String fakeName = "";
				Log.i(TAG, "Auth response's json: "+ result);
				try {
					String[] auth = readAuth(new StringReader(result));
					userId = auth[0];
					fakeName = auth[1];
					Log.i(TAG, "Auth response's userId: "+ userId + " fake name:" + fakeName);
					Controller.getInstance().setMyOwnUser(new User(fakeName, AvatarGenerator.generate(Controller.getInstance().getDimensionAvatar(), Controller.getInstance().getDimensionAvatar()), userId));
					for(UserAuthenticatedListener listener: userAuthenticatedListeners) {
						listener.onUserAuthenticated();
					}
				} catch(IOException ioe){
					ioe.printStackTrace();
					for(UserAuthenticatedListener listener: userAuthenticatedListeners) {
						listener.onUserServerErrorAuthentication();
					}
				} catch (Exception e) {
					Log.i(TAG, "Something went wrong while authenticating.");
					e.printStackTrace();
					for(UserAuthenticatedListener listener: userAuthenticatedListeners) {
						listener.onUserNetworkErrorAuthentication();
					}
				}
			}

		};
		request.execute(authenticationToken);
	}

	public static void connectToCrossbar(final Context context, final String city){
		try {
			// Create a builder and configure the client
			disconnect();
			WampClientBuilder builder = new WampClientBuilder();
			builder.withUri("wss://minou-crossbar.herokuapp.com/ws")
			.withRealm("minou")
			.withInfiniteReconnects()
			.withReconnectInterval(2, TimeUnit.SECONDS);
			// Create a client through the builder. This will not immediatly start
			// a connection attempt
			client = builder.build();
			// Subscribe on the clients status updates
			client.statusChanged()
			.subscribe(new Action1<WampClient.Status>() {
				@Override
				public void call(WampClient.Status t1) {
					Log.i(TAG, "Session status changed to " + t1);

					if (t1 == WampClient.Status.Connected) {
						subscribeToPrivateChannel(context, city);

						for(String channel : PreferencesController.getChannels(context)){
							subscribeToChannel(context, channel);
						}

						for(String channel : PreferencesController.getPrivateChannels(context)){
							subscribeToChannel(context, channel);
						}
					}
					else if (t1 == WampClient.Status.Disconnected) {
						closeSubscriptions();
					}
				}
			}, new Action1<Throwable>() {
				@Override
				public void call(Throwable t) {
					Log.i(TAG, "Session ended with error " + t);
				}
			}, new Action0() {
				@Override
				public void call() {
					Log.i(TAG, "Session ended normally");
				}
			});

			client.open();

		} catch (WampError e) {
			// Catch exceptions that will be thrown in case of invalid configuration
			Log.i(TAG, e.getMessage());
			return;
		}
	}

	private static void closeSubscriptions() {
		for(Observable<PubSubData> obs : subscribedChannels.values()){
			obs.unsubscribeOn(Schedulers.immediate());
		}
		subscribedChannels.clear();
	}

	public static void disconnect(){
		if(client != null){
			closeSubscriptions();
			client.close();
		}
	}

	public static void subscribeToChannel(final Context context, final String channel){
		if(subscribedChannels.containsKey(channel)){
			return;
		}
		final String fullChannelName = "minou." + Controller.getInstance().getCity().getName().toLowerCase() + "." + channel.toLowerCase().replace(" ", "_");
		Log.i(TAG, "Subscribing to: " + fullChannelName);
		Observable<PubSubData> channelSubscription = client.makeSubscription(fullChannelName);
		channelSubscription.forEach(new Action1<PubSubData>(){

			@Override
			public void call(PubSubData msg) {
				Log.i(TAG, "Received new message " + msg);
				final User user = Controller.getInstance().getUser(msg.keywordArguments().get("from").asText(), msg.keywordArguments().get("fake_name").asText());
				final String content = msg.keywordArguments().get("content").asText();
				byte[] data = null;
				try {
					data = msg.keywordArguments().get("picture") != null ? msg.keywordArguments().get("picture").binaryValue() : null;
				} catch (IOException e) {
					e.printStackTrace();
				}
				Message m = new Message(user, content, user.getName(), Controller.getInstance().getCity(), true, data);
				ArrayList<Event> events = new ArrayList<Event>();
				events.add(m);
				boolean isGoodChannel = true;
				if(eventsListeners != null){
					isGoodChannel = eventsListeners.onEventsReceived(events, channel);
				}
				Controller.getInstance().addMessage(channel, m);
				if(!MinouApplication.isActivityVisible() || !isGoodChannel){
					Log.i(TAG, "Application not visible, should send notification");
					NotificationBuilder.notify(context, channel, user, content);
				}
			}});

		subscribedChannels.put(fullChannelName, channelSubscription);
	}

	public static void subscribeToPrivateChannel(final Context context, final String channel){
		if(subscribedChannels.containsKey(channel)){
			return;
		}
		final String fullChannelName = "minou." + channel.toLowerCase();
		Log.i(TAG, "Subscribing to: " + fullChannelName);
		Observable<PubSubData> channelSubscription = client.makeSubscription(fullChannelName);
		channelSubscription.forEach(new Action1<PubSubData>(){

			@Override
			public void call(PubSubData msg) {
				Log.i(TAG, "Received new message " + msg);
				final User user = Controller.getInstance().getUser(msg.keywordArguments().get("from").asText(), msg.keywordArguments().get("fake_name").asText());
				final String content = msg.keywordArguments().get("content").asText();
				byte[] data = null;
				try {
					data = msg.keywordArguments().get("picture") != null ? msg.keywordArguments().get("picture").binaryValue() : null;
				} catch (IOException e) {
					e.printStackTrace();
				}
				Message m = new Message(user, content, user.getName(), Controller.getInstance().getCity(), true, data);
				ArrayList<Event> events = new ArrayList<Event>();
				events.add(m);
				boolean isGoodChannel = true;
				if(eventsListeners != null){
					isGoodChannel = eventsListeners.onEventsReceived(events, channel);
				}
				if(channel.equals(Controller.getInstance().getCity().getName())){
					Controller.getInstance().addMessage(channel, m);
				}
				if(!MinouApplication.isActivityVisible() || !isGoodChannel){
					Log.i(TAG, "Application not visible, should send notification");
					NotificationBuilder.notify(context, channel, user, content);
				}
			}});

		subscribedChannels.put(fullChannelName, channelSubscription);
	}

	public static void sendMessage(final String message, final String channel){
		String fullChannelName = "minou." + Controller.getInstance().getCity().getName().toLowerCase() + "." + channel.toLowerCase();
		if(Controller.getInstance().getCity().getName().toLowerCase().equals(channel.toLowerCase())){
			fullChannelName = "minou." + Controller.getInstance().getCity().getName().toLowerCase();
		}
		Log.i(TAG, "sendMessage message=" + message + " fullChannelName=" + fullChannelName + " channel=" + channel);
		client.publish(fullChannelName, new ArrayNode(JsonNodeFactory.instance), getObjectNodeMessage(message));
	}
	
	public static void sendMessage(final byte[] picture, final String channel){
		String fullChannelName = "minou." + Controller.getInstance().getCity().getName().toLowerCase() + "." + channel.toLowerCase();
		if(Controller.getInstance().getCity().getName().toLowerCase().equals(channel.toLowerCase())){
			fullChannelName = "minou." + Controller.getInstance().getCity().getName().toLowerCase();
		}
		Log.i(TAG, "sendMessage message=picture" + " fullChannelName=" + fullChannelName + " channel=" + channel);
		client.publish(fullChannelName, new ArrayNode(JsonNodeFactory.instance), getObjectNodeMessage(picture));
	}

	public static void sendPrivateMessage(final String message, final String remoteId){
		String fullChannelName = "minou." + remoteId.toLowerCase();
		Log.i(TAG, "sendMessage message=" + message + " fullChannelName=" + fullChannelName);

		client.publish(fullChannelName, new ArrayNode(JsonNodeFactory.instance), getObjectNodeMessage(message));
	}
	
	public static void sendPrivateMessage(final byte[] picture, final String remoteId){
		String fullChannelName = "minou." + remoteId.toLowerCase();
		Log.i(TAG, "sendMessage message=picture" + " fullChannelName=" + fullChannelName);

		client.publish(fullChannelName, new ArrayNode(JsonNodeFactory.instance), getObjectNodeMessage(picture));
	}

	private static ObjectNode getObjectNodeMessage(final String message){
		ObjectNode ob = new ObjectNode(JsonNodeFactory.instance);
		ob.put("from", Controller.getInstance().getMyself().getId());
		ob.put("fake_name", Controller.getInstance().getMyself().getName());
		ob.put("content", message);
		return ob;
	}
	
	private static ObjectNode getObjectNodeMessage(final byte[] picture){
		ObjectNode ob = new ObjectNode(JsonNodeFactory.instance);
		ob.put("from", Controller.getInstance().getMyself().getId());
		ob.put("fake_name", Controller.getInstance().getMyself().getName());
		ob.put("picture", picture);
		return ob;
	}

	//	public static void getEvents(final String channelName) {
	//
	//		AsyncTask<String, Void, String> request = new AsyncTask<String, Void, String>() {
	//
	//			@Override
	//			protected String doInBackground(String... arg0) {
	//				String finalAddress = address + "events";
	//				Log.i(TAG, "Events address: " + finalAddress);
	//				Log.i(TAG, "Events channel: " + arg0[1]);
	//
	//				List<NameValuePair> data = new ArrayList<NameValuePair>();
	//				data.add(new BasicNameValuePair("city", arg0[1]));
	//
	//				List<NameValuePair> headers = new ArrayList<NameValuePair>();
	//				headers.add(new BasicNameValuePair("X-User-Token", arg0[0]));
	//
	//				HTTPRequest request = new HTTPRequest(finalAddress, RequestType.GET, data, headers);
	//				return request.getOutput();
	//			}
	//
	//			@Override
	//			protected void onPostExecute(String result) {
	//				super.onPostExecute(result);
	//				List<Event> events = null;
	//				Log.i(TAG, "Events response's json: "+ result);
	//				try {
	//					events = readEvents(new StringReader(result));
	//					if(eventsListeners != null){
	//						eventsListeners.onEventsReceived(events, channelName);
	//					}
	//				} catch (IOException e) {
	//					Log.i(TAG, "Error while receiving events: ");
	//					e.printStackTrace();
	//				} catch (Exception e) {
	//					Log.i(TAG, "Something went wrong when fetching the events.");
	//					e.printStackTrace();
	//				}
	//			}
	//
	//		};
	//
	//		request.execute(Controller.getInstance().getMyself().getId(), Controller.getInstance().getCity().getName() + channelName);
	//	}

	public static ArrayList<String> getTrendingTopics(){
		return new ArrayList<String>();
	}

	public static void sendChannelMessage(User destination, String message) {
		AsyncTask<String, Void, String> request = new AsyncTask<String, Void, String>() {

			@Override
			protected String doInBackground(String... arg0) {
				String finalAddress = address + "events/message";
				Log.i(TAG, "Send private message address: " + finalAddress);

				List<NameValuePair> data = new ArrayList<NameValuePair>();
				data.add(new BasicNameValuePair("dst_user", arg0[1]));
				data.add(new BasicNameValuePair("message", arg0[2]));

				List<NameValuePair> headers = new ArrayList<NameValuePair>();
				headers.add(new BasicNameValuePair("X-User-Token", arg0[0]));

				HTTPRequest request = new HTTPRequest(finalAddress, RequestType.POST, data, headers);
				return request.getOutput();
			}

			@Override
			protected void onPostExecute(String result) {
				super.onPostExecute(result);
				Log.i(TAG, "Sent message's response: "+ result);
			}

		};
		request.execute(Controller.getInstance().getMyself().getId(), destination.getId(), message);
		Server.sendPrivateMessage(message, destination.getId());
	}

	public static void getUserEvents(User user) {
		// get les events avec un user en particulier
		AsyncTask<String, Void, String> request = new AsyncTask<String, Void, String>() {

			@Override
			protected String doInBackground(String... arg0) {
				String finalAddress = address + "events/" + arg0[1];
				Log.i(TAG, "Events avec user address: " + finalAddress);

				List<NameValuePair> headers = new ArrayList<NameValuePair>();
				headers.add(new BasicNameValuePair("X-User-Token", arg0[0]));

				List<NameValuePair> data = new ArrayList<NameValuePair>();
				long lastTime = DatabaseHelper.getInstance().getLastMessageFetched(arg0[1]);
				Log.i(TAG, "Last message fetched=" + lastTime);
				data.add(new BasicNameValuePair("since", String.valueOf(lastTime)));

				HTTPRequest request = new HTTPRequest(finalAddress, RequestType.GET, data, headers);
				return request.getOutput();
			}

			@Override
			protected void onPostExecute(String result) {
				super.onPostExecute(result);
				try {
					Log.i(TAG, "User events response: "+ result);
					List<Event> events = readEvents(new StringReader(result));
					if(eventsListeners != null){
						eventsListeners.onUserHistoryReceived(events);
					}
				} catch (IOException e) {
					Log.i(TAG, "Something went wrong when fetching the old matches.");
					e.printStackTrace();
				} catch (Exception e) {
					Log.i(TAG, "Something went wrong when fetching the old matches.");
					e.printStackTrace();
				}
			}
		};
		request.execute(Controller.getInstance().getMyself().getId(), user.getId());
	}


	public static void addUserAuthenticatedListener(UserAuthenticatedListener listener) {
		userAuthenticatedListeners.add(listener);
	}

	public static void removeUserAuthenticatedListener(UserAuthenticatedListener listener) {
		userAuthenticatedListeners.remove(listener);
	}

	public static void setEventsListener(EventsListener listener) {
		eventsListeners = listener;
	}

	public static void removeEventsListener(EventsListener listener) {
		eventsListeners = null;
	}

	private static String[] readAuth(Reader in) throws IOException {
		String[] auth = new String[2];
		JsonReader reader = new JsonReader(in);
		reader.beginObject();
		while(reader.hasNext()){
			String name = reader.nextName();
			if (name.equals("token")) {
				auth[0] = reader.nextString();
			} else if (name.equals("fake_name")) {
				auth[1] = reader.nextString();
			}
		}
		reader.endObject();
		reader.close();
		return auth;
	}

	private static ArrayList<Event> readEvents(Reader in) throws IOException{
		ArrayList<Event> events = new ArrayList<Event>();
		JsonReader reader = new JsonReader(in);
		reader.beginArray();
		while (reader.hasNext()) {
			events.add(readEvent(reader));
		}
		reader.endArray();
		reader.close();
		return events;
	}

	private static Event readEvent(JsonReader reader) throws IOException {
		UUID id = null;
		String type = null;
		String destination = null;
		Timestamp timestamp = null;
		String userId = null;
		String message = null;
		String gender = null;
		String fakeName = null;
		String realName = null;

		reader.beginObject();
		while(reader.hasNext()) {
			String name = reader.nextName();
			if (name.equals("event_id")) {
				id = UUID.fromString(reader.nextString());
			} else if (name.equals("type")) {
				type = reader.nextString();
			} else if (name.equals("dst")) {
				destination = reader.nextString();
			} else if (name.equals("sent_at")) {
				timestamp = new Timestamp((long)(reader.nextDouble()*1000));
			} else if (name.equals("src")) {
				userId = reader.nextString();
			} else if (name.equals("message")) {
				message = reader.nextString();
			} else if(name.equals("src_gender")) {
				gender = reader.nextString();
			} else if(name.equals("src_fake_name")) {
				fakeName = reader.nextString();
			} else if(name.equals("src_real_name")) {
				realName = reader.nextString();
			}else {
				reader.skipValue();
			}
		}
		reader.endObject();

		return EventBuilder.buildEvent(id, type, destination, timestamp, userId, message, gender, fakeName, realName);
	}
}
