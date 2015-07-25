package com.lesgens.minou.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;

import com.lesgens.minou.ChatActivity;
import com.lesgens.minou.R;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.controllers.PreferencesController;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.models.Topic;
import com.lesgens.minou.utils.ExpandCollapseAnimation;
import com.lesgens.minou.utils.Utils;

public class TopicDetailsFragment extends MinouFragment implements OnClickListener{
	private int width;
	private int originalX;
	private int originalY;
	private String channelNamespace;
	private Topic topic;
	private int statusBarOffset;

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
		originalX = getArguments().getInt("x", 0);
		originalY = getArguments().getInt("y", 0);
		channelNamespace = getArguments().getString("channelNamespace", PreferencesController.getDefaultChannel(getActivity()));

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(width, LayoutParams.WRAP_CONTENT);
		params.leftMargin = originalX;
		params.topMargin = originalY - Utils.getStatusBarHeight(getActivity());

		v.findViewById(R.id.topic_detail).setLayoutParams(params);

		topic = ((Topic) Controller.getInstance().getChannelsContainer().getChannelByName(channelNamespace));

		((TextView) v.findViewById(R.id.name)).setText(Utils.capitalizeFirstLetters(topic.getName()));
		((TextView) v.findViewById(R.id.description)).setText(Utils.capitalizeFirstLetters(topic.getDescription()));
		((TextView) v.findViewById(R.id.city)).setText(Utils.capitalizeFirstLetters(topic.getParentName()));
		((TextView) v.findViewById(R.id.users_connected)).setText(Utils.capitalizeFirstLetters(getActivity().getResources().getQuantityString(R.plurals.users_connected, topic.getCount(), topic.getCount())));

		Bitmap image = topic.getImage();
		if(image == null) {
			image = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.default_topic);
		}
		((ImageView) v.findViewById(R.id.image)).setImageBitmap(image);

		int unreadCount = DatabaseHelper.getInstance().getUnreadCountForTopic(topic.getNamespace());

		if(unreadCount == 0) {
			((TextView) v.findViewById(R.id.unread_count)).setVisibility(View.GONE);
		} else {
			((TextView) v.findViewById(R.id.unread_count)).setVisibility(View.VISIBLE);
			((TextView) v.findViewById(R.id.unread_count)).setText(unreadCount + "");
		}

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
		animBackground.setAnimationListener(new AnimationListener(){

			@Override
			public void onAnimationStart(Animation animation) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				moveViewToScreenCenter(getView().findViewById(R.id.topic_detail));
				getView().findViewById(R.id.description).startAnimation(animDesc);
				getView().findViewById(R.id.description_sep).startAnimation(animDescSep);
				getView().findViewById(R.id.users_connected).startAnimation(animUsers);
				getView().findViewById(R.id.users_connected_sep).startAnimation(animUsersSep);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub
				
			}});
		animBackground.setDuration(100);
		getView().findViewById(R.id.container).startAnimation(animBackground);
	}

	private void moveViewToScreenCenter(final View view)
	{
		View root = getView().findViewById(R.id.container);
		DisplayMetrics dm = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
		statusBarOffset = dm.heightPixels - root.getMeasuredHeight();

		final int originalPos[] = new int[2];
		view.getLocationOnScreen(originalPos);

		final int xDest = dm.widthPixels/2 - (view.getMeasuredWidth()/2);
		final int yDest = dm.heightPixels/2 - (view.getMeasuredHeight()/2) - statusBarOffset;

		TranslateAnimation anim = new TranslateAnimation(0, xDest - originalPos[0] , 0, yDest - originalPos[1]);
		anim.setDuration(200);
		anim.setFillEnabled(true);
		anim.setFillAfter(false);
		anim.setFillBefore(false);
		anim.setAnimationListener(new AnimationListener(){

			@Override
			public void onAnimationStart(Animation animation) {

			}

			@Override
			public void onAnimationEnd(Animation animation) {
				LayoutParams params = (LayoutParams) view.getLayoutParams();
				params.leftMargin = xDest;
				params.topMargin = yDest - statusBarOffset;
				view.setLayoutParams(params);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
				// TODO Auto-generated method stub
				
			}});
		view.startAnimation(anim);
	}

	public void slideOut(){
		final Animation animDesc = new ExpandCollapseAnimation(getView().findViewById(R.id.description), 200, 1);
		final Animation animDescSep = new ExpandCollapseAnimation(getView().findViewById(R.id.description_sep), 200, 1);
		final Animation animUsers = new ExpandCollapseAnimation(getView().findViewById(R.id.users_connected), 200, 1);
		final Animation animUsersSep = new ExpandCollapseAnimation(getView().findViewById(R.id.users_connected_sep), 200, 1);
		final Animation animBackground = AnimationUtils.loadAnimation(getActivity(), R.anim.alpha_out);
		final View view = getView().findViewById(R.id.topic_detail);
		final TranslateAnimation anim = new TranslateAnimation(0, originalX - view.getLeft(), 0, originalY - view.getTop() - statusBarOffset);
		anim.setDuration(200);
		anim.setFillAfter(true);
		animBackground.setAnimationListener(new AnimationListener(){

			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				getActivity().getSupportFragmentManager().beginTransaction().remove(TopicDetailsFragment.this).commit();
			}

			@Override
			public void onAnimationRepeat(Animation animation) {

			}});
		getView().findViewById(R.id.description).startAnimation(animDesc);
		getView().findViewById(R.id.description_sep).startAnimation(animDescSep);
		getView().findViewById(R.id.users_connected).startAnimation(animUsers);
		getView().findViewById(R.id.users_connected_sep).startAnimation(animUsersSep);
		view.startAnimation(anim);
		getView().findViewById(R.id.container).startAnimation(animBackground);
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.container) {
			slideOut();
		} else if(v.getId() == R.id.topic_detail) {
			ChatActivity.show(getActivity(), topic.getNamespace());
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
