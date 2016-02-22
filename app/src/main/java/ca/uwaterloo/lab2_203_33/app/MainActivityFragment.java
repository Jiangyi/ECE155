package ca.uwaterloo.lab2_203_33.app;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    // Stores the peak value of a cycle
    private float peakVal = 0;
    // Variable to store how many steps were taken
    private int stepCounter = 0;
    private int stepCounterRelaxed = 0;
    // Boolean for pause function
    private boolean isPaused = true;
    // Variable to store current state, used with FSM
    private State currentState = State.REST;
    // Variable to store current state for the relaxed FSM
    private State currentStateRelaxed = State.REST;
    // Arraylist of all data points gathered
    private ArrayList<Float> dataPoints = new ArrayList<Float>();
    // TextView that will be updated with step counter info
    private TextView stepCounterLabel, stepCounterLabelRelaxed;

    // Enum that contains the 5 different states of the FSM
    public enum State {
        REST,
        RISE,
        FALL,
        SMALL_RISE,
        SMALL_FALL
    }

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_main, container, false);
        // Set up the step counter TextView
        stepCounterLabel = (TextView) rootView.findViewById(R.id.step_counter_label);
        stepCounterLabelRelaxed = (TextView) rootView.findViewById(R.id.step_counter_relaxed_label);

        // Set up the reset button
        Button resetButton = (Button) rootView.findViewById(R.id.reset_button);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetStepCounter();
            }
        });

        // Set up the pause button
        final Button pauseButton = (Button) rootView.findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPaused = !isPaused;
                pauseButton.setText(isPaused ? getString(R.string.start) : getString(R.string.pause));
            }
        });

        // Set up the sensors
        setUpSensors(rootView.getContext());
        return rootView;
    }

    // Resets the step counter to its initial state
    private void resetStepCounter() {
        // Reset the counter itself and update the TextView
        stepCounter = 0;
        stepCounterRelaxed = 0;
        stepCounterLabel.setText(String.format(getResources().getQuantityString(R.plurals.step_counter, stepCounter), stepCounter));
        stepCounterLabelRelaxed.setText(String.format(getResources().getQuantityString(R.plurals.step_counter_relaxed, stepCounterRelaxed), stepCounterRelaxed));

        // Clear out the arraylist holding the data points
        dataPoints.clear();
        dataPoints.add(0.0f);

        // Set current state to rest
        setCurrentState(State.REST);
        setCurrentStateRelaxed(State.REST);
    }

    // Set up the sensor here
    private void setUpSensors(Context context) {
        // Reset the step counter; This will give us a clean slate to start with
        resetStepCounter();
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
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

    // Set current state and prints it to logcat
    private void setCurrentStateRelaxed(State state) {
        currentStateRelaxed = state;
        Log.d("CurrentStateRelaxed", state.toString());
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

    // Increments the tightened step counter, updates the TextView label, and logs the increment action to logcat
    private void incrementStepCounter() {
        Log.d("Step", "INCREMENTED");
        stepCounter++;
        stepCounterLabel.setText(String.format(getResources().getQuantityString(R.plurals.step_counter, stepCounter), stepCounter));
    }

    // Increments the relaxed step counter, updates the TextView label, and logs the increment action to logcat
    private void incrementStepCounterRelaxed() {
        Log.d("Step (Relaxed)", "INCREMENTED");
        stepCounterRelaxed++;
        stepCounterLabelRelaxed.setText(String.format(getResources().getQuantityString(R.plurals.step_counter_relaxed, stepCounterRelaxed), stepCounterRelaxed));
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

    // This is where the action (AKA the Finite State Machine logic) happens;
    // Update the state based on currentState and trends in the data
    private void updateState() {
        switch (currentState) {
            case REST:
                // Set state to rise if data is within the tolerance range and is increasing;
                // This takes care of anybody who's trying to shake the phone around
                if (1.75 < dataPoints.get(dataPoints.size() - 1)
                        && dataPoints.get(dataPoints.size() - 1) < 7
                        && isDataTrending(true, dataPoints.size() - 7, dataPoints.size() - 1)) {
                    setCurrentState(State.RISE);
                }
                break;
            case RISE:
                // Set state to FALL when the data starts decreasing, and also record the peak value
                if (dataPoints.get(dataPoints.size() - 1) <= dataPoints.get(dataPoints.size() - 2)) {
                    setCurrentState(State.FALL);
                    peakVal = dataPoints.get(dataPoints.size() - 2);
                }
                break;
            case FALL:
                // If the data is increasing again, set state to small rise
                if (isDataTrending(true, dataPoints.size() - 4, dataPoints.size() - 1)) {
                    setCurrentState(State.SMALL_RISE);
                }
                break;
            case SMALL_RISE:
                // Check if data is starting to decrease again
                if (dataPoints.get(dataPoints.size() - 1) <= dataPoints.get(dataPoints.size() - 2)) {
                    // Check if dataPoint is above the peak value from before or if the slope of recent data is less than 0.01
                    if (dataPoints.get(dataPoints.size() - 2) > peakVal
                            || (getSlope(dataPoints.get(dataPoints.size() - 5), dataPoints.get(dataPoints.size() - 3), 2) < 0.01)) {
                        // If so, it wasn't a small rise that had occurred; Start over
                        setCurrentState(State.REST);
                    } else {
                        // Else, proceed onto the small fall state
                        setCurrentState(State.SMALL_FALL);
                    }
                }
                break;
            case SMALL_FALL:
                // If the slope is positive and greater than 0.06, then data is increasing again; Things are not right, reset to rest state
                if (getSlope(dataPoints.get(dataPoints.size() - 4), dataPoints.get(dataPoints.size() - 1), 3) > 0.06) {
                    setCurrentState(State.REST);
                }

                // If the datapoint value drops below 1, increment the counter and reset the state to rest;
                // WE'VE DONE IT! (Probably, subject to errors )-: )
                if (dataPoints.get(dataPoints.size() - 1) < 1) {
                    incrementStepCounter();
                    setCurrentState(State.REST);
                }
                break;
        }
    }


    // This is where the action (AKA the Finite State Machine logic) for the relaxed counter happens;
    // Update the state based on currentState and trends in the data
    private void updateStateRelaxed() {
        switch (currentStateRelaxed) {
                // If the data is increasing again, set state to small rise
            case REST:
                // Set state to rise if data is within the tolerance range and is increasing;
                // This takes care of anybody who's trying to shake the phone around
                if (1.75 < dataPoints.get(dataPoints.size() - 1) && dataPoints.get(dataPoints.size() - 1) < 7 && isDataTrending(true, dataPoints.size() - 7, dataPoints.size() - 1)) {
                    setCurrentStateRelaxed(State.RISE);
                }
                break;
            case RISE:
                // Set state to FALL when the data starts decreasing
                if (dataPoints.get(dataPoints.size() - 1) <= dataPoints.get(dataPoints.size() - 2)) {
                    setCurrentStateRelaxed(State.FALL);
                }
                break;
            case FALL:
                // If the data is increasing again, set state to small rise
                if (isDataTrending(true, dataPoints.size() - 4, dataPoints.size() - 1)) {
                    setCurrentStateRelaxed(State.SMALL_RISE);
                }
                break;
            case SMALL_RISE:
                // Check if data is decreasing again
                if (dataPoints.get(dataPoints.size() - 1) <= dataPoints.get(dataPoints.size() - 2)) {
                    // Check the y value of the small rise peak
                    if (dataPoints.get(dataPoints.size() - 2) > 3) {
                        // Not a small rise; Start over
                        setCurrentStateRelaxed(State.REST);
                    } else {
                        // Else, move on to the small fall stage
                        setCurrentStateRelaxed(State.SMALL_FALL);
                    }
                }
                break;
            case SMALL_FALL:
                // If the datapoint value drops below 1, increment the counter and reset the state to rest;
                // WE'VE DONE IT! (Probably, subject to errors )-: )
                if (dataPoints.get(dataPoints.size() - 1) < 1) {
                    incrementStepCounterRelaxed();
                    setCurrentStateRelaxed(State.REST);
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
            if (isAdded() && !isPaused && se.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                // Variable to use for mathematical calculations
                float pytha = 0;
                for (int i = 0; i < 3; i++) {
                    // Perform pythagorean theorem on the X Y Z vectors
                    pytha += se.values[i] * se.values[i];
                }
                // Square root the result as to get a scalar sum of the three vectors
                pytha = (float) Math.sqrt(pytha);
                // Feed the result through the low pass filter, and add it to the data array
                dataPoints.add(lowPassFilter(pytha, dataPoints.get(dataPoints.size() - 1), 0.20f));

                // Once the data array has a sufficient initial result, start updating the state
                // (This is honestly more to prevent any ArrayIndexOutOfBoundsExceptions than anything heh.)
                if (dataPoints.size() > 15) {
                    updateState();
                    updateStateRelaxed();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not implemented, do nothing
        }
    }
}