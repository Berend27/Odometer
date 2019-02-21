package com.berend.odometer;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Locale;

// This activity needs to do four things to work with a bound service
// 1. Create a ServiceConnection
// 2. Bind the activity to the service
// 3. Interact with the service
// 4. Unbind from the service when you've finished with it

// This activity should also check permissions at runtime, issue a permission request dialog
// if necessary, and then issue a notification if permission is denied

public class MainActivity extends AppCompatActivity {

    // for recording a reference to the service
    private OdometerService odometer;

    // variable for whether or not the activity is bound to the service
    private boolean bound = false;


    private final int PERMISSION_REQUEST_CODE  = 698;  // arbitrary permission request code
    private final int NOTIFICATION_ID = 1423;  // arbitrary
    public static final String CHANNEL = "1";  // arbitrary

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            OdometerService.OdometerBinder odometerBinder =
                    (OdometerService.OdometerBinder) binder;
            odometer = odometerBinder.getOdometer();
            bound = true;  // the activity is bound to the service
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bound = false;  // since the activity is no longer bound to OdometerService
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        displayDistance();    // interacting with the service
    }

    // binding to the service in onStart()
    // request permission if it has not already been granted
    // else create an Intent to that service, then use the Intent in a call to bindService
    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, OdometerService.PERMISSION_STRING)
            != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{OdometerService.PERMISSION_STRING},
                    PERMISSION_REQUEST_CODE);
        }
        else {  // permission has already been granted
            Intent intent = new Intent(this, OdometerService.class);
            bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    // the requestPermissions() method makes an Asynchronously (on a different thread).
    // This means that you can't just check its return value like an ordinary method
    // Instead you use the onRequestPermissionsResult() method
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults)
    {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:  // the code matches the one used in requestPermissions
            {
                if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(this, OdometerService.class);
                    bindService(intent, connection, Context.BIND_AUTO_CREATE);
                } else {
                    // issue a notification if permission was denied
                    // first create a manager and a channel
                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    {
                        NotificationChannel channel
                                = new NotificationChannel(CHANNEL, "arbitrary",
                                NotificationManager.IMPORTANCE_HIGH);
                        manager.createNotificationChannel(channel);
                    }

                    // create a notification builder
                    NotificationCompat.Builder builder
                            = new NotificationCompat.Builder(this, CHANNEL)
                            .setSmallIcon(android.R.drawable.ic_menu_compass)
                            .setContentTitle(getResources().getString(R.string.app_name))
                            .setContentText(getResources().getString(R.string.permission_denied))
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setVibrate(new long[] {1000, 1000})
                            .setAutoCancel(true);

                    // create an action
                    Intent actionIntent = new Intent(this, MainActivity.class);
                    PendingIntent actionPendingIntent = PendingIntent.getActivity(
                            this,
                            0,
                            actionIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT);
                    builder.setContentIntent(actionPendingIntent);

                    // Issue the notification
                    manager.notify(NOTIFICATION_ID, builder.build());
                }
            }
        }
    }

    // this method interacts with the service by calling one of its methods
    // A handler.post(new Runnable()) statement is used to call the method once a second
    private void displayDistance() {
        final TextView distanceView = (TextView) findViewById(R.id.distance);
        final Handler handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                double distance = 0.0;
                if (bound && odometer != null) {
                    distance = odometer.getDistance();
                }
                String distanceStr = String.format(Locale.getDefault(),
                        "%1$,.2f miles", distance);
                distanceView.setText(distanceStr);
                handler.postDelayed(this, 1000);
            }
        });
    }

    // unbind from the service in onStop since the binding happened in onStart()
    @Override
    protected void onStop() {
        super.onStop();
        if (bound) {
            unbindService(connection);
            bound = false;
        }
    }
}
