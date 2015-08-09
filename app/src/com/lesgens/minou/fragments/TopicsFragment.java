package com.lesgens.minou.fragments;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.GridView;

import com.lesgens.minou.AddTopicActivity;
import com.lesgens.minou.ChatActivity;
import com.lesgens.minou.R;
import com.lesgens.minou.adapters.TopicsAdapter;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.listeners.TopicCountListener;
import com.lesgens.minou.models.Topic;
import com.lesgens.minou.network.Server;

public class TopicsFragment extends MinouFragment implements OnClickListener, OnItemClickListener, OnItemLongClickListener, TextWatcher, OnRefreshListener, TopicCountListener {
	private GridView gridView;
	private TopicsAdapter adapter;
	private EditText editText;
	private boolean firstLaunch;
	private Handler handler;
	private RefreshListRunnable refreshListRunnable;
	private SwipeRefreshLayout swipeRefreshLayout;

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		if(gridView != null) {
			if(getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
				gridView.setNumColumns(3);
			} else if(getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
				gridView.setNumColumns(2);
			}
		}
	}



	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.topics, container, false);

		getActivity().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		Server.getTopicsCount(this);
		firstLaunch = true;

		handler = new Handler(getActivity().getMainLooper());
		refreshListRunnable = new RefreshListRunnable();

		editText = ((EditText) v.findViewById(R.id.editText));
		editText.addTextChangedListener(this);

		gridView = (GridView) v.findViewById(R.id.grid_view);
		gridView.setOnItemLongClickListener(this);
		gridView.setOnItemClickListener(this);

		if(getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			gridView.setNumColumns(3);
		} else {
			gridView.setNumColumns(2);
		}

		if(getArguments() != null && getArguments().getBoolean("hideBottomBar", false)){
			v.findViewById(R.id.bottom_bar).setVisibility(View.GONE);
			v.findViewById(R.id.grid_view).getLayoutParams().width = LayoutParams.WRAP_CONTENT;
		}

		v.findViewById(R.id.add_channel).setOnClickListener(this);
		v.findViewById(R.id.add_location).setOnClickListener(this);
		swipeRefreshLayout = ((SwipeRefreshLayout) v.findViewById(R.id.swipe_refresh));
		swipeRefreshLayout.setOnRefreshListener(this);

		return v;
	}

	@Override
	public void onResume(){
		super.onResume();
		editText.setText("");
		refreshList();
	}

	public void notifyDataSet(){
		if(adapter != null) {
			adapter.notifyDataSetChanged();
		}
	}

	public void refreshList() {
		if(adapter != null) {
			ArrayList<Topic> topics = adapter.getItems();
			refreshList(topics);
		} else {
			refreshList(Controller.getInstance().getChannelsContainer().getTopics());
		}
	}

	public void refreshList(final ArrayList<Topic> topics){
		if(!firstLaunch && !topics.equals(adapter.getItems())){
			final Animation animFadeOut = AnimationUtils.loadAnimation(getActivity(), R.anim.alpha_out);
			animFadeOut.setDuration(200);
			final Animation animFadeIn = AnimationUtils.loadAnimation(getActivity(), R.anim.alpha_in);
			animFadeIn.setDuration(200);

			animFadeOut.setAnimationListener(new AnimationListener(){

				@Override
				public void onAnimationStart(Animation animation) {

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					adapter = new TopicsAdapter(getActivity(), topics);
					gridView.setAdapter(adapter);
					gridView.startAnimation(animFadeIn);
					getView().findViewById(R.id.progress_search).setVisibility(View.GONE);
				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}});

			gridView.startAnimation(animFadeOut);
			getView().findViewById(R.id.progress_search).setVisibility(View.VISIBLE);

		}

		if(firstLaunch) {
			adapter = new TopicsAdapter(getActivity(), topics);
			gridView.setAdapter(adapter);
			firstLaunch = false;
		}
	}

	@Override
	public int getTitleDrawableId() {
		return R.drawable.location;
	}


	@Override
	public boolean onItemLongClick(final AdapterView<?> arg0, final View arg1,
			final int arg2, final long arg3) {
		new AlertDialog.Builder(getActivity()).setPositiveButton(R.string.yes, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				final Topic channel = adapter.getItem(arg2);
				DatabaseHelper.getInstance().removeTopic(channel.getNamespace());
				DatabaseHelper.getInstance().removeAllMessages(channel.getNamespace());
				channel.getParent().remove(channel);
				adapter.remove(channel);
				refreshList(Controller.getInstance().getChannelsContainer().getTopics());
			}})
			.setNegativeButton(R.string.no, null)
			.setTitle(R.string.delete)
			.setMessage(R.string.delete_topic)
			.show();

		return true;
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.add_channel){
			AddTopicActivity.show(getActivity());
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		final Topic topic = adapter.getItem(position);
		ChatActivity.show(getActivity(), topic.getNamespace());
		getActivity().finish();
	}



	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {

	}



	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}



	@Override
	public void afterTextChanged(Editable s) {
		handler.removeCallbacks(refreshListRunnable);
		handler.postDelayed(refreshListRunnable, 200);
	}

	private class RefreshListRunnable implements Runnable{
		@Override
		public void run() {
			if(editText.getText().toString().length() > 0) {
				refreshList(Controller.getInstance().getChannelsContainer().getTopicsThatContains(editText.getText().toString().toLowerCase()));
			} else{
				refreshList(Controller.getInstance().getChannelsContainer().getTopics());
			}
		}
	}

	@Override
	public void onRefresh() {
		Server.getTopicsCount(this);
	}



	@Override
	public void onCountsReceived() {
		getActivity().runOnUiThread(new Runnable(){

			@Override
			public void run() {
				if(adapter != null){
					adapter.notifyDataSetChanged();
				}
				if(swipeRefreshLayout != null) {
					swipeRefreshLayout.setRefreshing(false);
				}

			}});
	}

}
