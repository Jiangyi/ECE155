package ca.uwaterloo.lab3_203_33.app;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.LinkedList;

/**
 * Created by jycyq on 2016-03-05.
 */
public class OrientationManager {

    // Matrix for containing rotation data
    private float[] rotationMatrix = new float[16];
    // Linked list to keep track of registered listeners
    private LinkedList<OrientationListener> listenerList = new LinkedList<OrientationListener>();
    // static instance of this class for singleton pattern
    private static OrientationManager orientationManager = null;

    public static OrientationManager getInstance() {
        // Only instantiate this class if it has not been done before
        if (orientationManager == null) {
            orientationManager = new OrientationManager();
        }
        return orientationManager;
    }

    private OrientationManager() {
        setUpSensors();
    }

    // Set up the sensor here
    private void setUpSensors() {
        SensorManager sensorManager = MainActivity.getSensorManager();
        /* Accelerometer + Magnetic Field is not that accurate. Use the rotation vector virtual sensor instead.
           Rotation vector takes into account data from Accelerometer + Magnetic Field + Gyroscope (Sensor Fusion)
           at the HAL level, so we don't have to worry about mathematical and matrix manipulations as much.
           There is also no filtering needed, as the gyroscope part of the data keeps everything in check. */
        Sensor rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        // Register the listener and request the least amount of delay possible
        sensorManager.registerListener(new RotationVectorEventListener(), rotationVector, SensorManager.SENSOR_DELAY_NORMAL);
    }

    // Declare a listener interface that other sections of the code can latch on to
    public interface OrientationListener {
        void onOrientationChanged(float azimuth);
    }

    // Method for registering the listener
    public void registerListener(OrientationListener listener) {
        listenerList.add(listener);
    }

    private class RotationVectorEventListener implements SensorEventListener {
        @Override
        public void onSensorChanged(SensorEvent se) {
            // Update if we're not paused and it's the right sensor type
            if (!StepDisplacementFragment.isPaused && se.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                // The rotation vector sensor returns a vector array of values; Transform that into a rotation matrix
                SensorManager.getRotationMatrixFromVector(rotationMatrix, se.values);

                float orientation[] = new float[3];
                // Get the orientation array from the rotation matrix
                SensorManager.getOrientation(rotationMatrix, orientation);

                // Notify any listeners that the orientation has changed
                for (OrientationListener listener : listenerList) {
                    if (listener != null) {
                        listener.onOrientationChanged(orientation[0]);
                    }
                }
            }
        }


        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do nothing
        }
    }
}
