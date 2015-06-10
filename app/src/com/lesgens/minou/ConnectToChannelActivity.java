package com.lesgens.minou;

import android.app.Activity;
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
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.User;
import com.lesgens.minou.network.Server;

public class ConnectToChannelActivity extends MinouActivity implements OnClickListener, TextWatcher{
	private boolean isPrivateChannelPicker;
	private ChannelsAdapter adapter;
	private String currentNamespace;

	public static void show(final Activity activity, final boolean isPrivateChannelPicker, final String currentNamespace, final int requestCode) {
		Intent i = new Intent(activity, ConnectToChannelActivity.class);
		i.putExtra("isPrivateChannelPicker", isPrivateChannelPicker);
		i.putExtra("currentNamespace", currentNamespace);
		activity.startActivityForResult(i, requestCode);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.channel_picker);

		isPrivateChannelPicker = getIntent().getBooleanExtra("isPrivateChannelPicker", false);
		currentNamespace = getIntent().getStringExtra("currentNamespace");
		if(currentNamespace == null) currentNamespace = Channel.BASE_CHANNEL;

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

		if(!isPrivateChannelPicker){
			findViewById(R.id.currently_written).setOnClickListener(this);
		} else {
			findViewById(R.id.currently_written).setVisibility(View.GONE);
		}

		adapter = new ChannelsAdapter(this, Server.getTrendingTopics());
		((ListView) findViewById(R.id.list_view)).setAdapter(adapter);
	}


	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.currently_written){
			final String text = ((TextView) v).getText().toString().trim();
			if(!text.trim().isEmpty() && !Controller.getInstance().getChannelsContainer().isContainSubscription(text.trim())){
				if(isPrivateChannelPicker){
					final User user = Controller.getInstance().getUserByName(text);
					DatabaseHelper.getInstance().addPrivateChannel(user.getName(), user.getId());
					Server.subscribeToChannel(this, user.getId());
					if(user != null){
						setResult(RESULT_OK, new Intent(user.getId()));
						finish();
					}
				} else{
					DatabaseHelper.getInstance().addPublicChannel(currentNamespace + text);
					Server.subscribeToChannel(this, currentNamespace + text);
					setResult(RESULT_OK, new Intent(text));
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
	}

	@Override
	public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
	}


}
