package com.lesgens.minou.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.lesgens.minou.R;
import com.lesgens.minou.adapters.ContactsAdapter;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.listeners.UserInformationsListener;
import com.lesgens.minou.models.User;
import com.lesgens.minou.network.Server;

public class ContactsFragment extends MinouFragment implements OnItemClickListener, OnItemLongClickListener, android.view.View.OnClickListener, OnRefreshListener, UserInformationsListener {
	private ListView listView;
	private ContactsAdapter adapter;
	private ContactPickerFragment contactPickerFragment;
	private SwipeRefreshLayout swipeRefreshLayout;

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.contacts, container, false);

		listView = (ListView) v.findViewById(R.id.list);
		listView.setOnItemClickListener(this);
		listView.setOnItemLongClickListener(this);
		
		listView.setEmptyView(v.findViewById(android.R.id.empty));
		
		swipeRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh);
		swipeRefreshLayout.setOnRefreshListener(this);
		
		v.findViewById(R.id.add_contact).setOnClickListener(this);
		return v;
	}

	@Override
	public void onResume(){
		super.onResume();

		refreshList();
	}

	@Override
	public int getTitleDrawableId() {
		return R.drawable.group_chat;
	}


	@Override
	public boolean onItemLongClick(final AdapterView<?> arg0, final View arg1,
			final int arg2, final long arg3) {

		final String userId = adapter.getItem(arg2);
		final User user = DatabaseHelper.getInstance().getUser(userId);

		new AlertDialog.Builder(getActivity()).setPositiveButton(R.string.yes, new OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				Server.removeContact(Controller.getInstance().getId(), userId);
				DatabaseHelper.getInstance().removeContact(user);
				adapter.remove(userId);
				adapter.notifyDataSetChanged();
			}})
			.setNegativeButton(R.string.no, null)
			.setTitle(R.string.delete)
			.setMessage(R.string.delete_contact)
			.show();	
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
		final String userId = adapter.getItem(position);
		showDialog(userId);
	}
	
	private void showDialog(final String userId) {
	    // Create the fragment and show it as a dialog.
		ContactDetailsFragment newFragment = ContactDetailsFragment.newInstance(userId);
	    newFragment.show(getFragmentManager(), "dialog");
	}

	public void refreshList() {
		adapter = new ContactsAdapter(getActivity(), DatabaseHelper.getInstance().getContacts());
		listView.setAdapter(adapter);
	}

	@Override
	public void onClick(View arg0) {
		if(arg0.getId() == R.id.add_contact){
			contactPickerFragment = ContactPickerFragment.newInstance(ContactPickerFragment.MODE_SEEN);
			FragmentTransaction ft = getFragmentManager().beginTransaction();
			ft.add(android.R.id.content, contactPickerFragment).commit();
		}
	}
	
	public boolean isPickerOpen(){
		if(contactPickerFragment != null && contactPickerFragment.isFragmentUIActive()){
			return true;
		}
		
		return false;
	}
	
	public void closePicker(){
		if(contactPickerFragment != null && contactPickerFragment.isFragmentUIActive()){
			contactPickerFragment.slideOut();
		}
	}

	@Override
	public void onUserInformationsReceived() {
		getActivity().runOnUiThread(new Runnable(){

			@Override
			public void run() {
				adapter.notifyDataSetChanged();
				swipeRefreshLayout.setRefreshing(false);
			}});
	}

	@Override
	public void onRefresh() {
		Server.getUsers(adapter.getItems(), this);
		Server.getMyself(getActivity(), this);
	}

}
