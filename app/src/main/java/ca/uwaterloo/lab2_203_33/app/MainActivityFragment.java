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

    private float peakVal = 0;
    private int stepCounter = 0;
    private boolean isPaused = false;
    private State currentState = State.REST;
    private ArrayList<Float> dataPoints = new ArrayList<Float>();
    private TextView stepCounterLabel;

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
        stepCounterLabel = (TextView) rootView.findViewById(R.id.step_counter_label);
        Button resetButton = (Button) rootView.findViewById(R.id.reset_button);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetStepCounter();
            }
        });
        final Button pauseButton = (Button) rootView.findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPaused = !isPaused;
                pauseButton.setText(isPaused ? getString(R.string.unpause) : getString(R.string.pause));
            }
        });

        setUpSensors(rootView.getContext());
        return rootView;
    }


    private void resetStepCounter() {
        stepCounter = 0;
        stepCounterLabel.setText(String.format(getResources().getQuantityString(R.plurals.step_counter, stepCounter), stepCounter));
        dataPoints.clear();
        dataPoints.add(0.0f);
        setCurrentState(State.REST);
    }
    private void setUpSensors(Context context) {
        // Set up the sensor here
        SensorManager sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        SensorEventListener listener = new StepCounterEventListener();
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        resetStepCounter();
    }

    private float getSlope(float y1, float y2, int deltaX) {
        return (y2 - y1) / deltaX;
    }

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

    private void incrementStepCounter() {
        Log.d("Step", "INCREMENTED");
        stepCounter++;
        stepCounterLabel.setText(String.format(getResources().getQuantityString(R.plurals.step_counter, stepCounter), stepCounter));
    }


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

    private void updateState() {
        switch (currentState) {
            case REST:
                if (1.75 < dataPoints.get(dataPoints.size() - 1)
                        && dataPoints.get(dataPoints.size() - 1) < 7.7
                        && isDataTrending(true, dataPoints.size() - 4, dataPoints.size() - 1)) {
                    setCurrentState(State.RISE);
                }
                break;
            case RISE:
//                Log.e("STATE", "RISE");
                if (dataPoints.get(dataPoints.size() - 1) <= dataPoints.get(dataPoints.size() - 2)) {
                    setCurrentState(State.FALL);
                    peakVal = dataPoints.get(dataPoints.size() - 2);
                }
//                else if (dataPoints.size() - 1 - statePos > 20) {
//                setCurrentState(State.REST);
//                }
                break;
            case FALL:
                if (isDataTrending(true, dataPoints.size() - 3, dataPoints.size() - 1)) {
                    setCurrentState(State.SMALL_RISE);
                }
                break;
            case SMALL_RISE:
                if (dataPoints.get(dataPoints.size() - 1) <= dataPoints.get(dataPoints.size() - 2)) {
                    if (
                            dataPoints.get(dataPoints.size() - 2) > peakVal ||
                                    (getSlope(dataPoints.get(dataPoints.size() - 4), dataPoints.get(dataPoints.size() - 2), 2) < 0.01)) {
                        // Not a small rise; Start over
//                        setCurrentState(State.INVALID);
                        setCurrentState(State.REST);
                    } else {
                        setCurrentState(State.SMALL_FALL);
                    }
                }
                break;
            case SMALL_FALL:
                if (getSlope(dataPoints.get(dataPoints.size() - 4), dataPoints.get(dataPoints.size() - 1), 3) > 0.06) {
                    setCurrentState(State.REST);
                }
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
            if (isAdded() && !isPaused && se.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
                float pytha = 0;
                for (int i = 0; i < 3; i++) {
                    pytha += se.values[i] * se.values[i];
//                    dataArray[i].add(se.values[i]);
                }
                pytha = (float) Math.sqrt(pytha);
                dataPoints.add(lowPassFilter(pytha, dataPoints.get(dataPoints.size() - 1), 0.20f));
                if (dataPoints.size() > 15) {
                    updateState();
                }
//                dataArray[PYTHA].add((float) pytha);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not implemented, do nothing
        }
    }
}