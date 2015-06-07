package com.lesgens.minou.utils;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.support.v4.app.NotificationCompat;

import com.lesgens.minou.ChannelChatActivity;
import com.lesgens.minou.R;
import com.lesgens.minou.models.User;

public class NotificationHelper {

	public static void notify(final Context context, final String channel, final User user, final String content){
		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(context)
		.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.minou_notif))
		.setSmallIcon(R.drawable.mail)
		.setContentTitle(user.getName())
		.setContentText(content)
		.setColor(Color.BLUE)
		.setLights(Color.BLUE, 1000, 1000)
		.setAutoCancel(true);
		
		if(!channel.equals(user.getId())){
			mBuilder.setContentInfo(channel);
		}
		
		Intent resultIntent = new Intent(context, ChannelChatActivity.class);
		resultIntent.putExtra("channelName", channel);
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
