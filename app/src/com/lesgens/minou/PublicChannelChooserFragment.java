package com.lesgens.minou;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.GridView;

import com.lesgens.minou.adapters.ChannelsAdapter;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.controllers.PreferencesController;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.models.Channel;
import com.lesgens.minou.models.User;
import com.lesgens.minou.views.CustomYesNoDialog;

public class PublicChannelChooserFragment extends MinouFragment {
	private GridView gridView;
	private ChannelsAdapter adapter;
	private Animation animFadeIn;
	private Animation animFadeOut;
	private boolean cameBack;

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.grid_view, container, false);

		gridView = (GridView) v.findViewById(R.id.grid_view);
		gridView.setOnItemClickListener(new OnItemClickListenerChannel());
		gridView.setOnItemLongClickListener(new OnItemLongClickListenerChannel());

		animFadeIn = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_in);
		animFadeOut = AnimationUtils.loadAnimation(getActivity(), android.R.anim.fade_out);
		animFadeOut.setDuration(150);
		animFadeIn.setDuration(150);
		return v;
	}

	@Override
	public void onResume(){
		super.onResume();
		cameBack = true;
		refreshList();
	}

	public void refreshList(){
		if(Controller.getInstance().getCurrentChannel() instanceof User){
			Controller.getInstance().setCurrentChannel(PreferencesController.getDefaultChannel(getActivity()));
		}

		ArrayList<Channel> channels = new ArrayList<Channel>();
		Channel parent = Controller.getInstance().getCurrentChannel().getParent();
		if(parent != null){
			channels.add(new Channel("up", null));
		}

		if(Controller.getInstance().getCurrentChannel().getNumberOfChildren() > 0){
			channels.add(new Channel("down", null));
		}

		channels.add(Controller.getInstance().getCurrentChannel());
		channels.addAll(Controller.getInstance().getCurrentChannel().getChannels());

		adapter = new ChannelsAdapter(getActivity(), channels);

		if(!cameBack){
			animFadeOut.setAnimationListener(new AnimationListener(){

				@Override
				public void onAnimationEnd(Animation arg0) {
					gridView.setAdapter(adapter);
					gridView.startAnimation(animFadeIn);
				}

				@Override
				public void onAnimationRepeat(Animation arg0) {}

				@Override
				public void onAnimationStart(Animation arg0) {}

			});

			gridView.startAnimation(animFadeOut);
		} else{
			gridView.setAdapter(adapter);
			cameBack = false;
		}
	}

	@Override
	public String getTitle() {
		return "Channels";
	}

	private class OnItemClickListenerChannel implements OnItemClickListener{
		@Override
		public void onItemClick(AdapterView<?> arg0, View v, int position, long arg3) {
			Channel channel = adapter.getItem(position);
			if(channel.getName().equals("up")){
				Controller.getInstance().setCurrentChannel(Controller.getInstance().getCurrentChannel().getParent());
				refreshList();
			} else if(channel.getName().equals("down")){
				Controller.getInstance().setCurrentChannel(Controller.getInstance().getCurrentChannel().getChannels().get(0));
				refreshList();
			} else{
				Controller.getInstance().setCurrentChannel(channel);
				ChatActivity.show(getActivity());
			}
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
