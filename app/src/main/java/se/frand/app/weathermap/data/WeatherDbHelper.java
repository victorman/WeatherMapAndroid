package se.frand.app.weathermap.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import se.frand.app.weathermap.data.CityWeatherContract.CityEntry;

/**
 * Created by victorfrandsen on 11/3/15.
 */
public class WeatherDbHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;

    static final String DATABASE_NAME = "weather.db";

    public WeatherDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_CREATE_CITIES_TABLE = "CREATE TABLE " + CityWeatherContract.CityEntry.TABLE_NAME + " (" +
                CityEntry._ID + " INTEGER PRIMARY KEY," +
                CityEntry.COLUMN_CITY_NAME + " TEXT NOT NULL, " +
                CityEntry.COLUMN_COORD_LAT + " REAL NOT NULL, " +
                CityEntry.COLUMN_COORD_LNG + " REAL NOT NULL, " +
                CityEntry.COLUMN_TEMP + " REAL NOT NULL, " +
                CityEntry.COLUMN_COND_ID + " INTEGER NOT NULL" +
                " );";

        db.execSQL(SQL_CREATE_CITIES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + CityEntry.TABLE_NAME);
        onCreate(db);
    }
}
