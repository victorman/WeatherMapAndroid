package se.frand.app.weathermap;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import se.frand.app.weathermap.data.CityWeatherContract.CityEntry;
import se.frand.app.weathermap.sync.WeatherSyncAdapter;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String LOG_TAG = MapsActivity.class.getSimpleName();

    private GoogleMap mMap;
    private GpsLocation gpsLocation;
    private TextView gpsStatusTextView;
    private ContentResolver resolver;


    private static final String[] projection = new String[] {
            CityEntry._ID,
            CityEntry.COLUMN_CITY_NAME,
            CityEntry.COLUMN_TEMP,
            CityEntry.COLUMN_COORD_LAT,
            CityEntry.COLUMN_COORD_LNG
    };

    private static final int REQUEST_CODE = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        resolver = getContentResolver();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQUEST_CODE
            );
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,

     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        LatLng location = Util.getPreferredLocation(this);

        mMap.moveCamera(CameraUpdateFactory.newLatLng(location));
        final Handler handler = new Handler();

        final ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(CityEntry.CONTENT_URI, true, new ContentObserver(handler) {
            @Override
            public void onChange(boolean selfChange) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        addMarkers();
                    }
                });
            }
        });

        addMarkers();

    }

    private void addMarkers() {
        Cursor cursor = resolver.query(CityEntry.CONTENT_URI, projection, null, null, null);

        while (cursor.moveToNext()) {
            double temp = cursor.getDouble(cursor.getColumnIndex(CityEntry.COLUMN_TEMP));
            String name = cursor.getString(cursor.getColumnIndex(CityEntry.COLUMN_CITY_NAME));

            LatLng pinlocation = new LatLng(
                    (double) cursor.getFloat(cursor.getColumnIndex(CityEntry.COLUMN_COORD_LAT)),
                    (double) cursor.getFloat(cursor.getColumnIndex(CityEntry.COLUMN_COORD_LNG))
            );

            mMap.addMarker(new MarkerOptions().position(pinlocation).title(name + ":  " + temp));
        }


        cursor.close();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case REQUEST_CODE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    gpsLocation = new GpsLocation(this, gpsStatusTextView);

                    double latitude = gpsLocation.getLatitude();
                    double longitude = gpsLocation.getLongitude();

                    SharedPreferences preferences =  getSharedPreferences(
                            getString(R.string.preference_file_key), Context.MODE_PRIVATE);

                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putFloat(getString(R.string.pref_latitude_key),(float)latitude);
                    editor.putFloat(getString(R.string.pref_longitude_key), (float) longitude);
                    editor.commit();

                    WeatherSyncAdapter.initializeSyncAdapter(this);
                } else {
                    finish();
                }
                break;
        }
    }
}
