package se.frand.app.weathermap.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by victorfrandsen on 11/3/15.
 */
public class WeatherSyncService extends Service {
    private static final Object syncAdapterLock = new Object();
    private static WeatherSyncAdapter weatherSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d(WeatherSyncService.class.getSimpleName(),"onCreate()");
        //android.os.Debug.waitForDebugger();
        synchronized (syncAdapterLock) {
            if(weatherSyncAdapter == null) {
                weatherSyncAdapter = new WeatherSyncAdapter(getApplicationContext(),true);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return weatherSyncAdapter.getSyncAdapterBinder();
    }
}
