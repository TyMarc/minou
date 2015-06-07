package com.lesgens.minou.controllers;

import java.util.ArrayList;
import java.util.Arrays;

import com.lesgens.minou.utils.Utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesController {
	private static final String PREF_NAME = "blindr_user_prefs";

	public static final String INTERESTED_IN = "USER_PREF_INTERESTED_IN";
	public static final String LAST_CONNECTION = "USER_PREF_LAST_CONNECTION";
	public static final String FIRST_TIME_USE = "USER_PREF_FIRST_TIME_USE";
	public static final String CHANNELS = "USER_PREF_CHANNELS";
	public static final String PRIVATE_CHANNELS = "USER_PREF_PRIVATES";

	public static void setPreference(final Context context, final String preference, final String value){
		SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
		editor.putString(preference, value);
		editor.commit();
	}

	public static void setPreference(final Context context, final String preference, final boolean value){
		SharedPreferences.Editor editor = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit();
		editor.putBoolean(preference, value);
		editor.commit();
	}

	public static String getInterestedIn(final Context context){
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); 
		String restoredText = prefs.getString(INTERESTED_IN, "");
		return restoredText;
	}

	public static String getLastConnection(final Context context){
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); 
		String restoredText = prefs.getString(LAST_CONNECTION, "");
		return restoredText;
	}

	public static ArrayList<String> getChannels(final Context context){
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); 
		String restoredText = prefs.getString(CHANNELS, null);
		if(restoredText == null || restoredText.isEmpty()) return new ArrayList<String>();
		return new ArrayList<String>(Arrays.asList(restoredText.split("/")));
	}

	public static ArrayList<String> getPrivateChannels(final Context context){
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); 
		String restoredText = prefs.getString(PRIVATE_CHANNELS, null);
		if(restoredText == null || restoredText.isEmpty()) return new ArrayList<String>();
		return new ArrayList<String>(Arrays.asList(restoredText.split("/")));
	}

	public static boolean isFirstUse(final Context context){
		SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); 
		boolean firstUse = prefs.getBoolean(FIRST_TIME_USE, true);
		return firstUse;
	}

	public static void addChannel(final Context context, final String channel){
		ArrayList<String> channels = getChannels(context);
		if(!channels.contains(channel)){
			channels.add(channel);
			setPreference(context, CHANNELS, Utils.transformArrayListToString(channels));
		}
	}

	public static void addPrivateChannel(final Context context, final String channel){
		ArrayList<String> channels = getPrivateChannels(context);
		if(!channels.contains(channel)){
			channels.add(channel);
			setPreference(context, PRIVATE_CHANNELS, Utils.transformArrayListToString(channels));
		}
	}

	public static void removeChannel(final Context context, final String channel){
		ArrayList<String> channels = getChannels(context);
		channels.remove(channel);
		setPreference(context, CHANNELS, Utils.transformArrayListToString(channels));
	}

	public static void removePrivateChannel(final Context context, final String channel){
		ArrayList<String> channels = getPrivateChannels(context);
		channels.remove(channel);
		setPreference(context, PRIVATE_CHANNELS, Utils.transformArrayListToString(channels));
	}

}