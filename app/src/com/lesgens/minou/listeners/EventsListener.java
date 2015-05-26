package com.lesgens.minou.listeners;

import java.util.List;

import com.lesgens.minou.models.Event;

public interface EventsListener {
	
	public void onEventsReceived(List<Event> events);
	
	public void onUserHistoryReceived(List<Event> events);
	
}
