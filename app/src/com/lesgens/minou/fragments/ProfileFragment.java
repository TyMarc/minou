package com.lesgens.minou.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.desmond.squarecamera.CameraActivity;
import com.lesgens.minou.R;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.listeners.AvatarUploadListener;
import com.lesgens.minou.listeners.MinouUploadAvatarProgressListener;
import com.lesgens.minou.listeners.UsernameListener;
import com.lesgens.minou.network.FileManagerS3;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.utils.AvatarGenerator;
import com.lesgens.minou.utils.Utils;

public class ProfileFragment extends MinouFragment implements OnClickListener, AvatarUploadListener, UsernameListener {
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private static final int PICK_IMAGE_ACTIVITY_REQUEST_CODE = 101;
	private Uri imageUri;
	private ImageView avatar;

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.profile, container, false);

		((TextView) v.findViewById(R.id.username)).setText(Controller.getInstance().getMyself().getUsername());

		avatar = ((ImageView) v.findViewById(R.id.avatar));
		avatar.setImageBitmap(Utils.cropToCircle(Controller.getInstance().getMyself().getAvatar()));

		v.findViewById(R.id.change_picture).setOnClickListener(this);
		v.findViewById(R.id.change_username).setOnClickListener(this);

		return v;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		RelativeLayout.LayoutParams params = (LayoutParams) avatar.getLayoutParams();
		if(getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			params.height = Utils.dpInPixels(getActivity(), 200);
			params.width = Utils.dpInPixels(getActivity(), 200);
		} else if(getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
			params.height = Utils.dpInPixels(getActivity(), 300);
			params.width = Utils.dpInPixels(getActivity(), 300);
		}
		
		avatar.setLayoutParams(params);

	}

	@Override
	public void onResume(){
		super.onResume();

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == PICK_IMAGE_ACTIVITY_REQUEST_CODE || requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE)
				&& resultCode == Activity.RESULT_OK) {
			imageUri = data.getData();
			changeAvatar(requestCode);
		}
	}

	private void showChangeUsername(){
		final EditText editUsername = new EditText(getActivity());

		editUsername.setText(Controller.getInstance().getMyself().getUsername());
		editUsername.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);

		new AlertDialog.Builder(getActivity())
		.setTitle(R.string.change_username)
		.setMessage(R.string.enter_username)
		.setView(editUsername)
		.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String username = editUsername.getText().toString();
				if(!username.isEmpty() && !username.equals(Controller.getInstance().getMyself().getUsername())){
					Server.changeUsername(username, ProfileFragment.this);
					getView().findViewById(R.id.progress_upload_username).setVisibility(View.VISIBLE);
					((TextView) getView().findViewById(R.id.username)).setText("");
				}
			}
		})
		.setNegativeButton(R.string.cancel, null)
		.show(); 
	}

	private void changeAvatar(final int requestCode){
		getActivity().getContentResolver().notifyChange(imageUri, null);

		new Handler(getActivity().getMainLooper()).post(new Runnable(){

			@Override
			public void run() {
				try {
					Bitmap bitmap = android.provider.MediaStore.Images.Media
							.getBitmap(getActivity().getContentResolver(), imageUri);

					if(requestCode == PICK_IMAGE_ACTIVITY_REQUEST_CODE) {
						bitmap = Utils.cropToSquare(bitmap);
					}

					final byte[] byteArray = Utils.prepareImageFT(getActivity(), bitmap, imageUri);
					String filename = Controller.getInstance().getId() + "_" + System.currentTimeMillis() + ".jpeg";

					MinouUploadAvatarProgressListener listener = new MinouUploadAvatarProgressListener(filename, byteArray, ProfileFragment.this);
					FileManagerS3.getInstance().uploadFile(filename, byteArray, listener);
					getView().findViewById(R.id.progress_upload_picture).setVisibility(View.VISIBLE);
					((ImageView) getView().findViewById(R.id.avatar)).setImageDrawable(getActivity().getResources().getDrawable(R.drawable.avatar_bg));
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
	public int getTitleDrawableId() {
		return R.drawable.settings;
	}

	private void takePhoto() {
		Intent startCustomCameraIntent = new Intent(getActivity(), CameraActivity.class);
		startActivityForResult(startCustomCameraIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
	}

	private void pickPicture() {
		Intent i = new Intent(
				Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

		startActivityForResult(i, PICK_IMAGE_ACTIVITY_REQUEST_CODE);
	}

	private void generateNewAvatar(){
		String filename = Controller.getInstance().getId() + "_" + System.currentTimeMillis() + ".jpeg";
		Bitmap bitmap = AvatarGenerator.generate(Controller.getInstance().getDimensionAvatar(), 
				Controller.getInstance().getDimensionAvatar());
		final byte[] byteArray = Utils.prepareImageFT(getActivity(), bitmap, imageUri);

		MinouUploadAvatarProgressListener listener = new MinouUploadAvatarProgressListener(filename, byteArray, this);
		FileManagerS3.getInstance().uploadFile(filename, byteArray, listener);
		getView().findViewById(R.id.progress_upload_picture).setVisibility(View.VISIBLE);
		((ImageView) getView().findViewById(R.id.avatar)).setImageDrawable(getActivity().getResources().getDrawable(R.drawable.avatar_bg));
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.change_picture) {
			showChangeAvatar();
		} else if(v.getId() == R.id.change_username) {
			showChangeUsername();
		}
	}

	@Override
	public void onAvatarUploaded(final String avatarUrl, final byte[] bytesAvatar) {
		getActivity().runOnUiThread(new Runnable(){

			@Override
			public void run() {
				getView().findViewById(R.id.progress_upload_picture).setVisibility(View.GONE);
				Bitmap bitmap = BitmapFactory.decodeByteArray(bytesAvatar, 0, bytesAvatar.length);
				Controller.getInstance().getMyself().setAvatar(bitmap, bytesAvatar, avatarUrl);
				((ImageView) getView().findViewById(R.id.avatar)).setImageBitmap(Utils.cropToCircle(Controller.getInstance().getMyself().getAvatar()));
			}});

	}

	@Override
	public void onAvatarUploadError() {
		getActivity().runOnUiThread(new Runnable(){

			@Override
			public void run() {
				getView().findViewById(R.id.progress_upload_picture).setVisibility(View.GONE);
				Toast.makeText(getActivity(), R.string.error_upload_avatar, Toast.LENGTH_SHORT).show();
				((ImageView) getView().findViewById(R.id.avatar)).setImageBitmap(Utils.cropToCircle(Controller.getInstance().getMyself().getAvatar()));
			}});
	}

	@Override
	public void onUsernameUploaded(final String username) {
		getActivity().runOnUiThread(new Runnable(){

			@Override
			public void run() {
				getView().findViewById(R.id.progress_upload_username).setVisibility(View.GONE);
				Controller.getInstance().getMyself().setUsername(username);
				DatabaseHelper.getInstance().updateUsername(Controller.getInstance().getId(), username);
				((TextView) getView().findViewById(R.id.username)).setText(username);
			}});
	}

	@Override
	public void onUsernameUploadError() {
		getActivity().runOnUiThread(new Runnable(){

			@Override
			public void run() {
				getView().findViewById(R.id.progress_upload_username).setVisibility(View.GONE);
				Toast.makeText(getActivity(), R.string.error_upload_username, Toast.LENGTH_SHORT).show();
				((TextView) getView().findViewById(R.id.username)).setText(Controller.getInstance().getMyself().getUsername());
			}});

	}

}
