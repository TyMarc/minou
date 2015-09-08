package com.lesgens.minou.fragments;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import com.lesgens.minou.R;
import com.lesgens.minou.controllers.Controller;
import com.lesgens.minou.db.DatabaseHelper;
import com.lesgens.minou.enums.MessageType;
import com.lesgens.minou.enums.SendingStatus;
import com.lesgens.minou.models.Message;
import com.lesgens.minou.network.Server;
import com.lesgens.minou.utils.Utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;

public class FileTransferDialogFragment extends DialogFragment implements OnClickListener{
	public static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	public static final int PICK_IMAGE_ACTIVITY_REQUEST_CODE = 101;
	public static final int CAPTURE_VIDEO_ACTIVITY_REQUEST_CODE = 102;
	public static final int PICK_VIDEO_ACTIVITY_REQUEST_CODE = 103;
	private FileTransferListener listener;
	private String channelNamespace;
	public static final String tempFilename = "file://" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + "/tempImage.jpeg";
	public static final String tempVideoFilename = "file://" + Environment.getExternalStorageDirectory().getAbsolutePath() + "/tempVideo.mp4";

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
			imagePath = getPath(context, imageUri, true);
		} catch(Exception npe) {
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
			String videoPath = getPath(activity, uri, false);

			if(videoPath != null){
				return sendVideo(activity, Utils.read(new File(videoPath)), channelNamespace);
			}
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
	
	public static Uri getImageUrlWithAuthority(Context context, Uri uri) {
	    InputStream is = null;
	    if (uri.getAuthority() != null) {
	        try {
	            is = context.getContentResolver().openInputStream(uri);
	            Bitmap bmp = BitmapFactory.decodeStream(is);
	            return writeToTempImageAndGetPathUri(context, bmp);
	        } catch (FileNotFoundException e) {
	            e.printStackTrace();
	        }finally {
	            try {
	                is.close();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }
	    return null;
	}
	
	public static String getVideoUrlWithAuthority(Context context, Uri uri) {
		String filename = context.getCacheDir().getAbsoluteFile().getAbsolutePath() + "/tempVideo.mp4";
	    InputStream is = null;
	    if (uri.getAuthority() != null) {
	        try {
	            is = context.getContentResolver().openInputStream(uri);
	            byte[] byteArray = Utils.read(is);
	            FileOutputStream fos = new FileOutputStream(filename);
				fos.write(byteArray);
				fos.close();
	            return filename;
	        } catch (FileNotFoundException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
				e.printStackTrace();
			}finally {
	            try {
	                is.close();
	            } catch (IOException e) {
	                e.printStackTrace();
	            }
	        }
	    }
	    return null;
	}

	public static Uri writeToTempImageAndGetPathUri(Context inContext, Bitmap inImage) {
	    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
	    inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
	    String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
	    return Uri.parse(path);
	}


	/**
	 * Method for return file path of Gallery image 
	 * 
	 * @param context
	 * @param uri
	 * @return path of the selected image file from gallery
	 */
	public static String getPath(final Context context, Uri uri, boolean isPicture) 
	{

		//check here to KITKAT or new version
		final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
		
		if(isKitKat && uri.getAuthority() != null) {
			if(isPicture) {
				uri = getImageUrlWithAuthority(context, uri);
			} else {
				return getVideoUrlWithAuthority(context, uri);
			}
		}

		// DocumentProvider
		if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {

			// ExternalStorageProvider
			if (isExternalStorageDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				if ("primary".equalsIgnoreCase(type)) {
					return Environment.getExternalStorageDirectory() + "/" + split[1];
				}
			}
			// DownloadsProvider
			else if (isDownloadsDocument(uri)) {

				final String id = DocumentsContract.getDocumentId(uri);
				final Uri contentUri = ContentUris.withAppendedId(
						Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

				return getDataColumn(context, contentUri, null, null);
			}
			// MediaProvider
			else if (isMediaDocument(uri)) {
				final String docId = DocumentsContract.getDocumentId(uri);
				final String[] split = docId.split(":");
				final String type = split[0];

				Uri contentUri = null;
				if ("image".equals(type)) {
					contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				} else if ("video".equals(type)) {
					contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				} else if ("audio".equals(type)) {
					contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				}

				final String selection = "_id=?";
				final String[] selectionArgs = new String[] {
						split[1]
				};

				return getDataColumn(context, contentUri, selection, selectionArgs);
			}
		}
		// MediaStore (and general)
		else if ("content".equalsIgnoreCase(uri.getScheme())) {

			// Return the remote address
			if (isGooglePhotosUri(uri))
				return uri.getLastPathSegment();

			return getDataColumn(context, uri, null, null);
		}
		// File
		else if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}

	/**
	 * Get the value of the data column for this Uri. This is useful for
	 * MediaStore Uris, and other file-based ContentProviders.
	 *
	 * @param context The context.
	 * @param uri The Uri to query.
	 * @param selection (Optional) Filter used in the query.
	 * @param selectionArgs (Optional) Selection arguments used in the query.
	 * @return The value of the _data column, which is typically a file path.
	 */
	public static String getDataColumn(Context context, Uri uri, String selection,
			String[] selectionArgs) {

		Cursor cursor = null;
		final String column = "_data";
		final String[] projection = {
				column
		};

		try {
			cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
					null);
			if (cursor != null && cursor.moveToFirst()) {
				final int index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(index);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is ExternalStorageProvider.
	 */
	public static boolean isExternalStorageDocument(Uri uri) {
		return "com.android.externalstorage.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is DownloadsProvider.
	 */
	public static boolean isDownloadsDocument(Uri uri) {
		return "com.android.providers.downloads.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is MediaProvider.
	 */
	public static boolean isMediaDocument(Uri uri) {
		return "com.android.providers.media.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is Google Photos.
	 */
	public static boolean isGooglePhotosUri(Uri uri) {
		return "com.google.android.apps.photos.content".equals(uri.getAuthority());
	}
}
