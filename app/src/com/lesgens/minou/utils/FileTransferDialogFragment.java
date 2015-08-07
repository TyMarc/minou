package com.lesgens.minou.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;

import com.desmond.squarecamera.CameraActivity;
import com.desmond.squarecamera.ImageUtility;
import com.lesgens.minou.R;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.enums.MessageType;
import com.lesgens.minou.enums.SendingStatus;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.network.Server;

public class FileTransferDialogFragment extends DialogFragment implements OnClickListener{
	public static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	public static final int PICK_IMAGE_ACTIVITY_REQUEST_CODE = 101;
	public static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 102;
	public static final int PICK_VIDEO_ACTIVITY_REQUEST_CODE = 103;
	private FileTransferListener listener;
	private String channelNamespace;
	
	public interface FileTransferListener{
		public abstract void onDialogClosed(final Message message);
	}
	
	public static FileTransferDialogFragment newInstance(final FileTransferListener listener, final String channelNamespace) {
		return new FileTransferDialogFragment(listener, channelNamespace);
	}
	
	public FileTransferDialogFragment(final FileTransferListener listener, final String channelNamespace){
		this.listener = listener;
		this.channelNamespace = channelNamespace;
	}
	
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == FileTransferDialogFragment.PICK_IMAGE_ACTIVITY_REQUEST_CODE || requestCode == FileTransferDialogFragment.CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) && resultCode == Activity.RESULT_OK) {
			Uri uri = data.getData();
			Message message = FileTransferDialogFragment.prepareAndSendPicture(getActivity(), uri, channelNamespace);
			if(listener != null) {
				listener.onDialogClosed(message);
			}
			dismiss();
		} else if ((requestCode == FileTransferDialogFragment.PICK_VIDEO_ACTIVITY_REQUEST_CODE || requestCode == FileTransferDialogFragment.CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) && resultCode == Activity.RESULT_OK) {
			Uri uri = data.getData();
			Message message = FileTransferDialogFragment.prepareAndSendVideo(getActivity(), uri, channelNamespace);
			if(listener != null) {
				listener.onDialogClosed(message);
			}
			dismiss();
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);

		// request a window without the title
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		return dialog;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.file_share_dialog, container, false);

		v.findViewById(R.id.pick_picture_btn).setOnClickListener(this);
		v.findViewById(R.id.capture_picture_btn).setOnClickListener(this);
		v.findViewById(R.id.pick_video_btn).setOnClickListener(this);
		v.findViewById(R.id.capture_video_btn).setOnClickListener(this);
		
		return v;
	}

	public static Message prepareAndSendPicture(final Context context, final Uri imageUri, final String channelNamespace){
		context.getContentResolver().notifyChange(imageUri, null);

		Bitmap bitmap;
		try {
			bitmap = android.provider.MediaStore.Images.Media
					.getBitmap(context.getContentResolver(), imageUri);

			final byte[] byteArray = Utils.prepareImageFT(context, bitmap, imageUri);

			return sendPicture(context, byteArray, channelNamespace);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static Message sendPicture(final Context context, byte[] byteArray, final String channelNamespace){
		String filename = Controller.getInstance().getId() + "_" + System.currentTimeMillis() + ".jpeg";

		Uri filenameSaved = ImageUtility.savePicture(context, BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length), false);
		Message message = new Message(Controller.getInstance().getMyself(), filename, filenameSaved.getPath(), false, SendingStatus.PENDING, MessageType.IMAGE);


		try {
			Server.sendFile(message, channelNamespace);
			DatabaseHelper.getInstance().addMessage(message, Controller.getInstance().getId(), channelNamespace, true);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return message;
	}

	public static Message prepareAndSendVideo(final Activity activity, final Uri uri, final String channelNamespace){
		activity.getContentResolver().notifyChange(uri, null);
		try{
			String videoPath = Utils.getRealPathFromURI(activity, uri);

			return sendVideo(activity, Utils.read(new File(videoPath)), channelNamespace);
		} catch (IOException io_e) {
			io_e.printStackTrace();
		}

		return null;
	}

	public static Message sendVideo(final Context context, byte[] byteArray, final String channelNamespace){
		String filename = Controller.getInstance().getId() + "_" + System.currentTimeMillis() + ".mp4";

		File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/" + context.getResources().getString(R.string.app_name) + "/" + filename);
		String absolutePath = file.getAbsolutePath();
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(byteArray);
			fos.close();
			Message message = new Message(Controller.getInstance().getMyself(), filename, absolutePath, false, SendingStatus.PENDING, MessageType.VIDEO);


			Server.sendFile(message, channelNamespace);
			DatabaseHelper.getInstance().addMessage(message, Controller.getInstance().getId(), channelNamespace, true);

			return message;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public void pickPicture() {
		Intent i = new Intent(
				Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

		startActivityForResult(i, PICK_IMAGE_ACTIVITY_REQUEST_CODE);
	}

	public void pickVideo() {
		Intent i = new Intent(
				Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);

		startActivityForResult(i, PICK_VIDEO_ACTIVITY_REQUEST_CODE);
	}

	public void takePhoto() {
		Intent i = new Intent(getActivity(), CameraActivity.class);
		startActivityForResult(i, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
	}

	public void takeVideo() {
		Intent i = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
		//MMS video quality for smaller transfers (400ko for 5 seconds video instead of 10mo)
		i.putExtra(android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 0);
		startActivityForResult(i, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.pick_picture_btn) {
			pickPicture();
		} else if(v.getId() == R.id.capture_picture_btn) {
			takePhoto();
		} else if(v.getId() == R.id.pick_video_btn) {
			pickVideo();
		} else if(v.getId() == R.id.capture_video_btn) {
			takeVideo();
		}
	}
}
