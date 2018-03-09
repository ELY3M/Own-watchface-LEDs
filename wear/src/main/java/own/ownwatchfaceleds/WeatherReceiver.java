package own.ownwatchfaceleds;


import android.net.Uri;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class WeatherReceiver extends WearableListenerService {
    private static final String TAG = "ownwatchface wxrec";

    private GoogleApiClient mGoogleApiClient;
    private static String mytemp;
    private static String myicon;
    private static String myweather;
    private static boolean alreadyInitialize;
    private static String path;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG, "onMessageReceived: " + messageEvent);

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .build();
        }

        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();

        DataMap dataMap = DataMap.fromByteArray(messageEvent.getData());

        path = messageEvent.getPath();
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);
        DataMap config = putDataMapRequest.getDataMap();

        if (path.equals(Settings.PATH_WEATHER_INFO)) {

            if (dataMap.containsKey(Settings.KEY_TEMP)) {
                mytemp = dataMap.getString(Settings.KEY_TEMP);
            }

            if (dataMap.containsKey(Settings.KEY_ICON)) {
                myicon = dataMap.getString(Settings.KEY_ICON);
            }

            if (dataMap.containsKey(Settings.KEY_WEATHER)) {
                myweather = dataMap.getString(Settings.KEY_WEATHER);
            }



            config.putLong(Settings.KEY_WEATHER_UPDATE_TIME, System.currentTimeMillis());
            config.putString(Settings.KEY_TEMP, mytemp);
            config.putString(Settings.KEY_ICON, myicon);
            config.putString(Settings.KEY_WEATHER, myweather);

            Log.i(TAG, "rec mytemp: " + mytemp);
            Log.i(TAG, "rec myweather: " + myweather);
            Log.i(TAG, "rec myicon: " + myicon);

        } else {
            if (!alreadyInitialize) {
                Wearable.NodeApi.getLocalNode(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetLocalNodeResult>() {
                    @Override
                    public void onResult(NodeApi.GetLocalNodeResult getLocalNodeResult) {
                        Uri uri = new Uri.Builder()
                                .scheme("wear")
                                .path(path)
                                .authority(getLocalNodeResult.getNode().getId())
                                .build();

                        Wearable.DataApi.getDataItem(mGoogleApiClient, uri)
                                .setResultCallback(
                                        new ResultCallback<DataApi.DataItemResult>() {
                                            @Override
                                            public void onResult(DataApi.DataItemResult dataItemResult) {
                                                if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
                                                    fetchConfig(DataMapItem.fromDataItem(dataItemResult.getDataItem()).getDataMap());
                                                }

                                                alreadyInitialize = true;
                                            }
                                        }
                                );
                    }
                });

                while (!alreadyInitialize) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }


        }

        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataMapRequest.asPutDataRequest())
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        Log.d(TAG, "SaveConfig: " + dataItemResult.getStatus() + ", " + dataItemResult.getDataItem().getUri());

                        mGoogleApiClient.disconnect();
                    }
                });
    }

    protected void fetchConfig(DataMap config) {
        if (config.containsKey(Settings.KEY_TEMP)) {
            mytemp = config.getString(Settings.KEY_TEMP);
        }

        if (config.containsKey(Settings.KEY_ICON)) {
            myicon = config.getString(Settings.KEY_ICON);
        }

        if (config.containsKey(Settings.KEY_WEATHER)) {
            myweather = config.getString(Settings.KEY_WEATHER);
        }


    }
}

