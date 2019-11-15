package io.crossbar.crossbarfxmarkets;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

import java.util.Arrays;
import java.util.List;

public class DeviceActivityReceiver extends BroadcastReceiver {

    private static final List<Integer> TYPES = Arrays.asList(
            DetectedActivity.STILL,
            DetectedActivity.WALKING,
            DetectedActivity.RUNNING,
            DetectedActivity.IN_VEHICLE,
            DetectedActivity.ON_BICYCLE
    );


    @Override
    public void onReceive(Context context, Intent intent) {
        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        if (result == null) {
            return;
        }
        for (DetectedActivity probableActivity : result.getProbableActivities()) {
            if (TYPES.contains(probableActivity.getType())) {
                if (probableActivity.getType() == DetectedActivity.STILL) {
                    System.out.println("Device became idle, lets stop location");
                    // Stop location updates
                } else {
                    System.out.println("Device became idle, lets stop location");
                    // start location updates
                }
                break;
            }
        }
    }
}
