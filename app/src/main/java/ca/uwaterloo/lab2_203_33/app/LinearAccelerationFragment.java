package ca.uwaterloo.lab2_203_33.app;

import android.content.Context;
import android.hardware.Sensor;
import android.os.Bundle;

/**
 * Created by jycyq on 2016-02-01.
 */
public class LinearAccelerationFragment extends GraphViewFragment {

    public static LinearAccelerationFragment newInstance(Context context) {
        LinearAccelerationFragment fragment = new LinearAccelerationFragment();
        // Use a bundle to attach extra arguments to the fragment,
        // including what type of sensor we want, and what the unit of measurement is.
        // This is so that everything in GraphViewElement will be set up properly.
        Bundle args = new Bundle();
        args.putInt("sensorType", Sensor.TYPE_LINEAR_ACCELERATION);
        args.putString("unit", context.getString(R.string.unit_accelerometer));
        fragment.setArguments(args);
        return fragment;
    }
}
