package com.lesgens.minou;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.utils.AvatarGenerator;
import com.lesgens.minou.utils.Utils;

public class ProfileFragment extends MinouFragment implements OnClickListener {
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int PICK_IMAGE_ACTIVITY_REQUEST_CODE = 101;
	private Uri imageUri;

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.profile, container, false);

		((TextView) v.findViewById(R.id.username)).setText(Controller.getInstance().getMyself().getUsername());

		((ImageView) v.findViewById(R.id.avatar)).setImageBitmap(Utils.cropToCircle(Controller.getInstance().getMyself().getAvatar()));

		v.findViewById(R.id.change_picture).setOnClickListener(this);
		v.findViewById(R.id.change_username).setOnClickListener(this);

		return v;
	}

	@Override
	public void onResume(){
		super.onResume();

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
			changeAvatar();
		} else if (requestCode == PICK_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
			imageUri = data.getData();
			changeAvatar();
		}
	}
	
	private void showChangeUsername(){
		final EditText editUsername = new EditText(getActivity());

		editUsername.setText(Controller.getInstance().getMyself().getUsername());

		new AlertDialog.Builder(getActivity())
		  .setTitle(R.string.change_username)
		  .setMessage(R.string.enter_username)
		  .setView(editUsername)
		  .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int whichButton) {
		      String username = editUsername.getText().toString();
		      if(!username.isEmpty()){
		    	  Controller.getInstance().getMyself().setUsername(username);
		    	  ((TextView) getView().findViewById(R.id.username)).setText(username);
		      }
		    }
		  })
		  .setNegativeButton(R.string.cancel, null)
		  .show(); 
	}

	private void changeAvatar(){
		getActivity().getContentResolver().notifyChange(imageUri, null);

		new Handler(getActivity().getMainLooper()).post(new Runnable(){

			@Override
			public void run() {
				try {
					Bitmap bitmap = android.provider.MediaStore.Images.Media
							.getBitmap(getActivity().getContentResolver(), imageUri);
					
					bitmap = Utils.cropToSquare(bitmap);

					final byte[] byteArray = Utils.prepareImageFT(getActivity(), bitmap, imageUri);

					Controller.getInstance().getMyself().setAvatar(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length));
					((ImageView) getView().findViewById(R.id.avatar)).setImageBitmap(Utils.cropToCircle(Controller.getInstance().getMyself().getAvatar()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}});
	}

	private void showChangeAvatar(){
		CharSequence fts[] = new CharSequence[] {getResources().getString(R.string.take_picture), getResources().getString(R.string.pick_picture), getResources().getString(R.string.generate)};

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.change_avatar);
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
				case 2:
					generateNewAvatar();
					break;
				}
			}
		});
		builder.show();
	}

	@Override
	public String getTitle(final Context context) {
		return context.getResources().getString(R.string.profile);
	}

	private void takePhoto() {
		Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
		File photo = new File(Environment.getExternalStorageDirectory(),  "Pic.jpg");
		intent.putExtra(MediaStore.EXTRA_OUTPUT,
				Uri.fromFile(photo));
		imageUri = Uri.fromFile(photo);
		startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
	}
	
	private void pickPicture() {
		Intent i = new Intent(
				Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

		startActivityForResult(i, PICK_IMAGE_ACTIVITY_REQUEST_CODE);
	}
	
	private void generateNewAvatar(){
		Controller.getInstance().getMyself().setAvatar(AvatarGenerator.generate(Controller.getInstance().getDimensionAvatar(), 
				Controller.getInstance().getDimensionAvatar()));
		((ImageView) getView().findViewById(R.id.avatar)).setImageBitmap(Utils.cropToCircle(Controller.getInstance().getMyself().getAvatar()));
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.change_picture) {
			showChangeAvatar();
		} else if(v.getId() == R.id.change_username) {
			showChangeUsername();
		}
	}

}
