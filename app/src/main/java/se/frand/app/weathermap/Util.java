package se.frand.app.weathermap;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.maps.model.LatLng;

/**
 * Created by victorfrandsen on 11/3/15.
 */
public class Util {
    public static LatLng getPreferredLocation(Context context) {

        // TODO get city lat long from preferences
        SharedPreferences preferences = context.getSharedPreferences(
                context.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
        );
        double latitude = (double) preferences.getFloat(context.getString(R.string.pref_latitude_key), 37.3f);
        double longitude = (double) preferences.getFloat(context.getString(R.string.pref_longitude_key), -121.8f);

        return new LatLng(latitude,longitude);
    }
}
