package se.frand.app.weathermap.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

import se.frand.app.weathermap.R;
import se.frand.app.weathermap.Util;
import se.frand.app.weathermap.data.CityWeatherContract.CityEntry;

/**
 * Created by victorfrandsen on 11/3/15.
 */
public class WeatherSyncAdapter extends AbstractThreadedSyncAdapter {
    private final String LOG_TAG = WeatherSyncAdapter.class.getSimpleName();

    // Interval at which to sync with the weather, in seconds.
    // 60 seconds (1 minute) * 180 = 3 hours
    public static final int SYNC_INTERVAL = 60 * 180;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int WEATHER_NOTIFICATION_ID = 3004;



    public WeatherSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    public WeatherSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {

        Log.d(LOG_TAG, "Starting sync");

        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        String weatherJsonStr = null;

        LatLng preferredLocation = Util.getPreferredLocation(getContext());
        Double latitude = preferredLocation.latitude;
        Double longitude = preferredLocation.longitude;
        int numCities = 10;

        try {
            final String BASE_URL = "http://api.openweathermap.org/data/2.5/find?";
            final String LAT = "lat";
            final String LNG = "lon";
            final String CITIES_PARAM = "cnt";
            final String UNITS = "units";
            final String APPID = "APPID";

            Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                    .appendQueryParameter(LAT, Double.toString(latitude))
                    .appendQueryParameter(LNG, Double.toString(longitude))
                    .appendQueryParameter(CITIES_PARAM,Integer.toString(numCities))
                    .appendQueryParameter(UNITS,"imperial")
                    .appendQueryParameter(APPID, getContext().getString(R.string.weather_api_key))
                    .build();

            URL url = new URL(builtUri.toString());

            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();

            if(inputStream == null) {
                return;
            }

            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while((line = reader.readLine()) != null) {
                buffer.append(line + "\n");
            }

            if(buffer.length() == 0) {
                return;
            }

            weatherJsonStr = buffer.toString();
            getWeatherDataFromJson(weatherJsonStr, preferredLocation);
        } catch (IOException e) {

            Log.e(LOG_TAG, "Error ", e);
        } catch (JSONException e) {

            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } finally {

            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
    }

    private void getWeatherDataFromJson(String weatherJsonStr, LatLng preferredLocation)
            throws JSONException {

        final String LIST = "list";
        final String CITY_NAME = "name";
        final String COORD = "coord";
        final String MAIN = "main";
        final String CITY_LAT = "lat";
        final String CITY_LNG = "lon";
        final String TEMP = "temp";
        final String WEATHER = "weather";
        final String COND = "id";
        final String ID = "id";

        try {
            JSONObject weatherJson = new JSONObject(weatherJsonStr);
            JSONArray weatherArray = weatherJson.getJSONArray(LIST);

            Vector<ContentValues> cValVector =  new Vector<ContentValues>(weatherArray.length());

            for(int i = 0; i < weatherArray.length(); i++) {

                Double lng;
                Double lat;
                Double temp;
                String name;
                int cond;
                int id;

                //iterate over each city in our weather list and sync the data;
                JSONObject cityObject = weatherArray.getJSONObject(i);

                //get city name;
                name = cityObject.getString(CITY_NAME);
                //get id
                id = cityObject.getInt(ID);

                // get latitude and longitude
                JSONObject coordObject = cityObject.getJSONObject(COORD);

                lat = coordObject.getDouble(CITY_LAT);
                lng = coordObject.getDouble(CITY_LNG);

                // get weather condition
                JSONObject weatherObject = cityObject.getJSONArray(WEATHER).getJSONObject(0);

                cond = weatherObject.getInt(COND);

                // get city temperature
                JSONObject mainObject = cityObject.getJSONObject(MAIN);

                temp = mainObject.getDouble(TEMP);

                ContentValues weatherValues = new ContentValues();

                weatherValues.put(CityEntry._ID,id);
                weatherValues.put(CityEntry.COLUMN_CITY_NAME,name);
                weatherValues.put(CityEntry.COLUMN_COND_ID,cond);
                weatherValues.put(CityEntry.COLUMN_COORD_LAT,lat);
                weatherValues.put(CityEntry.COLUMN_COORD_LNG,lng);
                weatherValues.put(CityEntry.COLUMN_TEMP,temp);

                cValVector.add(weatherValues);
            }
            if ( cValVector.size() > 0 ) {
                ContentValues[] cvArray = new ContentValues[cValVector.size()];
                cValVector.toArray(cvArray);
                ContentResolver resolver = getContext().getContentResolver();
                resolver.delete(CityEntry.CONTENT_URI,null,null);
                resolver.bulkInsert(CityEntry.CONTENT_URI, cvArray);
                resolver.notifyChange(CityEntry.CONTENT_URI,null);
            }

            Log.d(LOG_TAG, "Sync Complete. " + cValVector.size() + " Inserted");
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }

    }



    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
        }
    }

    /**
     * Helper method to have the sync adapter sync immediately
     * @param context The context used to access the account service
     */
    public static void syncImmediately(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority), bundle);
    }

    /**
     * Helper method to get the fake account to be used with SyncAdapter, or make a new one
     * if the fake account doesn't exist yet.  If we make a new account, we call the
     * onAccountCreated method so we can initialize things.
     *
     * @param context The context used to access the account service
     * @return a fake account.
     */
    public static Account getSyncAccount(Context context) {
        // Get an instance of the Android account manager
        AccountManager accountManager =
                (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);

        // Create the account type and default account
        Account newAccount = new Account(
                context.getString(R.string.app_name), context.getString(R.string.sync_account_type));

        // If the password doesn't exist, the account doesn't exist
        if ( null == accountManager.getPassword(newAccount) ) {

        /*
         * Add the account and account type, no password or user data
         * If successful, return the Account object, otherwise report an error.
         */
            if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
                return null;
            }
            /*
             * If you don't set android:syncable="true" in
             * in your <provider> element in the manifest,
             * then call ContentResolver.setIsSyncable(account, AUTHORITY, 1)
             * here.
             */

            onAccountCreated(newAccount, context);
        }
        return newAccount;
    }

    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
//        WeatherSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);

        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        syncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {
        getSyncAccount(context);
    }
}
