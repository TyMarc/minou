package com.lesgens.minou;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.Nullable;
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

public class PrivateChannelChooserFragment extends MinouFragment implements OnItemClickListener, OnItemLongClickListener {
	private ListView listView;
	private PrivateChannelsAdapter adapter;

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.private_channels, container, false);

		listView = (ListView) v.findViewById(R.id.list);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		return v;
	}

	@Override
	public void onResume(){
		super.onResume();

		adapter = new PrivateChannelsAdapter(getActivity(), DatabaseHelper.getInstance().getPrivateChannels());
		listView.setAdapter(adapter);
	}

	@Override
	public String getTitle(final Context context) {
		return context.getResources().getString(R.string.conversations);
	}


	@Override
	public boolean onItemLongClick(final AdapterView<?> arg0, final View arg1,
			final int arg2, final long arg3) {

		final User user = adapter.getItem(arg2);

		new AlertDialog.Builder(getActivity()).setPositiveButton(R.string.yes, new OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				DatabaseHelper.getInstance().removePrivateChannel(user.getId());
				DatabaseHelper.getInstance().removeAllMessages(user.getNamespace());
				adapter.remove(user);
			}})
			.setNegativeButton(R.string.no, null)
			.setTitle(R.string.delete)
			.setMessage(R.string.delete_channel)
			.show();	
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
		final User user = adapter.getItem(position);
		Controller.getInstance().setCurrentChannel(user);
		ChatActivity.show(getActivity());
		getActivity().finish();
	}

	public PrivateChannelsAdapter getAdapter() {
		return adapter;
	}


}
