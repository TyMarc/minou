package com.lesgens.minou.db;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.UUID;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.models.User;

public class DatabaseHelper extends SQLiteOpenHelper
{
	private static final String TAG = "DatabaseHelper";
	private static DatabaseHelper instance;
	
	private DatabaseHelper(Context context) {
		super(context, "db", null, 1);
	}
	
	public static void init(final Context context){
		if(instance != null){
			return;
		}
		instance = new DatabaseHelper(context);
	}
	
	public static DatabaseHelper getInstance(){
		return instance;
	}

	public void onCreate(SQLiteDatabase db)
	{
		db.execSQL("CREATE TABLE minou_message (id INTEGER PRIMARY KEY AUTOINCREMENT, message_id TEXT, channel TEXT, userToken TEXT, userName TEXT, message TEXT, data BLOB, timestamp LONG, isIncoming INTEGER DEFAULT 0);");
		db.execSQL("CREATE TABLE minou_last_message (id INTEGER PRIMARY KEY AUTOINCREMENT, channel TEXT, timestamp LONG);");
		db.execSQL("CREATE TABLE minou_private (id INTEGER PRIMARY KEY AUTOINCREMENT, channel TEXT, userName TEXT);");
		db.execSQL("CREATE TABLE minou_public (id INTEGER PRIMARY KEY AUTOINCREMENT, channel TEXT);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE if exists minou_message");
		db.execSQL("DROP TABLE if exists minou_last_message");
		db.execSQL("DROP TABLE if exists minou_private");
		db.execSQL("DROP TABLE if exists minou_public");
		onCreate(db);
	}

	public void addMessage(Message m, String userToken, String channel){
		SQLiteDatabase db = this.getWritableDatabase();

		Log.i(TAG, " adding message to database to channel=" + channel.toLowerCase().replace("-", "_"));
		ContentValues cv = new ContentValues();
		cv.put("message_id",m.getId().toString());
		cv.put("userName", m.getUser().getUsername());
		cv.put("message", m.getMessage());
		cv.put("data", m.getData());
		cv.put("isIncoming", m.isIncoming() ? 1 : 0);
		cv.put("timestamp", m.getTimestamp().getTime());
		cv.put("userToken", userToken);
		cv.put("channel", channel.toLowerCase().replace("-", "_"));
		db.insert("minou_message", null, cv);
		
		cv = new ContentValues();
		cv.put("timestamp", m.getTimestamp().getTime());
		if(getLastMessageFetched(channel) == 0){
			cv.put("channel", channel);
			db.insert("minou_last_message", null, cv);
		} else{
			db.update("minou_last_message", cv, "channel = ?", new String[]{channel});
		}
		
	}
	
	public void addPublicChannel(final String channel){
		SQLiteDatabase db = this.getWritableDatabase();

		Log.i(TAG, "Adding " + channel + " to db");
		ContentValues cv = new ContentValues();
		cv.put("channel", channel);
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

	public ArrayList<Message> getMessages(Channel channel){
		ArrayList<Message> messages = new ArrayList<Message>();
		SQLiteDatabase db = this.getReadableDatabase();
		Log.i(TAG, "get Messages for channel=" + channel.getNamespace().toLowerCase().replace("-", "_"));

		Cursor c = db.rawQuery("SELECT message_id, timestamp, userName, message, isIncoming, data, userToken FROM minou_message WHERE channel = ? ORDER BY timestamp ASC;", new String[]{channel.getNamespace().toLowerCase().replace("-", "_")} );
		Message message;
		while(c.moveToNext()){
			UUID id = UUID.fromString(c.getString(0));
			Timestamp timestamp = new Timestamp(c.getLong(1));
			String username = c.getString(2);
			String text = c.getString(3);
			boolean isIncoming = c.getInt(4) == 1;
			byte[] data = c.getBlob(5);
			String userToken = c.getString(6);
			User user = Controller.getInstance().getUser(userToken, username);
			message = new Message(id, timestamp, channel, 
					user, text, isIncoming, data);
			messages.add(message);
		}
		
		return messages;
	}
	
	public Message getLastMessage(Channel channel){
		SQLiteDatabase db = this.getReadableDatabase();
		Log.i(TAG, "get last Message for channel=" + channel.getNamespace().toLowerCase().replace("-", "_"));

		Cursor c = db.rawQuery("SELECT message_id, timestamp, userName, message, isIncoming, data, userToken FROM minou_message WHERE channel = ? ORDER BY timestamp ASC;", new String[]{channel.getNamespace().toLowerCase().replace("-", "_")} );
		Message message = null;
		if(c.moveToNext()){
			UUID id = UUID.fromString(c.getString(0));
			Timestamp timestamp = new Timestamp(c.getLong(1));
			String username = c.getString(2);
			String text = c.getString(3);
			boolean isIncoming = c.getInt(4) == 1;
			byte[] data = c.getBlob(5);
			String userToken = c.getString(6);
			User user = Controller.getInstance().getUser(userToken, username);
			message = new Message(id, timestamp, channel, 
					user, text, isIncoming, data);
		}
		
		return message;
	}
	
	public void removeAllMessages(final String channel){
		SQLiteDatabase db = this.getWritableDatabase();
		
		db.delete("minou_message", "channel LIKE ?", new String[]{channel + ".*"});
	}
	
	public void addPrivateChannel(String userName, String userToken){
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues cv = new ContentValues();
		cv.put("channel", userToken);
		cv.put("userName", userName);
		db.insert("minou_private", null, cv);
	}
	
	public User getPrivateChannel(String userName){
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c = db.rawQuery("SELECT channel FROM minou_private WHERE userName = ?;", new String[]{userName} );
		
		if(c.moveToFirst()){
			User user = Controller.getInstance().getUser(c.getString(0), userName);
			return user;
		}
		
		return null;		
	}
	
	public ArrayList<User> getPrivateChannels(){
		ArrayList<User> users = new ArrayList<User>();
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c = db.rawQuery("SELECT channel, userName FROM minou_private;", null);
		
		while(c.moveToNext()){
			User user = Controller.getInstance().getUser(c.getString(0), c.getString(1));
			users.add(user);
		}
		
		return users;		
	}
	
	public void removePrivateChannel(String remoteId) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		db.delete("minou_private", "channel = ?", new String[]{remoteId});
	}
	
	public void removePublicChannel(String channel) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		db.delete("minou_public", "channel LIKE ?", new String[]{channel + ".*"});
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

}