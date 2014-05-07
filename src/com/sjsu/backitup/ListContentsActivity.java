package com.sjsu.backitup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;

import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.os.Build;

public class ListContentsActivity extends ActionBarActivity {

	AWSCredentials credentials = null;
	Bucket buc;
	AmazonS3Client conn = null;
	List<S3ObjectSummary> list = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_list_contents);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
			.add(R.id.container, new PlaceholderFragment()).commit();
		}
		try {
			credentials = new PropertiesCredentials(AwsConsoleApp.class.getResourceAsStream("AwsCredentials.properties"));
			SharedPreferences sh_Pref = getSharedPreferences("Login Credentials", MODE_PRIVATE); 
			String userName = sh_Pref.getString("usrnmLogged", "");
			if("".equalsIgnoreCase(userName)){
				userName = "TestUser";
			}
			CreateBucketForUser cb = new CreateBucketForUser("mybucket", userName);
			AsyncTask<Void, Void, String> tt = cb.execute();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.list_contents, menu);
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

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_list_contents,
					container, false);
			return rootView;
		}
	}

	public void populateList(){
		// Get the reference of ListViewAnimals

		final ListView filesList=(ListView)findViewById(R.id.listView1);
		// Create The Adapter with passing ArrayList as 3rd parameter
		ArrayAdapter<String> arrayAdapter =      
				new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
		for(int i=0;i<list.size();i++){
			S3ObjectSummary selectedFile = list.get(i);
			arrayAdapter.add(selectedFile.getKey());
		}
		//		arrayAdapter.add();
		// Set The Adapter
		filesList.setAdapter(arrayAdapter); 

		// register onClickListener to handle click events on each item
		filesList.setOnItemClickListener(new OnItemClickListener(){

			// argument position gives the index of item which is clicked
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3){
				S3ObjectSummary selectedFile=list.get(position);
				String fName = selectedFile.getKey();
				Toast.makeText(getApplicationContext(), "Downloading File : "+fName,   Toast.LENGTH_LONG).show();
				DownloadFile df = new DownloadFile(selectedFile);
				df.execute();
			}
		});
	}

	class DownloadFile extends AsyncTask<Void, Void, Void>{

		S3ObjectSummary selectedFile;
		String fName;
		OutputStream writer;
		InputStream reader; 
		
		DownloadFile(S3ObjectSummary selectedFile){
			this.selectedFile = selectedFile;
			fName = selectedFile.getKey();
		}
		
		@Override
		protected Void doInBackground(Void... params) {
			
			try{
				S3Object file = conn.getObject(buc.getName(), selectedFile.getKey());
				reader = new BufferedInputStream(file.getObjectContent());
				String storage = Environment.getDataDirectory().getAbsolutePath()+"/BACKITUP Downloads";
				File downloadFol = new File(storage);
				if(!downloadFol.exists()){
					downloadFol.mkdir();
				}
				File downloadFile = new File(downloadFol.getAbsoluteFile()+"/"+fName);

				writer = new BufferedOutputStream(new FileOutputStream(downloadFile));
				int read = -1;
				while ( ( read = reader.read() ) != -1 ) {
					writer.write(read);
				}

				writer.flush();
				writer.close();
				reader.close();


			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;

		}
		
		@Override
		protected void onPostExecute(Void result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
		}

	}
	class CreateBucketForUser extends AsyncTask<Void, Void, String>{

		String strBucketName = "";
		String strUserName = "";

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			if(list != null && list.size() > 0){

				populateList();
			}
			/*SharedPreferences sh = getSharedPreferences("BACKITUP", MODE_PRIVATE);
			String path1 = sh.getString("path1", "");
			String path2 = sh.getString("path2", "");
			String path3 = sh.getString("path3", "");
			String path4 = sh.getString("path4", "");
			String path5 = sh.getString("path5", "");
			if(!"".equalsIgnoreCase(path1)){
				AsyncTask<Void, Void, Void> ut1 = new Uploadtask(path1);
				ut1.execute();
			}
			if(!"".equalsIgnoreCase(path2)){
				AsyncTask<Void, Void, Void> ut2 = new Uploadtask(path2);
				ut2.execute();
			}
			if(!"".equalsIgnoreCase(path3)){
				AsyncTask<Void, Void, Void> ut3 = new Uploadtask(path3);
				ut3.execute();
			}
			if(!"".equalsIgnoreCase(path4)){
				AsyncTask<Void, Void, Void> ut4 = new Uploadtask(path4);
				ut4.execute();
			}
			if(!"".equalsIgnoreCase(path5)){
				AsyncTask<Void, Void, Void> ut5 = new Uploadtask(path5);
				ut5.execute();
			}*/
		}
		public CreateBucketForUser(String strBucketName, String strUserName){
			this.strBucketName = strBucketName;
			this.strUserName = strUserName;
		}
		@Override
		protected String doInBackground(Void... params) {
			try{
				conn = new AmazonS3Client(credentials);
				String strUserBucket = (strBucketName+strUserName).toLowerCase();
				if(!conn.doesBucketExist(strUserBucket)){
					buc = conn.createBucket(strUserBucket);
					System.out.println(buc.getName());
				}
				else{
					buc = new Bucket(strUserBucket);
				}
				if(conn != null && buc != null){
					System.out.println(buc.getName());
					ObjectListing ol = conn.listObjects(new ListObjectsRequest().withBucketName(buc.getName()));
					list = ol.getObjectSummaries();
					//list.get(0);
				}
			}
			catch(Exception ex){
				ex.printStackTrace();
				System.out.println("Message#####"+ex.getMessage());
			}
			return null;
		}

	}
	class Uploadtask extends AsyncTask<Void, Void, Void>{

		//ProgressDialog dialog;
		String uploadPath;
		String strResponse;
		TransferManager manager;
		AmazonS3Client conn; 
		public Uploadtask(String path){
			manager = new TransferManager(credentials);
			conn = new AmazonS3Client(credentials);
			uploadPath = path;
			//dialog = new ProgressDialog(getApplicationContext());
		}
		public boolean UploadFile(String bucName, File file){
			boolean bSuccess = false;
			String fName = file.getName();
			Upload upload = manager.upload(bucName, fName, file);
			UploadResult ur = null;
			try {
				ur = upload.waitForUploadResult();
			} catch (AmazonServiceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (AmazonClientException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			//System.out.println(ur.getBucketName());
			//System.out.println(upload.isDone());
			if(upload.isDone()){
				bSuccess = true;
			}
			return bSuccess;
		}
		@Override
		protected Void doInBackground(Void... params) {

			try{
				File file = new File(uploadPath);
				if(file.isDirectory()){
					File[] files = file.listFiles();
					for(int i = 0;i<file.length();i++){
						boolean bsuccess = UploadFile(buc.getName(), files[i]);
						if(!bsuccess){
							strResponse = "Some files not uploaded properly";
						}
					}
				}
				//     Download myDownload = manager.download(strUserBucket,"/Downloads/sampletest.txt", file);
			}
			catch(Exception ex){
				Log.e("Exception",ex.toString());
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void Result) {
			//			if (dialog.isShowing()) {
			//				dialog.dismiss();
			//			}
		}

		@Override
		protected void onPreExecute() {
			//			dialog.setMessage("Uploading Content, please wait.");
			//			dialog.show();
		}

		@Override
		protected void onProgressUpdate(Void... values) {

		}
	}

}