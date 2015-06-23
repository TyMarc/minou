package com.lesgens.minou.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;

import com.lesgens.minou.ChatActivity;
import com.lesgens.minou.R;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.User;

public class NotificationHelper {

	public static void notify(final Context context, final Channel channel, final User user, final String content){
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(context)
		.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.minou_notif))
		.setSmallIcon(R.drawable.mail)
		.setContentTitle(user.getUsername())
		.setContentText(content)
		.setColor(Color.BLUE)
		.setVibrate(new long[] { 0, 500})
		.setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
		.setLights(Color.BLUE, 1000, 1000)
		.setAutoCancel(true);
		
		if(channel != null){
			mBuilder.setContentInfo(Utils.capitalizeFirstLetters(channel.getName()));
		}
		
		Intent resultIntent = new Intent(context, ChatActivity.class);
		resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
		resultIntent.putExtra("channelName", channel != null ? channel.getNamespace() : user.getNamespace());
		PendingIntent resultPendingIntent =
		    PendingIntent.getActivity(
		    context,
		    0,
		    resultIntent,
		    PendingIntent.FLAG_UPDATE_CURRENT
		);
		mBuilder.setContentIntent(resultPendingIntent);
		
		// Sets an ID for the notification
		int mNotificationId = 001;
		// Gets an instance of the NotificationManager service
		NotificationManager mNotifyMgr = 
		        (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		// Builds the notification and issues it.
		mNotifyMgr.notify(mNotificationId, mBuilder.build());
	}
	
	public static void cancelAll(final Context context){
		NotificationManager notifManager= (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notifManager.cancelAll();
	}
}
