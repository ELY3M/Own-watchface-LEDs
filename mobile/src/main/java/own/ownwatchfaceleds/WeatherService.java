package own.ownwatchfaceleds;


import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class WeatherService extends WearableListenerService
{



	private static final String TAG  = "ownwatchface wxservice";

	private GoogleApiClient mGoogleApiClient;
	private LocationManager mLocationManager;
	private Location        mLocation;
	private String          mPeerId;

	private double lat = 0.0;
	private double lon = 0.0;
	private String mylat = "0.0";
	private String mylon = "0.0";
	private String mytemp = "103°F";
	private String myicon = "unknown";
	private String finalicon = "unknown";
	private String myweather = "unknown";
	private int updatecount = 0;
	private String url = "https://forecast.weather.gov/MapClick.php?";
	private String finalurl = "setup";
	private String visiturl = "setup";


	@Override
	public int onStartCommand( Intent intent, int flags, int startId )
	{
		if ( intent != null )
		{
			if (Main.class.getSimpleName().equals( intent.getAction() ) )
			{
				mPeerId = intent.getStringExtra( "PeerId" );
				startTask();
			}
		}

		return super.onStartCommand( intent, flags, startId );
	}

	@Override
	public void onMessageReceived( MessageEvent messageEvent )
	{
		super.onMessageReceived( messageEvent );
		mPeerId = messageEvent.getSourceNodeId();
		Log.d( TAG, "MessageReceived: " + messageEvent.getPath() );
		if ( messageEvent.getPath().equals(Settings.PATH_WEATHER_REQUIRE))
		{
			startTask();
		}
	}

	private void startTask()
	{
		Log.d( TAG, "Start Weather AsyncTask" );
		mGoogleApiClient = new GoogleApiClient.Builder( this ).addApi( Wearable.API ).build();

		mLocationManager = (LocationManager) this.getSystemService( Context.LOCATION_SERVICE );
		mLocation = mLocationManager.getLastKnownLocation( LocationManager.GPS_PROVIDER );

		if ( mLocation == null )
		{
			mLocationManager.requestLocationUpdates(
				LocationManager.GPS_PROVIDER, 0, 0, new LocationListener()
				{
					@Override
					public void onLocationChanged( Location location )
					{
						Log.d( TAG, "onLocationChanged: " + location );
						mLocationManager.removeUpdates( this );
						mLocation = location;
						Task task = new Task();
						task.execute();
					}

					@Override
					public void onStatusChanged( String provider, int status, Bundle extras )
					{

					}

					@Override
					public void onProviderEnabled( String provider )
					{

					}

					@Override
					public void onProviderDisabled( String provider )
					{

					}
				}
			);
		}
		else
		{
			Task task = new Task();
			task.execute();
		}
	}

	private class Task extends AsyncTask
	{

		handlejson obj;
		@Override
		protected Object doInBackground( Object[] params )
		{
			try
			{
				Log.d(TAG, "Task Running");

				if ( !mGoogleApiClient.isConnected() )
				{ mGoogleApiClient.connect(); }

				DataMap config = new DataMap();

				//put real download stuff here
				lat = mLocation.getLatitude();
				lon = mLocation.getLongitude();

				finalurl = url + "&lat=" + lat  + "&lon=" + lon  + "&FcstType=json";
				Log.i(TAG, "finalurl: " + finalurl);

				obj = new handlejson(finalurl);
				obj.fetchJSON();
				while (obj.parsingComplete);

				mytemp = obj.getTemp() + "°F";
				myweather = obj.getWeather();

				if(myweather.isEmpty()) {
					Log.i(TAG, "myweather is null!");
					Log.i(TAG, "setting myweather to unknown");
					myweather = "unknown";
				}



				Log.i(TAG, "mytemp: " + mytemp);
				Log.i(TAG, "myweather: " + myweather);

				//setting up icon name//
				Pattern pattern = Pattern.compile("(.*?)(.png|.jpg|.gif)");
				Matcher geticon = pattern.matcher(obj.getIcon());
				while (geticon.find()) {
					finalicon = geticon.group(1);
				}
				myicon = finalicon;
				Log.i(TAG, "obj.getIcon: " + obj.getIcon());
				Log.i(TAG, "myicon: " + myicon);


				/*
				//test
				Random random = new Random();
				mytemp = String.valueOf(random.nextInt(100));
				myweather = new String[]{"clear","rain","snow","thunderstorm","overcast","fair"}[random.nextInt(5)];
				myicon = new String[]{"hot","skc","bkn","fc","nfc","cold","novc"}[random.nextInt(6)];
				*/



				config.putString(Settings.KEY_TEMP, mytemp);
				config.putString(Settings.KEY_ICON, myicon);
				config.putString(Settings.KEY_WEATHER, myweather);
				Log.i(TAG, "mytemp: " + mytemp);
				Log.i(TAG, "myweather: " + myweather);
				Log.i(TAG, "myicon: " + myicon);



				//logging
				SimpleDateFormat timestamp = new SimpleDateFormat("EEE M-d-yy h:mm:ss a");
				Calendar c = Calendar.getInstance();
				String mytimestamp = timestamp.format(c.getTime());
				updatecount++;
				String LogString = "Temp : " + mytemp + " Icon: " + myicon + " Weather: " + myweather + "\nLast Update: " + mytimestamp + "\nUpdate Count: " + updatecount;
				Log.i(TAG, LogString);
				try {
					FileWriter writer = new FileWriter("/sdcard/swarm-updates.txt", true);
					BufferedWriter bufferedWriter = new BufferedWriter(writer);
					bufferedWriter.write(LogString);
					bufferedWriter.newLine();
					bufferedWriter.write("-------------------------------------------------------------------");
					bufferedWriter.newLine();
					bufferedWriter.close();
				} catch (IOException e) {
					Log.i(TAG, "writer crash..." + e);
					e.printStackTrace();
				}



				Wearable.MessageApi.sendMessage( mGoogleApiClient, mPeerId, Settings.PATH_WEATHER_INFO, config.toByteArray() )
				                   .setResultCallback(
					                   new ResultCallback<MessageApi.SendMessageResult>()
					                   {
						                   @Override
						                   public void onResult( MessageApi.SendMessageResult sendMessageResult )
						                   {
							                   Log.d( TAG, "SendUpdateWeather: " + sendMessageResult.getStatus() );
						                   }
					                   }
				                   );
			}
			catch ( Exception e )
			{
				Log.d( TAG, "Task Fail: " + e );
			}
			return null;
		}
	}
}

