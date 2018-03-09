package own.ownwatchfaceleds;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StrictMode;
import android.os.Bundle;
import android.support.wearable.companion.WatchFaceCompanion;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import javax.net.ssl.HttpsURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class Main extends Activity implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {


    private static final String TAG = "ownwatchface Main";

    private String url = "https://forecast.weather.gov/MapClick.php?";
    private String finalurl = "setup";
    private String visiturl = "setup";
    private handlejson obj;

    //settings to be global//
    public int clockSize = 26;
    public int clocknosecsSize = 43;
    public int markerSize = 18;
    public int dateSize = 18;
    public int timeSize = 18;
    public int tempSize = 30;
    public int weatherSize = 13;

    public boolean clockAct = false;
    public boolean clockDim = false;
    public boolean clocknosecsAct = false;
    public boolean clocknosecsDim = true;
    public boolean markerAct = false;
    public boolean markerDim = true;
    public boolean dateDim = true;
    public boolean timeDim = false;
    public boolean tempDim = false;
    public boolean weatherDim = false;
    public boolean alwaysUtc = true;
    public boolean showtime = false;
    public boolean northernhemi = true;

    private EditText clockSizeEditText;
    private EditText clocknosecsSizeEditText;
    private EditText markerSizeEditText;
    private EditText dateSizeEditText;
    private EditText timeSizeEditText;
    private EditText tempSizeEditText;
    private EditText weatherSizeEditText;
    private CheckBox clockactCheckBox;
    private CheckBox clockdimCheckBox;
    private CheckBox clocknosecsactCheckBox;
    private CheckBox clocknosecsdimCheckBox;
    private CheckBox markerActCheckBox;
    private CheckBox markerDimCheckBox;
    private CheckBox dateCheckBox;
    private CheckBox timeCheckBox;
    private CheckBox tempCheckBox;
    private CheckBox weatherCheckBox;
    private CheckBox alwaysUtcCheckBox;
    private CheckBox showtimeCheckBox;
    private CheckBox northernhemiCheckBox;


    private TextView mygps;
    private double lat = 0.0;
    private double lon = 0.0;
    private String mylat = "0.0";
    private String mylon = "0.0";
    private String mytemp = "103°F";
    private String myicon = "skc";
    private String finalicon = "skc";
    private String myweather = "Clear";
    private int updatecount = 0;
    private TextView temp;
    private TextView weather;
    private ImageView icon;
    private TextView checkicon;
    private TextView checkfinalicon;
    private TextView lastupdatetime;
    private Button applyButton;
    private Button resetButton;
    private Button getweather;
    private Button refreshweather;
    private Button visitnws;
    private GoogleApiClient GoogleApiClient;

    private String mPeerId;
    //private boolean alreadyInitialize;

    public static final long GPSUPDATE_INTERVAL_IN_MILLISECONDS = Settings.gpsinterval;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = GPSUPDATE_INTERVAL_IN_MILLISECONDS / 2;
    private final static String REQUESTING_UPDATES_KEY = "requesting-updates-key";
    private final static String LOCATION_KEY = "location-key";
    private LocationRequest mLocationRequest;
    private Location CurrentLocation;
    private Boolean mRequestingUpdates;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //needed for isOnline() to work
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        initializeGoogleAPI();

        mPeerId = getIntent().getStringExtra(WatchFaceCompanion.EXTRA_PEER_ID);
        Log.d(TAG, "mPeerid: " + mPeerId);


        clockSizeEditText = (EditText) findViewById(R.id.clockSizeEditText);
        clocknosecsSizeEditText = (EditText) findViewById(R.id.clocknosecsSizeEditText);
        markerSizeEditText = (EditText) findViewById(R.id.markerSizeEditText);
        dateSizeEditText = (EditText) findViewById(R.id.dateSizeEditText);
        timeSizeEditText = (EditText) findViewById(R.id.timeSizeEditText);
        tempSizeEditText = (EditText) findViewById(R.id.tempSizeEditText);
        weatherSizeEditText = (EditText) findViewById(R.id.weatherSizeEditText);
        clockactCheckBox = (CheckBox) findViewById(R.id.clockactCheckBox);
        clockdimCheckBox = (CheckBox) findViewById(R.id.clockdimCheckBox);
        clocknosecsactCheckBox = (CheckBox) findViewById(R.id.clocknosecsactCheckBox);
        clocknosecsdimCheckBox = (CheckBox) findViewById(R.id.clocknosecsdimCheckBox);
        markerActCheckBox = (CheckBox) findViewById(R.id.markerActCheckBox);
        markerDimCheckBox = (CheckBox) findViewById(R.id.markerDimCheckBox);
        dateCheckBox = (CheckBox) findViewById(R.id.dateCheckBox);
        timeCheckBox = (CheckBox) findViewById(R.id.timeCheckBox);
        tempCheckBox = (CheckBox) findViewById(R.id.tempCheckBox);
        weatherCheckBox = (CheckBox) findViewById(R.id.weatherCheckBox);
        alwaysUtcCheckBox = (CheckBox) findViewById(R.id.alwaysUtcCheckBox);
        showtimeCheckBox = (CheckBox) findViewById(R.id.showtimeCheckBox);
        northernhemiCheckBox = (CheckBox) findViewById(R.id.northernhemiCheckBox);
        applyButton = (Button) findViewById(R.id.applyButton);
        resetButton = (Button) findViewById(R.id.resetButton);
        getweather = (Button) findViewById(R.id.getweather);
        mygps = (TextView) findViewById(R.id.gpstext);
        temp = (TextView) findViewById(R.id.temp);
        weather = (TextView) findViewById(R.id.weather);
        icon = (ImageView) findViewById(R.id.icon);
        checkicon = (TextView) findViewById(R.id.checkicon);
        checkfinalicon = (TextView) findViewById(R.id.checkiconurl);
        lastupdatetime = (TextView) findViewById(R.id.lastupdatetime);
        refreshweather = (Button) findViewById(R.id.refreshweather);
        visitnws = (Button) findViewById(R.id.visitnws);

        mRequestingUpdates = false;
        //start gps updates auto//
        updateValuesFromBundle(savedInstanceState);

        View.OnClickListener numberpicker = new View.OnClickListener() {
            @Override public void onClick(View v) {
                showNumberPicker((EditText) v);
            }
        };

        clockSizeEditText.setOnClickListener(numberpicker);
        clocknosecsSizeEditText.setOnClickListener(numberpicker);
        markerSizeEditText.setOnClickListener(numberpicker);
        dateSizeEditText.setOnClickListener(numberpicker);
        timeSizeEditText.setOnClickListener(numberpicker);
        tempSizeEditText.setOnClickListener(numberpicker);
        weatherSizeEditText.setOnClickListener(numberpicker);

        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                //TODO
                //resetValues();
            }
        });

        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                saveValues();
                pushValuesToWearable();
            }
        });

        getweather.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                weatherupdate();
            }
        });


        refreshweather = (Button)findViewById(R.id.refreshweather);
        refreshweather.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Main.this, WeatherService.class);
                intent.setAction(Main.class.getSimpleName());
                intent.putExtra("PeerId",mPeerId);
                startService(intent);
                Toast.makeText(Main.this, "Weather Refresh Succeeded!", Toast.LENGTH_SHORT).show();


            }
        });

        visitnws.setOnClickListener(new View.OnClickListener() {

            @Override public void onClick(View v) {
                Uri uri = Uri.parse(visiturl);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        loadValues();

    }




    @Override
    protected void onStart() {
        super.onStart();
        GoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (GoogleApiClient != null && GoogleApiClient.isConnected()) {
            GoogleApiClient.disconnect();
        }
        super.onStop();
    }



    ///config stufff
    private void showNumberPicker(final EditText view) {
        RelativeLayout layout = new RelativeLayout(this);
        final NumberPicker numberPicker = new NumberPicker(this);
        numberPicker.setMinValue(0);
        numberPicker.setMaxValue(99);
        numberPicker.setValue(Integer.parseInt(view.getText().toString()));

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(50, 50);
        RelativeLayout.LayoutParams numPickerParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        numPickerParams.addRule(RelativeLayout.CENTER_HORIZONTAL);

        layout.setLayoutParams(params);
        layout.addView(numberPicker, numPickerParams);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Font Size");
        builder.setView(layout);
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                view.setText(String.valueOf(numberPicker.getValue()));
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void saveValues() {
        Log.d(TAG, "saveValues()");
        Settings.setInt(this, Settings.KEY_CLOCK_SIZE, Integer.parseInt(clockSizeEditText.getText().toString()));
        Settings.setInt(this, Settings.KEY_CLOCK_NOSECS_SIZE, Integer.parseInt(clocknosecsSizeEditText.getText().toString()));
        Settings.setInt(this, Settings.KEY_MARKER_SIZE, Integer.parseInt(markerSizeEditText.getText().toString()));
        Settings.setInt(this, Settings.KEY_DATE_SIZE, Integer.parseInt(dateSizeEditText.getText().toString()));
        Settings.setInt(this, Settings.KEY_TIME_SIZE, Integer.parseInt(timeSizeEditText.getText().toString()));
        Settings.setInt(this, Settings.KEY_TEMP_SIZE, Integer.parseInt(tempSizeEditText.getText().toString()));
        Settings.setInt(this, Settings.KEY_WEATHER_SIZE, Integer.parseInt(weatherSizeEditText.getText().toString()));
        Settings.setBoolean(this, Settings.KEY_CLOCK_ACT, clockactCheckBox.isChecked());
        Settings.setBoolean(this, Settings.KEY_CLOCK_DIM, clockdimCheckBox.isChecked());
        Settings.setBoolean(this, Settings.KEY_CLOCK_NOSECS_ACT, clocknosecsactCheckBox.isChecked());
        Settings.setBoolean(this, Settings.KEY_CLOCK_NOSECS_DIM, clocknosecsdimCheckBox.isChecked());
        Settings.setBoolean(this, Settings.KEY_MARKER_ACT, markerActCheckBox.isChecked());
        Settings.setBoolean(this, Settings.KEY_MARKER_DIM, markerDimCheckBox.isChecked());
        Settings.setBoolean(this, Settings.KEY_DATE_DIM, dateCheckBox.isChecked());
        Settings.setBoolean(this, Settings.KEY_TIME_DIM, timeCheckBox.isChecked());
        Settings.setBoolean(this, Settings.KEY_TEMP_DIM, tempCheckBox.isChecked());
        Settings.setBoolean(this, Settings.KEY_WEATHER_DIM, weatherCheckBox.isChecked());
        Settings.setBoolean(this, Settings.KEY_ALWAYS_UTC, alwaysUtcCheckBox.isChecked());
        Settings.setBoolean(this, Settings.KEY_SHOW_TIME, showtimeCheckBox.isChecked());
        Settings.setBoolean(this, Settings.KEY_NORTHERNHEMI, northernhemiCheckBox.isChecked());
        Settings.setString(this, Settings.KEY_LAT, mylat);
        Settings.setString(this, Settings.KEY_LON, mylon);
        //Settings.setString(this, Settings.KEY_TEMP, mytemp);
        //Settings.setString(this, Settings.KEY_ICON, finalicon);
        //Settings.setString(this, Settings.KEY_WEATHER, myweather);
    }



    private void loadValues() {
        Log.d(TAG, "loadValues()");

        clockSizeEditText.setText(String.valueOf(Settings.getInt(this, Settings.KEY_CLOCK_SIZE, clockSize)));
        clocknosecsSizeEditText.setText(String.valueOf(Settings.getInt(this, Settings.KEY_CLOCK_NOSECS_SIZE, clocknosecsSize)));
        markerSizeEditText.setText(String.valueOf(Settings.getInt(this, Settings.KEY_MARKER_SIZE, markerSize)));
        dateSizeEditText.setText(String.valueOf(Settings.getInt(this, Settings.KEY_DATE_SIZE, dateSize)));
        timeSizeEditText.setText(String.valueOf(Settings.getInt(this, Settings.KEY_TIME_SIZE, timeSize)));
        tempSizeEditText.setText(String.valueOf(Settings.getInt(this, Settings.KEY_TEMP_SIZE, tempSize)));
        weatherSizeEditText.setText(String.valueOf(Settings.getInt(this, Settings.KEY_WEATHER_SIZE, weatherSize)));
        clockactCheckBox.setChecked(Settings.getBoolean(this, Settings.KEY_CLOCK_ACT, clockAct));
        clockdimCheckBox.setChecked(Settings.getBoolean(this, Settings.KEY_CLOCK_DIM, clockDim));
        clocknosecsactCheckBox.setChecked(Settings.getBoolean(this, Settings.KEY_CLOCK_NOSECS_ACT, clocknosecsAct));
        clocknosecsdimCheckBox.setChecked(Settings.getBoolean(this, Settings.KEY_CLOCK_NOSECS_DIM, clocknosecsDim));
        markerActCheckBox.setChecked(Settings.getBoolean(this, Settings.KEY_MARKER_ACT, markerAct));
        markerDimCheckBox.setChecked(Settings.getBoolean(this, Settings.KEY_MARKER_DIM, markerDim));
        dateCheckBox.setChecked(Settings.getBoolean(this, Settings.KEY_DATE_DIM, dateDim));
        timeCheckBox.setChecked(Settings.getBoolean(this, Settings.KEY_TIME_DIM, timeDim));
        tempCheckBox.setChecked(Settings.getBoolean(this, Settings.KEY_TEMP_DIM, tempDim));
        weatherCheckBox.setChecked(Settings.getBoolean(this, Settings.KEY_WEATHER_DIM, weatherDim));
        alwaysUtcCheckBox.setChecked(Settings.getBoolean(this, Settings.KEY_ALWAYS_UTC, alwaysUtc));
        showtimeCheckBox.setChecked(Settings.getBoolean(this, Settings.KEY_SHOW_TIME, showtime));
        northernhemiCheckBox.setChecked(Settings.getBoolean(this, Settings.KEY_NORTHERNHEMI, northernhemi));

    }


    private void resetValues() {
        //Settings.resetAllPrefs(this);
        loadValues();
    }


    public void pushValuesToWearable() {
        Log.d(TAG, "pushValuesToWearable()");
        clockSize = Integer.parseInt(clockSizeEditText.getText().toString());
        clocknosecsSize = Integer.parseInt(clocknosecsSizeEditText.getText().toString());
        markerSize = Integer.parseInt(markerSizeEditText.getText().toString());
        dateSize = Integer.parseInt(dateSizeEditText.getText().toString());
        timeSize = Integer.parseInt(timeSizeEditText.getText().toString());
        tempSize = Integer.parseInt(tempSizeEditText.getText().toString());
        weatherSize = Integer.parseInt(weatherSizeEditText.getText().toString());
        clockAct = clockactCheckBox.isChecked();
        clockDim = clockdimCheckBox.isChecked();
        clocknosecsAct = clocknosecsactCheckBox.isChecked();
        clocknosecsDim = clocknosecsdimCheckBox.isChecked();
        markerAct = markerActCheckBox.isChecked();
        markerDim = markerDimCheckBox.isChecked();
        dateDim = dateCheckBox.isChecked();
        timeDim = timeCheckBox.isChecked();
        tempDim = tempCheckBox.isChecked();
        weatherDim = weatherCheckBox.isChecked();
        alwaysUtc = alwaysUtcCheckBox.isChecked();
        showtime = showtimeCheckBox.isChecked();
        northernhemi = northernhemiCheckBox.isChecked();

        DataMap dataMap = new DataMap();
        dataMap.putInt(Settings.KEY_CLOCK_SIZE, clockSize);
        dataMap.putInt(Settings.KEY_CLOCK_NOSECS_SIZE, clocknosecsSize);
        dataMap.putInt(Settings.KEY_MARKER_SIZE, markerSize);
        dataMap.putInt(Settings.KEY_DATE_SIZE, dateSize);
        dataMap.putInt(Settings.KEY_TIME_SIZE, timeSize);
        dataMap.putInt(Settings.KEY_TEMP_SIZE, tempSize);
        dataMap.putInt(Settings.KEY_WEATHER_SIZE, weatherSize);
        dataMap.putBoolean(Settings.KEY_CLOCK_ACT, clockAct);
        dataMap.putBoolean(Settings.KEY_CLOCK_DIM, clockDim);
        dataMap.putBoolean(Settings.KEY_CLOCK_NOSECS_ACT, clocknosecsAct);
        dataMap.putBoolean(Settings.KEY_CLOCK_NOSECS_DIM, clocknosecsDim);
        dataMap.putBoolean(Settings.KEY_MARKER_ACT, markerAct);
        dataMap.putBoolean(Settings.KEY_MARKER_DIM, markerDim);
        dataMap.putBoolean(Settings.KEY_DATE_DIM, dateDim);
        dataMap.putBoolean(Settings.KEY_TIME_DIM, timeDim);
        dataMap.putBoolean(Settings.KEY_TEMP_DIM, tempDim);
        dataMap.putBoolean(Settings.KEY_WEATHER_DIM, weatherDim);
        dataMap.putBoolean(Settings.KEY_ALWAYS_UTC, alwaysUtc);
        dataMap.putBoolean(Settings.KEY_SHOW_TIME, showtime);
        dataMap.putBoolean(Settings.KEY_NORTHERNHEMI, northernhemi);
        dataMap.putString(Settings.KEY_LAT, mylat);
        dataMap.putString(Settings.KEY_LON, mylon);
        //dataMap.putString(Settings.KEY_TEMP, mytemp);
        //dataMap.putString(Settings.KEY_ICON, finalicon);
        //dataMap.putString(Settings.KEY_WEATHER, myweather);
        sendConfigUpdateMessage(dataMap);

    }


    ///new config
    private void sendConfigUpdateMessage(DataMap config) {
        Log.d(TAG, "trying to Send Config");
        if (mPeerId != null) {
            Log.d(TAG, "Sending Config: " + config);
            Wearable.MessageApi.sendMessage(GoogleApiClient, mPeerId, Settings.PATH_CONFIG, config.toByteArray())
                    .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.d(TAG, "Send Config Result: " + sendMessageResult.getStatus());
                        }
                    });
        }
    }


    /// weather stuff///
    public void weatherupdate() {
        ///get weather////
        Log.i(TAG,"weatherupdate() started");
        finalurl = url + "lat=" + mylat + "&lon=" + mylon + "&FcstType=json";
        visiturl = url + "lat=" + mylat + "&lon=" + mylon;
        Log.i(TAG, "finalurl: " + finalurl);
        //if (isOnlineSimple(getApplicationContext())) {
        if (isOnline(getApplicationContext())) {
            //if (checknet()) {
            if (GoogleApiClient.isConnected()) {
                //checkwear();
                //if (isConnectedToWearable) {
                obj = new handlejson(finalurl);
                obj.fetchJSON();
                while (obj.parsingComplete) ;
                mytemp = obj.getTemp();
                myicon = obj.getIcon();
                myweather = obj.getWeather();
                Log.i(TAG, "mytemp: " + mytemp);
                Log.i(TAG, "myicon: " + myicon);
                Log.i(TAG, "myweather: " + myweather);
                Pattern pattern = Pattern.compile("(.*?)(.png|.jpg|.gif)");
                Matcher geticon = pattern.matcher(myicon);
                while (geticon.find()) {
                    finalicon = geticon.group(1);
                }
                double finaltemp = Math.ceil(Double.valueOf(mytemp));
                mytemp = String.valueOf((int) finaltemp) + "°F";

                temp.setText(mytemp);
                weather.setText(myweather);
                int res = getResources().getIdentifier(finalicon, "drawable", getApplicationContext().getPackageName());
                icon.setImageResource(res);
                checkicon.setText(myicon);
                checkfinalicon.setText(finalicon);
                Log.i(TAG, "finalicon: " + finalicon);
                //saveValues();
                //pushValuesToWearable();
                SimpleDateFormat timestamp = new SimpleDateFormat("EEE M-d-yy h:mm:ss a");
                Calendar c = Calendar.getInstance();
                String mytimestamp = timestamp.format(c.getTime());
                updatecount++;
                lastupdatetime.setTextSize(13);
                lastupdatetime.setText("Last Update: " + mytimestamp + "\nUpdate Count: " + updatecount);
                Log.i(TAG, "Last Update: " + mytimestamp + "\nUpdate Count: " + updatecount);
                ///Toast.makeText(getApplicationContext(), "Weather Update: " + mytimestamp + "  Update Count: " + updatecount, Toast.LENGTH_SHORT).show();

                //logging
                //SimpleDateFormat timestamp = new SimpleDateFormat("EEE M-d-yy h:mm:ss a");
                //Calendar c = Calendar.getInstance();
                //String mytimestamp = timestamp.format(c.getTime());
                updatecount++;
                String LogString = "Temp : " + mytemp + " Icon: " + myicon + " Weather: " + myweather + "\nLast Update: " + mytimestamp + "\nUpdate Count: " + updatecount;
                Log.i(TAG, LogString);
                try {
                    FileWriter writer = new FileWriter("/sdcard/ownwatchtext-updates.txt", true);
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


                /*
        } else { ///wear connection
            SimpleDateFormat timestamp = new SimpleDateFormat("EEE M-d-yy h:mm:ss a");
            Calendar c = Calendar.getInstance();
            String mytimestamp = timestamp.format(c.getTime());
            lastupdatetime.setTextSize(13);
            lastupdatetime.setText("Failed Update (Wear Disconnected): " + mytimestamp + "\nUpdate Count: " + updatecount);

        }*/
            } else { ///GoogleApi connection
                SimpleDateFormat timestamp = new SimpleDateFormat("EEE M-d-yy h:mm:ss a");
                Calendar c = Calendar.getInstance();
                String mytimestamp = timestamp.format(c.getTime());
                updatecount++;
                lastupdatetime.setTextSize(13);
                lastupdatetime.setText("Failed Update (Google): " + mytimestamp + "\nUpdate Count: " + updatecount);
                Log.i(TAG, "Failed Update (Google): " + mytimestamp + "\nUpdate Count: " + updatecount);
            }

        } else { //internet check
            SimpleDateFormat timestamp = new SimpleDateFormat("EEE M-d-yy h:mm:ss a");
            Calendar c = Calendar.getInstance();
            String mytimestamp = timestamp.format(c.getTime());
            updatecount++;
            lastupdatetime.setTextSize(13);
            lastupdatetime.setText("Failed Update (Offline): " + mytimestamp + "\nUpdate Count: " + updatecount);
            Log.i(TAG, "Failed Update (Offline): " + mytimestamp + "\nUpdate Count: " + updatecount);
        }


    }


    public static boolean isOnline(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            Log.i(TAG, "network state = true");
            //do more checking
            try {
                //check if we can dns weather.gov
                try {
                    Log.i(TAG, "trying to resolve weather.gov");
                    InetAddress Address = InetAddress.getByName("weather.gov");
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    Log.i(TAG, "cant resolve weather.gov");
                    return false;
                }
                try {
                    URL url = new URL("https://www.weather.gov/");
                    HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
                    try {
                        con.connect();
                        Log.i(TAG, "url connect response code: " + con.getResponseCode());
                        if (con.getResponseCode() == 200){
                            Log.i(TAG, "Connection test passed");
                            return true;
                        }
                    } catch (Exception e) {
                        Log.i(TAG, "con.connect() failed");
                        e.printStackTrace();
                        return false;
                    }
                } catch (Exception exception) {
                    Log.i(TAG, "Connection test failed");
                    exception.printStackTrace();
                    return false;
                }
            } catch (Exception e) {
                Log.i(TAG, "Connection test exception");
                e.printStackTrace();
                return false;
            }

        } else {
            Log.i(TAG, "network state = false");
            return false;
        }

        return false;
    }


    ///Simple online checker///
    public boolean isOnlineSimple(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            Log.i(TAG, "network state = true");
            return true;

        } else {
            Log.i(TAG, "network state = false");
            return false;
        }

    }


    ////gps stufff///
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(REQUESTING_UPDATES_KEY)) {
                mRequestingUpdates = savedInstanceState.getBoolean(REQUESTING_UPDATES_KEY);
            }
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                CurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
            lat = CurrentLocation.getLatitude();
            lon = CurrentLocation.getLongitude();
            mylat = String.valueOf(lat);
            mylon = String.valueOf(lon);
            mygps.setText("lat: " + mylat + " lon:" + mylon);
            //saveValues();
            //pushValuesToWearable();
            //make sure to setup latlon with weather url
            finalurl = url + "lat=" + mylat + "&lon=" + mylon + "&FcstType=json";
            visiturl = url + "lat=" + mylat + "&lon=" + mylon;
            Log.i(TAG, "updateValuesFromBundle finalurl: " + finalurl);
            Log.i(TAG, "updateValuesFromBundle lat: " + mylat + " lon: " + mylon);

        }
    }


    private void initializeGoogleAPI() {
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
                == ConnectionResult.SUCCESS) {
            GoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(Wearable.API)
                    .build();
            createLocationRequest();
        }
    }


    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(GPSUPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void startGPSUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(GoogleApiClient, mLocationRequest, this);
        mRequestingUpdates = true;
        Log.d(TAG, "GPS updates started");

    }

    private void stopGPSUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(GoogleApiClient, this);
        mRequestingUpdates = false;
        Log.d(TAG, "GPS updates stopped");
    }



    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");
        if (CurrentLocation == null) {
            CurrentLocation = LocationServices.FusedLocationApi.getLastLocation(GoogleApiClient);
            lat = CurrentLocation.getLatitude();
            lon = CurrentLocation.getLongitude();
            mylat = String.valueOf(lat);
            mylon = String.valueOf(lon);
            mygps.setText("lat: " + mylat + " lon:" + mylon);
            //make sure to setup latlon with weather url
            finalurl = url + "lat=" + mylat + "&lon=" + mylon + "&FcstType=json";
            visiturl = url + "lat=" + mylat + "&lon=" + mylon;
            Log.i(TAG, "onConnected finalurl: " + finalurl);
            Log.i(TAG, "onConnected lat: " + mylat + " lon: " + mylon);
        }
        if (mRequestingUpdates) {
            startGPSUpdates();
        }
    }


    @Override
    public void onLocationChanged(Location location) {
        CurrentLocation = location;
        lat = CurrentLocation.getLatitude();
        lon = CurrentLocation.getLongitude();
        mylat = String.valueOf(lat);
        mylon = String.valueOf(lon);
        mygps.setText("lat: " + mylat + " lon:" + mylon);
        //make sure to setup latlon with weather url
        finalurl = url + "lat=" + mylat + "&lon=" + mylon + "&FcstType=json";
        visiturl = url + "lat=" + mylat + "&lon=" + mylon;
        Log.i(TAG, "onLocationChanged finalurl: " + finalurl);
        Log.i(TAG, "onLocationChanged lat: " + mylat + " lon: " + mylon);

    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "Connection suspended");
        GoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_UPDATES_KEY, mRequestingUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, CurrentLocation);
        super.onSaveInstanceState(savedInstanceState);
    }
/////end of gps stuff//////





}
