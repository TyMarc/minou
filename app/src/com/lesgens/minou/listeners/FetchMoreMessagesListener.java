package com.lesgens.minou.listeners;

import java.util.ArrayList;

import com.lesgens.minou.models.Message;

public interface FetchMoreMessagesListener {
	public void onMessagesFetch(ArrayList<Message> messages);	
}
