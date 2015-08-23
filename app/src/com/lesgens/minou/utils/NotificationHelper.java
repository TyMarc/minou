package com.lesgens.minou.utils;

import java.util.ArrayList;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.WearableExtender;
import android.support.v4.app.RemoteInput;
import android.text.Html;

import com.lesgens.minou.ChatActivity;
import com.lesgens.minou.R;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.enums.MessageType;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.models.User;
import com.lesgens.minou.receivers.ReplyVoiceReceiver;

public class NotificationHelper {

	public static void notify(final Context context, final Channel channel, final User user, final Message message){
		final String namespace = channel != null ? channel.getNamespace() : user.getNamespace();
		Intent resultVoiceReplyIntent = new Intent(context, ReplyVoiceReceiver.class);
		resultVoiceReplyIntent.putExtra("namespace", namespace);
		PendingIntent resultVoiceReplyPendingIntent =
				PendingIntent.getBroadcast(
						context,
						0,
						resultVoiceReplyIntent,
						PendingIntent.FLAG_UPDATE_CURRENT
						);


		String replyLabel = context.getResources().getString(R.string.reply);

		RemoteInput remoteInput = new RemoteInput.Builder(ReplyVoiceReceiver.EXTRA_VOICE_REPLY)
		.setLabel(replyLabel)
		.build();

		// Create the reply action and add the remote input
		NotificationCompat.Action action =
				new NotificationCompat.Action.Builder(R.drawable.ic_voice_search,
						context.getResources().getString(R.string.reply), resultVoiceReplyPendingIntent)
		.addRemoteInput(remoteInput)
		.build();

		
		String history = "";
		
		ArrayList<Message> messages = DatabaseHelper.getInstance().getLast25Messages(namespace);
		for(Message m : messages) {
			if(messages.indexOf(m) != 0) {
				history += "<br/><br/>";
			}
			history += "<b>" + m.getUser().getUsername() + "</b>: ";
			
			if(m.getMsgType() == MessageType.TEXT){
				history += m.getContent();
			} else if(m.getMsgType() == MessageType.IMAGE){
				history += context.getResources().getString(R.string.picture);
			} else if(m.getMsgType() == MessageType.VIDEO){
				history += context.getResources().getString(R.string.video);
			} else if(m.getMsgType() == MessageType.AUDIO){
				history += context.getResources().getString(R.string.audio);
			}
		}
		
		history += "<br/><br/>...";
		
		Notification secondPageNotification =
				new NotificationCompat.Builder(context)
		.setContentText(Html.fromHtml(history))
		.build();
		
		String content = "";
		if(message.getMsgType() == MessageType.TEXT){
			content = message.getContent();
		} else if(message.getMsgType() == MessageType.IMAGE){
			content = context.getResources().getString(R.string.picture);
		} else if(message.getMsgType() == MessageType.VIDEO){
			content = context.getResources().getString(R.string.video);
		} else if(message.getMsgType() == MessageType.AUDIO){
			content = context.getResources().getString(R.string.audio);
		}

		NotificationCompat.Builder mBuilder =
				new NotificationCompat.Builder(context)
		.setLargeIcon(user != null ? Utils.cropToCircle(user.getAvatar()) : BitmapFactory.decodeResource(context.getResources(), R.drawable.minou_notif))
		.setSmallIcon(R.drawable.mail)
		.setContentTitle(user.getUsername())
		.setContentText(content)
		.setColor(Color.DKGRAY)
		.setVibrate(new long[] { 0, 500})
		.setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
		.extend(new WearableExtender().addAction(action).addPage(secondPageNotification))
		.setLights(context.getResources().getColor(R.color.main_color), 1000, 3000)
		.setAutoCancel(true);

		if(channel != null){
			mBuilder.setContentInfo(Utils.capitalizeFirstLetters(channel.getName()));
		}

		Intent resultIntent = new Intent(context, ChatActivity.class);
		resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_CLEAR_TASK);
		resultIntent.putExtra("namespace", channel != null ? channel.getNamespace() : user.getNamespace());
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
