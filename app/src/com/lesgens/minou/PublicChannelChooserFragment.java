package com.lesgens.minou;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ExpandableListView;

import com.lesgens.minou.adapters.RootAdapter;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.controllers.PreferencesController;
import com.lesgens.minou.models.User;
import com.lesgens.minou.views.CustExpListview;
import com.lesgens.minou.views.CustomYesNoDialog;

public class PublicChannelChooserFragment extends MinouFragment {
	private CustExpListview elv;
	private RootAdapter adapter;

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.exp_list_view, container, false);

		elv = (CustExpListview) v.findViewById(R.id.exp_list_view);
		//elv.setOnItemLongClickListener(new OnItemLongClickListenerChannel());

		return v;
	}

	@Override
	public void onResume(){
		super.onResume();
		refreshList();
	}

	public void refreshList(){
		if(Controller.getInstance().getCurrentChannel() instanceof User){
			Controller.getInstance().setCurrentChannel(PreferencesController.getDefaultChannel(getActivity()));
		}

		elv.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {

			@Override
			public boolean onGroupClick(ExpandableListView parent, View v,
					int groupPosition, long id) {
			
				Controller.getInstance().setCurrentChannel(adapter.getGroup(groupPosition));
				return false; /* or false depending on what you need */
			}
		});


		ExpandableListView.OnGroupClickListener grpLst = new ExpandableListView.OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView eListView, View view, int groupPosition,
					long id) {

				return false/* or false depending on what you need */;
			}
		};


		ExpandableListView.OnChildClickListener childLst = new ExpandableListView.OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView eListView, View view, int groupPosition,
					int childPosition, long id) {
				Controller.getInstance().setCurrentChannel(adapter.getChild(groupPosition, childPosition));
				ChatActivity.show(getActivity());
				getActivity().finish();
				return true/* or false depending on what you need */;
			}
		};

		ExpandableListView.OnGroupExpandListener grpExpLst = new ExpandableListView.OnGroupExpandListener() {
			@Override
			public void onGroupExpand(int groupPosition) {
				
			}
		};

		adapter = new RootAdapter(getActivity(), Controller.getInstance().getChannelsContainer().getChannels().get(0), grpLst, childLst, grpExpLst);
		elv.setAdapter(adapter);
	}

	@Override
	public String getTitle(final Context context) {
		return context.getResources().getString(R.string.channels);
	}

	private class OnItemLongClickListenerChannel implements OnItemLongClickListener{


		@Override
		public boolean onItemLongClick(final AdapterView<?> arg0, final View arg1,
				final int arg2, final long arg3) {
			CustomYesNoDialog dialog = new CustomYesNoDialog(getActivity()){

				@Override
				public void onPositiveClick() {
					super.onPositiveClick();
					//final Channel channel = adapter.getItem(arg2);
					//DatabaseHelper.getInstance().removePublicChannel(channel.getNamespace());
					//DatabaseHelper.getInstance().removeAllMessages(channel.getNamespace());
					//adapter.remove(channel);
				}

			};

			dialog.show();
			dialog.setDialogText(R.string.delete_channel);			
			return true;
		}

	}


}
