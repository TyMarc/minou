package com.lesgens.minou.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.lesgens.minou.ChatActivity;
import com.lesgens.minou.R;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.enums.SendingStatus;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.utils.Utils;

public class MessageDialogFragment extends DialogFragment implements OnClickListener {
	private String messageId;
	private Message message;
	private ChatActivity chatActivity;
	
	public static MessageDialogFragment newInstance(final ChatActivity chatActivity, final String userId) {
		return new MessageDialogFragment(chatActivity, userId);
	}
	
	public MessageDialogFragment(final ChatActivity chatActivity, final String messageId){
		this.messageId = messageId;
		this.chatActivity = chatActivity;
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
	  Dialog dialog = super.onCreateDialog(savedInstanceState);

	  // request a window without the title
	  dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
	  return dialog;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.message_dialog, container, false);
		
		message = DatabaseHelper.getInstance().getMessageById(messageId);
		
		((TextView) v.findViewById(R.id.name)).setText(message.getUser().getUsername());
		((ImageView) v.findViewById(R.id.avatar)).setImageBitmap(Utils.cropToCircle(message.getUser().getAvatar()));
		
		if(message.getUserId().equals(Controller.getInstance().getId())){
			v.findViewById(R.id.add_contact_btn).setVisibility(View.GONE);
			v.findViewById(R.id.add_contact_pm_btn).setVisibility(View.GONE);
			v.findViewById(R.id.pm_btn).setVisibility(View.GONE);
			if(message.getStatus() != SendingStatus.FAILED){
				v.findViewById(R.id.retry_message_btn).setVisibility(View.GONE);
			} else {
				v.findViewById(R.id.retry_message_btn).setOnClickListener(this);
			}
			v.findViewById(R.id.delete_message_btn).setOnClickListener(this);
		} else {
			if(message.getUser().isContact()){
				v.findViewById(R.id.add_contact_btn).setVisibility(View.GONE);
				v.findViewById(R.id.add_contact_pm_btn).setVisibility(View.GONE);
				v.findViewById(R.id.pm_btn).setOnClickListener(this);
			} else{
				v.findViewById(R.id.pm_btn).setVisibility(View.GONE);
				v.findViewById(R.id.add_contact_btn).setOnClickListener(this);
				v.findViewById(R.id.add_contact_pm_btn).setOnClickListener(this);
			}
			
			if(message.getStatus() != SendingStatus.FAILED){
				v.findViewById(R.id.retry_message_btn).setVisibility(View.GONE);
			} else {
				v.findViewById(R.id.retry_message_btn).setOnClickListener(this);
			}
			v.findViewById(R.id.delete_message_btn).setOnClickListener(this);
		}
		
		return v;
	}
	
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.add_contact_btn) {
			chatActivity.addContact(message.getUser());
			dismiss();
		} else if(v.getId() == R.id.add_contact_pm_btn) {
			chatActivity.addContact(message.getUser());
			chatActivity.pmUser(message.getUser());
			dismiss();
		} else if(v.getId() == R.id.pm_btn) {
			chatActivity.pmUser(message.getUser());
			dismiss();
		} else if(v.getId() == R.id.retry_message_btn) {
			chatActivity.retryMessage(message);
			dismiss();
		} else if(v.getId() == R.id.delete_message_btn) {
			chatActivity.deleteMessage(message);
			dismiss();
		}
	}
}