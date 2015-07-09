package com.lesgens.minou.db;

import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.Log;

import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.models.User;
import com.lesgens.minou.utils.AvatarGenerator;

public class DatabaseHelper extends SQLiteOpenHelper
{
	private static final String TAG = "DatabaseHelper";
	private static DatabaseHelper instance;
	private static HashMap<String, User> userCache;
	
	private DatabaseHelper(Context context) {
		super(context, "db", null, 1);
	}
	
	public static void init(final Context context){
		if(instance != null){
			return;
		}
		instance = new DatabaseHelper(context);
		userCache = new HashMap<String, User>();
	}
	
	public static DatabaseHelper getInstance(){
		return instance;
	}

	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL("CREATE TABLE minou_message (id INTEGER PRIMARY KEY AUTOINCREMENT, message_id TEXT, channel TEXT, userId TEXT, message TEXT, data BLOB, timestamp LONG, isIncoming INTEGER DEFAULT 0);");
		db.execSQL("CREATE TABLE minou_last_message (id INTEGER PRIMARY KEY AUTOINCREMENT, channel TEXT, timestamp LONG);");
		db.execSQL("CREATE TABLE minou_public (id INTEGER PRIMARY KEY AUTOINCREMENT, channel TEXT);");
		db.execSQL("CREATE TABLE minou_users (id INTEGER PRIMARY KEY AUTOINCREMENT, userId TEXT, username TEXT, avatar BLOB, isContact INTEGER DEFAULT 0);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE if exists minou_message");
		db.execSQL("DROP TABLE if exists minou_last_message");
		db.execSQL("DROP TABLE if exists minou_public");
		db.execSQL("DROP TABLE if exists minou_users");
		onCreate(db);
	}

	public void addMessage(Message m, String userId, String channel){
		addMessage(m, userId, channel, m.getTimestamp().getTime());		
	}
	
	public void addMessage(Message m, String userId, String channel, long timestamp){
		SQLiteDatabase db = this.getWritableDatabase();

		Log.i(TAG, " adding message to database to channel=" + channel.toLowerCase().replace("-", "_") + " timestamp=" + timestamp);
		ContentValues cv = new ContentValues();
		cv.put("message_id",m.getId().toString());
		cv.put("message", m.getMessage());
		cv.put("data", m.getData());
		cv.put("isIncoming", m.isIncoming() ? 1 : 0);
		cv.put("timestamp", timestamp);
		cv.put("userId", userId);
		cv.put("channel", channel.toLowerCase().replace("-", "_"));
		db.insert("minou_message", null, cv);
		
		cv = new ContentValues();
		cv.put("timestamp", timestamp);
		if(getLastMessageFetched(channel.toLowerCase().replace("-", "_")) == 0){
			cv.put("channel", channel);
			db.insert("minou_last_message", null, cv);
		} else{
			db.update("minou_last_message", cv, "channel = ?", new String[]{channel.toLowerCase().replace("-", "_")});
		}
		
	}
	
	public void addPublicChannel(final String channel){
		SQLiteDatabase db = this.getWritableDatabase();

		Log.i(TAG, "Adding " + channel + " to db");
		ContentValues cv = new ContentValues();
		cv.put("channel", channel.toLowerCase().replace("-", "_"));
		db.insert("minou_public", null, cv);
	}
	
	public boolean isPublicChannelAlreadyIn(final String channel){
		SQLiteDatabase db = this.getReadableDatabase();
		
		Cursor c = db.rawQuery("SELECT channel FROM minou_public WHERE channel = ?;", new String[]{channel} );

		while(c.moveToNext()){
			return true;
		}
		
		return false;
	}
	
	public ArrayList<String> getPublicChannels(){
		SQLiteDatabase db = this.getReadableDatabase();
		ArrayList<String> channels = new ArrayList<String>();
		
		Cursor c = db.rawQuery("SELECT channel FROM minou_public ORDER BY id;", null );

		while(c.moveToNext()){
			channels.add(c.getString(0));
		}
		
		return channels;
	}
	
	public void preloadUsers(){
		Log.i(TAG, "preloading users");
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.rawQuery("SELECT userId, username, avatar, isContact FROM minou_users;", null);
		
		User user = null;
		while(c.moveToNext()){
			String userId = c.getString(0);
			String username = c.getString(1);
			byte[] dataAvatar = c.getBlob(2);
			Bitmap avatar = BitmapFactory.decodeByteArray(dataAvatar, 0, dataAvatar.length);
			boolean isContact = c.getInt(3) == 1;
			user = new User(username, Channel.BASE_CHANNEL + userId.replace(".", "_"), avatar, userId, isContact);
			userCache.put(userId, user);
		}
	}
	
	public User getUser(String userId){
		User user = userCache.get(userId);
		
		if(user != null){
			Log.i(TAG, "found user in cache");
			return user;
		}
		SQLiteDatabase db = this.getReadableDatabase();
		Log.i(TAG, "get user with userId=" + userId);

		Cursor c = db.rawQuery("SELECT username, avatar, isContact FROM minou_users WHERE userId = ?;", new String[]{userId} );
		while(c.moveToNext()){
			String username = c.getString(0);
			byte[] dataAvatar = c.getBlob(1);
			Bitmap avatar = BitmapFactory.decodeByteArray(dataAvatar, 0, dataAvatar.length);
			boolean isContact = c.getInt(2) == 1;
			user = new User(username, Channel.BASE_CHANNEL + userId.replace(".", "_"), avatar, userId, isContact);
			break;
		}
		
		if(user == null){
			user = new User(userId, Channel.BASE_CHANNEL + userId.replace(".", "_"), AvatarGenerator.generate(Controller.getInstance().getDimensionAvatar(), Controller.getInstance().getDimensionAvatar()), userId, false);
		}
		
		userCache.put(userId, user);
		
		return user;
	}
	
	public User getUser(String userId, String username){
		User user = userCache.get(userId);
		if(user != null){
			Log.i(TAG, "found user in cache");
			if(!user.getUsername().equals(username)){
				user.setUsername(username);
				setUsernameForUser(userId, username);
			}
			
			return user;
		}
		SQLiteDatabase db = this.getReadableDatabase();
		Log.i(TAG, "get user with userId=" + userId + " and username=" + username);

		Cursor c = db.rawQuery("SELECT username, avatar, isContact FROM minou_users WHERE userId = ?;", new String[]{userId} );
		while(c.moveToNext()){
			Log.i(TAG, "found user");
			String usernameDB = c.getString(0);
			byte[] dataAvatar = c.getBlob(1);
			Bitmap avatar = BitmapFactory.decodeByteArray(dataAvatar, 0, dataAvatar.length);
			boolean isContact = c.getInt(2) == 1;
			user = new User(username, Channel.BASE_CHANNEL + userId.replace(".", "_"), avatar, userId, isContact);
			
			if(!username.equals(usernameDB)){
				setUsernameForUser(userId, username);
			}
			break;
		}
		
		if(user == null){
			user = new User(username, Channel.BASE_CHANNEL + userId.replace(".", "_"), AvatarGenerator.generate(Controller.getInstance().getDimensionAvatar(), Controller.getInstance().getDimensionAvatar()), userId, false);
			addUser(user);
		}
		
		userCache.put(userId, user);
		
		return user;
	}
	
	private void addUser(User user){
		SQLiteDatabase db = this.getWritableDatabase();
		
		Log.i(TAG, "adding user to database with userId=" + user.getId() + " and username=" + user.getUsername());

		ContentValues cv = new ContentValues();
		cv.put("userId", user.getId());
		cv.put("username", user.getUsername());
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		Bitmap newBitmap = Bitmap.createBitmap(user.getAvatar().getWidth(), user.getAvatar().getHeight(), user.getAvatar().getConfig());
		Canvas canvas = new Canvas(newBitmap);
		canvas.drawColor(Color.WHITE);
		canvas.drawBitmap(user.getAvatar(), 0, 0, null);
		newBitmap.compress(CompressFormat.JPEG, 70, bos);
		cv.put("avatar", bos.toByteArray());
		db.insert("minou_users", null, cv);
	}
	
	private void setUsernameForUser(String userId, String username){
		SQLiteDatabase db = this.getWritableDatabase();
		Log.i(TAG, "setting username for userId=" + userId + " for username=" + username);

		ContentValues cv = new ContentValues();
		cv.put("username", username);
		db.update("minou_users", cv, "userId = ?", new String[]{userId});
	}

	public ArrayList<Message> getMessages(Channel channel){
		ArrayList<Message> messages = new ArrayList<Message>();
		SQLiteDatabase db = this.getReadableDatabase();
		Log.i(TAG, "get Messages for channel=" + channel.getNamespace().toLowerCase().replace("-", "_"));

		Cursor c = db.rawQuery("SELECT message_id, timestamp, message, isIncoming, data, userId FROM minou_message WHERE channel = ? ORDER BY timestamp ASC;", new String[]{channel.getNamespace().toLowerCase().replace("-", "_")} );
		Message message;
		while(c.moveToNext()){
			UUID id = UUID.fromString(c.getString(0));
			Timestamp timestamp = new Timestamp(c.getLong(1));
			String text = c.getString(2);
			boolean isIncoming = c.getInt(3) == 1;
			byte[] data = c.getBlob(4);
			String userId = c.getString(5);
			User user = getUser(userId);
			message = new Message(id, timestamp, channel, 
					user, text, isIncoming, data);
			messages.add(message);
		}
		
		return messages;
	}
	
	public Message getLastMessage(Channel channel){
		SQLiteDatabase db = this.getReadableDatabase();
		Log.i(TAG, "get last Message for channel=" + channel.getNamespace().toLowerCase().replace("-", "_"));

		Cursor c = db.rawQuery("SELECT message_id, timestamp, message, isIncoming, data, userId FROM minou_message WHERE channel = ? ORDER BY timestamp DESC;", new String[]{channel.getNamespace().toLowerCase().replace("-", "_")} );
		Message message = null;
		if(c.moveToNext()){
			UUID id = UUID.fromString(c.getString(0));
			Timestamp timestamp = new Timestamp(c.getLong(1));
			String text = c.getString(2);
			boolean isIncoming = c.getInt(3) == 1;
			byte[] data = c.getBlob(4);
			String userId = c.getString(5);
			User user = getUser(userId);
			message = new Message(id, timestamp, channel, 
					user, text, isIncoming, data);
		}
		
		return message;
	}
	
	public void removeAllMessages(final String channel){
		SQLiteDatabase db = this.getWritableDatabase();
		
		db.delete("minou_message", "channel = ?", new String[]{channel.toLowerCase().replace("-", "_")});
		db.delete("minou_message", "channel LIKE ?", new String[]{channel.toLowerCase().replace("-", "_") + ".%"});
		db.delete("minou_last_message", "channel = ?", new String[]{channel.toLowerCase().replace("-", "_")});
		db.delete("minou_last_message", "channel LIKE ?", new String[]{channel.toLowerCase().replace("-", "_") + ".%"});
	}
	
	public ArrayList<String> getPrivateChannels(){
		ArrayList<String> usersId = new ArrayList<String>();
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c = db.rawQuery("SELECT channel FROM minou_last_message WHERE (NOT channel LIKE 'minou.public.%') GROUP BY channel;", null);
		
		while(c.moveToNext()){
			String userId = c.getString(0).replace("minou.", "");
			usersId.add(userId);
		}
		
		return usersId;		
	}
	
	public ArrayList<String> getContacts(){
		ArrayList<String> usersId = new ArrayList<String>();
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c = db.rawQuery("SELECT userId FROM minou_users WHERE isContact = 1;", null);
		
		while(c.moveToNext()){
			String userId = c.getString(0);
			usersId.add(userId);
		}
		
		return usersId;		
	}
	
	public void removePublicChannel(String channel) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		db.delete("minou_public", "channel = ?", new String[]{channel.toLowerCase().replace("-", "_")});
		db.delete("minou_public", "channel LIKE ?", new String[]{channel.toLowerCase().replace("-", "_") + ".%"});
	}
	
	public long getLastMessageFetched(String channel){
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c = db.rawQuery("SELECT timestamp FROM minou_last_message WHERE channel = ?;", new String[]{channel.toLowerCase().replace("-", "_")} );
		
		if(c.moveToFirst()){
			return c.getLong(0);
		}
		
		return 0;		
	}

	public void eraseBD(){
		this.onUpgrade(getWritableDatabase(), 0, 1);
	}

	public boolean isContact(String userId) {
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c = db.rawQuery("SELECT isContact FROM minou_users WHERE userId = ?;", new String[]{userId});
		
		while(c.moveToNext()){
			return c.getInt(0) == 1;
		}
		
		return false;		
	}

	public void setUserAsContact(final User user) {
		SQLiteDatabase db = this.getWritableDatabase();

		final String userId = user.getId();
		ContentValues cv = new ContentValues();
		cv.put("isContact", 1);
		db.update("minou_users", cv, "userId = ?", new String[]{userId});
		
		if(userCache.containsKey(userId)){
			userCache.get(userId).setIsContact(true);
		} else{
			userCache.put(userId, user);
		}
	}

	public void removeMessage(Message message) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		db.delete("minou_message", "message_id = ?", new String[]{message.getId().toString()});
	}

	public void removeContact(User user) {
		SQLiteDatabase db = this.getWritableDatabase();

		final String userId = user.getId();
		ContentValues cv = new ContentValues();
		cv.put("isContact", 0);
		db.update("minou_users", cv, "userId = ?", new String[]{userId});
		
		if(userCache.containsKey(userId)){
			userCache.get(userId).setIsContact(false);
		} else{
			userCache.put(userId, user);
		}
	}

}