package com.lesgens.minou.controllers;

import java.text.Normalizer;

import android.content.Context;

import com.facebook.Session;
import com.lesgens.minou.enums.Roles;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.Geolocation;
import com.lesgens.minou.models.User;
import com.lesgens.minou.utils.Utils;

public class Controller {
	//private static final String TAG = "Controller";
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
		dimensionAvatar = Utils.dpInPixels(context, 100);
	}

	public int getDimensionAvatar() {
		return dimensionAvatar;
	}

}
