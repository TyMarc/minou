package com.lesgens.minou;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.lesgens.minou.adapters.ChannelsAdapter;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.controllers.PreferencesController;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.City;
import com.lesgens.minou.models.User;
import com.lesgens.minou.utils.Utils;

public class PublicChannelChooserFragment extends MinouFragment implements OnClickListener, OnItemClickListener, OnItemLongClickListener {
	private static final int REQUEST_ADD_CHANNEL = 101;
	private static final int REQUEST_ADD_LOCATION= 102;
	private ListView listView;
	private ChannelsAdapter adapter;
	
	public static PublicChannelChooserFragment createFragmentWithoutBottomBar(){
		PublicChannelChooserFragment fragment = new PublicChannelChooserFragment();
		Bundle b = new Bundle();
		b.putBoolean("hideBottomBar", true);
		fragment.setArguments(b);
		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.public_channels, container, false);

		listView = (ListView) v.findViewById(R.id.list);
		listView.setOnItemLongClickListener(this);
		listView.setOnItemClickListener(this);
		
		if(getArguments() != null && getArguments().getBoolean("hideBottomBar", false)){
			v.findViewById(R.id.bottom_bar).setVisibility(View.GONE);
			v.findViewById(R.id.list).getLayoutParams().width = LayoutParams.WRAP_CONTENT;
		}
		
		v.findViewById(R.id.add_channel).setOnClickListener(this);
		//v.findViewById(R.id.add_location).setOnClickListener(this);
		v.findViewById(R.id.current_city).setOnClickListener(this);

		return v;
	}

	@Override
	public void onResume(){
		super.onResume();
		refreshList();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == REQUEST_ADD_CHANNEL && resultCode == Activity.RESULT_OK){
			ChatActivity.show(getActivity());
			getActivity().finish();
		}
	}

	public void refreshList(){
		if(Controller.getInstance().getCurrentChannel() == null || Controller.getInstance().getCurrentChannel() instanceof User){
			Controller.getInstance().setCurrentChannel(PreferencesController.getDefaultChannel(getActivity()));
		} else if(!(Controller.getInstance().getCurrentChannel() instanceof City)){
			Controller.getInstance().setCurrentChannel(Controller.getInstance().getCurrentChannel().getParent());
		}
		
		((TextView) getView().findViewById(R.id.current_city)).setText(Utils.capitalizeFirstLetters(Controller.getInstance().getCurrentChannel().getName()));

		adapter = new ChannelsAdapter(getActivity(), Controller.getInstance().getCurrentChannel().getChannels());
		listView.setAdapter(adapter);
	}

	@Override
	public String getTitle(final Context context) {
		return context.getResources().getString(R.string.channels);
	}


	@Override
	public boolean onItemLongClick(final AdapterView<?> arg0, final View arg1,
			final int arg2, final long arg3) {
		new AlertDialog.Builder(getActivity()).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				final Channel channel = adapter.getItem(arg2);
				DatabaseHelper.getInstance().removePublicChannel(channel.getNamespace());
				DatabaseHelper.getInstance().removeAllMessages(channel.getNamespace());
				adapter.remove(channel);
				refreshList();
			}})
			.setNegativeButton(R.string.no, null)
			.setTitle(R.string.delete)
			.setMessage(R.string.delete_channel)
			.show();

		return true;
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.add_channel){
			AddAChannelActivity.show(getActivity(), false, Controller.getInstance().getCurrentChannel().getNamespace(), REQUEST_ADD_CHANNEL);
		} else if(v.getId() == R.id.add_location){
			AddAChannelActivity.show(getActivity(), false, Controller.getInstance().getCurrentChannel().getNamespace(), REQUEST_ADD_LOCATION);
		} else if(v.getId() == R.id.current_city) {
			if(Controller.getInstance().getCurrentChannel().getParent() != null){
				Controller.getInstance().setCurrentChannel(Controller.getInstance().getCurrentChannel().getParent());
				refreshList();
			}
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Channel channel = adapter.getItem(position);
		Log.i("PublicChannelChooserFragment", "Is it a city=" + (channel instanceof City));
		
		if(channel instanceof City) {
			Controller.getInstance().setCurrentChannel(channel);
			refreshList();
		} else{
			Controller.getInstance().setCurrentChannel(channel);
			ChatActivity.show(getActivity());
			getActivity().finish();
		}
	}

}
