package xyz.dingoes.where;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class OverlayService extends Service {

    private WindowManager windowManager;
    private View floatingView;
    private String name = "", id = "";

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


        floatingView = LayoutInflater.from(this).inflate(R.layout.overlay_layout, null);
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.LEFT;        //Initially view will be added to top-left corner
        params.x = 0;
        params.y = 100;

        //Add the view to the window
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
            }
        });

        ((Button)floatingView.findViewById(R.id.negative_button)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSelf();
            }
        });
    }
}
