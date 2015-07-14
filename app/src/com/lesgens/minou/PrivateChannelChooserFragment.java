package com.lesgens.minou;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.lesgens.minou.adapters.PrivateChannelsAdapter;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.models.User;

public class PrivateChannelChooserFragment extends MinouFragment implements OnItemClickListener, OnItemLongClickListener, android.view.View.OnClickListener {
	private ListView listView;
	private PrivateChannelsAdapter adapter;
	private ContactPickerFragment contactPickerFragment;

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.private_channels, container, false);

		listView = (ListView) v.findViewById(R.id.list);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);

		v.findViewById(R.id.create_single).setOnClickListener(this);
		v.findViewById(R.id.create_group).setOnClickListener(this);

		listView.setEmptyView(v.findViewById(android.R.id.empty));
		return v;
	}

	@Override
	public void onResume(){
		super.onResume();

		refreshList();
	}

	@Override
	public int getTitleDrawableId() {
		return R.drawable.single_chat;
	}


	@Override
	public boolean onItemLongClick(final AdapterView<?> arg0, final View arg1,
			final int arg2, final long arg3) {

		final String userId = adapter.getItem(arg2);
		final User user = DatabaseHelper.getInstance().getUser(userId);

		new AlertDialog.Builder(getActivity()).setPositiveButton(R.string.yes, new OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				DatabaseHelper.getInstance().removeAllMessages(user.getNamespace());
				adapter.remove(userId);
			}})
			.setNegativeButton(R.string.no, null)
			.setTitle(R.string.delete)
			.setMessage(R.string.delete_conversation)
			.show();	
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
		final String userId = adapter.getItem(position);
		final User user = DatabaseHelper.getInstance().getUser(userId);
		Controller.getInstance().setCurrentChannel(user);
		ChatActivity.show(getActivity());
		getActivity().finish();
	}

	public void refreshList() {
		adapter = new PrivateChannelsAdapter(getActivity(), DatabaseHelper.getInstance().getPrivateChannels());
		listView.setAdapter(adapter);
	}

	@Override
	public void onClick(View arg0) {
		if(arg0.getId() == R.id.create_single){
			contactPickerFragment = ContactPickerFragment.newInstance(ContactPickerFragment.MODE_SINGLE);
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.add(android.R.id.content, contactPickerFragment).commit();
		} else if(arg0.getId() == R.id.create_group) {
			contactPickerFragment = ContactPickerFragment.newInstance(ContactPickerFragment.MODE_GROUP);
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.add(android.R.id.content, contactPickerFragment).commit();
		}
	}
	
	public boolean isPickerOpen(){
		if(contactPickerFragment.isFragmentUIActive()){
			return true;
		}
		
		return false;
	}
	
	public void closePicker(){
		if(contactPickerFragment.isFragmentUIActive()){
			contactPickerFragment.slideOut();
		}
	}


}
