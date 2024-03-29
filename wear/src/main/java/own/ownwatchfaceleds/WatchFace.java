package own.ownwatchfaceleds;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.NodeClient;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.SimpleTimeZone;
import java.util.TimeZone;


public class WatchFace extends CanvasWatchFaceService  {

    private static final String TAG = "ownwatchface watchface";

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    private class Engine extends CanvasWatchFaceService.Engine implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener /*, NodeApi.NodeListener*/ {



        protected static final int MSG_UPDATE_TIME = 0;
        protected long UPDATE_RATE_MS = 500;
        protected static final long WEATHER_INFO_TIME_OUT = DateUtils.HOUR_IN_MILLIS * 6;

        final BroadcastReceiver TimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };


        /**
         * Handler to update the time periodically in interactive mode.
         */
        protected final Handler mUpdateTimeHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_UPDATE_TIME:
                        invalidate();

                        if (shouldUpdateTimerBeRunning()) {
                            long timeMs = System.currentTimeMillis();
                            long delayMs = UPDATE_RATE_MS - (timeMs % UPDATE_RATE_MS);
                            mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
                            requireWeatherInfo();
                        }
                        break;
                }
            }
        };



        Resources resources = WatchFace.this.getResources();

        Paint mBackgroundPaint;
        Bitmap mBackgroundBitmap = BitmapFactory.decodeResource(resources, R.drawable.back);
        Bitmap mBackgroundScaledBitmap;

        float mXOffset;
        float mYOffset;
        float mPadding;
        int width;
        int height;
        float centerX;
        float centerY;

        SimpleDateFormat timeSdf;
        SimpleDateFormat timenosecsSdf;
        SimpleDateFormat periodSdf;
        SimpleDateFormat dateStampSdf;
        SimpleDateFormat timeStampSdf;


        Paint mClockPaint;
        Paint mClocknosecsPaint;
        Paint mPeriodPaint;
        Paint mDatestampPaint;
        Paint mTimestampPaint;
        Paint mTempPaint;
        Paint mWeatherPaint;

        int mInteractiveTextColor = getResources().getColor(R.color.aqua);
        int mAmbientTextColor = getResources().getColor(R.color.aqua);
        int mBackgroundColor = getResources().getColor(R.color.black);


        boolean mLowBitAmbient;
        boolean mRegisteredService = false;


        String lat = "0.0";
        String lon = "0.0";
        String temp = "103";
        String icon = "unknown";
        String finalicon;
        String weather = "unknown";
        String TempString = temp;
        String WeatherString = weather;

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
        public boolean temponright = false;
        public boolean alwaysUtc = true;
        public boolean showtime = false;
        public boolean northernhemi = true;
        public boolean roundwatch = false;


        boolean mTimeZoneReceiver = false;
        Date mDate;

        Rect cardPeekRectangle = new Rect(0, 0, 0, 0);



        protected long mWeatherInfoReceivedTime;
        protected long mWeatherInfoRequiredTime;
        int mWeatherRequireInterval;




        Typeface BOLD_TYPEFACE =
                Typeface.createFromAsset(resources.getAssets(), "fonts/DS-DIGIB.TTF");

        Typeface NORMAL_TYPEFACE =
                Typeface.createFromAsset(resources.getAssets(), "fonts/DS-DIGI.TTF");

        Typeface ITALIC_TYPEFACE =
                Typeface.createFromAsset(resources.getAssets(), "fonts/DS-DIGIT.TTF");

        Typeface PIXELLCD =
                Typeface.createFromAsset(resources.getAssets(), "fonts/PixelLCD-7.ttf");

        Calendar mCalendar;
        private static final String TIME_FORMAT_12_NOSECS = "h:mm";
        private static final String TIME_FORMAT_12 = "h:mm:ss";
        private static final String TIME_FORMAT_24_NOSECS = "H:mm";
        private static final String TIME_FORMAT_24 = "H:mm:ss";
        private static final String PERIOD_FORMAT = "a";
        private static final String DATESTAMP_FORMAT = "EEE MMM, dd yyyy";
        private static final String TIMESTAMP_FORMAT = "HH:mm:ss zzz";
        private static final String TIMEZONE_FORMAT = "zzz";


        private static final double MOON_PHASE_LENGTH = 29.530588853;
        private Calendar moonCalendar;





        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(WatchFace.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();


        @Override
        public void onConnected(Bundle bundle) {
            Log.d(TAG, "Connected: " + bundle);
            getWeather();
            getConfig();
            //Wearable.NodeApi.addListener(mGoogleApiClient, this);
            Wearable.DataApi.addListener(mGoogleApiClient, this);
            requireWeatherInfo();
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, " ConnectionSuspended: " + i);
        }

/*
        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
            Log.d(TAG, "onDataChanged");
            try {
                for (DataEvent dataEvent : dataEvents) {
                    if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                        continue;
                    }

                    DataItem dataItem = dataEvent.getDataItem();
                    if (!dataItem.getUri().getPath().equals(Settings.PATH_CONFIG)) {
                        continue;
                    }
                    if (!dataItem.getUri().getPath().equals(Settings.PATH_WEATHER_INFO)) {
                        continue;
                    }

                    DataMapItem dataMapItem = DataMapItem.fromDataItem(dataItem);
                    DataMap config = dataMapItem.getDataMap();
                    Log.d(TAG, "Config DataItem updated:" + config);
                    if (config != null && !config.isEmpty()) {
                        //updateUiForConfigDataMap(config);
                        fetchWeather(config);
                        fetchConfig(config);
                    }
                }
            } finally {
                dataEvents.close();
            }
        }
        */


        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {
                Log.d(TAG, "onDataChanged");
            for (int i = 0; i < dataEvents.getCount(); i++) {
                DataEvent event = dataEvents.get(i);
                DataMap dataMap = DataMap.fromByteArray(event.getDataItem().getData());
                Log.d(TAG, "onDataChanged: " + dataMap);

                fetchWeather(dataMap);
                fetchConfig(dataMap);
            }

        }



        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            Log.d(TAG, "ConnectionFailed: " + connectionResult);

        }

        protected Paint createTextPaint(int color, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(color);
            if (typeface != null)
                paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
        }

        private void updateTimeZone(TimeZone tz) {
            timeSdf.setTimeZone(tz);
            timenosecsSdf.setTimeZone(tz);
            periodSdf.setTimeZone(tz);
            dateStampSdf.setTimeZone(tz);
            if (alwaysUtc) {
                timeStampSdf.setTimeZone(new SimpleTimeZone(0, "UTC"));
            } else {
                timeStampSdf.setTimeZone(tz);
            }
            mDate.setTime(System.currentTimeMillis());
        }

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            mCalendar = Calendar.getInstance();

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFace.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setHotwordIndicatorGravity(Gravity.BOTTOM | Gravity.RIGHT)
                    .setStatusBarGravity(Gravity.BOTTOM | Gravity.LEFT)
                    .setShowSystemUiTime(false)
                    .setAcceptsTapEvents(true)
                    .build());

