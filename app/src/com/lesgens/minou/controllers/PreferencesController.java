package com.lesgens.minou.controllers;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesController {
	private static final String PREF_NAME = "minou_user_prefs";

	public static final String DEFAULT_CHANNEL = "USER_PREF_DEFAULT_CHANNEL";

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

}