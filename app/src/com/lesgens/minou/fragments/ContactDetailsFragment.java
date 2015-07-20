package com.lesgens.minou.fragments;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.desmond.squarecamera.CameraActivity;
import com.lesgens.minou.ChatActivity;
import com.lesgens.minou.R;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.enums.MessageType;
import com.lesgens.minou.enums.SendingStatus;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.models.User;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.utils.Utils;

public class ContactDetailsFragment extends DialogFragment implements OnClickListener {
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int PICK_IMAGE_ACTIVITY_REQUEST_CODE = 101;
	private String userId;
	private User user;
	private Uri imageUri;
	
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
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			addPicture();
			ChatActivity.show(getActivity());
			getActivity().finish();
		} else if (requestCode == PICK_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK && null != data) {
			imageUri = data.getData();
			addPicture();
			ChatActivity.show(getActivity());
			getActivity().finish();
		}
	}

	private void addPicture(){
		getActivity().getContentResolver().notifyChange(imageUri, null);

		new Handler(getActivity().getMainLooper()).post(new Runnable(){

			@Override
			public void run() {
				try {
					Bitmap bitmap = android.provider.MediaStore.Images.Media
							.getBitmap(getActivity().getContentResolver(), imageUri);

					final byte[] byteArray = Utils.prepareImageFT(getActivity(), bitmap, imageUri);
					
					String filename = Controller.getInstance().getId() + "_" + System.currentTimeMillis() + ".jpeg";
					
					File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), filename);
					String absolutePath = file.getAbsolutePath();
					file.mkdirs();
					try {
						FileOutputStream fos = new FileOutputStream(file);
						fos.write(byteArray);
						fos.close();
						Message message = new Message(Controller.getInstance().getMyself(), filename, byteArray, absolutePath, false, SendingStatus.PENDING, MessageType.IMAGE);

						final String channelNamespace = Controller.getInstance().getCurrentChannel().getNamespace();
						Server.sendPicture(message, channelNamespace);
						
						DatabaseHelper.getInstance().addMessage(message, Controller.getInstance().getId(), channelNamespace);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}		

				} catch (Exception e) {
					e.printStackTrace();
				}
			}});
	}
	
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.send_message_btn) {
			Controller.getInstance().setCurrentChannel(user);
			ChatActivity.show(getActivity());
			getActivity().finish();
		} else if(v.getId() == R.id.share_btn) {
			Controller.getInstance().setCurrentChannel(user);
			showMenuFT();
		}
	}
	
	private void showMenuFT(){
		CharSequence fts[] = new CharSequence[] {getResources().getString(R.string.take_picture), getResources().getString(R.string.pick_picture)};

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.file_transfer);
		builder.setItems(fts, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch(which){
				case 0:
					takePhoto();
					break;
				case 1:
					pickPicture();
					break;
				}
			}
		});
		builder.show();
	}
	
	public void pickPicture() {
		Intent i = new Intent(
				Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

		startActivityForResult(i, PICK_IMAGE_ACTIVITY_REQUEST_CODE);
	}

	public void takePhoto() {
		Intent intent = new Intent(getActivity(), CameraActivity.class);
		startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
	}

}