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
import ws.wamp.jawampa.WampMessages.AuthenticateMessage;
import ws.wamp.jawampa.WampMessages.ChallengeMessage;
import ws.wamp.jawampa.auth.client.ClientSideAuthentication;
import android.content.Context;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

import com.checkin.avatargenerator.AvatarGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lesgens.minou.application.MinouApplication;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.listeners.EventsListener;
import com.lesgens.minou.listeners.UserAuthenticatedListener;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.Event;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.models.User;
import com.lesgens.minou.network.HTTPRequest.RequestType;
import com.lesgens.minou.utils.NotificationHelper;

public class Server {

	private static List<UserAuthenticatedListener> userAuthenticatedListeners = new ArrayList<UserAuthenticatedListener>();
	private static EventsListener eventsListeners = null;
	private static String address = "https://minou-backend.herokuapp.com/";
	private static String TAG = "Server";
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
				String userId = "";
				String fakeName = "";
				String secret = "";
				Log.i(TAG, "Auth response's json: "+ result);
				try {
					String[] auth = readAuth(new StringReader(result));
					userId = auth[0];
					fakeName = auth[1];
					secret = auth[2];
					Log.i(TAG, "Auth response's userId: "+ userId + " fake name:" + fakeName + " secret:" + secret);
					Controller.getInstance().setMyOwnUser(new User(fakeName, AvatarGenerator.generate(Controller.getInstance().getDimensionAvatar(), Controller.getInstance().getDimensionAvatar()), userId));
					Controller.getInstance().setSecret(secret);
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
			.withAuthId(Controller.getInstance().getMyId())
			.withAuthMethod(new ClientSideAuthentication() {
				
				@Override
				public AuthenticateMessage handleChallenge(ChallengeMessage message,
						ObjectMapper objectMapper) {
					
						return new AuthenticateMessage(Controller.getInstance().getMyId(), objectMapper.createObjectNode());
				}
				
				@Override
				public String getAuthMethod() {
					return "wampcra";
				}
			})
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
						for(String channel : DatabaseHelper.getInstance().getPublicChannelsNameSpace()){
							subscribeToChannel(context, channel);
						}

						for(User user : DatabaseHelper.getInstance().getPrivateChannels()){
							subscribeToChannel(context, user.getId());
						}
						
						 timer = new Timer();
						 timer.schedule(new PokeCrossbarServer(), 0, 35000);
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
		Controller.getInstance().getChannelsContainer().closeSubscriptions();
	}

	public static void disconnect(){
		if(client != null){
			closeSubscriptions();
			client.close();
		}
	}

	public static void subscribeToChannel(final Context context, final String channelName){
		if(Controller.getInstance().getChannelsContainer().isContainSubscription(channelName)){
			return;
		}

		String fullChannelName = channelName.toLowerCase().replace(".", "_").replace("-", "_");
		fullChannelName = Normalizer.normalize(fullChannelName, Normalizer.Form.NFD);
		fullChannelName = fullChannelName.replaceAll("\\p{M}", "");
		Log.i(TAG, "Subscribing to: " + fullChannelName);
		Observable<PubSubData> channelSubscription = client.makeSubscription(fullChannelName);
		final Channel channel = new Channel(channelName, channelSubscription);
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
				Message m = new Message(user, content, user.getName(), channel, true, data);
				ArrayList<Event> events = new ArrayList<Event>();
				events.add(m);
				boolean isGoodChannel = true;
				if(eventsListeners != null){
					isGoodChannel = eventsListeners.onEventsReceived(events, channelName);
				}
				DatabaseHelper.getInstance().addMessage(m, user.getId(), channelName);
				if(!MinouApplication.isActivityVisible() || !isGoodChannel){
					Log.i(TAG, "Application not visible, should send notification");
					NotificationHelper.notify(context, channelName, user, content);
				}
			}});

		Controller.getInstance().getChannelsContainer().addSubscription(channel);
	}

	public static void sendMessage(final String message){
		String fullChannelName = Controller.getInstance().getCurrentNameSpace();
		fullChannelName = Normalizer.normalize(fullChannelName, Normalizer.Form.NFD);
		fullChannelName = fullChannelName.replaceAll("\\p{M}", "");
		Log.i(TAG, "sendMessage message=" + message + " fullChannelName=" + fullChannelName);
		client.publish(fullChannelName, new ArrayNode(JsonNodeFactory.instance), getObjectNodeMessage(message));
	}
	
	public static void sendMessage(final byte[] picture, final String channel){
		String fullChannelName = Controller.getInstance().getCurrentNameSpace();
		fullChannelName = Normalizer.normalize(fullChannelName, Normalizer.Form.NFD);
		fullChannelName = fullChannelName.replaceAll("\\p{M}", "");
		Log.i(TAG, "sendMessage message=picture" + " fullChannelName=" + fullChannelName + " channel=" + channel);
		client.publish(fullChannelName, new ArrayNode(JsonNodeFactory.instance), getObjectNodeMessage(picture));
	}
	
	public static void sendStayAliveMessage(){
		client.publish("minou.ping", new ArrayNode(JsonNodeFactory.instance), getObjectNodeMessage(""));
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

	public static ArrayList<Channel> getTrendingTopics(){
		return new ArrayList<Channel>();
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
		Server.sendMessage(message);
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
		String[] auth = new String[5];
		JsonReader reader = new JsonReader(in);
		reader.beginObject();
		while(reader.hasNext()){
			String name = reader.nextName();
			if (name.equals("token")) {
				auth[0] = reader.nextString();
			} else if (name.equals("fake_name")) {
				auth[1] = reader.nextString();
			} else if (name.equals("secret")) {
				auth[2] = reader.nextString();
			} else if (name.equals("id")) {
				auth[3] = reader.nextString();
			} else if (name.equals("role")) {
				auth[4] = reader.nextString();
			}
		}
		reader.endObject();
		reader.close();
		return auth;
	}
}
