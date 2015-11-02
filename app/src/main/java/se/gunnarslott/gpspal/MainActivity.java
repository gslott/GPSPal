package se.gunnarslott.gpspal;

/**
 * Created by Gunnar Slott (mail@gunnarslott.se) on 15-05-24.
 * This app is intended to be useful when traveling in small to medium sized boats mainly,
 * but can also be of use in cars etc.
 * It´ main function is to show speed, bearing and location
 * Speed can be shown in different common speed units via a setting
 *
 * NOTE 1: "Bearing" is not a compass. It´ a value calc by difference in location between times.
 * This is a very useful value when passing large open water and drifting makes a compass hard to use.
 * - "Bearing" is the direction your vessel is moving
 * - A compass shows you the pointing direction of the device itself.
 *
 * NOTE 2: Don´t be stupid. Batteries dies, phones get wet, programmers do wrong etc.
 * NEVER put your self or other in danger if your device should malfunction when using this application.
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
//import android.support.v4.view.ActionProvider;
import android.support.v7.app.ActionBarActivity;
//import android.app.ActionBar;
//import android.app.ActionBar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.support.v7.widget.ShareActionProvider;
//import android.widget.ShareActionProvider;
import android.widget.TextView;
import android.widget.Toast;

//public class MainActivity {

public class MainActivity extends ActionBarActivity {
//public class MainActivity {

    private final String TAG = "GPS Pal";

    private LocationManager lm;
    private LocationListener ll;
    private static Boolean locationProviderAvalible = false;
    private static Boolean isMoving = false;

    private ShareActionProvider mShareActionProvider;
    private Intent mShareIntent;


    private TextView tvLat;
    private TextView tvLong;
    private TextView tvSpeed;
    private static TextView tvBearing;

    private String speed_unit = "km/h";

    private double convert = 3.6; //km/h is default
    private final double[] speed_avg = {0, 0, 0};
    private BearingAnimator ba;

    private double last_known_lat = 0;
    private double last_known_long = 0;
    //SendSMSActivity sendSmsActivity = new SendSMSActivity();

    //Handle callbacks from BearingAnimator reg bearing
    public static final Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            if (locationProviderAvalible && isMoving) {
                tvBearing.setText(String.valueOf(msg.arg1) + "°");
            } else tvBearing.setText("(" + String.valueOf(msg.arg1) + "°)");

            //tvBearing.setText(String.valueOf(msg.arg1) + "°");

        }

    };

    @Override
    public void onPause() {

        super.onPause();

        Log.d(TAG, "onPause");
        isMoving = false;
        locationProviderAvalible = false;

        //Remove location listeners and destroy
        lm.removeUpdates(ll);
        ll = null;
        lm = null;

        //Stop and quit BearingAnimator
        ba.quit(true);
        ba = null;

    }

    @Override
    protected void onResume() {

        Log.d(TAG, "onResume");
        super.onResume();

        isMoving = false;
        locationProviderAvalible = false;

        //BearingAnimator with handler to handle callbacks with bearing values
        ba = new BearingAnimator();

        //Create and find TextViews
        tvLong = (TextView) findViewById(R.id.tvLong);
        tvLat = (TextView) findViewById(R.id.tvLatitude);
        tvSpeed = (TextView) findViewById(R.id.tvSpeed);
        tvBearing = (TextView) findViewById(R.id.tvBearing);


        //Recall saved preferred speed unit, km/h is default
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String temp = preferences.getString("pref_speed_unit", "kmph");
        Log.d(TAG, temp);

        assert temp != null;
        switch (temp) {
            case "mph":
                convert = 2.236936;
                        speed_unit = "mph";
                        break;
            case "knots":
                convert = 1.943844;
                            speed_unit = "kn";
                            break;
            case "mps":
                convert = (double) 1;
                        speed_unit = "m/s";
                        break;
            case "kmph":
                convert = 3.6;
                            speed_unit = "km/h";
                            break;
        }

        Log.d(TAG, speed_unit);

        //Set start values for speed and bearing
        if (locationProviderAvalible){
            tvSpeed.setText("0 " + speed_unit);
        } else tvSpeed.setText("(0 " + speed_unit + ")");

        if (isMoving && locationProviderAvalible) {
            tvBearing.setText("0°");
        } else tvBearing.setText("(0°)");

        //Get location manager and add listener
        lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        //Add location listener
        ll = new LocationListener() {


            public void onLocationChanged(Location location) {

                Log.d(TAG, "location changed");
                String format_lat, format_long;
                String long_suffix, lat_suffix;

                last_known_lat = location.getLatitude();
                last_known_long = location.getLongitude();

                //Update link to "Share my position"
                if (mShareIntent != null) {
                    mShareIntent.putExtra(Intent.EXTRA_TEXT, "http://maps.google.com/?q=" + last_known_lat + "," + last_known_long);
                }

                if (last_known_lat < 0){
                    lat_suffix = "S";
                    //last_known_lat = Math.abs(last_known_lat);
                } else lat_suffix = "N";

                if (last_known_long < 0){
                    long_suffix = "W";
                    //last_known_long = Math.abs(last_known_long);

                } else long_suffix = "E";



                //Format and print lat/long
                format_lat = Location.convert(location.getLatitude(), Location.FORMAT_SECONDS);
                format_long = Location.convert(location.getLongitude(), Location.FORMAT_SECONDS);
                Log.d(TAG, format_lat);
                Log.d(TAG, format_long);

                format_lat = format_lat.replaceFirst("-", "");
                format_lat = format_lat.replaceFirst(":", "° ");
                format_lat = format_lat.replaceFirst(":","′ ");

                CharSequence cs = ".";
                CharSequence cs2 = ",";

                if (format_lat.contains(cs)) {
                    Log.d(TAG, "Lat has period");
                    //Handle US and EU separator
                    format_lat = format_lat.replaceFirst("\\..*", "″");

                } else if (format_lat.contains(cs2)) {
                    Log.d(TAG, "Lat has comma");
                    //Handle US and EU separator
                    format_lat = format_lat.replaceFirst("\\,.*", "″");

                } else format_lat = format_lat + "″";

                format_long = format_long.replaceFirst("-","");
                format_long = format_long.replaceFirst(":","° ");
                format_long = format_long.replaceFirst(":","′ ");

                if (format_long.contains(cs)) {
                    Log.d(TAG, "Long has period");
                    //Handle US and EU separator
                    format_long = format_long.replaceFirst("\\..*", "″");

                } else if (format_long.contains(cs2)) {
                    Log.d(TAG, "Long has Comma");
                    //Handle US and EU separator
                    format_long = format_long.replaceFirst("\\,.*", "″");

                } else format_long = format_long + "″";

                tvLat.setText("Lat: " + format_lat + lat_suffix);
                tvLong.setText("Lon: " + format_long + long_suffix);

                //Print speed
                if (location.hasSpeed()) {
                    Log.d(TAG, "hasSpeed");
                    printSpeed(location.getSpeed());
                }

                //Send bearing to BearingAnimator
                if (location.hasBearing()) {
                    Log.d(TAG, "hasBearing");
                    ba.setNewBearing(location.getBearing());
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {

                if (status == LocationProvider.OUT_OF_SERVICE || status == LocationProvider.TEMPORARILY_UNAVAILABLE) {
                    if (!locationProviderAvalible) {
                        Log.d(TAG, "location onStatusChanged not available");
                        showToast(getString(R.string.GPS_not_available));
                        locationProviderAvalible = false;
                    }
                } else if (status == LocationProvider.AVAILABLE && !locationProviderAvalible){

                    Log.d(TAG, "location onStatusChanged available");
                    showToast(getString(R.string.GPS_available));
                    locationProviderAvalible = true;
                }

            }//onStatusChange

            public void onProviderEnabled(String provider) {

                Log.d(TAG, "location onProviderEnabled");
                locationProviderAvalible = true;
                showToast(getString(R.string.GPS_available));

            }//onProviderEnabled

            public void onProviderDisabled(String provider) {

                Log.d(TAG, "location onProviderDisabled");
                locationProviderAvalible = false;
                showToast(getString(R.string.GPS_not_available));

            }//onProviderDisabled

        };

        // Register the listener with the Location Manager to receive location updates
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, ll);


    }

    private void showToast(String s){

        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, s, duration);
        toast.show();
    }

    private void printSpeed(double currentSpeed) {
        //This method has a floating average calc function to make smoother changes in speed

        int avg_count = 3;
        int ii = avg_count - 1;
        int i = 0;
        double sum = 0;
        double avgCurrentSpeed;

        //Shift all values "one up" in array leaving [0] for new speed
        while (ii > 0) {
            speed_avg[ii] = speed_avg[ii - 1];
            ii--;
        }

        //Add current speed to [0]
        speed_avg[0] = currentSpeed;

        //Sum recent speeds
        while (i < avg_count) {
            sum = sum + speed_avg[i];
            i++;
        }

        //Calc avg speed
        avgCurrentSpeed = sum / avg_count;

        //Convert m/s to preferred unit, kmph is default
        double tempSpeed = avgCurrentSpeed * convert;

        //Round off, could have used Math.round as well
        tempSpeed = tempSpeed + 0.5;
        int speed = (int) tempSpeed;

       if (!locationProviderAvalible){
            tvSpeed.setText("(" + String.valueOf(speed) + " " + speed_unit + ")");

        } else if (speed == 0){
            isMoving = false;
            //Print speed to screen
            tvSpeed.setText(String.valueOf(speed) + " " + speed_unit);

        } else {
            isMoving = true;
            //Print speed to screen
            tvSpeed.setText(String.valueOf(speed) + " " + speed_unit);

        }
        //tvSpeed.setText(String.valueOf(speed) + " " + speed_unit);

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Create intent for "Share my position"
        mShareIntent = new Intent();
        mShareIntent.setAction(Intent.ACTION_SEND);
        mShareIntent.setType("text/plain");
        mShareIntent.putExtra(Intent.EXTRA_TEXT, R.string.not_found_position_yet);

        //Keep screen lit
        //TODO ADD SETTING FOR SCREEN
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Locate MenuItem for Share Action
        MenuItem share_item = menu.findItem(R.id.action_share);

        //Connect it to the ShareActionProvider
        ShareActionProvider mShareActionProvider =
                (ShareActionProvider) MenuItemCompat.getActionProvider(share_item);

        //Connect it to the intent
        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(mShareIntent);
        }


        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {

            Log.d(TAG, "settings");
            startActivity(new Intent(this, SettingsActivity.class));
            return true;

        } else if (id == R.id.action_about){

            Log.d(TAG, "showAboutDialog");
            showAboutDialog();
            return true;
/*
        } else if (id == R.id.action_send_sms){

            Log.d(TAG, "sendSMS  Lat " + Double.toString(last_known_lat) + " Long " + Double.toString(last_known_long));

            Intent i = new Intent(this, SendSMSActivity.class);

            i.putExtra("lat", last_known_lat);
            i.putExtra("long", last_known_long);

            startActivity(i);
*/
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAboutDialog() {

        //Create dialog
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();

        //Get version number
        PackageInfo pInfo = null;

        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String version = pInfo.versionName;

        String screen_size = getString(R.string.screen_size);

        //TODO add references to resourses instead of text
        alertDialog.setTitle("GPS Pal " + version);

//        alertDialog.setMessage(screen_size + " GPS Pal " + version + " is an application that shows speed, bearing and location. \nIt´s a hobby project, treat it as such.\nwww.gpspal.se");
        alertDialog.setMessage("GPS Pal " + version + " is an application that shows speed, bearing and location. \nIt´s a hobby project, treat it as such.\nwww.gpspal.se");

        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        alertDialog.show();

    }
}
