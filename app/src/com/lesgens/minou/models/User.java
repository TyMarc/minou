package com.lesgens.minou.models;

import com.lesgens.minou.db.DatabaseHelper;

import android.graphics.Bitmap;

public class User extends Channel{
	private Bitmap avatar;
	private String userId;
	private String name;
	private boolean isContact;
	
	public User(String name, String namespace, Bitmap avatar, String userId, boolean isContact){
		super(namespace, null);
		this.name = name;
		this.avatar = avatar;
		this.userId = userId;
		this.isContact = isContact;
	}
	
	public boolean isContact(){
		return isContact;
	}
	
	public Bitmap getAvatar(){
		return avatar;
	}

	public String getId() {
		return userId;
	}
	
	public String getUsername(){
		return name;
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof User){
			if(((User) o).getId() != null && userId != null){
				if(((User) o).getId().toLowerCase().equals(userId.toLowerCase())){
					return true;
				}
			}
		}
		return false;
	}

	public void setAvatar(Bitmap avatar, byte[] avatarByteArray, String avatarUrl) {
		this.avatar = avatar;
		DatabaseHelper.getInstance().updateAvatar(userId, avatarUrl, avatarByteArray);
	}

	public void setUsername(String username) {
		this.name = username;
	}

	public void setIsContact(boolean isContact) {
		this.isContact = isContact;
	}
	
}
