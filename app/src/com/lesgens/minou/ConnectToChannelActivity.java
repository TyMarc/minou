package com.lesgens.minou;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.lesgens.minou.adapters.ChannelsAdapter;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.controllers.PreferencesController;
import com.lesgens.minou.network.Server;

public class ConnectToChannelActivity extends MinouActivity implements OnClickListener, TextWatcher{
	private boolean isPrivateChannelPicker;
	private ChannelsAdapter adapter;

	public static void show(final Context context, final boolean isPrivateChannelPicker) {
		Intent i = new Intent(context, ConnectToChannelActivity.class);
		i.putExtra("isPrivateChannelPicker", isPrivateChannelPicker);
		context.startActivity(i);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.channel_picker);

		isPrivateChannelPicker = getIntent().getBooleanExtra("isPrivateChannelPicker", false);

		((EditText) findViewById(R.id.editText)).addTextChangedListener(this);
		InputFilter filter = new InputFilter() { 
			@Override
			public CharSequence filter(CharSequence source, int start, int end, 
					Spanned dest, int dstart, int dend) { 
				for (int i = start; i < end; i++) { 
					if (!Character.isLetterOrDigit(source.charAt(i)) && source.charAt(i) != ' ') { 
						return ""; 
					} 
				} 
				return null; 
			}
		};
		((EditText) findViewById(R.id.editText)).setFilters(new InputFilter[]{filter}); 
		findViewById(R.id.currently_written).setOnClickListener(this);

		adapter = new ChannelsAdapter(this, Server.getTrendingTopics());
		((ListView) findViewById(R.id.list_view)).setAdapter(adapter);
	}


	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.currently_written){
			final String text = ((TextView) v).getText().toString().trim();
			if(!text.trim().isEmpty() && !text.trim().toLowerCase().equals(Controller.getInstance().getCity()
					.getName().toLowerCase())){
				if(isPrivateChannelPicker){
					PreferencesController.addPrivateChannel(this, text);
					Server.subscribeToPrivateChannel(this, text);
					finish();
				} else{
					PreferencesController.addChannel(this, text);
					Server.subscribeToChannel(this, text);
					ChannelChatActivity.show(this, text);
					finish();
				}
			}
		}
	}

	@Override
	public void afterTextChanged(Editable arg0) {
		((TextView) findViewById(R.id.currently_written)).setText(arg0.toString());
	}

	@Override
	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
			int arg3) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub

	}


}
