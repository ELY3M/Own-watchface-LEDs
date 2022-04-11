package own.ownwatchfaceleds;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Settings {
    public static final String TAG = "ownwatchface";
    public static final int gpsinterval = 60000;
    public static final String KEY_CLOCK_SIZE = "clock_size";
    public static final String KEY_CLOCK_ACT = "clock_act";
    public static final String KEY_CLOCK_DIM = "clock_dim";
    public static final String KEY_CLOCK_NOSECS_SIZE = "clock_nosecs_size";
    public static final String KEY_CLOCK_NOSECS_ACT = "clock_nosecs_act";
    public static final String KEY_CLOCK_NOSECS_DIM = "clock_nosecs_dim";
    public static final String KEY_MARKER_SIZE = "marker_size";
    public static final String KEY_MARKER_ACT = "marker_act";
    public static final String KEY_MARKER_DIM = "marker_dim";
    public static final String KEY_DATE_SIZE = "date_size";
    public static final String KEY_DATE_DIM = "date_dim";
    public static final String KEY_TIME_SIZE = "time_size";
    public static final String KEY_TIME_DIM = "time_dim";
    public static final String KEY_TEMPONRIGHT = "temponright";
    public static final String KEY_ALWAYS_UTC = "always_utc";
    public static final String KEY_SHOW_TIME = "show_time";
    public static final String KEY_NORTHERNHEMI = "northernhemi";
    public static final String KEY_ROUNDWATCH = "roundwatch";
    public static final String KEY_LAT = "lat";
    public static final String KEY_LON = "lon";
    public static final String KEY_TEMP = "temp";
    public static final String KEY_ICON = "icon";
    public static final String KEY_WEATHER = "weather";
    public static final String KEY_TEMP_SIZE = "temp_size";
    public static final String KEY_TEMP_DIM = "temp_dim";
    public static final String KEY_WEATHER_SIZE = "weather_size";
    public static final String KEY_WEATHER_DIM = "weather_dim";
    public static final String PATH_CONFIG = "/ownwatchfaceleds/Config/";
    public static final String PATH_WEATHER_INFO = "/ownwatchfaceleds/WeatherInfo";
    public static final String PATH_WEATHER_REQUIRE = "/ownwatchfaceleds/Require";



    public static String getString(final Context context, final String key, final String defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key, defaultValue);
    }

    public static int getInt(final Context context, final String key, final int defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(key, defaultValue);
    }

    public static boolean getBoolean(final Context context, final String key,
                                     final boolean defaultValue) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key, defaultValue);
    }

    public static void setString(final Context context, final String key, final String value) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static void setInt(final Context context, final String key, final int value) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public static void setBoolean(final Context context, final String key, final boolean value) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }




}
