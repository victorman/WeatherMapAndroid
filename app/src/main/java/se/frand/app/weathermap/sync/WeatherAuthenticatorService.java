package se.frand.app.weathermap.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * Created by victorfrandsen on 11/3/15.
 */
public class WeatherAuthenticatorService extends Service {
    private WeatherAuthenticator weatherAuthenticator;

    @Override
    public void onCreate() {
        weatherAuthenticator = new WeatherAuthenticator(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return weatherAuthenticator.getIBinder();
    }
}
