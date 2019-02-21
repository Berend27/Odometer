package com.berend.odometer;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Binder;
import android.support.v4.content.ContextCompat;


public class OdometerService extends Service {

    private LocationListener listener;
    private LocationManager locManager;

    private final IBinder binder = new OdometerBinder();

    // these variables are static so that their values are retained when the service is destroyed
    private static double distanceInMeters;
    private static Location lastLocation = null;

    public static final String PERMISSION_STRING
            = Manifest.permission.ACCESS_FINE_LOCATION;

    // Steps to get location updates (can be done after the listener's instantiation)
    // 1. Create a location manager
    // 2. Check for permission
    // 3. Specify the location provider with a String
    // 4. Request location updates
    // 5. Remove the updates in onDestroy() after getting permission again
    @Override
    public void onCreate() {
        super.onCreate();
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // code to keep track of distance
                if (lastLocation == null) {  // set the user's starting location
                    lastLocation = location;
                }
                distanceInMeters += location.distanceTo(lastLocation);
                lastLocation = location;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        // now instantiate a LocationManager
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Location Permission Check (required for devices with API Level 23 or higher)
        if (ContextCompat.checkSelfPermission(this, PERMISSION_STRING)
                == PackageManager.PERMISSION_GRANTED)
        {
            String provider = locManager.getBestProvider(new Criteria(), true);
            if (provider != null) {
                locManager.requestLocationUpdates(provider, 1000, 1, listener);
            }
        }
    }

    // Implementing IBinder, since onBind(Intent) needs an IBinder to return
    public class OdometerBinder extends Binder {
        OdometerService getOdometer() {
            return OdometerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public double getDistance() {
        return this.distanceInMeters / 1609.344;  // miles
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (locManager != null && listener != null) {
            if (ContextCompat.checkSelfPermission(this, PERMISSION_STRING)
                == PackageManager.PERMISSION_GRANTED)
            {
                locManager.removeUpdates(listener);
            }
            // set the LocationManager and LocationListener variables to null to save resources
            locManager = null;
            listener = null;
        }
    }
}
