package com.lesgens.minou;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

import com.lesgens.minou.adapters.ChannelsAdapter;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.views.CustomYesNoDialog;

public class PublicChannelChooserFragment extends MinouFragment {
	private ListView listView;
	private ChannelsAdapter adapter;

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.list_view, container, false);
		
		listView = (ListView) v.findViewById(android.R.id.list);
		listView.setOnItemClickListener(new OnItemClickListenerChannel());
		listView.setOnItemLongClickListener(new OnItemLongClickListenerChannel());
		return v;
	}
	
	@Override
	public void onResume(){
		super.onResume();
		
		adapter = new ChannelsAdapter(getActivity(), Controller.getInstance().getCurrentChannel().getChannels());
		listView.setAdapter(adapter);
	}

	@Override
	public String getTitle() {
		return "Public";
	}
	
	private class OnItemClickListenerChannel implements OnItemClickListener{
		@Override
		public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
			Channel channel = adapter.getItem(position);
			Controller.getInstance().setCurrentChannel(channel);
			ChatActivity.show(getActivity());
		}
	}

	private class OnItemLongClickListenerChannel implements OnItemLongClickListener{


		@Override
		public boolean onItemLongClick(final AdapterView<?> arg0, final View arg1,
				final int arg2, final long arg3) {
			CustomYesNoDialog dialog = new CustomYesNoDialog(getActivity()){

				@Override
				public void onPositiveClick() {
					super.onPositiveClick();
					final Channel channel = adapter.getItem(arg2);
					DatabaseHelper.getInstance().removePublicChannel(channel.getNamespace());
					DatabaseHelper.getInstance().removeAllMessages(channel.getNamespace());
					adapter.remove(channel);
				}

			};

			dialog.show();
			dialog.setDialogText(R.string.delete_channel);			
			return true;
		}

	}

	
}
