package com.lesgens.minou.controllers;

import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesController {
	private static final String PREF_NAME = "minou_user_prefs";

	public static final String DEFAULT_CHANNEL = "USER_PREF_DEFAULT_CHANNEL";
	public static final String BLOCKED_USERS = "USER_PREF_BLOCKED_USERS";
	public static final String NOTIFICATIONS_PUBLIC_CHANNEL = "USER_PREF_NOTIFICATIONS_PUBLIC_CHANNEL";
	public static final String BLOCKED_NOTIFICATIONS_PRIVATE_CHANNEL = "USER_PREF_BLOCKED_NOTIFICATIONS_PRIVATE_CHANNEL";
	public static final String BLOCKED_NOTIFICATIONS_MENTION_CHANNEL = "USER_PREF_BLOCKED_NOTIFICATIONS_MENTION_CHANNEL";
	public static final String FETCH_ALL_MESSAGES = "USER_PREF_FETCH_ALL_MESSAGES";

	public static void setPreference(final Context context, final String preference, final String value){
		SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
		editor.putString(preference, value);
		editor.commit();
	}

	public static String getDefaultChannel(final Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); 
		String restoredText = prefs.getString(DEFAULT_CHANNEL, "minou.public.worldwide");
		return restoredText;
	}

	public static boolean isPublicNotificationsEnabled(final Context context, final String channelNamespace) {
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); 
		String restoredText = prefs.getString(NOTIFICATIONS_PUBLIC_CHANNEL, "");
		return restoredText.contains(channelNamespace);
	}

	public static void addPublicNotificationEnabled(final Context context, final String channelNamespace){
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); 
		String restoredText = prefs.getString(NOTIFICATIONS_PUBLIC_CHANNEL, "");
		if(!restoredText.contains(channelNamespace)){
			if(!restoredText.isEmpty()){
				restoredText += ",";
			}
			restoredText += channelNamespace;
			SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
			editor.putString(NOTIFICATIONS_PUBLIC_CHANNEL, restoredText);
			editor.commit();
		}
	}
	
	public static void removePublicNotificationEnabled(final Context context, final String channelNamespace){
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); 
		String restoredText = prefs.getString(NOTIFICATIONS_PUBLIC_CHANNEL, "");
		if(restoredText.contains(channelNamespace)){
			if(restoredText.indexOf(channelNamespace) > 0){
				restoredText = restoredText.replace("," + channelNamespace, "");
			} else{
				restoredText = restoredText.replace(channelNamespace, "");
			}
			SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
			editor.putString(NOTIFICATIONS_PUBLIC_CHANNEL, restoredText);
			editor.commit();
		}
	}
	
	public static boolean isPrivateNotificationsDisabled(final Context context, final String channelNamespace) {
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); 
		String restoredText = prefs.getString(BLOCKED_NOTIFICATIONS_PRIVATE_CHANNEL, "");
		return restoredText.contains(channelNamespace);
	}
	
	public static void addPrivateNotificationDisabled(final Context context, final String channelNamespace){
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); 
		String restoredText = prefs.getString(BLOCKED_NOTIFICATIONS_PRIVATE_CHANNEL, "");
		if(!restoredText.contains(channelNamespace)){
			if(!restoredText.isEmpty()){
				restoredText += ",";
			}
			restoredText += channelNamespace;
			SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
			editor.putString(BLOCKED_NOTIFICATIONS_PRIVATE_CHANNEL, restoredText);
			editor.commit();
		}
	}
	
	public static void removePrivateNotificationDisabled(final Context context, final String channelNamespace){
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); 
		String restoredText = prefs.getString(BLOCKED_NOTIFICATIONS_PRIVATE_CHANNEL, "");
		if(restoredText.contains(channelNamespace)){
			if(restoredText.indexOf(channelNamespace) > 0){
				restoredText = restoredText.replace("," + channelNamespace, "");
			} else{
				restoredText = restoredText.replace(channelNamespace, "");
			}
			SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
			editor.putString(BLOCKED_NOTIFICATIONS_PRIVATE_CHANNEL, restoredText);
			editor.commit();
		}
	}
	
	public static boolean isMentionNotificationsDisabled(final Context context, final String channelNamespace) {
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); 
		String restoredText = prefs.getString(BLOCKED_NOTIFICATIONS_MENTION_CHANNEL, "");
		return restoredText.contains(channelNamespace);
	}
	
	public static void addMentionNotificationDisabled(final Context context, final String channelNamespace){
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); 
		String restoredText = prefs.getString(BLOCKED_NOTIFICATIONS_MENTION_CHANNEL, "");
		if(!restoredText.contains(channelNamespace)){
			if(!restoredText.isEmpty()){
				restoredText += ",";
			}
			restoredText += channelNamespace;
			SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
			editor.putString(BLOCKED_NOTIFICATIONS_MENTION_CHANNEL, restoredText);
			editor.commit();
		}
	}
	
	public static void removeMentionNotificationDisabled(final Context context, final String channelNamespace){
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); 
		String restoredText = prefs.getString(BLOCKED_NOTIFICATIONS_MENTION_CHANNEL, "");
		if(restoredText.contains(channelNamespace)){
			if(restoredText.indexOf(channelNamespace) > 0){
				restoredText = restoredText.replace("," + channelNamespace, "");
			} else{
				restoredText = restoredText.replace(channelNamespace, "");
			}
			SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
			editor.putString(BLOCKED_NOTIFICATIONS_MENTION_CHANNEL, restoredText);
			editor.commit();
		}
	}
	
	public static void addBlockPerson(Context context, String id){
		SharedPreferences sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sharedPref.edit();
		String blocked = getBlockedPeopleString(context);
		if(blocked.isEmpty()){
			blocked = id;
		} else{
			blocked += "," + id;
		}
		editor.putString(BLOCKED_USERS, blocked);
		editor.commit();
	}

	private static String getBlockedPeopleString(Context context){
		SharedPreferences sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		String blocked = sharedPref.getString(BLOCKED_USERS, "");

		return blocked;

	}

	public static ArrayList<String> getBlockedPeople(Context context){
		SharedPreferences sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
		String blocked = sharedPref.getString(BLOCKED_USERS, "");

		ArrayList<String> blockedPeople = new ArrayList<String>();
		for(String b : blocked.split(",")){
			blockedPeople.add(b);
		}

		return blockedPeople;

	}
	
	public static boolean isTopicFetchAllMessagesEnabled(final Context context, final String channelNamespace) {
		if(context == null) {
			return false;
		}
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); 
		String restoredText = prefs.getString(FETCH_ALL_MESSAGES, "");
		return restoredText.contains(channelNamespace);
	}
	
	public static void addTopicFetchAllMessages(final Context context, final String channelNamespace){
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); 
		String restoredText = prefs.getString(FETCH_ALL_MESSAGES, "");
		if(!restoredText.contains(channelNamespace)){
			if(!restoredText.isEmpty()){
				restoredText += ",";
			}
			restoredText += channelNamespace;
			SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
			editor.putString(FETCH_ALL_MESSAGES, restoredText);
			editor.commit();
		}
	}
	
	public static void removeTopicFetchAllMessages(final Context context, final String channelNamespace){
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); 
		String restoredText = prefs.getString(FETCH_ALL_MESSAGES, "");
		if(restoredText.contains(channelNamespace)){
			if(restoredText.indexOf(channelNamespace) > 0){
				restoredText = restoredText.replace("," + channelNamespace, "");
			} else{
				restoredText = restoredText.replace(channelNamespace, "");
			}
			SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
			editor.putString(FETCH_ALL_MESSAGES, restoredText);
			editor.commit();
		}
	}
}