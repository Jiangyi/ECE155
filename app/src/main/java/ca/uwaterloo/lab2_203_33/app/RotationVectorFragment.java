package ca.uwaterloo.lab2_203_33.app;

import android.content.Context;
import android.hardware.Sensor;
import android.os.Bundle;

/**
 * Created by Jiangyi on 2016-01-31.
 */
public class RotationVectorFragment extends GraphViewFragment {
    /**
     * Returns a new instance of this fragment.
     */
    public static RotationVectorFragment newInstance(Context context) {
        RotationVectorFragment fragment = new RotationVectorFragment();
        // Use a bundle to attach extra arguments to the fragment,
        // including what type of sensor we want, and what the unit of measurement is.
        // This is so that everything in GraphViewElement will be set up properly.
        Bundle args = new Bundle();
        args.putInt("sensorType", Sensor.TYPE_ROTATION_VECTOR);
        args.putString("unit", context.getString(R.string.unit_rotation_vector));
        fragment.setArguments(args);
        return fragment;
    }

    public RotationVectorFragment() {
    }

}
