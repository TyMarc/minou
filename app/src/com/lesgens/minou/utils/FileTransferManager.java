package com.lesgens.minou.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.Fragment;

import com.desmond.squarecamera.CameraActivity;
import com.desmond.squarecamera.ImageUtility;
import com.lesgens.minou.R;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.enums.MessageType;
import com.lesgens.minou.enums.SendingStatus;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.network.Server;

public class FileTransferManager {
	public static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	public static final int PICK_IMAGE_ACTIVITY_REQUEST_CODE = 101;
	public static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 102;
	public static final int PICK_VIDEO_ACTIVITY_REQUEST_CODE = 103;

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

	public static void pickPicture(final Activity context) {
		Intent i = new Intent(
				Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

		context.startActivityForResult(i, PICK_IMAGE_ACTIVITY_REQUEST_CODE);
	}
	
	public static void pickPicture(final Fragment context) {
		Intent i = new Intent(
				Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

		context.startActivityForResult(i, PICK_IMAGE_ACTIVITY_REQUEST_CODE);
	}

	public static void pickVideo(final Activity context) {
		Intent i = new Intent(
				Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);

		context.startActivityForResult(i, PICK_VIDEO_ACTIVITY_REQUEST_CODE);
	}
	
	public static void pickVideo(final Fragment context) {
		Intent i = new Intent(
				Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);

		context.startActivityForResult(i, PICK_VIDEO_ACTIVITY_REQUEST_CODE);
	}

	public static void takePhoto(final Activity context) {
		Intent i = new Intent(context, CameraActivity.class);
		context.startActivityForResult(i, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
	}
	
	public static void takePhoto(final Fragment context) {
		Intent i = new Intent(context.getActivity(), CameraActivity.class);
		context.startActivityForResult(i, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
	}

	public static void takeVideo(final Activity context) {
		Intent i = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
		//MMS video quality for smaller transfers (400ko for 5 seconds video instead of 10mo)
		i.putExtra(android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 0);
		context.startActivityForResult(i, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
	}
	
	public static void takeVideo(final Fragment context) {
		Intent i = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
		//MMS video quality for smaller transfers (400ko for 5 seconds video instead of 10mo)
		i.putExtra(android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 0);
		context.startActivityForResult(i, CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE);
	}
	
	public static void showMenuFT(final Activity activity){
		CharSequence fts[] = new CharSequence[] {activity.getResources().getString(R.string.take_picture), 
				activity.getResources().getString(R.string.pick_picture), activity.getResources().getString(R.string.take_video), 
				activity.getResources().getString(R.string.pick_video)};

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.file_transfer);
		builder.setItems(fts, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch(which){
				case 0:
					takePhoto(activity);
					break;
				case 1:
					pickPicture(activity);
					break;
				case 2:
					takeVideo(activity);
					break;
				case 3:
					pickVideo(activity);
					break;
				}
			}
		});
		builder.show();
	}
	
	public static void showMenuFT(final Fragment fragment){
		CharSequence fts[] = new CharSequence[] {fragment.getResources().getString(R.string.take_picture), 
				fragment.getResources().getString(R.string.pick_picture), fragment.getResources().getString(R.string.take_video), 
				fragment.getResources().getString(R.string.pick_video)};

		AlertDialog.Builder builder = new AlertDialog.Builder(fragment.getActivity());
		builder.setTitle(R.string.file_transfer);
		builder.setItems(fts, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch(which){
				case 0:
					takePhoto(fragment);
					break;
				case 1:
					pickPicture(fragment);
					break;
				case 2:
					takeVideo(fragment);
					break;
				case 3:
					pickVideo(fragment);
					break;
				}
			}
		});
		builder.show();
	}
}
