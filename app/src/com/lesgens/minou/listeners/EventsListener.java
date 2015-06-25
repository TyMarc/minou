package com.lesgens.minou.listeners;

import java.util.List;

import com.lesgens.minou.models.Event;

public interface EventsListener {
	
	public void onNewEvent(Event event, final String channel);
	
	public void onUserHistoryReceived(List<Event> events);
	
}
