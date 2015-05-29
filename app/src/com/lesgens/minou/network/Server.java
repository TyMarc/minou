package com.lesgens.minou.network;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

import com.checkin.avatargenerator.AvatarGenerator;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.listeners.EventsListener;
import com.lesgens.minou.listeners.UserAuthenticatedListener;
import com.lesgens.minou.models.City;
import com.lesgens.minou.models.Event;
import com.lesgens.minou.models.User;
import com.lesgens.minou.network.HTTPRequest.RequestType;

public class Server {

	private static List<UserAuthenticatedListener> userAuthenticatedListeners = new ArrayList<UserAuthenticatedListener>();
	private static List<EventsListener> eventsListeners = new ArrayList<EventsListener>();
	private static String address = "https://blindr-backend.herokuapp.com/";
	private static String TAG = "Blindr_Server";

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

	public static void getEvents(final String channelName) {

		AsyncTask<String, Void, String> request = new AsyncTask<String, Void, String>() {

			@Override
			protected String doInBackground(String... arg0) {
				String finalAddress = address + "events";
				Log.i(TAG, "Events address: " + finalAddress);
				Log.i("SERVER+INFOS", "Events channel: " + arg0[1]);

				List<NameValuePair> data = new ArrayList<NameValuePair>();
				data.add(new BasicNameValuePair("city", arg0[1]));

				List<NameValuePair> headers = new ArrayList<NameValuePair>();
				headers.add(new BasicNameValuePair("X-User-Token", arg0[0]));

				HTTPRequest request = new HTTPRequest(finalAddress, RequestType.GET, data, headers);
				return request.getOutput();
			}

			@Override
			protected void onPostExecute(String result) {
				super.onPostExecute(result);
				List<Event> events = null;
				Log.i(TAG, "Events response's json: "+ result);
				try {
					events = readEvents(new StringReader(result));
					for(EventsListener listener:eventsListeners){
						listener.onEventsReceived(events);
					}
				} catch (IOException e) {
					Log.i(TAG, "Error while receiving events: ");
					e.printStackTrace();
				} catch (Exception e) {
					Log.i(TAG, "Something went wrong when fetching the events.");
					e.printStackTrace();
				}
			}

		};

		request.execute(Controller.getInstance().getMyself().getId(), Controller.getInstance().getCity().getName() + channelName);
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
	}

	public static void sendPublicMessage(City destination, String message) {
		AsyncTask<String, Void, String> request = new AsyncTask<String, Void, String>() {

			@Override
			protected String doInBackground(String... arg0) {
				String finalAddress = address + "events/message";
				Log.i(TAG, "Send public message address: " + finalAddress);

				List<NameValuePair> data = new ArrayList<NameValuePair>();
				data.add(new BasicNameValuePair("dst_city", arg0[1]));
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
					for(EventsListener listener: eventsListeners) {
						listener.onUserHistoryReceived(events);
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

	public static void addEventsListener(EventsListener listener) {
		eventsListeners.add(listener);
	}

	public static void removeEventsListener(EventsListener listener) {
		eventsListeners.remove(listener);
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
