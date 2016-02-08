package ca.uwaterloo.lab2_203_33.app;

import android.content.Context;
import android.hardware.Sensor;
import android.os.Bundle;

/**
 * Created by Jiangyi on 2016-01-31.
 */
public class MagneticSensorFragment extends GraphViewFragment {
    /**
     * Returns a new instance of this fragment.
     */
    public static MagneticSensorFragment newInstance(Context context) {
        MagneticSensorFragment fragment = new MagneticSensorFragment();
        // Use a bundle to attach extra arguments to the fragment,
        // including what type of sensor we want, and what the unit of measurement is.
        // This is so that everything in GraphViewElement will be set up properly.
        Bundle args = new Bundle();
        args.putInt("sensorType", Sensor.TYPE_MAGNETIC_FIELD);
        args.putString("unit", context.getString(R.string.unit_magnetic_sensor));
        fragment.setArguments(args);
        return fragment;
    }

    public MagneticSensorFragment() {
    }

}
