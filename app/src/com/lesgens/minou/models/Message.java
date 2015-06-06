package com.lesgens.minou.models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.UUID;



public class Message extends Event{

	public enum Gender {
		Male, Female, Custom
	};

	private String message;
	private ArrayList<UUID> idsMessage;
	private boolean isIncoming;
	private Gender gender;

	public Message(User user, String message, boolean isIncoming){
		this(UUID.randomUUID(), new Timestamp(System.currentTimeMillis()), null, user, message, null, Gender.Custom, isIncoming, null);
	}
	
	public Message(User user, String message, IDestination iDestionation, boolean isIncoming){
		this(UUID.randomUUID(), new Timestamp(System.currentTimeMillis()), iDestionation, user, message, null, Gender.Custom, isIncoming, null);
	}
	
	public Message(UUID id, Timestamp timestamp, User user, String realName, String fakeName, String message, boolean isIncoming){
		this(id, timestamp, null, user, message, fakeName, Gender.Custom, isIncoming, realName);
	}

	public Message(UUID id, Timestamp timestamp, IDestination destination, User user, String message, String fakeName, Gender gender, boolean isIncoming, String realName) {
		super(id, timestamp, destination, user, realName, fakeName);
		this.message = message;
		this.isIncoming = isIncoming;
		idsMessage = new ArrayList<UUID>();
		idsMessage.add(id);
		this.gender = gender;
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

	public Gender getGender() {
		return gender;
	}

	public void setIsIncoming(boolean b) {
		isIncoming = b;
	}

}
