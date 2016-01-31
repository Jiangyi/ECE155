package ca.uwaterloo.lab1_203_33.app;

import android.content.Context;
import android.hardware.Sensor;
import android.os.Bundle;

/**
 * Created by Jiangyi on 2016-01-31.
 */
public class AccelerometerFragment extends GraphViewFragment {
    /**
     * Returns a new instance of this fragment.
     */
    public static AccelerometerFragment newInstance(Context context) {
        AccelerometerFragment fragment = new AccelerometerFragment();
        // Use a bundle to attach extra arguments to the fragment,
        // including what type of sensor we want, and what the unit of measurement is.
        // This is so that everything in GraphViewElement will be set up properly.
        Bundle args = new Bundle();
        args.putInt("sensorType", Sensor.TYPE_ACCELEROMETER);
        args.putString("unit", context.getString(R.string.unit_accelerometer));
        fragment.setArguments(args);
        return fragment;
    }

    public AccelerometerFragment() {
        super();
    }

}
