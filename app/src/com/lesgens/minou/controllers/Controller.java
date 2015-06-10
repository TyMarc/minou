package com.lesgens.minou.controllers;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.checkin.avatargenerator.AvatarGenerator;
import com.facebook.Session;
import com.lesgens.minou.enums.Roles;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.Geolocation;
import com.lesgens.minou.models.User;
import com.lesgens.minou.utils.Utils;

public class Controller {
	private static final String TAG = "Controller";
	private HashMap<String, User> users;
	private Session session;
	private User myselfUser;
	private int dimensionAvatar;
	private String secret;
	private Channel channelsContainer;
	private Channel currentChannel;
	private Geolocation geolocation;
	private String authId;
	private Roles role;

	private static Controller controller;

	private Controller(){
		secret = "";
		users = new HashMap<String, User>();
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

	public void addUser(User user){
		users.put(user.getId(), user);
	}

	public User getUser(String tokenId){
		if(users.get(tokenId) == null){
			users.put(tokenId, new User(tokenId, Channel.BASE_CHANNEL + tokenId, AvatarGenerator.generate(dimensionAvatar, dimensionAvatar), tokenId));
		}
		return users.get(tokenId);
	}
	
	public User getUserByName(String name){
		for(User user : users.values()){
			if(user.getName() != null && user.getName().toLowerCase().equals(name.toLowerCase())){
				return user;
			}
		}
		
		return null;
	}
	
	public User getUser(final String remoteId, final String userName){
		if(users.get(remoteId) == null){
			users.put(remoteId, new User(userName, Channel.BASE_CHANNEL + remoteId, AvatarGenerator.generate(dimensionAvatar, dimensionAvatar), remoteId));
		}
		return users.get(remoteId);
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

	public String getMyId(){
		return myselfUser.getId().substring(0, myselfUser.getId().indexOf("."));
	}

	public void setDimensionAvatar(Context context) {
		dimensionAvatar = Utils.dpInPixels(context, 50);
	}

	public int getDimensionAvatar() {
		return dimensionAvatar;
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

	public void setAuthId(String authId) {
		this.authId = authId;
	}
	
	public String getAuthId(){
		return authId;
	}

	public void setRole(Roles role) {
		this.role = role;
	}
	
	public Roles getRole(){
		return role;
	}

}
