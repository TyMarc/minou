package com.lesgens.minou.network;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import ws.wamp.jawampa.PubSubData;
import ws.wamp.jawampa.WampClient;
import ws.wamp.jawampa.WampClientBuilder;
import ws.wamp.jawampa.WampError;
import android.content.Context;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lesgens.minou.ChatActivity;
import com.lesgens.minou.application.MinouApplication;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.controllers.PreferencesController;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.enums.Roles;
import com.lesgens.minou.listeners.CrossbarConnectionListener;
import com.lesgens.minou.listeners.EventsListener;
import com.lesgens.minou.listeners.UserAuthenticatedListener;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.City;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.models.User;
import com.lesgens.minou.network.HTTPRequest.RequestType;
import com.lesgens.minou.utils.AvatarGenerator;
import com.lesgens.minou.utils.NotificationHelper;
import com.lesgens.minou.utils.Utils;

public class Server {

	private static List<UserAuthenticatedListener> userAuthenticatedListeners = new ArrayList<UserAuthenticatedListener>();
	private static ArrayList<EventsListener> eventsListeners = new ArrayList<EventsListener>();
	private static CrossbarConnectionListener crossbarConnectionListener = null;
	private static String address = "https://minou-backend.herokuapp.com/";
	private static String TAG = "Server";
	private static boolean isConnected = false;
	private static WampClient client;
	private static class PokeCrossbarServer extends TimerTask {
		public void run() {
			sendStayAliveMessage();
		}
	}
	private static Timer timer;

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
				Log.i(TAG, "Auth response's json: "+ result);
				try {
					readAuth(new StringReader(result));
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

	public static void connectToCrossbar(final Context context){
		try {
			// Create a builder and configure the client
			disconnect();
			WampClientBuilder builder = new WampClientBuilder();
			builder.withUri("wss://minou-crossbar.herokuapp.com/ws")
			.withRealm("minou")
			.withInfiniteReconnects()
			.withAuthId(Controller.getInstance().getId())
			.withAuthMethod(new WampCraAuthentication())
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

					if (t1 instanceof WampClient.ClientConnected) {		

						initChannels(context);

						subscribeToPrivateChannel(context, Controller.getInstance().getMyself());

						subscribeToCity(context, Controller.getInstance().getGeolocation().getCountryNameSpace());
						subscribeToCity(context, Controller.getInstance().getGeolocation().getStateNameSpace());
						subscribeToCity(context, Controller.getInstance().getGeolocation().getCityNameSpace());

						if(!DatabaseHelper.getInstance().isPublicChannelAlreadyIn(Controller.getInstance().getGeolocation().getCountryNameSpace())) {
							DatabaseHelper.getInstance().addPublicChannel(Controller.getInstance().getGeolocation().getCountryNameSpace());
						}

						if(!DatabaseHelper.getInstance().isPublicChannelAlreadyIn(Controller.getInstance().getGeolocation().getStateNameSpace())) {
							DatabaseHelper.getInstance().addPublicChannel(Controller.getInstance().getGeolocation().getStateNameSpace());
						}


						if(!DatabaseHelper.getInstance().isPublicChannelAlreadyIn(Controller.getInstance().getGeolocation().getCityNameSpace())) {
							DatabaseHelper.getInstance().addPublicChannel(Controller.getInstance().getGeolocation().getCityNameSpace());
						}

						if(!DatabaseHelper.getInstance().isPublicChannelAlreadyIn(Controller.getInstance().getGeolocation().getCityNameSpace() + ".general")) {
							DatabaseHelper.getInstance().addPublicChannel(Controller.getInstance().getGeolocation().getCityNameSpace() + ".general");
						}
						
						if(!DatabaseHelper.getInstance().isPublicChannelAlreadyIn(Controller.getInstance().getGeolocation().getStateNameSpace() + ".general")) {
							DatabaseHelper.getInstance().addPublicChannel(Controller.getInstance().getGeolocation().getStateNameSpace() + ".general");
						}
						
						if(!DatabaseHelper.getInstance().isPublicChannelAlreadyIn(Controller.getInstance().getGeolocation().getCountryNameSpace() + ".general")) {
							DatabaseHelper.getInstance().addPublicChannel(Controller.getInstance().getGeolocation().getCountryNameSpace() + ".general");
						}
						
						

						for(String channel : DatabaseHelper.getInstance().getPublicChannels()){
							subscribeToChannel(context, channel);
						}
						
						for(User channel : DatabaseHelper.getInstance().getPrivateChannels()){
							subscribeToPrivateChannel(context, channel);
						}

						timer = new Timer();
						timer.schedule(new PokeCrossbarServer(), 0, 35000);

						if(crossbarConnectionListener != null){
							crossbarConnectionListener.onConnection();
						}
						
						isConnected = true;
					}
					else if (t1 instanceof WampClient.ClientDisconnected) {
						closeSubscriptions();
						isConnected = false;
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
			e.printStackTrace();
			return;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void closeSubscriptions() {
		if(Controller.getInstance().getChannelsContainer() != null){
			Controller.getInstance().getChannelsContainer().closeSubscriptions();
		}
	}

	public static void disconnect(){
		if(client != null){
			closeSubscriptions();
			client.close();
		}
	}

	private static void initChannels(final Context context){
		final String channelName = Channel.WORLDWIDE_CHANNEL;		
		Controller.getInstance().initChannelContainer(createChannelCity(context, Channel.BASE_PUBLIC_CHANNEL));
		subscribeToCity(context, channelName);
	}
	
	public static void subscribeToCity(final Context context, final String channelName){
		if(Controller.getInstance().getChannelsContainer().isContainSubscription(channelName)){
			return;
		}

		final City city = createChannelCity(context, channelName);

		Controller.getInstance().getChannelsContainer().addSubscription(city);
	}

	public static void subscribeToChannel(final Context context, final String channelName){
		if(Controller.getInstance().getChannelsContainer().isContainSubscription(channelName)){
			return;
		}

		final Channel channel = createChannel(context, channelName);

		Controller.getInstance().getChannelsContainer().addSubscription(channel);
	}

	public static void subscribeToPrivateChannel(final Context context, final User user){
		if(Controller.getInstance().getChannelsContainer().isContainSubscription(user.getNamespace())){
			return;
		}

		final User userToAdd = createPrivateChannel(context, user);

		Controller.getInstance().getChannelsContainer().addByForceSubscription(userToAdd);
	}

	private static Channel createChannel(final Context context, final String channelName){
		final String fullChannelName = Utils.getNormalizedString(channelName);
		Log.i(TAG, "Subscribing to: " + fullChannelName);
		Observable<PubSubData> channelSubscription = client.makeSubscription(fullChannelName);
		final Channel channel = new Channel(fullChannelName, channelSubscription);
		channelSubscription.forEach(new Action1<PubSubData>(){

			@Override
			public void call(PubSubData msg) {
				Log.i(TAG, "Received new message " + msg);
				final String id = msg.keywordArguments().get("from").asText();
				final User user = Controller.getInstance().getUser(id, msg.keywordArguments().get("fake_name").asText());
				final String content = msg.keywordArguments().get("content") != null ? msg.keywordArguments().get("content").asText() : "";
				byte[] data = null;
				Log.i(TAG, "From=" + id + " me=" + Controller.getInstance().getId());
				try {
					data = msg.keywordArguments().get("picture") != null ? msg.keywordArguments().get("picture").binaryValue() : null;
				} catch (IOException e) {
					e.printStackTrace();
				}
				final boolean isIncoming = !id.equals(Controller.getInstance().getId());
				Message m = new Message(user, content, user.getName(), channel, isIncoming, data);
				boolean isGoodChannel = false;
				if(MinouApplication.getCurrentActivity() instanceof ChatActivity){
					if(channel.getNamespace().equals(Controller.getInstance().getCurrentChannel().getNamespace())){
						isGoodChannel = true;
					}
				}
				for(EventsListener el : eventsListeners) {
					el.onNewEvent(m, channel.getNamespace());
				}
				DatabaseHelper.getInstance().addMessage(m, user.getId(), channelName);
				if((!MinouApplication.isActivityVisible() || !isGoodChannel) 
						&& (PreferencesController.isPublicNotificationsEnabled(context, fullChannelName) 
								|| m.getMessage().toLowerCase().contains(Controller.getInstance().getMyself().getUsername().toLowerCase()))
								&& isIncoming){
					Log.i(TAG, "Application not visible, should send notification");
					NotificationHelper.notify(context, channel, user, content);
				}
			}});

		return channel;
	}
	
	private static City createChannelCity(final Context context, final String channelName){
		final String fullChannelName = Utils.getNormalizedString(channelName);
		Log.i(TAG, "Subscribing to: " + fullChannelName);
		Observable<PubSubData> channelSubscription = client.makeSubscription(fullChannelName);
		final City city = new City(fullChannelName, channelSubscription);
		channelSubscription.forEach(new Action1<PubSubData>(){

			@Override
			public void call(PubSubData msg) {
				Log.i(TAG, "Received new message " + msg);
				final String id = msg.keywordArguments().get("from").asText();
				final User user = Controller.getInstance().getUser(id, msg.keywordArguments().get("fake_name").asText());
				final String content = msg.keywordArguments().get("content") != null ? msg.keywordArguments().get("content").asText() : "";
				byte[] data = null;
				Log.i(TAG, "From=" + id + " me=" + Controller.getInstance().getId());
				try {
					data = msg.keywordArguments().get("picture") != null ? msg.keywordArguments().get("picture").binaryValue() : null;
				} catch (IOException e) {
					e.printStackTrace();
				}
				final boolean isIncoming = !id.equals(Controller.getInstance().getId());
				Message m = new Message(user, content, user.getName(), city, isIncoming, data);
				boolean isGoodChannel = false;
				if(MinouApplication.getCurrentActivity() instanceof ChatActivity){
					if(city.getNamespace().equals(Controller.getInstance().getCurrentChannel().getNamespace())){
						isGoodChannel = true;
					}
				}
				for(EventsListener el : eventsListeners) {
					el.onNewEvent(m, city.getNamespace());
				}
				DatabaseHelper.getInstance().addMessage(m, user.getId(), channelName);
				if((!MinouApplication.isActivityVisible() || !isGoodChannel) 
						&& (PreferencesController.isPublicNotificationsEnabled(context, fullChannelName) 
								|| m.getMessage().toLowerCase().contains(Controller.getInstance().getMyself().getUsername().toLowerCase()))
								&& isIncoming){
					Log.i(TAG, "Application not visible, should send notification");
					NotificationHelper.notify(context, city, user, content);
				}
			}});

		return city;
	}

	private static User createPrivateChannel(final Context context, final User userToCreate){
		final String fullChannelName = Utils.getNormalizedString(Channel.BASE_CHANNEL + userToCreate.getId().replace(".", "_").replace("-", "_"));
		Log.i(TAG, "Subscribing to: " + fullChannelName);
		Observable<PubSubData> channelSubscription = client.makeSubscription(fullChannelName);
		channelSubscription.forEach(new Action1<PubSubData>(){

			@Override
			public void call(PubSubData msg) {
				Log.i(TAG, "Received new private message " + msg);
				final String id = msg.keywordArguments().get("from").asText();
				final User user = Controller.getInstance().getUser(id, msg.keywordArguments().get("fake_name").asText());
				final String content = msg.keywordArguments().get("content") != null ? msg.keywordArguments().get("content").asText() : "";
				byte[] data = null;
				try {
					data = msg.keywordArguments().get("picture") != null ? msg.keywordArguments().get("picture").binaryValue() : null;
				} catch (IOException e) {
					e.printStackTrace();
				}
				final boolean isIncoming = !id.equals(Controller.getInstance().getId());
				Message m = new Message(user, content, user.getUsername(), userToCreate, isIncoming, data);
				boolean isGoodChannel = false;
				if(MinouApplication.getCurrentActivity() instanceof ChatActivity){
					if(user.getNamespace().equals(Controller.getInstance().getCurrentChannel().getNamespace())){
						isGoodChannel = true;
					}
				}
				for(EventsListener el : eventsListeners) {
					el.onNewEvent(m, user.getNamespace());
				}
				DatabaseHelper.getInstance().addMessage(m, user.getId(), user.getNamespace());
				if((!MinouApplication.isActivityVisible() || !isGoodChannel) && 
						!PreferencesController.isPrivateNotificationsDisabled(context, fullChannelName) 
						&& isIncoming && DatabaseHelper.getInstance().getPrivateChannels().contains(user)){
					Log.i(TAG, "Application not visible, should send notification");
					NotificationHelper.notify(context, null, user, content);
				}
			}});

		userToCreate.setSubscription(channelSubscription);
		return userToCreate;
	}

	public static void sendMessage(final String message, final String channelNamespace){
		String fullChannelName = channelNamespace.toLowerCase().replace("-", "_");
		fullChannelName = Normalizer.normalize(fullChannelName, Normalizer.Form.NFD);
		fullChannelName = fullChannelName.replaceAll("\\p{M}", "");
		Log.i(TAG, "sendMessage message=" + message + " fullChannelName=" + fullChannelName);
		client.publish(fullChannelName, new ArrayNode(JsonNodeFactory.instance), getObjectNodeMessage(message));
	}

	public static void sendMessage(final byte[] picture, final String channelNamespace){
		String fullChannelName = channelNamespace.toLowerCase().replace("-", "_");
		fullChannelName = Normalizer.normalize(fullChannelName, Normalizer.Form.NFD);
		fullChannelName = fullChannelName.replaceAll("\\p{M}", "");
		Log.i(TAG, "sendMessage message=picture" + " fullChannelName=" + fullChannelName);
		client.publish(fullChannelName, new ArrayNode(JsonNodeFactory.instance), getObjectNodeMessage(picture));
	}

	public static void sendStayAliveMessage(){
		client.publish("minou.ping", new ArrayNode(JsonNodeFactory.instance), getObjectNodeMessage(""));
	}

	private static ObjectNode getObjectNodeMessage(final String message){
		ObjectNode ob = new ObjectNode(JsonNodeFactory.instance);
		ob.put("from", Controller.getInstance().getId());
		ob.put("fake_name", Controller.getInstance().getMyself().getUsername());
		ob.put("content", message);
		return ob;
	}

	private static ObjectNode getObjectNodeMessage(final byte[] picture){
		ObjectNode ob = new ObjectNode(JsonNodeFactory.instance);
		ob.put("from", Controller.getInstance().getId());
		ob.put("fake_name", Controller.getInstance().getMyself().getName());
		ob.put("picture", picture);
		return ob;
	}

	public static ArrayList<Channel> getTrendingTopics(){
		return new ArrayList<Channel>();
	}

	public static void addUserAuthenticatedListener(UserAuthenticatedListener listener) {
		userAuthenticatedListeners.add(listener);
	}

	public static void removeUserAuthenticatedListener(UserAuthenticatedListener listener) {
		userAuthenticatedListeners.remove(listener);
	}

	public static void addEventsListener(EventsListener listener) {
		if(!eventsListeners.contains(listener)) eventsListeners.add(listener);
	}

	public static void removeEventsListener(EventsListener listener) {
		eventsListeners.remove(listener);
	}

	public static void setCrossbarConnectionListener(CrossbarConnectionListener listener) {
		crossbarConnectionListener = listener;
	}

	public static void removeCrossbarConnectionListener(CrossbarConnectionListener listener) {
		crossbarConnectionListener = null;
	}
	
	public static boolean isConnected(){
		return isConnected;
	}
	
	public static void changeUsername(String newUsername) {
		AsyncTask<String, Void, String> request = new AsyncTask<String, Void, String>() {

			@Override
			protected String doInBackground(String... arg0) {
				String finalAddress = address + "me";
				List<NameValuePair> data = new ArrayList<NameValuePair>();
				data.add(new BasicNameValuePair("fake_name", arg0[0]));
				Log.i(TAG, "New username: " + arg0[0]);
				List<NameValuePair> headers = new ArrayList<NameValuePair>();
				headers.add(new BasicNameValuePair("X-User-Token", arg0[1]));
				HTTPRequest request = new HTTPRequest(finalAddress, RequestType.PUT, data, headers);
				return request.getOutput();
			}

			@Override
			protected void onPostExecute(String result) {
				super.onPostExecute(result);
				Log.i(TAG, "Auth response's json: "+ result);
			}

		};
		request.execute(newUsername, Controller.getInstance().getToken());
	}  
	
	public static void getMyself() {
		AsyncTask<String, Void, String> request = new AsyncTask<String, Void, String>() {

			@Override
			protected String doInBackground(String... arg0) {
				String finalAddress = address + "me";
				List<NameValuePair> headers = new ArrayList<NameValuePair>();
				headers.add(new BasicNameValuePair("X-User-Token", arg0[0]));
				HTTPRequest request = new HTTPRequest(finalAddress, RequestType.GET, null, headers);
				return request.getOutput();
			}

			@Override
			protected void onPostExecute(String result) {
				super.onPostExecute(result);
				Log.i(TAG, "Auth response's json: "+ result);
			}

		};
		request.execute(Controller.getInstance().getToken());
	}  

	private static void readAuth(Reader in) throws IOException {
		JsonReader reader = new JsonReader(in);
		reader.beginObject();
		String fakeName = "";
		while(reader.hasNext()){
			String name = reader.nextName();
			if (name.equals("token")) {
				Controller.getInstance().setToken(reader.nextString());
			} else if (name.equals("fake_name")) {
				fakeName = reader.nextString();
			} else if (name.equals("secret")) {
				Controller.getInstance().setSecret(reader.nextString());
			} else if (name.equals("id")) {
				Controller.getInstance().setId(reader.nextString());
			} else if (name.equals("role")) {
				Controller.getInstance().setRole(Roles.fromString(reader.nextString()));
			}
		}
		reader.endObject();
		reader.close();
		Controller.getInstance().setMyOwnUser(new User(fakeName, Channel.BASE_CHANNEL + Controller.getInstance().getId(), AvatarGenerator.generate(Controller.getInstance().getDimensionAvatar(), Controller.getInstance().getDimensionAvatar()), Controller.getInstance().getId()));
	}
}
