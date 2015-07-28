package com.lesgens.minou;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.CheckBox;

import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.controllers.PreferencesController;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.User;

public class ChannelSettingsActivity extends MinouActivity implements OnClickListener{
	private Channel channel;
	private String namespace;

	public static void show(Context context, final String namespace) {
		Intent i = new Intent(context, ChannelSettingsActivity.class);
		i.putExtra("namespace", namespace);
		context.startActivity(i);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.user_settings);
		
		namespace = getIntent().getStringExtra("namespace");
		channel = Controller.getInstance().getChannelsContainer().getChannelByName(namespace);

		findViewById(R.id.back_btn).setOnClickListener(this);

		boolean isChecked = false;
		if(isPrivate()){
			isChecked = !PreferencesController.isPrivateNotificationsDisabled(this, namespace);
			findViewById(R.id.setting_fetch_all_messages_sep).setVisibility(View.GONE);
			findViewById(R.id.setting_fetch_all_messages).setVisibility(View.GONE);
		} else{
			isChecked = PreferencesController.isPublicNotificationsEnabled(this, namespace);
		}
		((CheckBox) findViewById(R.id.setting_notifications)).setChecked(isChecked);
		findViewById(R.id.setting_notifications).setOnClickListener(this);
		
		isChecked = !PreferencesController.isMentionNotificationsDisabled(this, namespace);
		((CheckBox) findViewById(R.id.setting_notifications_mention)).setChecked(isChecked);
		findViewById(R.id.setting_notifications_mention).setOnClickListener(this);
		
		isChecked = PreferencesController.isTopicFetchAllMessagesEnabled(this, namespace);
		((CheckBox) findViewById(R.id.setting_fetch_all_messages)).setChecked(isChecked);
		findViewById(R.id.setting_fetch_all_messages).setOnClickListener(this);
	}

	private boolean isPrivate(){
		return channel instanceof User;
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.back_btn){
			finish();
		} else if(v.getId() == R.id.setting_notifications){
			final boolean isChecked = ((CheckBox) findViewById(R.id.setting_notifications)).isChecked();
			if(isChecked){
				if(isPrivate()){
					PreferencesController.removePrivateNotificationDisabled(this, namespace);
				} else{
					PreferencesController.addPublicNotificationEnabled(this, namespace);	
				}
			} else{
				if(isPrivate()){
					PreferencesController.addPrivateNotificationDisabled(this, namespace);
				} else{
					PreferencesController.removePublicNotificationEnabled(this, namespace);
				}
			}
		} else if(v.getId() == R.id.setting_notifications_mention){
			final boolean isChecked = ((CheckBox) findViewById(R.id.setting_notifications_mention)).isChecked();
			if(isChecked){
				PreferencesController.removeMentionNotificationDisabled(this, namespace);
			} else{
				PreferencesController.addMentionNotificationDisabled(this, namespace);

			}
		} else if(v.getId() == R.id.setting_fetch_all_messages){
			final boolean isChecked = ((CheckBox) findViewById(R.id.setting_fetch_all_messages)).isChecked();
			if(isChecked){
				PreferencesController.addTopicFetchAllMessages(this, namespace);
			} else{
				PreferencesController.removeTopicFetchAllMessages(this, namespace);

			}
		}
	}

}
