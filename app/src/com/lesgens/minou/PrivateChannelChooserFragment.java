package com.lesgens.minou;

import android.content.Context;
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
import com.lesgens.minou.views.CustomYesNoDialog;

public class PrivateChannelChooserFragment extends MinouFragment {
	private ListView listView;
	private PrivateChannelsAdapter adapter;

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.list_view, container, false);
		
		listView = (ListView) v.findViewById(android.R.id.list);
		listView.setOnItemClickListener(new OnItemClickListenerPrivate());
		listView.setOnItemLongClickListener(new OnItemLongClickListenerPrivateChannel());
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
	
	private class OnItemLongClickListenerPrivateChannel implements OnItemLongClickListener{

		@Override
		public boolean onItemLongClick(final AdapterView<?> arg0, final View arg1,
				final int arg2, final long arg3) {

			final User user = adapter.getItem(arg2);
			CustomYesNoDialog dialog = new CustomYesNoDialog(getActivity()){

				@Override
				public void onPositiveClick() {
					super.onPositiveClick();
					DatabaseHelper.getInstance().removePrivateChannel(user.getId());
					DatabaseHelper.getInstance().removeAllMessages(user.getId());
					adapter.remove(user);
				}

			};

			dialog.show();
			dialog.setDialogText(R.string.delete_channel);			
			return true;
		}

	}
	
	private class OnItemClickListenerPrivate implements OnItemClickListener{
		@Override
		public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
			final User user = adapter.getItem(position);
			Controller.getInstance().setCurrentChannel(user);
			ChatActivity.show(getActivity());
			getActivity().finish();
		}
	}

	
}
