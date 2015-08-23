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
import com.lesgens.minou.enums.MessageType;
import com.lesgens.minou.enums.SendingStatus;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.ContactPicker;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.models.User;
import com.lesgens.minou.utils.AvatarGenerator;
import com.lesgens.minou.utils.Utils;

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
		db.execSQL("CREATE TABLE minou_message (id INTEGER PRIMARY KEY AUTOINCREMENT, message_id TEXT, channel TEXT, userId TEXT, content TEXT, dataPath String DEFAULT null, timestamp LONG, isIncoming INTEGER DEFAULT 0, status INTEGER DEFAULT 0, msgType TEXT, thumbnail BLOB, read INTEGER DEFAULT 0);");
		db.execSQL("CREATE TABLE minou_last_message (id INTEGER PRIMARY KEY AUTOINCREMENT, channel TEXT, timestamp LONG);");
		db.execSQL("CREATE TABLE minou_public (id INTEGER PRIMARY KEY AUTOINCREMENT, channel TEXT);");
		db.execSQL("CREATE TABLE minou_users (id INTEGER PRIMARY KEY AUTOINCREMENT, userId TEXT, username TEXT, avatarURL TEXT, avatar BLOB, isContact INTEGER DEFAULT 0);");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE if exists minou_message");
		db.execSQL("DROP TABLE if exists minou_last_message");
		db.execSQL("DROP TABLE if exists minou_public");
		db.execSQL("DROP TABLE if exists minou_users");
		onCreate(db);
	}

	public void addMessage(Message m, String userId, String channel,
			boolean isRead) {
		addMessage(m, userId, channel, m.getTimestamp().getTime(), isRead);
	}

	public void addMessage(Message m, String userId, String channel){
		addMessage(m, userId, channel, m.getTimestamp().getTime(), false);		
	}

	public void addMessage(Message m, String userId, String channel, long timestamp, boolean isRead){
		SQLiteDatabase db = this.getWritableDatabase();

		Log.i(TAG, " adding message to database to channel=" + channel.toLowerCase().replace("-", "_") + " timestamp=" + timestamp);
		ContentValues cv = new ContentValues();
		cv.put("message_id",m.getId().toString());
		cv.put("content", m.getContent());
		cv.put("dataPath", m.getDataPath());
		cv.put("isIncoming", m.isIncoming() ? 1 : 0);
		cv.put("timestamp", timestamp);
		cv.put("userId", userId);
		cv.put("channel", channel.toLowerCase().replace("-", "_"));
		cv.put("msgType", m.getMsgType().toString());
		cv.put("status", m.getStatus().getIntValue());
		cv.put("thumbnail", m.getThumbnail());
		cv.put("read", isRead ? 1 : 0);
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

	public void updateMessageData(Message m){
		SQLiteDatabase db = this.getWritableDatabase();

		Log.i(TAG, " updating message with content=" + m.getContent());
		ContentValues cv = new ContentValues();
		cv.put("msgType", m.getMsgType().toString());
		cv.put("status", m.getStatus().getIntValue());
		cv.put("dataPath", m.getDataPath());
		db.update("minou_message", cv, "message_id = ?", new String[]{m.getId().toString()});
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
		userCache.clear();
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.rawQuery("SELECT userId, username, avatar, isContact FROM minou_users;", null);

		User user = null;
		while(c.moveToNext()){
			String userId = c.getString(0);
			String username = c.getString(1);
			byte[] dataAvatar = c.getBlob(2);
			Bitmap avatar = BitmapFactory.decodeByteArray(dataAvatar, 0, dataAvatar.length);
			boolean isContact = c.getInt(3) == 1;
			Log.i(TAG, "preload userId=" + userId + " isContact=" + isContact);
			user = new User(username, Utils.getFullPrivateChannel(userId), avatar, userId, isContact);
			userCache.put(userId, user);
		}
	}

	public User getUser(String userId){
		User user = userCache.get(userId);

		if(user != null){
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
			user = new User(username, Utils.getFullPrivateChannel(userId), avatar, userId, isContact);
			break;
		}

		if(user != null){
			userCache.put(userId, user);
		}

		return user;
	}

	public User getUser(String userId, String username){
		User user = userCache.get(userId);
		if(user != null){
			if(!user.getUsername().equals(username)){
				user.setUsername(username);
				updateUsername(userId, username);
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
			user = new User(username, Utils.getFullPrivateChannel(userId), avatar, userId, isContact);

			if(!username.equals(usernameDB)){
				updateUsername(userId, username);
			}
			break;
		}

		if(user != null){
//			user = new User(username, Utils.getFullPrivateChannel(userId), AvatarGenerator.generate(Controller.getInstance().getDimensionAvatar(), Controller.getInstance().getDimensionAvatar()), userId, false);
//			addUser(user);
			userCache.put(userId, user);
		}

		

		return user;
	}

	public User addUser(String username, String id){
		User user = new User(username, Utils.getFullPrivateChannel(id), AvatarGenerator.generate(Controller.getInstance().getDimensionAvatar(), Controller.getInstance().getDimensionAvatar()), id, false);
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
		
		return user;
	}

	public void updateUsername(String userId, String username){
		SQLiteDatabase db = this.getWritableDatabase();
		Log.i(TAG, "setting username for userId=" + userId + " for username=" + username);

		ContentValues cv = new ContentValues();
		cv.put("username", username);
		db.update("minou_users", cv, "userId = ?", new String[]{userId});

		User user = userCache.get(userId);
		if(user != null){
			user.setUsername(username);
		}
	}

	public ArrayList<Message> getMessages(Channel channel){
		ArrayList<Message> messages = new ArrayList<Message>();
		SQLiteDatabase db = this.getReadableDatabase();
		Log.i(TAG, "get Messages for channel=" + channel.getNamespace().toLowerCase().replace("-", "_"));

		Cursor c = db.rawQuery("SELECT message_id, timestamp, content, isIncoming, dataPath, userId, status, msgType FROM minou_message WHERE channel = ? ORDER BY timestamp ASC;", new String[]{channel.getNamespace().toLowerCase().replace("-", "_")} );
		Message message;
		while(c.moveToNext()){
			UUID id = UUID.fromString(c.getString(0));
			Timestamp timestamp = new Timestamp(c.getLong(1));
			String text = c.getString(2);
			boolean isIncoming = c.getInt(3) == 1;
			String dataPath = c.getString(4);
			String userId = c.getString(5);
			User user = getUser(userId);
			int status = c.getInt(6);
			String msgType = c.getString(7);
			message = new Message(id, timestamp, channel.getNamespace(), 
					user, text, isIncoming, dataPath, SendingStatus.fromInt(status), MessageType.fromString(msgType));
			messages.add(message);
			updateMessageAsRead(id.toString());
		}

		return messages;
	}
	
	public ArrayList<Message> getLast25Messages(String namespace) {
		ArrayList<Message> messages = new ArrayList<Message>();
		SQLiteDatabase db = this.getReadableDatabase();
		Log.i(TAG, "get Messages for channel=" + namespace.toLowerCase().replace("-", "_"));

		Cursor c = db.rawQuery("SELECT message_id, timestamp, content, isIncoming, dataPath, userId, status, msgType FROM minou_message WHERE channel = ? ORDER BY timestamp DESC LIMIT 25;", new String[]{namespace.toLowerCase().replace("-", "_")} );
		Message message;
		while(c.moveToNext()){
			UUID id = UUID.fromString(c.getString(0));
			Timestamp timestamp = new Timestamp(c.getLong(1));
			String text = c.getString(2);
			boolean isIncoming = c.getInt(3) == 1;
			String dataPath = c.getString(4);
			String userId = c.getString(5);
			User user = getUser(userId);
			int status = c.getInt(6);
			String msgType = c.getString(7);
			message = new Message(id, timestamp, namespace, 
					user, text, isIncoming, dataPath, SendingStatus.fromInt(status), MessageType.fromString(msgType));
			messages.add(message);
		}

		return messages;
	}

	public Message getLastMessage(String channelNamespace){
		SQLiteDatabase db = this.getReadableDatabase();
		Log.i(TAG, "get last Message for channel=" + channelNamespace);

		Cursor c = db.rawQuery("SELECT message_id, timestamp, content, isIncoming, dataPath, userId, status, msgType FROM minou_message WHERE channel = ? ORDER BY timestamp DESC;", new String[]{channelNamespace} );
		Message message = null;
		if(c.moveToNext()){
			UUID id = UUID.fromString(c.getString(0));
			Timestamp timestamp = new Timestamp(c.getLong(1));
			String text = c.getString(2);
			boolean isIncoming = c.getInt(3) == 1;
			String dataPath = c.getString(4);
			String userId = c.getString(5);
			User user = getUser(userId);
			int status = c.getInt(6);
			String msgType = c.getString(7);
			message = new Message(id, timestamp, channelNamespace, 
					user, text, isIncoming, dataPath, SendingStatus.fromInt(status), MessageType.fromString(msgType));
		}

		return message;
	}
	
	public Message getMessageById(String messageId) {
		SQLiteDatabase db = this.getReadableDatabase();
		Log.i(TAG, "get Message with id=" + messageId);

		Cursor c = db.rawQuery("SELECT message_id, timestamp, content, isIncoming, dataPath, userId, status, msgType, channel FROM minou_message WHERE message_id = ?;", new String[]{messageId} );
		Message message = null;
		if(c.moveToNext()){
			UUID id = UUID.fromString(c.getString(0));
			Timestamp timestamp = new Timestamp(c.getLong(1));
			String text = c.getString(2);
			boolean isIncoming = c.getInt(3) == 1;
			String dataPath = c.getString(4);
			String userId = c.getString(5);
			User user = getUser(userId);
			int status = c.getInt(6);
			String msgType = c.getString(7);
			String channelNamespace = c.getString(8);
			message = new Message(id, timestamp, channelNamespace, 
					user, text, isIncoming, dataPath, SendingStatus.fromInt(status), MessageType.fromString(msgType));
		}

		return message;
	}

	public String getPicturePathFromMessageId(String messageId) {
		SQLiteDatabase db = this.getReadableDatabase();
		Log.i(TAG, "get Message for messageId=" + messageId);

		Cursor c = db.rawQuery("SELECT dataPath FROM minou_message WHERE message_id = ? ORDER BY timestamp DESC;", new String[]{messageId} );
		if(c.moveToNext()){
			String dataPath = c.getString(0);
			return dataPath;
		}

		return null;
	}

	public int getUnreadCountForTopic(final String namespace) {
		SQLiteDatabase db = this.getReadableDatabase();
		Log.i(TAG, "get unread count for namespace=" + namespace);
		Cursor c = db.rawQuery("SELECT COUNT(*) FROM minou_message WHERE read = 0 AND channel = ? ORDER BY timestamp ASC;", new String[]{namespace.toLowerCase().replace("-", "_")} );

		if(c.moveToNext()){
			return c.getInt(0);
		}

		return 0;
	}

	public void removeAllMessages(final String channel){
		SQLiteDatabase db = this.getWritableDatabase();

		db.beginTransaction();
		try{
			db.delete("minou_message", "channel = ?", new String[]{channel.toLowerCase().replace("-", "_")});
			db.delete("minou_message", "channel LIKE ?", new String[]{channel.toLowerCase().replace("-", "_") + ".%"});
			ContentValues cv = new ContentValues();
			cv.put("timestamp", 999999999999999999L);
			db.update("minou_last_message", cv, "channel = ?", new String[]{channel.toLowerCase().replace("-", "_")});
			db.update("minou_last_message", cv, "channel LIKE ?", new String[]{channel.toLowerCase().replace("-", "_") + ".%"});
			db.setTransactionSuccessful();
		} catch(Exception e) {
			e.printStackTrace();
		} finally{
			db.endTransaction();
		}

	}

	public ArrayList<String> getConversations(){
		ArrayList<String> usersId = new ArrayList<String>();
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c = db.rawQuery("SELECT channel FROM minou_last_message WHERE (channel LIKE '" + Channel.BASE_PRIVATE_CHANNEL + ".%') GROUP BY channel ORDER BY timestamp DESC;", null);

		while(c.moveToNext()){
			String userId = c.getString(0).replace(Channel.BASE_PRIVATE_CHANNEL, "").replace(Controller.getInstance().getId(), "").replace(".", "");
			usersId.add(userId);
		}

		return usersId;		
	}

	public ArrayList<String> getContacts(){
		ArrayList<String> usersId = new ArrayList<String>();
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c = db.rawQuery("SELECT userId FROM minou_users WHERE isContact = 1 AND userId != ?;", new String[]{Controller.getInstance().getId()});

		while(c.moveToNext()){
			String userId = c.getString(0);
			usersId.add(userId);
		}

		return usersId;		
	}

	public ArrayList<ContactPicker> getContactsForPicker(){
		ArrayList<ContactPicker> contacts = new ArrayList<ContactPicker>();
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c = db.rawQuery("SELECT userId FROM minou_users WHERE isContact = 1 AND userId != ?;", new String[]{Controller.getInstance().getId()});

		while(c.moveToNext()){
			String userId = c.getString(0);
			contacts.add(new ContactPicker(userId));
		}

		return contacts;		
	}

	public ArrayList<ContactPicker> getNonContactsForPicker() {
		ArrayList<ContactPicker> contacts = new ArrayList<ContactPicker>();
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c = db.rawQuery("SELECT userId FROM minou_users WHERE isContact = 0 AND userId != ?;", new String[]{Controller.getInstance().getId()});

		while(c.moveToNext()){
			String userId = c.getString(0);
			contacts.add(new ContactPicker(userId));
		}

		return contacts;
	}

	public void removeTopic(String channel) {
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

	public boolean isContainMessage(String userId, String content, String type, String channel) {
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c = db.rawQuery("SELECT COUNT(*) FROM minou_message WHERE channel = ? AND userId = ? AND content = ? AND msgType = ?;", new String[]{channel.toLowerCase().replace("-", "_"), userId, content, type} );

		if(c.moveToFirst()){
			return c.getInt(0) > 0;
		}

		return false;
	}

	public void eraseBD(){
		this.onUpgrade(getWritableDatabase(), 0, 1);
	}

	public boolean isAvatarNeededToChange(String userId, String avatarUrl) {
		SQLiteDatabase db = this.getReadableDatabase();

		Cursor c = db.rawQuery("SELECT COUNT(*) FROM minou_users WHERE userId = ? AND avatarURL = ?;", new String[]{userId, avatarUrl});

		while(c.moveToNext()){
			return c.getInt(0) == 0;
		}

		return true;		
	}

	public void updateAvatar(String userId, String avatarUrl, byte[] avatar) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues cv = new ContentValues();
		cv.put("avatarURL", avatarUrl);
		cv.put("avatar", avatar);
		db.update("minou_users", cv, "userId = ?", new String[]{userId});

		if(userCache.get(userId) != null){
			userCache.get(userId).setAvatar(BitmapFactory.decodeByteArray(avatar, 0, avatar.length), avatar, avatarUrl);
		}
	}

	public void updateMessageAsRead(final String messageId) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues cv = new ContentValues();
		cv.put("read", 1);
		db.update("minou_message", cv, "message_id = ?", new String[]{messageId});
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
		Log.i(TAG, " setUserAsContact: userId=" + user.getId());
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

	public ArrayList<String> getUsersId() {
		ArrayList<String> usersId = new ArrayList<String>();
		SQLiteDatabase db = this.getReadableDatabase();
		Log.i(TAG, "get usersId");

		Cursor c = db.rawQuery("SELECT userId FROM minou_users WHERE userId != ?;", new String[]{Controller.getInstance().getId()} );
		while(c.moveToNext()){
			String userId = c.getString(0);
			Log.i(TAG, "userId=" + userId);
			usersId.add(userId);
		}

		return usersId;
	}
}