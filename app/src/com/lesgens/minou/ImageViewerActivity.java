package com.lesgens.minou;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageView;

import com.lesgens.minou.db.DatabaseHelper;

public class ImageViewerActivity extends MinouActivity implements OnClickListener{

	public static void show(Context context, String messageId) {
		Intent i = new Intent(context, ImageViewerActivity.class);
		i.putExtra("messageId", messageId);
		context.startActivity(i);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.image_viewer);

		String messageId = getIntent().getStringExtra("messageId");


		if(messageId == null){
			finish();
		}
		byte[] picture = DatabaseHelper.getInstance().getPictureFromMessageId(messageId);
		try{
			Bitmap bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.length);
			ImageView imageView = (ImageView) findViewById(R.id.image);
			imageView.setImageBitmap(bitmap);
		} catch(Exception e){
			e.printStackTrace();
			finish();
		}

		findViewById(R.id.container).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.container){
			finish();
		}
	}


}
