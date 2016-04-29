package com.example.facerecognition;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity{

	private static final String TAG = "MainActivity";
	protected static final int REQUEST_CAMERA = 1;
	protected static final int SELECT_FILE = 2;
	ImageView ivImage;
	TextView textView1;
	TextView textView2;
	TextView textView3;
	TextView textView4;
	TextView textView5;
	TextView textView6;
	byte[] img_arr;
	
	private final String SERVERURL = "http://129.114.110.221/facerecog_script.php";
	private Context mContext = this;
	
	private String cameraImgPath;
	private String galleryImgPath;
	public boolean isCameraImg = false;
	public boolean isGalleryImg = false;
	public Bitmap resultImage;
	public String server;
	public double startTime;
	public double endTime;
	public String proc_time;
	
	public static final int MEDIA_TYPE_IMAGE = 1;
    // directory name to store captured images and videos
    private static final String IMAGE_DIRECTORY_NAME = "Facerecognition";
    private Uri fileUri; // file url to store image/video
 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		ivImage = (ImageView) findViewById(R.id.ivImage);
		textView1 = (TextView) findViewById(R.id.textView1);
		textView2 = (TextView) findViewById(R.id.textView2);
		textView3 = (TextView) findViewById(R.id.textView3);
		textView4 = (TextView) findViewById(R.id.textView4);
		textView5 = (TextView) findViewById(R.id.textView5);
		textView6 = (TextView) findViewById(R.id.textView6);
		
		textView1.setVisibility(View.GONE);
    	textView2.setVisibility(View.GONE);
    	textView3.setVisibility(View.GONE);
    	textView4.setVisibility(View.GONE);
    	textView5.setVisibility(View.GONE);
    	textView6.setVisibility(View.GONE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void selectImage(View view) {
		final CharSequence[] items = { "Take Photo", "Choose from Library", "Cancel" };
		
		AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		
		builder.setTitle("Add Photo!");
		builder.setItems(items, new DialogInterface.OnClickListener() {
			
				@Override
				public void onClick(DialogInterface dialog, int item) {
					if (items[item].equals("Take Photo")) {
						//Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
						//startActivityForResult(intent, REQUEST_CAMERA);
						// capture picture
		                captureImage();		
					} 
					else if (items[item].equals("Choose from Library")) {
						Intent intent = new Intent(
						Intent.ACTION_PICK,
						android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
						intent.setType("image/*");
						startActivityForResult(
						Intent.createChooser(intent, "Select File"),
						SELECT_FILE);
					} 
					else if (items[item].equals("Cancel")) {
						dialog.dismiss();
					}
				}
		});
		builder.show();
	}
	
	/*
	 * Capturing Camera Image will lauch camera app requrest image capture
	 */
	private void captureImage() {
	    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
	 
	    fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
	 
	    intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
	 
	    // start the image capture Intent
	    startActivityForResult(intent, REQUEST_CAMERA);
	}
	
	/**
     * Creating file uri to store image/video
     */
    public Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }
 
    /**
     * returning image / video
     */
    private static File getOutputMediaFile(int type) {
 
        // External sdcard location
        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                IMAGE_DIRECTORY_NAME);
 
        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(IMAGE_DIRECTORY_NAME, "Oops! Failed create "
                        + IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }
 
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }
 
        return mediaFile;
    }
    
    /**
     * Here we store the file url as it will be null after returning from camera
     * app
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
 
        // save file url in bundle as it will be null on scren orientation
        // changes
        outState.putParcelable("file_uri", fileUri);
    }
 
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
 
        // get the file url
        fileUri = savedInstanceState.getParcelable("file_uri");
    }
 
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
				if (requestCode == REQUEST_CAMERA) {
					
					try{
							cameraImgPath = fileUri.getPath();
							// bimatp factory
				            BitmapFactory.Options options = new BitmapFactory.Options();
				 
				            // downsizing image as it throws OutOfMemory Exception for larger
				            // images
				            options.inSampleSize = 8;
				 
				            final Bitmap bitmap = BitmapFactory.decodeFile(fileUri.getPath(), options);
				 
				            isCameraImg = true;
				            isGalleryImg = false;
				            ivImage.setImageBitmap(bitmap);
				            
					  }catch (NullPointerException e) {
			            e.printStackTrace();
			        }
					
				} 
				else if (requestCode == SELECT_FILE) {
					Uri selectedImageUri = data.getData();
				        String[] projection = { MediaColumns.DATA };
				        CursorLoader cursorLoader = new CursorLoader(this,selectedImageUri, projection, null, null,
				                null);
				        Cursor cursor =cursorLoader.loadInBackground();
				        int column_index = cursor.getColumnIndexOrThrow(MediaColumns.DATA);
				        cursor.moveToFirst();
				 
				        String selectedImagePath = cursor.getString(column_index);
				 
				        Bitmap bm;
				        BitmapFactory.Options options = new BitmapFactory.Options();
				        options.inJustDecodeBounds = true;
				        BitmapFactory.decodeFile(selectedImagePath, options);
				        final int REQUIRED_SIZE = 200;
				        int scale = 1;
				        while (options.outWidth / scale / 2 >= REQUIRED_SIZE
				                && options.outHeight / scale / 2 >= REQUIRED_SIZE)
				            scale *= 2;
				        options.inSampleSize = scale;
				        options.inJustDecodeBounds = false;
				        bm = BitmapFactory.decodeFile(selectedImagePath, options);
				 
				        isCameraImg = false;
				        isGalleryImg = true;
				        galleryImgPath = selectedImagePath;
				        ivImage.setImageBitmap(bm);
				        
				      	        
				}
		}
	}

    public void uploadImage(View view){
    	String imgPath = "";
		textView1.setVisibility(View.GONE);
    	textView2.setVisibility(View.GONE);
    	textView3.setVisibility(View.GONE);
    	textView4.setVisibility(View.GONE);
    	textView5.setVisibility(View.GONE);
    	textView6.setVisibility(View.GONE);
    	
    	if(isCameraImg == true){
    		imgPath = cameraImgPath;
    	}
    	else if(isGalleryImg == true) imgPath = galleryImgPath;
    	
    	//Toast.makeText(mContext, imgPath, Toast.LENGTH_SHORT).show();
    	
    	//** Send image and offload image processing task  to server by starting async task ** 
		if(imgPath != ""){
			startTime = System.currentTimeMillis();
    		ServerTask task = new ServerTask();
			task.execute( imgPath);
		}
		else{
			Toast.makeText(mContext, "Select the image first!", Toast.LENGTH_SHORT).show();
		}
		

    }
    
  //*******************************************************************************
  	//Push image processing task to server
  	//*******************************************************************************
  	
  	public class ServerTask  extends AsyncTask<String, Integer , Void>
  	{
  		public byte[] dataToServer;
  				
  		//Task state
  		private final int UPLOADING_PHOTO_STATE  = 0;
  		private final int SERVER_PROC_STATE  = 1;
  		
  		private ProgressDialog dialog;
  		
  		//upload photo to server
  		HttpURLConnection uploadPhoto(FileInputStream fileInputStream)
  		{
  			
  			final String serverFileName = "test"+ (int) Math.round(Math.random()*1000) + ".jpg";		
  			final String lineEnd = "\r\n";
  			final String twoHyphens = "--";
  			final String boundary = "*****";
  			
  			try
  			{
  				URL url = new URL(SERVERURL);
  				
  				Log.d(TAG, "In uploadPhoto");
  				// Open a HTTP connection to the URL
  				final HttpURLConnection conn = (HttpURLConnection)url.openConnection();
  				// Allow Inputs
  				conn.setDoInput(true);				
  				// Allow Outputs
  				conn.setDoOutput(true);				
  				// Don't use a cached copy.
  				conn.setUseCaches(false);
  				
  				// Use a post method.
  				conn.setRequestMethod("POST");
  				conn.setRequestProperty("Connection", "Keep-Alive");
  				conn.setRequestProperty("Content-Type", "multipart/form-data;boundary="+boundary);
  				
  				DataOutputStream dos = new DataOutputStream( conn.getOutputStream() );
  				
  				dos.writeBytes(twoHyphens + boundary + lineEnd);
  				dos.writeBytes("Content-Disposition: form-data; name=\"uploadedfile\";filename=\"" + serverFileName +"\"" + lineEnd);
  				dos.writeBytes(lineEnd);

  				// create a buffer of maximum size
  				int bytesAvailable = fileInputStream.available();
  				int maxBufferSize = 1024;
  				int bufferSize = Math.min(bytesAvailable, maxBufferSize);
  				byte[] buffer = new byte[bufferSize];
  				
  				// read file and write it into form...
  				int bytesRead = fileInputStream.read(buffer, 0, bufferSize);
  				
  				while (bytesRead > 0)
  				{
  					dos.write(buffer, 0, bufferSize);
  					bytesAvailable = fileInputStream.available();
  					bufferSize = Math.min(bytesAvailable, maxBufferSize);
  					bytesRead = fileInputStream.read(buffer, 0, bufferSize);
  				}
  				
  				// send multipart form data after file data...
  				dos.writeBytes(lineEnd);
  				dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
  				publishProgress(SERVER_PROC_STATE);
  				// close streams
  				dos.flush();
  				fileInputStream.close();
  				return conn;
  			}
  			catch (MalformedURLException ex){
  				Log.e(TAG, "error: " + ex.getMessage(), ex);
  				return null;
  			}
  			catch (IOException ioe){
  				Log.e(TAG, "error: " + ioe.getMessage(), ioe);
  				return null;
  			}
  		}
  		
  	    //get image result from server and display it in result view
  		void getResultImage(HttpURLConnection conn){
  			Log.d(TAG, "In getResultImage");
  			// retrieve the response from server
  			InputStream is;
  			try {
  				is = conn.getInputStream();
  				
  				//get result image from server
  				resultImage = null;
  		        resultImage = BitmapFactory.decodeStream(is);
  		        is.close();		        
  		        
  		         
  			} catch (IOException e) {
  				Log.e(TAG,e.toString());
  				e.printStackTrace();
  			}
  		}
  		
  		//Main code for processing image algorithm on the server
  		
  		void processImage(String inputImageFilePath){			
  			Log.d(TAG, "In processImage");
  			publishProgress(UPLOADING_PHOTO_STATE);
  			File inputFile = new File(inputImageFilePath);
  			try {
  				
  				//create file stream for captured image file
  				FileInputStream fileInputStream  = new FileInputStream(inputFile);
  		    	
  				//upload photo
  		    	final HttpURLConnection  conn = uploadPhoto(fileInputStream);
  		    	
  		    	//get processed photo from server
  		    	if (conn != null){
		  		    	server = conn.getHeaderField("Servername");
		  		    	proc_time = conn.getHeaderField("Process_time");
		  		    	getResultImage(conn);
  		    	}
  				fileInputStream.close();
  			}
  	        catch (FileNotFoundException ex){
  	        	Log.e(TAG, ex.toString());
  	        }
  	        catch (IOException ex){
  	        	Log.e(TAG, ex.toString());
  	        }
  		}
  		
  	    public ServerTask() {
  	        dialog = new ProgressDialog(mContext);
  	    }		
  		
  	    protected void onPreExecute() {
  	    	//startTime = System.currentTimeMillis();
  	        this.dialog.setMessage("Photo captured");
  	        this.dialog.show();
  	    }
  	    
  		@Override
  		protected Void doInBackground(String... params) {
  			Log.d(TAG, "In doInBackground");
  			//background operation 
  			String uploadFilePath = params[0];
  			processImage(uploadFilePath);
  			//release camera when previous image is processed
  			 
  			return null;
  		}		
  		//progress update, display dialogs
  		@Override
  	     protected void onProgressUpdate(Integer... progress) {
  	    	 if(progress[0] == UPLOADING_PHOTO_STATE){
  	    		 dialog.setMessage("Uploading");
  	    		 dialog.show();
  	    	 }
  	    	 else if (progress[0] == SERVER_PROC_STATE){
  		           if (dialog.isShowing()) {
  		               dialog.dismiss();
  		           }	    	 
  	    		 dialog.setMessage("Processing");
  	    		 dialog.show();
  	    	 }	         
  	     }		
  	       @Override
  	       protected void onPostExecute(Void param) {
  	    	 Log.d(TAG, "In onPostExecute");
  	    	 if(resultImage != null){
 				ivImage.setImageBitmap(resultImage);
 				textView2.setText(server);
 				
 				endTime = System.currentTimeMillis();
 				double duration = (endTime - startTime)/1000;
 				textView4.setText("" + duration);
 				
 				if(proc_time != null){
	 				float ptime = Float.parseFloat(proc_time);
	 				textView6.setText("" + ptime);
 				}
 				
 				textView1.setVisibility(View.VISIBLE);
 		    	textView2.setVisibility(View.VISIBLE);
 		    	textView3.setVisibility(View.VISIBLE);
 		    	textView4.setVisibility(View.VISIBLE);
 		    	textView5.setVisibility(View.VISIBLE);
 		    	textView6.setVisibility(View.VISIBLE);
 			 }
  	    	 
  	           if (dialog.isShowing()) {
  	               dialog.dismiss();
  	           }
  	       }
  	}      
}
