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
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.models.User;
import com.lesgens.minou.utils.FileTransferDialogFragment;
import com.lesgens.minou.utils.FileTransferDialogFragment.FileTransferListener;
import com.lesgens.minou.utils.Utils;

public class ContactDetailsFragment extends DialogFragment implements OnClickListener, FileTransferListener {
	private String userId;
	private User user;
	
	public static ContactDetailsFragment newInstance(final String userId) {
		return new ContactDetailsFragment(userId);
	}
	
	public ContactDetailsFragment(final String userId){
		this.userId = userId;
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
		View v = inflater.inflate(R.layout.contact, container, false);
		
		user = DatabaseHelper.getInstance().getUser(userId);
		
		((TextView) v.findViewById(R.id.name)).setText(user.getUsername());
		((ImageView) v.findViewById(R.id.avatar)).setImageBitmap(Utils.cropToCircle(user.getAvatar()));
		
		v.findViewById(R.id.send_message_btn).setOnClickListener(this);
		v.findViewById(R.id.share_btn).setOnClickListener(this);
		return v;
	}
	
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.send_message_btn) {
			ChatActivity.show(getActivity(), user.getNamespace());
			getActivity().finish();
		} else if(v.getId() == R.id.share_btn) {
			new FileTransferDialogFragment(this, user.getNamespace()).show(getFragmentManager(), "file_share_dialog");
		}
	}

	@Override
	public void onDialogClosed(final Message message) {
		ChatActivity.show(getActivity(), user.getNamespace());
		getActivity().finish();
	}
}