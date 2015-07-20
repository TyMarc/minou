package com.lesgens.minou.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.lesgens.minou.ChatActivity;
import com.lesgens.minou.R;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.controllers.PreferencesController;
import com.lesgens.minou.models.Topic;
import com.lesgens.minou.utils.ExpandCollapseAnimation;
import com.lesgens.minou.utils.Utils;

public class TopicDetailsFragment extends MinouFragment implements OnClickListener{
	private int width;
	private int x;
	private int y;
	private String channelNamespace;
	private Topic topic;
	
	public static TopicDetailsFragment newInstance(int width, int x, int y, String channelNamespace){
		TopicDetailsFragment fragment = new TopicDetailsFragment();
		Bundle b = new Bundle();
		b.putInt("width", width);
		b.putInt("x", x);
		b.putInt("y", y);
		b.putString("channelNamespace", channelNamespace);
		fragment.setArguments(b);

		return fragment;
	}

	@Override
	public int getTitleDrawableId() {
		return 0;
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.topics_detail, container, false);
		
		width = getArguments().getInt("width", 150);
		x = getArguments().getInt("x", 0);
		y = getArguments().getInt("y", 0);
		channelNamespace = getArguments().getString("channelNamespace", PreferencesController.getDefaultChannel(getActivity()));
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, LayoutParams.WRAP_CONTENT);
		params.leftMargin = x;
		params.topMargin = y - Utils.getStatusBarHeight(getActivity());
		
		v.findViewById(R.id.topic_detail).setLayoutParams(params);

		topic = ((Topic) Controller.getInstance().getChannelsContainer().getChannelByName(channelNamespace));
		
		((TextView) v.findViewById(R.id.name)).setText(Utils.capitalizeFirstLetters(topic.getName()));
		((TextView) v.findViewById(R.id.description)).setText(Utils.capitalizeFirstLetters(topic.getDescription()));
		((TextView) v.findViewById(R.id.city)).setText(Utils.capitalizeFirstLetters(topic.getParentName()));
		((TextView) v.findViewById(R.id.users_connected)).setText(Utils.capitalizeFirstLetters(getActivity().getResources().getQuantityString(R.plurals.users_connected, topic.getCount(), topic.getCount())));
		
		v.findViewById(R.id.container).setOnClickListener(this);
		v.findViewById(R.id.topic_detail).setOnClickListener(this);

		return v;
	}
	
	@Override
	public void onStart(){
		super.onStart();
		slideIn();
	}
	
	private void slideIn(){
		final Animation animDesc = new ExpandCollapseAnimation(getView().findViewById(R.id.description), 200, 0);
		final Animation animDescSep = new ExpandCollapseAnimation(getView().findViewById(R.id.description_sep), 200, 0);
		final Animation animUsers = new ExpandCollapseAnimation(getView().findViewById(R.id.users_connected), 200, 0);
		final Animation animUsersSep = new ExpandCollapseAnimation(getView().findViewById(R.id.users_connected_sep), 200, 0);
		final Animation animBackground = AnimationUtils.loadAnimation(getActivity(), R.anim.alpha_in);
		getView().findViewById(R.id.description).startAnimation(animDesc);
		getView().findViewById(R.id.description_sep).startAnimation(animDescSep);
		getView().findViewById(R.id.users_connected).startAnimation(animUsers);
		getView().findViewById(R.id.users_connected_sep).startAnimation(animUsersSep);
		getView().findViewById(R.id.container).startAnimation(animBackground);
	}

	public void slideOut(){
		final Animation animDesc = new ExpandCollapseAnimation(getView().findViewById(R.id.description), 200, 1);
		final Animation animDescSep = new ExpandCollapseAnimation(getView().findViewById(R.id.description_sep), 200, 1);
		final Animation animUsers = new ExpandCollapseAnimation(getView().findViewById(R.id.users_connected), 200, 1);
		final Animation animUsersSep = new ExpandCollapseAnimation(getView().findViewById(R.id.users_connected_sep), 200, 1);
		final Animation animBackground = AnimationUtils.loadAnimation(getActivity(), R.anim.alpha_out);
		animBackground.setAnimationListener(new AnimationListener(){

			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				getActivity().getSupportFragmentManager().beginTransaction().remove(TopicDetailsFragment.this).commit();
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub
				
			}});
		getView().findViewById(R.id.description).startAnimation(animDesc);
		getView().findViewById(R.id.description_sep).startAnimation(animDescSep);
		getView().findViewById(R.id.users_connected).startAnimation(animUsers);
		getView().findViewById(R.id.users_connected_sep).startAnimation(animUsersSep);
		getView().findViewById(R.id.container).startAnimation(animBackground);
	}
	
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.container) {
			slideOut();
		} else if(v.getId() == R.id.topic_detail) {
			Controller.getInstance().setCurrentChannel(topic);
			ChatActivity.show(getActivity());
			getActivity().finish();
		}
	}
	
	public boolean isFragmentUIActive() {
		return isAdded() && !isDetached() && !isRemoving();
	}

	public void setWidth(int width) {
		getView().findViewById(R.id.topic_detail).getLayoutParams().width = width;
	}

}
