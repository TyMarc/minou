package com.lesgens.minou.fragments;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.lesgens.minou.ChatActivity;
import com.lesgens.minou.HomeActivity;
import com.lesgens.minou.R;
import com.lesgens.minou.adapters.ContactPickerAdapter;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.models.ContactPicker;
import com.lesgens.minou.models.User;
import com.lesgens.minou.network.Server;


public class ContactPickerFragment extends MinouFragment implements OnItemClickListener, OnClickListener {
	public static final int MODE_SINGLE = 0;
	public static final int MODE_GROUP = 1;
	public static final int MODE_SEEN = 2;
	private int mode = -1;
	private ListView listView;
	private ContactPickerAdapter adapter;
	private SparseBooleanArray checkedUsers;


	public static ContactPickerFragment newInstance(int mode){
		ContactPickerFragment fragment = new ContactPickerFragment();
		Bundle b = new Bundle();
		b.putInt("mode", mode);
		fragment.setArguments(b);

		return fragment;
	}

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.contact_picker, container, false);

		listView = (ListView) v.findViewById(R.id.list);
		listView.setOnItemClickListener(this);

		listView.setEmptyView(v.findViewById(android.R.id.empty));

		mode = getArguments().getInt("mode");

		if(mode == MODE_SINGLE){
			listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			((TextView) v.findViewById(R.id.header)).setText(getActivity().getResources().getString(R.string.create_single));
		} else if(mode == MODE_GROUP){
			listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			((TextView) v.findViewById(R.id.header)).setText(getActivity().getResources().getString(R.string.create_group));
		} else if(mode == MODE_SEEN) {
			listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			((TextView) v.findViewById(R.id.header)).setText(getActivity().getResources().getString(R.string.dialog_add_contact));
		}

		v.findViewById(R.id.close).setOnClickListener(this);
		v.findViewById(R.id.done).setOnClickListener(this);

		return v;
	}

	@Override
	public void onStart(){
		super.onStart();
		slideIn();
	}

	private void slideIn(){
		Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_up);
		getView().findViewById(R.id.container).startAnimation(anim);
	}

	public void slideOut(){
		Animation anim = AnimationUtils.loadAnimation(getActivity(), R.anim.slide_down);
		anim.setAnimationListener(new AnimationListener(){

			@Override
			public void onAnimationEnd(Animation arg0) {
				getActivity().getSupportFragmentManager().beginTransaction().remove(ContactPickerFragment.this).commit();
			}

			@Override
			public void onAnimationRepeat(Animation arg0) {

			}

			@Override
			public void onAnimationStart(Animation arg0) {

			}});
		getView().findViewById(R.id.container).startAnimation(anim);
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

	public boolean isFragmentUIActive() {
		return isAdded() && !isDetached() && !isRemoving();
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
		Log.i("ContactPickerFragment", "itemClicked");
		checkedUsers = listView.getCheckedItemPositions();
		if(mode != MODE_GROUP){
			adapter.unCheckAll();
		}
		adapter.checkContact(position);
		adapter.notifyDataSetChanged();
	}

	private void finish(){
		if(checkedUsers != null){
			if(mode == MODE_SINGLE && checkedUsers.size() > 0){
				final String userId = adapter.getItem(checkedUsers.keyAt(0)).getUserId();
				final User user = DatabaseHelper.getInstance().getUser(userId);
				ChatActivity.show(getActivity(), user.getNamespace());
				getActivity().finish();
			} else if(mode == MODE_GROUP && checkedUsers.size() >= 2) {
			} else if(mode == MODE_SEEN && checkedUsers.size() > 0){
				final String userId = adapter.getItem(checkedUsers.keyAt(0)).getUserId();
				final User user = DatabaseHelper.getInstance().getUser(userId);
				Server.subscribeToConversation(getActivity(), user);
				DatabaseHelper.getInstance().setUserAsContact(user);
				if(getActivity() instanceof HomeActivity){
					((HomeActivity) getActivity()).getContactsFragment().refreshList();
				}
				slideOut();
			} else{
				slideOut();
			}
		} else{
			slideOut();
		}
	}


	public void refreshList() {
		ArrayList<ContactPicker> list = new ArrayList<ContactPicker>();
		if(mode == MODE_GROUP || mode == MODE_SINGLE){
			list = DatabaseHelper.getInstance().getContactsForPicker();
		} else if(mode == MODE_SEEN){
			list = DatabaseHelper.getInstance().getNonContactsForPicker();
		}
		adapter = new ContactPickerAdapter(getActivity(), list);
		listView.setAdapter(adapter);
	}

	@Override
	public void onClick(View arg0) {
		if(arg0.getId() == R.id.close) {
			slideOut();
		} else if(arg0.getId() == R.id.done) {
			finish();
		}
	}
}
