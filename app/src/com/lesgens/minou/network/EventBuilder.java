package com.lesgens.minou.network;

import java.sql.Timestamp;
import java.util.UUID;

import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.models.City;
import com.lesgens.minou.models.Event;
import com.lesgens.minou.models.IDestination;
import com.lesgens.minou.models.Message;

public class EventBuilder {
	
	public static Event buildEvent(UUID id, String type, String destination, Timestamp timestamp, String userId, String message, String fakeName){
		
		IDestination destinationObj;
		String[] parts = destination.split(":");
		if (parts[0].equals("user")) {
			destinationObj = Controller.getInstance().getUser(parts[1]);
		} else if(destination.startsWith("city")) {
			destinationObj = new City(parts[1], "", "");
		}
		else {
			return null;
		}
		
		
		if(type.equals("message")) {
			return new Message(id, timestamp, destinationObj, Controller.getInstance().getUser(userId), message, fakeName, true, null);
		}
		
		return null;
	}

}
