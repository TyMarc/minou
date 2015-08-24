package com.lesgens.minou.fragments;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;

import com.lesgens.minou.R;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.enums.MessageType;
import com.lesgens.minou.enums.SendingStatus;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.utils.Utils;

public class FileTransferDialogFragment extends DialogFragment implements OnClickListener{
	public static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	public static final int PICK_IMAGE_ACTIVITY_REQUEST_CODE = 101;
	public static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 102;
	public static final int PICK_VIDEO_ACTIVITY_REQUEST_CODE = 103;
	private FileTransferListener listener;
	private String channelNamespace;
	public static final String tempFilename = "file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/tempImage.jpeg";

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
			Uri uri = data != null ? data.getData() : null;
			if(uri == null) {
				uri = Uri.parse(tempFilename);
			}
			Message message = prepareAndSendPicture(getActivity(), uri, channelNamespace);
			if(listener != null) {
				listener.onDialogClosed(message);
			}
			dismiss();
		} else if ((requestCode == FileTransferDialogFragment.PICK_VIDEO_ACTIVITY_REQUEST_CODE || requestCode == FileTransferDialogFragment.CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE) && resultCode == Activity.RESULT_OK) {
			Uri uri = data.getData();
			Message message = prepareAndSendVideo(getActivity(), uri, channelNamespace);
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

	public static Message prepareAndSendPicture(final Activity context, final Uri imageUri, final String channelNamespace){
		Bitmap bitmap;
		String imagePath = null;
		try{
			imagePath = Utils.getRealPathFromURI(context, imageUri);
		} catch(NullPointerException npe) {
			npe.printStackTrace();
		}
		try {
			
			byte[] bitmapArray = null;
			
			if(imagePath != null) {
				bitmapArray = Utils.read(new File(imagePath));
			} else {
				bitmapArray = Utils.read(new File(imageUri.getPath()));
			}
			bitmap = BitmapFactory.decodeByteArray(bitmapArray, 0, bitmapArray.length);

			final byte[] byteArray = Utils.prepareImageFT(context, bitmap, imageUri);

			new File(imageUri.getPath()).delete();
			return sendPicture(context, byteArray, channelNamespace);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static Message sendAudio(final Context context, final String filepath, final String channelNamespace){
		String filename = filepath.substring(filepath.lastIndexOf("/") + 1);
		Log.i("FileTransferDialogFragment", "filename=" + filename);

		Message message = new Message(Controller.getInstance().getMyself(), filename, filepath, false, SendingStatus.PENDING, MessageType.AUDIO);

		try {
			Server.sendFile(message, channelNamespace);
			DatabaseHelper.getInstance().addMessage(message, Controller.getInstance().getId(), channelNamespace, true);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return message;
	}

	public static Message sendPicture(final Context context, byte[] byteArray, final String channelNamespace){
		String filename = Controller.getInstance().getId() + "_" + System.currentTimeMillis() + ".jpeg";
		File cacheDir = new File(context.getCacheDir().getAbsoluteFile().getAbsolutePath());
		cacheDir.mkdirs();
		String filepath = context.getCacheDir().getAbsolutePath() + "/" + filename;
		try{
			FileOutputStream fos = new FileOutputStream(filepath);
			fos.write(byteArray);
			fos.close();
			Message message = new Message(Controller.getInstance().getMyself(), filename, filepath, false, SendingStatus.PENDING, MessageType.IMAGE);


			Server.sendFile(message, channelNamespace);
			DatabaseHelper.getInstance().addMessage(message, Controller.getInstance().getId(), channelNamespace, true);

			return message;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
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
		File cacheDir = new File(context.getCacheDir().getAbsoluteFile().getAbsolutePath());
		cacheDir.mkdirs();
		File file = new File(context.getCacheDir().getAbsoluteFile().getAbsolutePath() + "/" + filename);
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
		Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
		i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, Uri.parse(tempFilename));
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
