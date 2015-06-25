package com.lesgens.minou;

import java.io.File;

import android.app.Activity;
import android.content.Context;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.utils.Utils;

public class ProfileFragment extends MinouFragment implements OnClickListener {
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private Uri imageUri;

	@Override
	public View onCreateView(LayoutInflater inflater,
			@Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.profile, container, false);

		((TextView) v.findViewById(R.id.username)).setText(Controller.getInstance().getMyself().getUsername());

		((ImageView) v.findViewById(R.id.avatar)).setImageBitmap(Controller.getInstance().getMyself().getAvatar());
		
		v.findViewById(R.id.change_picture).setOnClickListener(this);

		return v;
	}

	@Override
	public void onResume(){
		super.onResume();

	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
			if (resultCode == Activity.RESULT_OK) {
				getActivity().getContentResolver().notifyChange(imageUri, null);

				new Handler(getActivity().getMainLooper()).post(new Runnable(){

					@Override
					public void run() {
						try {
							Bitmap bitmap = android.provider.MediaStore.Images.Media
									.getBitmap(getActivity().getContentResolver(), imageUri);

							final byte[] byteArray = Utils.prepareImageFT(getActivity(), bitmap, imageUri);

							Controller.getInstance().getMyself().setAvatar(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length));
							((ImageView) getView().findViewById(R.id.avatar)).setImageBitmap(Controller.getInstance().getMyself().getAvatar());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}});
			}
		}
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

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.change_picture) {
			takePhoto();
		}
	}

}
