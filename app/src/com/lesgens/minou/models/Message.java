package com.lesgens.minou.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.UUID;

import android.util.Log;

import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.lesgens.minou.enums.SendingStatus;
import com.lesgens.minou.network.Server;



public class Message extends Event implements ProgressListener{

	private String message;
	private ArrayList<UUID> idsMessage;
	private boolean isIncoming;
	private byte[] data;
	private String userId;
	private SendingStatus status;
	private String filename;

	public Message(User user, String message, boolean isIncoming, SendingStatus status){
		this(UUID.randomUUID(), new Timestamp(System.currentTimeMillis()), null, user, message,  isIncoming, null, status);
	}
	
	public Message(User user, byte[] data, boolean isIncoming, SendingStatus status){
		this(UUID.randomUUID(), new Timestamp(System.currentTimeMillis()), null, user, null,  isIncoming, data, status);
	}
	
	public Message(User user, String message, String userName, Channel iDestination, boolean isIncoming, byte[] data, SendingStatus status){
		this(UUID.randomUUID(), new Timestamp(System.currentTimeMillis()), iDestination, user, message,  isIncoming, data, status);
	}
	
	public Message(UUID id, Timestamp timestamp, Channel channel, User user, String message, boolean isIncoming, byte[] data, SendingStatus status) {
		super(id, timestamp, channel, user);
		userId = user.getId();
		this.message = message;
		this.isIncoming = isIncoming;
		idsMessage = new ArrayList<UUID>();
		idsMessage.add(id);
		this.data = data;
		this.status = status;
	}
	
	public String getUserId(){
		return userId;
	}

	public boolean isIncoming(){
		return isIncoming;
	}

	public String getMessage(){
		return message;
	}

	public void addMessage(String newMessage, UUID id){
		message = message + "\n" + newMessage;
		idsMessage.add(id);
	}

	public ArrayList<UUID> getIdsMessage(){
		return idsMessage;
	}

	public byte[] getData(){
		return data;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof Message){
			Message other = (Message) o;
			for(UUID id : other.getIdsMessage()){
				if(idsMessage.contains(id)){
					return true;
				}
			}
		}

		return false;
	}

	public void setIsIncoming(boolean b) {
		isIncoming = b;
	}

	@Override
	public void progressChanged(ProgressEvent event) {
		if(event.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE){
			Server.publishMessage(this);
		}
		
		Log.i("Message", "progressChanged: eventCode=" + event.getEventCode() + " byteTransfered=" + event.getBytesTransferred());
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getFilename(){
		return filename;
	}
}
