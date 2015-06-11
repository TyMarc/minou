package com.lesgens.minou.models;

import java.util.ArrayList;

import rx.Observable;
import rx.schedulers.Schedulers;
import ws.wamp.jawampa.PubSubData;
import android.util.Log;

import com.lesgens.minou.controllers.Controller;

public class Channel {
	private static final String TAG = "Channel";
	public static final String BASE_CHANNEL = "minou.";
	public static final String BASE_PUBLIC_CHANNEL = BASE_CHANNEL + "public";
	public static final String WORLDWIDE_CHANNEL = BASE_PUBLIC_CHANNEL + ".worldwide";
	private String name;
	private String namespace;
	private Observable<PubSubData> subscription;
	private ArrayList<Channel> channels;

	public Channel(String namespace, Observable<PubSubData> subscription) {
		channels = new ArrayList<Channel>();
		this.name = namespace.substring(namespace.lastIndexOf(".") + 1);
		this.namespace = namespace;
		this.subscription = subscription;
	}

	public Observable<PubSubData> getSubscription(){
		return subscription;
	}
	
	public void setSubscription(final Observable<PubSubData> subscription){
		this.subscription = subscription;
	}
	
	public String getNamespace(){
		return namespace;
	}
	
	public ArrayList<Channel> getChannels(){
		return channels;
	}

	public boolean isContainSubscription(final String channelName){
		if(namespace.equals(channelName)) return true;
		
		for(Channel channel : channels){
			if(channel.isContainSubscription(channelName)) return true;
		}
		
		return false;
	}
	
	public Channel getParent(){
		Log.i(TAG, "Looking for parent=" + namespace.substring(0,namespace.lastIndexOf(".")));
		if(!namespace.equals(WORLDWIDE_CHANNEL)){
			return Controller.getInstance().getChannelsContainer().getChannelByName(namespace.substring(0,namespace.lastIndexOf(".")));
		}
		return null;
	}

	public void addSubscription(final Channel channel){
		if(channel.getNamespace().substring(0,channel.getNamespace().lastIndexOf(".")).equals(namespace)){
			channels.add(channel);
		} else{
			for(Channel c : channels){
				c.addSubscription(channel);
			}
		}
	}
	
	public void addByForceSubscription(final Channel channel){
		channels.add(channel);
	}

	public void closeSubscriptions(){
		for(Channel channel : channels){
			channel.closeSubscriptions();
		}

		if(subscription != null){
			subscription.unsubscribeOn(Schedulers.immediate());
		}
	}

	public String getName(){
		return name;
	}

	public String getId(){
		return name;
	}
	
	public int getNumberOfChildren(){
		int nb = channels.size();
		for(Channel c: channels){
			nb += c.getNumberOfChildren();
		}
		
		return nb;
	}

	public Channel getChannelByName(String channel) {
		Log.i(TAG, "Searching for=" + channel + " this one=" + namespace);
		if(namespace.equals(channel)){
			return this;
		}
		for(Channel c : channels){
			Channel rep = c.getChannelByName(channel);
			if(rep != null) return rep;
		}
		
		return null;
	}
}
