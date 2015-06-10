package com.lesgens.minou.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.UUID;



public class Message extends Event{

	private String message;
	private ArrayList<UUID> idsMessage;
	private boolean isIncoming;
	private byte[] data;

	public Message(User user, String message, boolean isIncoming){
		this(UUID.randomUUID(), new Timestamp(System.currentTimeMillis()), null, user, message,  isIncoming, null);
	}
	
	public Message(User user, byte[] data, boolean isIncoming){
		this(UUID.randomUUID(), new Timestamp(System.currentTimeMillis()), null, user, null,  isIncoming, data);
	}
	
	public Message(User user, String message, String userName, Channel iDestination, boolean isIncoming, byte[] data){
		this(UUID.randomUUID(), new Timestamp(System.currentTimeMillis()), iDestination, user, message,  isIncoming, data);
	}
	
	public Message(UUID id, Timestamp timestamp, Channel channel, User user, String message, boolean isIncoming, byte[] data) {
		super(id, timestamp, channel, user);
		this.message = message;
		this.isIncoming = isIncoming;
		idsMessage = new ArrayList<UUID>();
		idsMessage.add(id);
		this.data = data;
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

}
