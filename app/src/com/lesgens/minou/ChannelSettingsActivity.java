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
import com.lesgens.minou.models.User;

public class ChannelSettingsActivity extends MinouActivity implements OnClickListener{

	public static void show(Context context) {
		Intent i = new Intent(context, ChannelSettingsActivity.class);
		context.startActivity(i);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.user_settings);

		if(!isPrivate()){
			findViewById(R.id.topBar).setBackgroundColor(getResources().getColor(R.color.dark_main_color));
		}

		findViewById(R.id.back_btn).setOnClickListener(this);

		boolean isChecked = false;
		if(isPrivate()){
			isChecked = !PreferencesController.isPrivateNotificationsDisabled(this, Controller.getInstance().getCurrentChannel().getNamespace());
		} else{
			isChecked = PreferencesController.isPublicNotificationsEnabled(this, Controller.getInstance().getCurrentChannel().getNamespace());
		}
		((CheckBox) findViewById(R.id.setting_notifications)).setChecked(isChecked);
		findViewById(R.id.setting_notifications).setOnClickListener(this);
	}

	private boolean isPrivate(){
		return Controller.getInstance().getCurrentChannel() instanceof User;
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.back_btn){
			finish();
		} else if(v.getId() == R.id.setting_notifications){
			final boolean isChecked = ((CheckBox) findViewById(R.id.setting_notifications)).isChecked();
			if(isChecked){
				if(isPrivate()){
					PreferencesController.removePrivateNotificationDisabled(this, Controller.getInstance().getCurrentChannel().getNamespace());
				} else{
					PreferencesController.addPublicNotificationEnabled(this, Controller.getInstance().getCurrentChannel().getNamespace());	
				}
			} else{
				if(isPrivate()){
					PreferencesController.addPrivateNotificationDisabled(this, Controller.getInstance().getCurrentChannel().getNamespace());
				} else{
					PreferencesController.removePublicNotificationEnabled(this, Controller.getInstance().getCurrentChannel().getNamespace());
				}
			}
		}
	}

}
