package com.lesgens.minou.controllers;

import java.text.Normalizer;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.facebook.Session;
import com.lesgens.minou.enums.Roles;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.Geolocation;
import com.lesgens.minou.models.User;
import com.lesgens.minou.utils.Utils;

public class Controller {
	private static final String TAG = "Controller";
	private Session session;
	private User myselfUser;
	private String secret;
	private Channel channelsContainer;
	private Channel currentChannel;
	private Geolocation geolocation;
	private String id;
	private Roles role;
	private String token;
	private int dimensionAvatar;

	private static Controller controller;

	private Controller(){
		secret = "";
		token = "";
	}

	public static Controller getInstance(){
		if(controller == null){
			controller = new Controller();
		}

		return controller;
	}
	
	public Channel getChannelsContainer(){
		return channelsContainer;
	}
	
	public void initChannelContainer(final Channel channel){
		channelsContainer = channel;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public void setMyOwnUser(User user){
		this.myselfUser = user;
	}

	public User getMyself(){
		return myselfUser;
	}

	public void addBlockPerson(Activity activity, String id){
		SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		String blocked = getBlockedPeopleString(activity);
		if(blocked.isEmpty()){
			blocked = id;
		} else{
			blocked += "," + id;
		}
		editor.putString("blockedList", blocked);
		editor.commit();
	}

	private String getBlockedPeopleString(Activity activity){
		SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
		String blocked = sharedPref.getString("blockedList", "");

		return blocked;

	}

	public ArrayList<String> getBlockedPeople(Activity activity){
		SharedPreferences sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
		String blocked = sharedPref.getString("blockedList", "");

		ArrayList<String> blockedPeople = new ArrayList<String>();
		for(String b : blocked.split(",")){
			blockedPeople.add(b);
		}

		Log.i(TAG, "blockedPeople=" + blockedPeople.toString());
		return blockedPeople;

	}

	public void setSecret(String secret) {
		this.secret = secret;
	}
	
	public String getSecret(){
		return secret;
	}

	public Channel getCurrentChannel() {
		return currentChannel;
	}
	
	public void setCurrentChannel(final Channel channel) {
		currentChannel = channel;
	}

	public boolean setCurrentChannel(String channel) {
		String fullChannelName = channel.toLowerCase().replace("-", "_");
		fullChannelName = Normalizer.normalize(fullChannelName, Normalizer.Form.NFD);
		fullChannelName = fullChannelName.replaceAll("\\p{M}", "");
		Channel c = channelsContainer.getChannelByName(fullChannelName);
		if(c != null){
			setCurrentChannel(c);
			return true;
		}
		
		return false;
	}
	
	public Geolocation getGeolocation(){
		return geolocation;
	}

	public void setCity(Geolocation geolocation) {
		this.geolocation = geolocation;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public String getId(){
		return id;
	}

	public void setRole(Roles role) {
		this.role = role;
	}
	
	public Roles getRole(){
		return role;
	}

	public void setToken(String token) {
		this.token = token;
	}
	
	public String getToken(){
		return token;
	}
	
	public void setDimensionAvatar(final Context context){
		dimensionAvatar = Utils.dpInPixels(context, 50);
	}

	public int getDimensionAvatar() {
		return dimensionAvatar;
	}

}
