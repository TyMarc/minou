package com.lesgens.minou.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.enums.MessageType;
import com.lesgens.minou.enums.SendingStatus;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.utils.NotificationHelper;
import com.lesgens.minou.utils.Utils;

public class ReplyVoiceReceiver extends BroadcastReceiver {
	public static final String EXTRA_VOICE_REPLY = "extra_voice_reply";

    public void onReceive(Context context, Intent intent) {
    	Log.i("ReplyVoiceActivity", "intent=" + intent);
        if (intent == null)
            return;

        String channelNamespace = intent.getStringExtra("namespace");
		CharSequence text = getMessageText(intent);
		
		if(text != null) {
			text = Utils.capitalizeFirstLetter(text.toString());
			Message message = new Message(Controller.getInstance().getMyself(), text.toString(), false, SendingStatus.PENDING, MessageType.TEXT);
			Server.sendMessage(message, channelNamespace);
			DatabaseHelper.getInstance().addMessage(message, Controller.getInstance().getId(), channelNamespace, true);
		}
		NotificationHelper.cancelAll(context);
    }

    private CharSequence getMessageText(Intent intent) {
	    Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);
	    if (remoteInput != null) {
	        return remoteInput.getCharSequence(EXTRA_VOICE_REPLY);
	    }
	    return null;
	}
}