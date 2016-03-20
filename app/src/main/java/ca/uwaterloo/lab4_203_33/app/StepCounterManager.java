package ca.uwaterloo.lab4_203_33.app;

/**
 * Created by jycyq on 2016-03-05.
 */

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * A placeholder fragment containing a simple view.
 */
public class StepCounterManager {

    // Variable to store how many steps were taken
    private int stepCounter = 0;
    // Variable to store current state, used with FSM
    private State currentState = State.REST;
    // Arraylist of all data points gathered
    private ArrayList<Float> dataPoints = new ArrayList<Float>();
    private LinkedList<StepCounterListener> listenerList = new LinkedList<StepCounterListener>();
    private static StepCounterManager stepCounterManager = null;

    // Enum that contains the 5 different states of the FSM
    public enum State {
        REST,
        RISE,
        FALL,
        SMALL_RISE,
        SMALL_FALL
    }

    public static StepCounterManager getInstance() {
        // If the manager has not been instantiated before, instantiate it
        if (stepCounterManager == null) {
            stepCounterManager = new StepCounterManager();
        }
        return stepCounterManager;
    }

    private StepCounterManager() {
        setUpSensors();
    }

    // Method to register a listener on the manager
    public void registerListener(StepCounterListener listener) {
        listenerList.add(listener);
    }

    // Returns the current step count
    public int getStepCounter() {
        return stepCounter;
    }

    // Resets the step counter to its initial state
    public void resetStepCounter() {
        // Reset the counter itself and update the TextView
        stepCounter = 0;

        // Clear out the arraylist holding the data points
        dataPoints.clear();
        dataPoints.add(0.0f);

        // Set current state to rest
        setCurrentState(State.REST);
    }

    // Set up the sensor here
    private void setUpSensors() {
        // Reset the step counter; This will give us a clean slate to start with
        resetStepCounter();
        SensorManager sensorManager = MainActivity.getSensorManager();
        // Get linear acceleration sensor
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        SensorEventListener listener = new StepCounterEventListener();
        // Register the listener and request the least amount of delay possible
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    // Gets the slope between two points given the Y values and the X distance between them
    private float getSlope(float y1, float y2, int deltaX) {
        return (y2 - y1) / deltaX;
    }

    // Set current state and prints it to logcat
    private void setCurrentState(State state) {
        currentState = state;
        Log.d("CurrentState", state.toString());
    }

    // Return RC low-pass filter output samples, given input samples,
    // and alpha constant
    private float[] lowPassFilter(float[] in, float alpha) {
        float[] out = new float[in.length];
        out[0] = in[0];
        for (int i = 1; i < out.length; i++) {
            out[i] = alpha * in[i] + (1 - alpha) * out[i - 1];
        }
        return out;
    }

    // Return RC low-pass filter output value, given current and past input values,
    // and alpha constant
    private float lowPassFilter(float current, float past, float alpha) {
        return alpha * current + (1 - alpha) * past;
    }

    // Increments the step counter, updates the TextView label, and logs the increment action to logcat
    private void incrementStepCounter() {
        Log.d("Step", "INCREMENTED");
        stepCounter++;
        for (StepCounterListener listener : listenerList) {
            if (listener != null) {
                listener.onStepChanged(stepCounter);
            }
        }
    }

    // Check if the data is trending (Either increasing or decreasing), given sections of the dataPoints arraylist
    private boolean isDataTrending(boolean checkRising, int start, int end) {
        for (int i = start; i < end; i++) {
            if (dataPoints.get(i) > dataPoints.get(i + 1)) {
                if (checkRising) {
                    return false;
                } else {
                    continue;
                }
            } else if (dataPoints.get(i) < dataPoints.get(i + 1)) {
                if (checkRising) {
                    continue;
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    // This is where the action (AKA the Finite State Machine logic) for the counter happens;
    // Update the state based on currentState and trends in the data
    private void updateState() {
        switch (currentState) {
            // If the data is increasing again, set state to small rise
            case REST:
                // Set state to rise if data is within the tolerance range and is increasing;
                // This takes care of anybody who's trying to shake the phone around
                if (1.75 < dataPoints.get(dataPoints.size() - 1) && dataPoints.get(dataPoints.size() - 1) < 7 && isDataTrending(true, dataPoints.size() - 7, dataPoints.size() - 1)) {
                    setCurrentState(State.RISE);
                }
                break;
            case RISE:
                // Set state to FALL when the data starts decreasing
                if (dataPoints.get(dataPoints.size() - 1) <= dataPoints.get(dataPoints.size() - 2)) {
                    setCurrentState(State.FALL);
                }
                break;
            case FALL:
                // If the data is increasing again, set state to small rise
                if (isDataTrending(true, dataPoints.size() - 4, dataPoints.size() - 1)) {
                    setCurrentState(State.SMALL_RISE);
                }
                break;
            case SMALL_RISE:
                // Check if data is decreasing again
                if (dataPoints.get(dataPoints.size() - 1) <= dataPoints.get(dataPoints.size() - 2)) {
                    // Check the y value of the small rise peak
                    if (dataPoints.get(dataPoints.size() - 2) > 3) {
                        // Not a small rise; Start over
                        setCurrentState(State.REST);
                    } else {
                        // Else, move on to the small fall stage
                        setCurrentState(State.SMALL_FALL);
                    }
                }
                break;
            case SMALL_FALL:
                // If the datapoint value drops below 1, increment the counter and reset the state to rest;
                // WE'VE DONE IT! (Probably, subject to errors )-: )
                if (dataPoints.get(dataPoints.size() - 1) < 1) {
                    incrementStepCounter();
                    setCurrentState(State.REST);
                }
                break;
        }
    }

    private class StepCounterEventListener implements SensorEventListener {

        private StepCounterEventListener() {
            // Do nothing
        }

        @Override
        public void onSensorChanged(SensorEvent se) {
            // Update the interface only if the sensor type is the one we want
            if (MapViewFragment.isPathSet() && se.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                // Variable to use for mathematical calculations
                float pytha = 0;
                for (int i = 0; i < 3; i++) {
                    // Perform pythagorean theorem on the X Y Z vectors
                    pytha += se.values[i] * se.values[i];
                }
                // Square root the result as to get a scalar sum of the three vectors
                pytha = (float) Math.sqrt(pytha);

                // Feed the result through the low pass filter, and add it to the data array
                float datapoint = lowPassFilter(pytha, dataPoints.get(dataPoints.size() - 1), 0.20f);
                dataPoints.add(datapoint);

                // Notify all registered listeners
                for (StepCounterListener listener : listenerList) {
                    if (listener != null) {
                        listener.onDataPointAdded(datapoint);
                    }
                }
                // Once the data array has a sufficient initial result, start updating the state
                // (This is honestly more to prevent any ArrayIndexOutOfBoundsExceptions than anything heh.)
                if (dataPoints.size() > 15) {
                    updateState();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not implemented, do nothing
        }
    }

    // Interface for other components to receive data change notices
    public interface StepCounterListener {
        void onStepChanged(int stepCounter);

        void onDataPointAdded(float dataPoint);
    }
}