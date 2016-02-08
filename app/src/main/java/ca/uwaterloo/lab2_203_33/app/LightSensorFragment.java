package ca.uwaterloo.lab2_203_33.app;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by Jiangyi on 2016-01-31.
 * As the light sensor fragment should not display a graph, we'll separate it out into its own fragment
 */
public class LightSensorFragment extends Fragment {
    // Member variables that will be used by the sensor listener
    private TextView currentLightLabel;
    private TextView maxLightLabel;
    private static Sensor sensor;
    private float maxValue = 0;

    /**
     * Returns a new instance of this fragment.
     */
    public static LightSensorFragment newInstance() {
        LightSensorFragment fragment = new LightSensorFragment();
        // Set up the sensor here
        sensor = MainActivity.getSensorManager().getDefaultSensor(Sensor.TYPE_LIGHT);
        return fragment;
    }

    public LightSensorFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_light, container, false);
        // Get the appropriate TextView labels that we will be needing to update
        currentLightLabel = (TextView) rootView.findViewById(R.id.light_current_label);
        maxLightLabel = (TextView) rootView.findViewById(R.id.light_max_label);
        // Set up the listener for the sensor event
        SensorEventListener listener = new lightSensorEventListener();
        MainActivity.getSensorManager().registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Set up the reset graph button
        Button resetButton = (Button) rootView.findViewById(R.id.reset_button);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    // Reset the max values
                    maxValue = 0;
            }
        });
        return rootView;
    }

    // Light sensor data is a tad different, since only 1 value is interesting;
    // Hence, we'll implement its listener separately for better code clarity
    private class lightSensorEventListener implements SensorEventListener {

        private lightSensorEventListener() {
            // Do nothing
        }

        @Override
        public void onSensorChanged(SensorEvent se) {
            // Update the interface only if the fragment is currently attached to the activity
            // and the sensor type is the one we want
            if (isAdded() && se.sensor.getType() == Sensor.TYPE_LIGHT) {
                // Set the max value to be the current value if the current value is larger
                if (Math.abs(se.values[0]) > maxValue) {
                    maxValue = Math.abs(se.values[0]);
                }
                // Update the labels accordingly to reflect the updated information
                currentLightLabel.setText(String.format(getString(R.string.light_value_label), se.values[0]));
                maxLightLabel.setText(String.format(getString(R.string.light_value_max_label), maxValue));
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not implemented, do nothing
        }
    }
}
