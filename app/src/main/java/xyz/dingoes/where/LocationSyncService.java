package xyz.dingoes.where;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by siddhant on 22/07/17.
 */

public class LocationSyncService extends Service {

    FusedLocationProviderClient mFusedLocationProviderClient;
    LocationCallback locationCallback;
    String BASE_URL = "";
    CountDownTimer fcmCheckTimer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            BASE_URL = getString(R.string.BASE_URL);
            initializeLocationManager();
            startLocationUpdates();

            if(!getSharedPreferences(getString(R.string.preferences_main), Context.MODE_PRIVATE).getBoolean("fcm_synced", false)) {
                fcmCheckTimer = new CountDownTimer(10000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                        String email = getSharedPreferences(getString(R.string.preferences_main), Context.MODE_PRIVATE).getString("email", "");
                        String fcm = getSharedPreferences(getString(R.string.preferences_main), Context.MODE_PRIVATE).getString("fcm", "");
                        if (!email.equalsIgnoreCase("") && !fcm.equalsIgnoreCase("")) {
                            sendFCMToken(email, fcm);
                        }
                    }

                    @Override
                    public void onFinish() {
                        if(!getSharedPreferences(getString(R.string.preferences_main), Context.MODE_PRIVATE).getBoolean("fcm_synced", false)) {
                            fcmCheckTimer.cancel();
                            fcmCheckTimer.start();
                        }
                    }
                };
                fcmCheckTimer.start();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        stopLocationUpdates();
        super.onDestroy();
    }

    /*
    * Location relation functions
    * */
    void initializeLocationManager() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(5*1000*30);
        locationRequest.setFastestInterval(10*1000);

        locationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for(Location location : locationResult.getLocations()) {
                    final JSONObject locationJSON = makeLocationJson(location);
                    if(locationJSON != null) {
                        Log.d("LocationString", locationJSON.toString());
                        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
                        String url = BASE_URL + "/v1/sendLocation";


                        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                                new Response.Listener<String>() {
                                    @Override
                                    public void onResponse(String response) {
                                        // Display the first 500 characters of the response string.
                                        Log.d("Location-sync", response);
                                    }
                                }, new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Log.d("Location-sync", error.toString());
                            }

                        }){
                            @Override
                            protected Map<String, String> getParams() throws AuthFailureError {
                                Map<String,String> params = new HashMap<String, String>();
                                String email = getSharedPreferences(getString(R.string.preferences_main), Context.MODE_PRIVATE).getString("email", "");
                                params.put("user_id", email);
                                params.put("location_json", locationJSON.toString());
                                return params;
                            }
                        };
                        queue.add(stringRequest);
                    }
                }
                super.onLocationResult(locationResult);
            }
        };
        mFusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    void stopLocationUpdates() {
        mFusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }


    /*
    * Helper functions
    * */
    JSONObject makeLocationJson(Location location) {
        try {
            JSONObject locationDetails;
            locationDetails = new JSONObject();
            locationDetails.put("latitude", location.getLatitude());
            locationDetails.put("longitude", location.getLongitude());
            locationDetails.put("speed", location.getSpeed());
            locationDetails.put("time", location.getTime());
            locationDetails.put("bearing", location.getBearing());
            return locationDetails;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }


    void sendFCMToken(final String email, final String fcmID) {
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        String url = BASE_URL + "/v1/registerGcm";

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        Log.d("Location-sync", response);
                        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preferences_main),
                                Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putBoolean("fcm_synced", true);
                        editor.apply();
                        fcmCheckTimer.cancel();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("Location-sync", error.toString());
            }

        }){
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String,String> params = new HashMap<String, String>();
                params.put("user_id", email);
                params.put("gcm", fcmID);
                return params;
            }
        };
        queue.add(stringRequest);
    }
}
