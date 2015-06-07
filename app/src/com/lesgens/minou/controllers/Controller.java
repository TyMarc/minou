package com.lesgens.minou.controllers;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.checkin.avatargenerator.AvatarGenerator;
import com.facebook.Session;
import com.lesgens.minou.models.City;
import com.lesgens.minou.models.User;
import com.lesgens.minou.utils.Utils;

public class Controller {
	private static final String TAG = "Controller";
	private City city;
	private HashMap<String, User> users;
	private Session session;
	private User myselfUser;
	private int dimensionAvatar;

	private static Controller controller;

	private Controller(){
		city = new City("", null, null);
		users = new HashMap<String, User>();
	}

	public static Controller getInstance(){
		if(controller == null){
			controller = new Controller();
		}

		return controller;
	}

	public void setCity(City city){
		this.city = city;
	}

	public City getCity(){
		return city;
	}

	public void addUser(User user){
		users.put(user.getId(), user);
	}
	
	public ArrayList<String> getCityList(){
		ArrayList<String> list = new ArrayList<String>();
		list.add(city.getCountry());
		list.add(city.getState());
		list.add(city.getName());
		
		return list;
	}

	public User getUser(String tokenId){
		if(users.get(tokenId) == null){
			users.put(tokenId, new User(tokenId, AvatarGenerator.generate(dimensionAvatar, dimensionAvatar), tokenId));
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
			users.put(remoteId, new User(userName, AvatarGenerator.generate(dimensionAvatar, dimensionAvatar), remoteId));
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

}
