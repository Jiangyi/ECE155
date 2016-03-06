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
public class OrientationManager implements SensorEventListener {

    private float[] rotationMatrix = new float[16];
    private LinkedList<OrientationListener> listenerList = new LinkedList<OrientationListener>();
    private static OrientationManager orientationManager = null;

    public static OrientationManager getInstance() {
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
        // Get rotation vector sensor
        Sensor rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        // Register the listener and request the least amount of delay possible
        sensorManager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_NORMAL);
        // Initialize matrix to identity matrix
        rotationMatrix[0] = 1;
        rotationMatrix[4] = 1;
        rotationMatrix[8] = 1;
        rotationMatrix[12] = 1;
    }

    public interface OrientationListener {
        void onOrientationChanged(float azimuth);
    }

    public void registerListener(OrientationListener listener) {
        listenerList.add(listener);
    }

    @Override
    public void onSensorChanged(SensorEvent se) {
        if (!StepDisplacementFragment.isPaused && se.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, se.values);
            float orientation[] = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientation);

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