/* from my fav watch face
            xxxxxxs = new WatchFaceStyle.Builder(WeatherWatchFaceService.this)
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_VISIBLE)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setHotwordIndicatorGravity(Gravity.BOTTOM | Gravity.RIGHT)
                    .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
                    .setShowSystemUiTime(false)
                    .setShowUnreadCountIndicator(false)
                    .setStatusBarGravity(Gravity.BOTTOM | Gravity.LEFT)
                    .setViewProtection(WatchFaceStyle.PROTECT_STATUS_BAR)
                    .build();
*/

            resources = WatchFace.this.getResources();

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(Color.BLACK);


            if (DateFormat.is24HourFormat(getApplicationContext())) {
                timeSdf = new SimpleDateFormat(TIME_FORMAT_24);
                timenosecsSdf = new SimpleDateFormat(TIME_FORMAT_24_NOSECS);
            } else {
                timeSdf = new SimpleDateFormat(TIME_FORMAT_12);
                timenosecsSdf = new SimpleDateFormat(TIME_FORMAT_12_NOSECS);
            }

            //date formatters
            periodSdf = new SimpleDateFormat(PERIOD_FORMAT);
            dateStampSdf = new SimpleDateFormat(DATESTAMP_FORMAT);
            timeStampSdf = new SimpleDateFormat(TIMESTAMP_FORMAT);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mBackgroundColor);
            mClockPaint = createTextPaint(mInteractiveTextColor, PIXELLCD);
            mClocknosecsPaint = createTextPaint(mInteractiveTextColor, PIXELLCD);
            mPeriodPaint = createTextPaint(mInteractiveTextColor, PIXELLCD);
            mDatestampPaint = createTextPaint(mInteractiveTextColor, NORMAL_TYPEFACE);
            mTimestampPaint = createTextPaint(mInteractiveTextColor, NORMAL_TYPEFACE);
            mTempPaint = createTextPaint(mInteractiveTextColor, NORMAL_TYPEFACE);
            mWeatherPaint = createTextPaint(mInteractiveTextColor, NORMAL_TYPEFACE);

            // initial setup, load persisted or default values, can be overridden by companion app
            Context context = getApplicationContext();
            clockSize = Settings.getInt(context, Settings.KEY_CLOCK_SIZE, clockSize);
            clocknosecsSize = Settings.getInt(context, Settings.KEY_CLOCK_NOSECS_SIZE, clocknosecsSize);
            markerSize = Settings.getInt(context, Settings.KEY_MARKER_SIZE, markerSize);
            dateSize = Settings.getInt(context, Settings.KEY_DATE_SIZE, dateSize);
            timeSize = Settings.getInt(context, Settings.KEY_TIME_SIZE, timeSize);
            tempSize = Settings.getInt(context, Settings.KEY_TEMP_SIZE, tempSize);
            weatherSize = Settings.getInt(context, Settings.KEY_WEATHER_SIZE, weatherSize);

            //get gps
            lat = Settings.getString(context, Settings.KEY_LAT, lat);
            lon = Settings.getString(context, Settings.KEY_LON, lon);

            //get weather
            temp = Settings.getString(context, Settings.KEY_TEMP, temp);
            icon = Settings.getString(context, Settings.KEY_ICON, icon);
            weather = Settings.getString(context, Settings.KEY_WEATHER, weather);


            Log.i(TAG, "oncreate firstget Weather: Temp: " + temp + " icon: " + icon + " Weather: " + weather + " EOF");

            // set the text sizes scaled according to the screen density
            float density = getResources().getDisplayMetrics().density;
            mClockPaint.setTextSize(clockSize * density);
            mClocknosecsPaint.setTextSize(clocknosecsSize * density);
            mPeriodPaint.setTextSize(markerSize * density);
            mDatestampPaint.setTextSize(dateSize * density);
            mTimestampPaint.setTextSize(timeSize * density);
            mTempPaint.setTextSize(tempSize * density);
            mWeatherPaint.setTextSize(weatherSize * density);


            clockAct = Settings.getBoolean(context, Settings.KEY_CLOCK_ACT, clockAct);
            clockDim = Settings.getBoolean(context, Settings.KEY_CLOCK_DIM, clockDim);
            clocknosecsAct = Settings.getBoolean(context, Settings.KEY_CLOCK_NOSECS_ACT, clocknosecsAct);
            clocknosecsDim = Settings.getBoolean(context, Settings.KEY_CLOCK_NOSECS_DIM, clocknosecsDim);
            markerDim = Settings.getBoolean(context, Settings.KEY_MARKER_DIM, markerDim);
            dateDim = Settings.getBoolean(context, Settings.KEY_DATE_DIM, dateDim);
            timeDim = Settings.getBoolean(context, Settings.KEY_TIME_DIM, timeDim);
            tempDim = Settings.getBoolean(context, Settings.KEY_TEMP_DIM, tempDim);
            weatherDim = Settings.getBoolean(context, Settings.KEY_WEATHER_DIM, weatherDim);
            temponright = Settings.getBoolean(context, Settings.KEY_TEMPONRIGHT, temponright);
            alwaysUtc = Settings.getBoolean(context, Settings.KEY_ALWAYS_UTC, alwaysUtc);
            showtime = Settings.getBoolean(context, Settings.KEY_SHOW_TIME, showtime);
            northernhemi = Settings.getBoolean(context, Settings.KEY_NORTHERNHEMI, northernhemi);
            roundwatch = Settings.getBoolean(context, Settings.KEY_ROUNDWATCH, roundwatch);

            Log.d(TAG, "clockSize: "+clockSize);
            Log.d(TAG, "clocknosecsSize: "+clocknosecsSize);

            Log.d(TAG, "showtime: "+showtime);
            Log.d(TAG, "northernhemi: "+northernhemi);
            Log.d(TAG, "roundwatch: "+roundwatch);

            mDate = new Date();

            //weather service timer!!!//
            mWeatherRequireInterval = Settings.weatherinterval;
            mWeatherInfoRequiredTime = System.currentTimeMillis() - (DateUtils.SECOND_IN_MILLIS * 58);

            mGoogleApiClient.connect();
        }


        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            Log.d(TAG, "onTapCommand tapType: "+tapType+ " x: "+x+" y: "+y+" eventTime: "+eventTime);
            switch(tapType) {
                case TAP_TYPE_TAP:
                        //manual update weather
                    if (x > 3 && y > 143 && y < 263) {
                        Log.d(TAG, "onTapCommand Update Weather");
                        ManualWeatherUpdate();
                    }
                        //x 30-width
                        //y 263-height
                        // toggle UTC display
                    if (x > 3 && y > 263) {
                        Log.d(TAG, "onTapCommand toggle UTC display");
                        if (!showtime) {
                            Log.d(TAG, "Tap showtime is true");
                            showtime = true;
                        } else {
                            Log.d(TAG, "Tap showtime is false");
                            showtime = false;
                        }
                    }
                    invalidate();
                    break;
                default:
                    super.onTapCommand(tapType, x, y, eventTime);
                    break;
            }
        }



        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            Log.d(TAG, "onAmbientModeChanged: " + inAmbientMode);

            adjustPaintColorToCurrentMode(mClockPaint, mInteractiveTextColor, mAmbientTextColor);
            adjustPaintColorToCurrentMode(mClocknosecsPaint, mInteractiveTextColor, mAmbientTextColor);
            adjustPaintColorToCurrentMode(mPeriodPaint, mInteractiveTextColor, mAmbientTextColor);
            adjustPaintColorToCurrentMode(mTimestampPaint, mInteractiveTextColor, mAmbientTextColor);
            adjustPaintColorToCurrentMode(mTempPaint, mInteractiveTextColor, mAmbientTextColor);
            adjustPaintColorToCurrentMode(mWeatherPaint, mInteractiveTextColor, mAmbientTextColor);

            // these are always ambient
            //adjustPaintColorToCurrentMode(mDatestampPaint, mInteractiveTextColor, mAmbientTextColor);
            //adjustPaintColorToCurrentMode(mTimestampPaint, mInteractiveTextColor, mAmbientTextColor);

            // When this property is set to true, the screen supports fewer bits for each color in
            // ambient mode. In this case, watch faces should disable anti-aliasing in ambient mode.
            if (mLowBitAmbient) {
                boolean antiAlias = !inAmbientMode;
                mDatestampPaint.setAntiAlias(antiAlias);
                mClockPaint.setAntiAlias(antiAlias);
                mClocknosecsPaint.setAntiAlias(antiAlias);
                mPeriodPaint.setAntiAlias(antiAlias);
                mTimestampPaint.setAntiAlias(antiAlias);
                mTempPaint.setAntiAlias(antiAlias);
                mWeatherPaint.setAntiAlias(antiAlias);

            }

            invalidate();

            // Whether the timer should be running depends on whether we're in ambient mode (as well
            // as whether we're visible), so we may need to start or stop the timer.
            updateTimer();
        }

        private void adjustPaintColorToCurrentMode(Paint paint, int interactiveColor,
                                                   int ambientColor) {
            paint.setColor(isInAmbientMode() ? ambientColor : interactiveColor);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            Log.d(TAG, "onDraw");
            mDate.setTime(System.currentTimeMillis());

            mXOffset = 10;
            mYOffset = 10;
            mPadding = 5;

            mDatestampPaint.setTextAlign(Paint.Align.CENTER);
            mClockPaint.setTextAlign(Paint.Align.LEFT);
            mClocknosecsPaint.setTextAlign(Paint.Align.CENTER);
            mPeriodPaint.setTextAlign(Paint.Align.RIGHT);
            mTimestampPaint.setTextAlign(Paint.Align.CENTER);
            mTempPaint.setTextAlign(Paint.Align.CENTER);
            mWeatherPaint.setTextAlign(Paint.Align.CENTER);

            width = bounds.width();
            height = bounds.height();
            centerX = width / 2f;
            centerY = height / 2f;

            // Draw the background.
            if (isInAmbientMode()) {
                // black background
                canvas.drawRect(0, 0, width, height, mBackgroundPaint);
            } else {
                if (mBackgroundScaledBitmap == null
                        || mBackgroundScaledBitmap.getWidth() != width
                        || mBackgroundScaledBitmap.getHeight() != height) {
                    mBackgroundScaledBitmap = Bitmap
                            .createScaledBitmap(mBackgroundBitmap, width, height, true);
                }
                // fancy image background
                canvas.drawBitmap(mBackgroundScaledBitmap, 0, 0, null);
                ///moon
                moonupdate(canvas);
                //clockleds
                clockleds(canvas, bounds);
                //weather
                weathericon(canvas);



            }


            // Update the strings
            String datestampString = dateStampSdf.format(mDate);
            String clockString = timeSdf.format(mDate);
            String clocknosecsString = timenosecsSdf.format(mDate);
            String periodString = periodSdf.format(mDate);
            String timestampString = timeStampSdf.format(mDate);
            TempString = temp;
            WeatherString = weather;

            Log.d(TAG, "onDraw Temp: "+temp);
            Log.d(TAG, "onDraw icon: "+icon);
            Log.d(TAG, "onDraw Weather: "+weather);

            float xClock, yClock;
            float xClocknosecs, yClocknosecs;
            float xPeriod, yPeriod;
            float xDatestamp, yDatestamp;
            float xTimestamp, yTimestamp;
            float xTemp, yTemp;
            float xWeather, yWeather;


            if (!roundwatch) {
                xDatestamp = width / 2f;
                yDatestamp = 25;
            } else {
                xDatestamp = width / 2f;
                yDatestamp = 75;
            }

            xPeriod = width - mPadding;
            yPeriod = height / 2f;

            ///xClock = width / 2f;
            xClock = mPadding;
            yClock = height / 2f;
            xClocknosecs = (width / 2f) - 30;
            yClocknosecs = height / 2f;

            xTimestamp = width / 2f;

            if (!roundwatch) {
                yTimestamp = height - 4;
            } else {
                yTimestamp = height - 18;
            }

            if (!roundwatch) {
                if (temponright) {
                    xTemp = (width / 2f) + 90;
                    yTemp = height - 80;
                } else {
                    xTemp = 50;
                    yTemp = height - 80;
                }
            } else {

                if (temponright) {
                    xTemp = (width / 2f) + 120;
                    yTemp = height - 130;
                } else {
                    xTemp = 93;
                    yTemp = height - 130;
                }
            }
            xWeather = width / 2f;
            if (!roundwatch) {
                yWeather = height - 30;
            } else {
                yWeather = height / 2f + 160;
            }


            if (cardPeekRectangle.top == 0) {
                cardPeekRectangle.top = height;
            }

            if (isInAmbientMode()) {
                // draw these when ambient
                if (clockDim) {
                    if (yClock < cardPeekRectangle.top) {
                        canvas.drawText(clockString, xClock, yClock, mClockPaint);
                    }
                }
                if (clocknosecsDim) {
                    if (yClocknosecs < cardPeekRectangle.top) {
                        canvas.drawText(clocknosecsString, xClocknosecs, yClocknosecs, mClocknosecsPaint);
                    }
                }
                if (markerDim) {
                    if (!DateFormat.is24HourFormat(getApplicationContext())) {
                        if (yPeriod < cardPeekRectangle.top) {
                            canvas.drawText(periodString, xPeriod, yPeriod, mPeriodPaint);
                        }
                    }
                }

                if (dateDim) {
                    if (yDatestamp < cardPeekRectangle.top) {
                        canvas.drawText(datestampString, xDatestamp, yDatestamp, mDatestampPaint);
                    }
                }

                if (tempDim) {
                    if (yTemp < cardPeekRectangle.top) {
                        canvas.drawText(TempString, xTemp, yTemp, mTempPaint);
                    }
                }

                if (weatherDim) {
                    if (yWeather < cardPeekRectangle.top) {
                        canvas.drawText(WeatherString, xWeather, yWeather, mWeatherPaint);
                    }
                }

                if (timeDim) {
                    if (yTimestamp < cardPeekRectangle.top) {
                        canvas.drawText(timestampString, xTimestamp, yTimestamp, mTimestampPaint);
                    }
                }


            } else {
                // draw these when interactive
                if (clockAct) {
                    if (yClock < cardPeekRectangle.top) {
                        canvas.drawText(clockString, xClock, yClock, mClockPaint);
                    }
                }
                if (clocknosecsAct) {
                    if (yClocknosecs < cardPeekRectangle.top) {
                        canvas.drawText(clocknosecsString, xClocknosecs, yClocknosecs, mClocknosecsPaint);
                    }
                }

                if (markerAct) {
                    if (!DateFormat.is24HourFormat(getApplicationContext())) {
                        if (yPeriod < cardPeekRectangle.top) {
                            canvas.drawText(periodString, xPeriod, yPeriod, mPeriodPaint);
                        }
                    }
                }

                if (yDatestamp < cardPeekRectangle.top) {
                    canvas.drawText(datestampString, xDatestamp, yDatestamp, mDatestampPaint);
                }

                if (yTemp < cardPeekRectangle.top) {
                    canvas.drawText(TempString, xTemp, yTemp, mTempPaint);
                }

                if (yWeather < cardPeekRectangle.top) {
                    canvas.drawText(WeatherString, xWeather, yWeather, mWeatherPaint);
                }

                Log.d(TAG,"onDraw showtime: "+showtime);
                if (showtime) {
                    if (yTimestamp < cardPeekRectangle.top) {
                        canvas.drawText(timestampString, xTimestamp, yTimestamp, mTimestampPaint);
                    }
                }

            }




        }


        @Override
        public void onDestroy() {
            Log.d(TAG, "Destroy");
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onInterruptionFilterChanged(int interruptionFilter) {
            super.onInterruptionFilterChanged(interruptionFilter);

            Log.d(TAG, "onInterruptionFilterChanged: " + interruptionFilter);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false);

            Log.d(TAG, "onPropertiesChanged: LowBitAmbient=" + mLowBitAmbient);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            Log.d(TAG, "TimeTick");
            invalidate();
            requireWeatherInfo();
            getConfig();
        }


        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            Log.d(TAG, "onVisibilityChanged: " + visible);

            if (visible) {
                mGoogleApiClient.connect();
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                updateTimeZone(TimeZone.getDefault());
                mDate.setTime(System.currentTimeMillis());
            } else {
                unregisterReceiver();
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    //Wearable.NodeApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        protected boolean shouldUpdateTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }


        protected void fetchConfig(DataMap config) {


            Log.d(TAG, "fetchConfig(DataMap config)");

            if (config.containsKey(Settings.KEY_CLOCK_SIZE)) {
                clockSize = config.getInt(Settings.KEY_CLOCK_SIZE);
                Log.d(TAG, "clockSize: "+clockSize);

            }
            if (config.containsKey(Settings.KEY_CLOCK_NOSECS_SIZE)) {
                clocknosecsSize = config.getInt(Settings.KEY_CLOCK_NOSECS_SIZE);
                Log.d(TAG, "clocknosecsSize: "+clocknosecsSize);

            }
            if (config.containsKey(Settings.KEY_MARKER_SIZE)) {
                markerSize = config.getInt(Settings.KEY_MARKER_SIZE);
                Log.d(TAG, "markerSize: "+markerSize);

            }
            if (config.containsKey(Settings.KEY_DATE_SIZE)) {
                dateSize = config.getInt(Settings.KEY_DATE_SIZE);
                Log.d(TAG, "dateSize: "+dateSize);

            }
            if (config.containsKey(Settings.KEY_TIME_SIZE)) {
                timeSize = config.getInt(Settings.KEY_TIME_SIZE);
                Log.d(TAG, "timeSize: "+timeSize);

            }
            if (config.containsKey(Settings.KEY_TEMP_SIZE)) {
                tempSize = config.getInt(Settings.KEY_TEMP_SIZE);
                Log.d(TAG, "tempSize: "+tempSize);

            }
            if (config.containsKey(Settings.KEY_WEATHER_SIZE)) {
                weatherSize = config.getInt(Settings.KEY_WEATHER_SIZE);
                Log.d(TAG, "weatherSize: "+weatherSize);

            }

            if (config.containsKey(Settings.KEY_CLOCK_ACT)) {
                clockAct = config.getBoolean(Settings.KEY_CLOCK_ACT);
                Log.d(TAG, "clockAct: "+clockAct);

            }
            if (config.containsKey(Settings.KEY_CLOCK_DIM)) {
                clockDim = config.getBoolean(Settings.KEY_CLOCK_DIM);
                Log.d(TAG, "clockDim: "+clockDim);

            }
            if (config.containsKey(Settings.KEY_CLOCK_NOSECS_ACT)) {
                clocknosecsAct = config.getBoolean(Settings.KEY_CLOCK_NOSECS_ACT);
                Log.d(TAG, "clocknosecsAct: "+clocknosecsAct);

            }
            if (config.containsKey(Settings.KEY_CLOCK_NOSECS_DIM)) {
                clocknosecsDim = config.getBoolean(Settings.KEY_CLOCK_NOSECS_DIM);
                Log.d(TAG, "clocknosecsDim: "+clocknosecsDim);

            }
            if (config.containsKey(Settings.KEY_MARKER_ACT)) {
                markerAct = config.getBoolean(Settings.KEY_MARKER_ACT);
                Log.d(TAG, "markerAct: "+markerAct);

            }
            if (config.containsKey(Settings.KEY_MARKER_DIM)) {
                markerDim = config.getBoolean(Settings.KEY_MARKER_DIM);
                Log.d(TAG, "markerDim: "+markerDim);

            }
            if (config.containsKey(Settings.KEY_DATE_DIM)) {
                dateDim = config.getBoolean(Settings.KEY_DATE_DIM);
                Log.d(TAG, "dateDim: "+dateDim);

            }
            if (config.containsKey(Settings.KEY_TIME_DIM)) {
                timeDim = config.getBoolean(Settings.KEY_TIME_DIM);
                Log.d(TAG, "timeDim: "+timeDim);

            }
            if (config.containsKey(Settings.KEY_TEMP_DIM)) {
                tempDim = config.getBoolean(Settings.KEY_TEMP_DIM);
                Log.d(TAG, "tempDim: "+tempDim);

            }
            if (config.containsKey(Settings.KEY_WEATHER_DIM)) {
                weatherDim = config.getBoolean(Settings.KEY_WEATHER_DIM);
                Log.d(TAG, "weatherDim: "+weatherDim);

            }
            if (config.containsKey(Settings.KEY_TEMPONRIGHT)) {
                temponright = config.getBoolean(Settings.KEY_TEMPONRIGHT);
                Log.d(TAG, "temponright: "+temponright);

            }
            if (config.containsKey(Settings.KEY_ALWAYS_UTC)) {
                alwaysUtc = config.getBoolean(Settings.KEY_ALWAYS_UTC);
                Log.d(TAG, "alwaysUtc: "+alwaysUtc);

            }
            if (config.containsKey(Settings.KEY_SHOW_TIME)) {
                showtime = config.getBoolean(Settings.KEY_SHOW_TIME);
                Log.d(TAG, "showtime: "+showtime);

            }
            if (config.containsKey(Settings.KEY_NORTHERNHEMI)) {
                northernhemi = config.getBoolean(Settings.KEY_NORTHERNHEMI);
                Log.d(TAG, "northernhemi: "+northernhemi);

            }

            if (config.containsKey(Settings.KEY_ROUNDWATCH)) {
                roundwatch = config.getBoolean(Settings.KEY_ROUNDWATCH);
                Log.d(TAG, "roundwatch: "+roundwatch);

            }

            Log.d(TAG, "fetchConfig clockSize: "+clockSize);
            Log.d(TAG, "fetchConfig clocknosecsSize: "+clocknosecsSize);


            Log.d(TAG, "fetchConfig clockAct: "+clockAct);
            Log.d(TAG, "fetchConfig clockdim: "+clockDim);

            Log.d(TAG, "fetchConfig markerAct: "+markerAct);
            Log.d(TAG, "fetchConfig markerDim: "+markerDim);

            Log.d(TAG, "fetchConfig showtime: "+showtime);
            Log.d(TAG, "fetchConfig northernhemi: "+northernhemi);
            Log.d(TAG, "fetchConfig roundwatch: "+roundwatch);

            invalidate();
        }


        protected void fetchWeather(DataMap config) {

            Log.d(TAG, "fetchWeather(DataMap config)");

            if (config.containsKey(Settings.KEY_WEATHER_UPDATE_TIME)) {
                mWeatherInfoReceivedTime = config.getLong(Settings.KEY_WEATHER_UPDATE_TIME);
            }

            if (config.containsKey(Settings.KEY_TEMP)) {
                String gettemp = config.getString(Settings.KEY_TEMP);
                if (TextUtils.isEmpty(gettemp)) {
                    temp = "103";
                } else {
                    temp = gettemp;
                }
            }

            if (config.containsKey(Settings.KEY_ICON)) {
                String geticon = config.getString(Settings.KEY_ICON);
                if (TextUtils.isEmpty(geticon)) {
                    icon = "unknown";
                } else {
                    icon = geticon;
                }
            }

            if (config.containsKey(Settings.KEY_WEATHER)) {
                String getweather = config.getString(Settings.KEY_WEATHER);
                if (TextUtils.isEmpty(getweather)) {
                    weather = "unknown";
                } else {
                    weather = getweather;
                }
            }

            Log.d(TAG, "fetchWeather temp: "+temp);
            Log.d(TAG, "fetchWeather icon: "+icon);
            Log.d(TAG, "fetchWeather weather: "+weather);

            invalidate();
        }

        protected void getWeather() {
            Log.d(TAG, "Start getting Weather");
            Wearable.NodeApi.getLocalNode(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetLocalNodeResult>() {
                @Override
                public void onResult(NodeApi.GetLocalNodeResult getLocalNodeResult) {
                    Uri uri = new Uri.Builder()
                            .scheme("wear")
                            .path(Settings.PATH_WEATHER_INFO)
                            .authority(getLocalNodeResult.getNode().getId())
                            .build();

                    getWeather(uri);

                }
            });
        }

        protected void getConfig() {
            Log.d(TAG, "Start getting Config");
            Wearable.NodeApi.getLocalNode(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetLocalNodeResult>() {
                @Override
                public void onResult(NodeApi.GetLocalNodeResult getLocalNodeResult) {
                    Uri uri = new Uri.Builder()
                            .scheme("wear")
                            .path(Settings.PATH_CONFIG)
                            .authority(getLocalNodeResult.getNode().getId())
                            .build();

                    getConfig(uri);
                }
            });
        }

        protected void getWeather(Uri uri) {

            Wearable.DataApi.getDataItem(mGoogleApiClient, uri)
                    .setResultCallback(
                            new ResultCallback<DataApi.DataItemResult>() {
                                @Override
                                public void onResult(DataApi.DataItemResult dataItemResult) {
                                    Log.d(TAG, "Finish Weather: " + dataItemResult.getStatus());
                                    if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
                                        fetchWeather(DataMapItem.fromDataItem(dataItemResult.getDataItem()).getDataMap());
                                    }
                                }
                            }
                    );
        }

        protected void getConfig(Uri uri) {

            Wearable.DataApi.getDataItem(mGoogleApiClient, uri)
                    .setResultCallback(
                            new ResultCallback<DataApi.DataItemResult>() {
                                @Override
                                public void onResult(DataApi.DataItemResult dataItemResult) {
                                    Log.d(TAG, "Finish Config: " + dataItemResult.getStatus());
                                    if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
                                        fetchConfig(DataMapItem.fromDataItem(dataItemResult.getDataItem()).getDataMap());
                                    }
                                }
                            }
                    );
        }

        private void registerReceiver() {
            if (mTimeZoneReceiver) {
                return;
            }
            mTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            WatchFace.this.registerReceiver(TimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mTimeZoneReceiver) {
                return;
            }
            mTimeZoneReceiver = false;
            WatchFace.this.unregisterReceiver(TimeZoneReceiver);
        }

        protected void ManualWeatherUpdate() {
            Log.d(TAG, "ManualWeatherUpdate()");
            if (!mGoogleApiClient.isConnected()) {
                return;
            }
            mWeatherInfoRequiredTime = System.currentTimeMillis();
            Wearable.MessageApi.sendMessage(mGoogleApiClient, "", Settings.PATH_WEATHER_REQUIRE, null)
                    .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.d(TAG, " SendRequireMessage:" + sendMessageResult.getStatus());
                        }
                    });
        }

        protected void requireWeatherInfo() {
            Log.d(TAG, "requireWeatherInfo()");
            if (!mGoogleApiClient.isConnected()) {
                return;
            }

            long timeMs = System.currentTimeMillis();

            // The weather info is still up to date.
            if ((timeMs - mWeatherInfoReceivedTime) <= mWeatherRequireInterval) {
                return;
            }

            // Try once in a min.
            if ((timeMs - mWeatherInfoRequiredTime) <= DateUtils.MINUTE_IN_MILLIS) {
                return;
            }

            mWeatherInfoRequiredTime = timeMs;
            Wearable.MessageApi.sendMessage(mGoogleApiClient, "", Settings.PATH_WEATHER_REQUIRE, null)
                    .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            Log.d(TAG, " SendRequireMessage:" + sendMessageResult.getStatus());
                        }
                    });
        }



        protected void unregisterTimeZoneService() {
            if (!mRegisteredService) {
                return;
            }
            mRegisteredService = false;

            //TimeZone
            WatchFace.this.unregisterReceiver(TimeZoneReceiver);
        }

        protected void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldUpdateTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }


        /////moon stuff/////
        public void moonupdate(Canvas canvas) {
            Resources resources = WatchFace.this.getResources();
            Bitmap mMoonBitmap;
            Bitmap mMoonResizedBitmap;
            northernhemi = Settings.getBoolean(getApplicationContext(), Settings.KEY_NORTHERNHEMI, northernhemi);

            double phase = computeMoonPhase();
            Log.i(TAG, "Computed moon phase: " + phase);

            int phaseValue = ((int) Math.floor(phase)) % 30;
            Log.i(TAG, "Discrete phase value: " + phaseValue);

            Drawable moonDrawable = resources.getDrawable(IMAGE_LOOKUP[phaseValue]);

            int moonleft = 23;
            int moontop = 33;

            if (roundwatch) {
                moonleft = 75;
                moontop = 90;
            }


            if (northernhemi) {
                mMoonBitmap = ((BitmapDrawable) moonDrawable).getBitmap();
                mMoonResizedBitmap = Bitmap.createScaledBitmap(mMoonBitmap, 73, 73, false);
                canvas.drawBitmap(mMoonResizedBitmap, moonleft, moontop, null);
            } else {
                Matrix matrix = new Matrix();
                matrix.postRotate(180);
                mMoonBitmap = ((BitmapDrawable) moonDrawable).getBitmap();
                mMoonResizedBitmap = Bitmap.createScaledBitmap(mMoonBitmap, 73, 73, false);
                Bitmap mMoonrotatedBitmap = Bitmap.createBitmap(mMoonResizedBitmap, 0, 0, mMoonResizedBitmap.getWidth(), mMoonResizedBitmap.getHeight(), matrix, true);
                canvas.drawBitmap(mMoonrotatedBitmap, moonleft, moontop, null);

            }
        }

        /* not all fucking watches have gps :(
                private boolean isNorthernHemi() {
                    LocationManager locationManager =
                            (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    Location location =
                            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                    if (location != null) {
                        return location.getLongitude() > 0;
                    }

                    return false;
                }
        */
        // Computes moon phase based upon Bradley E. Schaefer's moon phase algorithm.
        private double computeMoonPhase() {
            moonCalendar = Calendar.getInstance();
            int year = moonCalendar.get(Calendar.YEAR);
            int month = moonCalendar.get(Calendar.MONTH) + 1;
            int day = moonCalendar.get(Calendar.DAY_OF_MONTH);
            int minute = moonCalendar.get(Calendar.MINUTE); ///for testing///
            Log.i(TAG, "year: " + year + " month: " + month + " day: " + day);
            Log.i(TAG, "test: " + minute); ///testing///
            // Convert the year into the format expected by the algorithm.
            double transformedYear = year - Math.floor((12 - month) / 10);
            Log.i(TAG, "transformedYear: " + transformedYear);

            // Convert the month into the format expected by the algorithm.
            int transformedMonth = month + 9;
            if (transformedMonth >= 12) {
                transformedMonth = transformedMonth - 12;
            }
            Log.i(TAG, "transformedMonth: " + transformedMonth);

            // Logic to compute moon phase as a fraction between 0 and 1
            double term1 = Math.floor(365.25 * (transformedYear + 4712));
            double term2 = Math.floor(30.6 * transformedMonth + 0.5);
            double term3 = Math.floor(Math.floor((transformedYear / 100) + 49) * 0.75) - 38;

            double intermediate = term1 + term2 + day + 59;
            if (intermediate > 2299160) {
                intermediate = intermediate - term3;
            }
            Log.i(TAG, "intermediate: " + intermediate);

            double normalizedPhase = (intermediate - 2451550.1) / MOON_PHASE_LENGTH;
            normalizedPhase = normalizedPhase - Math.floor(normalizedPhase);
            if (normalizedPhase < 0) {
                normalizedPhase = normalizedPhase + 1;
            }
            Log.i(TAG, "normalizedPhase: " + normalizedPhase);

            // Return the result as a value between 0 and MOON_PHASE_LENGTH
            return normalizedPhase * MOON_PHASE_LENGTH;
        }

        private final int[] IMAGE_LOOKUP = {
                R.drawable.moon0,
                R.drawable.moon1,
                R.drawable.moon2,
                R.drawable.moon3,
                R.drawable.moon4,
                R.drawable.moon5,
                R.drawable.moon6,
                R.drawable.moon7,
                R.drawable.moon8,
                R.drawable.moon9,
                R.drawable.moon10,
                R.drawable.moon11,
                R.drawable.moon12,
                R.drawable.moon13,
                R.drawable.moon14,
                R.drawable.moon15,
                R.drawable.moon16,
                R.drawable.moon17,
                R.drawable.moon18,
                R.drawable.moon19,
                R.drawable.moon20,
                R.drawable.moon21,
                R.drawable.moon22,
                R.drawable.moon23,
                R.drawable.moon24,
                R.drawable.moon25,
                R.drawable.moon26,
                R.drawable.moon27,
                R.drawable.moon28,
                R.drawable.moon29,
        };
