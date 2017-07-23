package xyz.dingoes.where;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.util.HashMap;
import java.util.Map;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private String name = "", id = "";
    private String BASE_URL;
    private CountDownTimer timer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        name = intent.getExtras().getString("name");
        id = intent.getExtras().getString("id");
        ((TextView)floatingView.findViewById(R.id.askerName)).setText(name + " asked for your location history");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        BASE_URL = getString(R.string.BASE_URL);

        floatingView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null);
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.CENTER ;        //Initially view will be added to top-left corner

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(floatingView, params);

        setupUIComponents();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);
    }

    /*
    * Custom functions
    * */

    void setupUIComponents() {
        ((Button)floatingView.findViewById(R.id.affirmative_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                floatingView.findViewById(R.id.pin_layout).setVisibility(View.VISIBLE);
                floatingView.findViewById(R.id.choice_layout).setVisibility(View.GONE);

                floatingView.findViewById(R.id.pin_layout).findViewById(R.id.pin_deny_button).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                        notificationManager.cancel(0);
                        String pin = String.valueOf(((EditText)floatingView.findViewById(R.id.pin_layout).findViewById(R.id.pin_edittext))
                                .getText());
                        String pin_pref = getSharedPreferences(getString(R.string.preferences_main), Context.MODE_PRIVATE).getString("pin", "");
                        if(pin.equalsIgnoreCase(pin_pref) && !pin.equalsIgnoreCase("")) {
                            permitAction("rejected");
                            timer.cancel();
                            stopSelf();
                        }
                    }
                });
            }
        });

        ((Button)floatingView.findViewById(R.id.negative_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(0);
                permitAction("granted");
                timer.cancel();
                stopSelf();
            }
        });

        final TextView timerView = (TextView)floatingView.findViewById(R.id.timer_textview);
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(1000);
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
        timer = new CountDownTimer(30000, 1000) {
            public void onTick(long millisUntilFinished) {
                timerView.setText((millisUntilFinished / 1000) + "");
            }

            public void onFinish() {
                timerView.setText("Location history sent");
                permitAction("granted");
                stopSelf();
            }
        }.start();

    }

    void permitAction(final String permit) {
        RequestQueue queue = Volley.newRequestQueue(getApplicationContext());
        String url = BASE_URL + "/v1/locationPermit";


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
                params.put("id", id);
                params.put("permit", permit);
                return params;
            }
        };
// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

}