////  end of moon stuff /////


        ///clockleds
        public void clockleds(Canvas canvas, Rect bounds) {
            int width = bounds.width();
            int height = bounds.height();
            float centerX = ((float) width) / 2.0f;
            float centerY = ((float) height) / 2.0f;
            float setclock = 3.0f;

            int clockspace = 1;
            int clockwidth = 33;
            int clockheight = 43;
            int colonwidth = 18;
            float clocktop = 113.0f;

        //I hate round watches!
        if (roundwatch) {
            Log.i(TAG, "My watch is round " + roundwatch);
            clockspace = 30;
            clockwidth = 43;
            clockheight = 53;
            colonwidth = 28;
            clocktop = 175.0f;
        }



            Resources resources = WatchFace.this.getResources();
            String textTime = new SimpleDateFormat("hh:mm:ss").format(new Date());
            int ampm = Calendar.getInstance().get(Calendar.AM_PM);
            String logstring = textTime + " " + ampm;
            Log.i(TAG, "my screen size: width: " + width + " height: " + height);
            Log.i(TAG, "My Clock Image built with " + logstring);


            Bitmap clearBitmap = ((BitmapDrawable) resources.getDrawable(R.drawable.nc)).getBitmap();
            Bitmap resizeclearBitmap = Bitmap.createScaledBitmap(clearBitmap, clockspace, clockheight, false);

            setclock = setclock + resizeclearBitmap.getWidth();
            Log.i(TAG, "setclock: " + setclock);

            Bitmap hour1Bitmap = ((BitmapDrawable) resources.getDrawable(this.CLOCKNUMBERS[Character.getNumericValue(textTime.charAt(0))])).getBitmap();
            Bitmap resizehour1Bitmap = Bitmap.createScaledBitmap(hour1Bitmap, clockwidth, clockheight, false);
            if (Character.getNumericValue(textTime.charAt(0)) == 1) {
                canvas.drawBitmap(resizehour1Bitmap, setclock, clocktop, null);
            } else {
                setclock = (float) (-(hour1Bitmap.getWidth() / 2));
            }
            Log.i(TAG, "bitmap width: " + hour1Bitmap.getWidth());

            Bitmap resizehour2Bitmap = Bitmap.createScaledBitmap(((BitmapDrawable) resources.getDrawable(this.CLOCKNUMBERS[Character.getNumericValue(textTime.charAt(1))])).getBitmap(), clockwidth, clockheight, false);
            canvas.drawBitmap(resizehour2Bitmap, ((float) resizehour1Bitmap.getWidth()) + setclock, clocktop, null);
            Bitmap resizecolonBitmap = Bitmap.createScaledBitmap(((BitmapDrawable) resources.getDrawable(R.drawable.dot)).getBitmap(), colonwidth, clockheight, false);
            canvas.drawBitmap(resizecolonBitmap, (((float) resizehour1Bitmap.getWidth()) + setclock) + ((float) resizehour2Bitmap.getWidth()), clocktop, null);
            Bitmap resizeminute1Bitmap = Bitmap.createScaledBitmap(((BitmapDrawable) resources.getDrawable(this.CLOCKNUMBERS[Character.getNumericValue(textTime.charAt(3))])).getBitmap(), clockwidth, clockheight, false);
            canvas.drawBitmap(resizeminute1Bitmap, ((((float) resizehour1Bitmap.getWidth()) + setclock) + ((float) resizehour2Bitmap.getWidth())) + ((float) resizecolonBitmap.getWidth()), clocktop, null);
            Bitmap resizeminute2Bitmap = Bitmap.createScaledBitmap(((BitmapDrawable) resources.getDrawable(this.CLOCKNUMBERS[Character.getNumericValue(textTime.charAt(4))])).getBitmap(), clockwidth, clockheight, false);
            canvas.drawBitmap(resizeminute2Bitmap, (((((float) resizehour1Bitmap.getWidth()) + setclock) + ((float) resizehour2Bitmap.getWidth())) + ((float) resizecolonBitmap.getWidth())) + ((float) resizeminute1Bitmap.getWidth()), clocktop, null);
            canvas.drawBitmap(resizecolonBitmap, ((((((float) resizehour1Bitmap.getWidth()) + setclock) + ((float) resizehour2Bitmap.getWidth())) + ((float) resizecolonBitmap.getWidth())) + ((float) resizeminute1Bitmap.getWidth())) + ((float) resizeminute2Bitmap.getWidth()), clocktop, null);
            Bitmap resizeseconds1Bitmap = Bitmap.createScaledBitmap(((BitmapDrawable) resources.getDrawable(this.CLOCKNUMBERS[Character.getNumericValue(textTime.charAt(6))])).getBitmap(), clockwidth, clockheight, false);
            canvas.drawBitmap(resizeseconds1Bitmap, (((((((float) resizehour1Bitmap.getWidth()) + setclock) + ((float) resizehour2Bitmap.getWidth())) + ((float) resizecolonBitmap.getWidth())) + ((float) resizeminute1Bitmap.getWidth())) + ((float) resizeminute2Bitmap.getWidth())) + ((float) resizecolonBitmap.getWidth()), clocktop, null);
            Bitmap resizeseconds2Bitmap = Bitmap.createScaledBitmap(((BitmapDrawable) resources.getDrawable(this.CLOCKNUMBERS[Character.getNumericValue(textTime.charAt(7))])).getBitmap(), clockwidth, clockheight, false);
            canvas.drawBitmap(resizeseconds2Bitmap, ((((((((float) resizehour1Bitmap.getWidth()) + setclock) + ((float) resizehour2Bitmap.getWidth())) + ((float) resizecolonBitmap.getWidth())) + ((float) resizeminute1Bitmap.getWidth())) + ((float) resizeminute2Bitmap.getWidth())) + ((float) resizecolonBitmap.getWidth())) + ((float) resizeseconds1Bitmap.getWidth()), clocktop, null);
            canvas.drawBitmap(Bitmap.createScaledBitmap(((BitmapDrawable) resources.getDrawable(this.AMPM[ampm])).getBitmap(), clockwidth, clockheight, false), (((((((((float) resizehour1Bitmap.getWidth()) + setclock) + ((float) resizehour2Bitmap.getWidth())) + ((float) resizecolonBitmap.getWidth())) + ((float) resizeminute1Bitmap.getWidth())) + ((float) resizeminute2Bitmap.getWidth())) + ((float) resizecolonBitmap.getWidth())) + ((float) resizeseconds1Bitmap.getWidth())) + ((float) resizeseconds2Bitmap.getWidth()), clocktop, null);
        }

        private final int[] CLOCKNUMBERS = {
                R.drawable.n0,
                R.drawable.n1,
                R.drawable.n2,
                R.drawable.n3,
                R.drawable.n4,
                R.drawable.n5,
                R.drawable.n6,
                R.drawable.n7,
                R.drawable.n8,
                R.drawable.n9,
        };

        private final int[] AMPM = {
                R.drawable.am,
                R.drawable.pm,
        };


        ////weather stuff
        public void weathericon(Canvas canvas) {
            Resources resources = WatchFace.this.getResources();
            Bitmap mIconBitmap;
            Bitmap mIconResizedBitmap;
            Log.i(TAG, "in weathericon() Temp: " + temp + " icon: " + icon + " Weather: " + weather);
            int res = getResources().getIdentifier(icon, "drawable", getPackageName());
            Log.i(TAG, "in weathericon() getPackageName(): " + getPackageName());
            Log.i(TAG, "in weathericon() res: " + res);
            Drawable IconDrawable = resources.getDrawable(res);
            mIconBitmap = ((BitmapDrawable) IconDrawable).getBitmap();
            if (!roundwatch) {
                mIconResizedBitmap = Bitmap.createScaledBitmap(mIconBitmap, 100, 70, false);
            } else {
                mIconResizedBitmap = Bitmap.createScaledBitmap(mIconBitmap, 150, 120, false);
            }
            if (!roundwatch) {
                canvas.drawBitmap(mIconResizedBitmap, 90, 160, null);
            } else {
                canvas.drawBitmap(mIconResizedBitmap, width / 2f -75, height / 2f + 18, null);
            }

        }


    }
}

