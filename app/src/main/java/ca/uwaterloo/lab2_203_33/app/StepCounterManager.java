package ca.uwaterloo.lab2_203_33.app;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

/**
 * Created by jycyq on 2016-02-13.
 */
public class StepCounterManager {

    // NOTE: Each sensor update is about 10ms from the next one
    private static StepCounterManager manager = null;
    private LinkedList<StepCounterListener> listenerList = new LinkedList<StepCounterListener>();

    public enum State {
        REST (0),
        RISE (1),
        FALL (2),
        SMALL_RISE (3),
        SMALL_FALL (4);

        private final int value;

        State(int value) {
            this.value = value;
        }
    }
//    private int statePos[] = new int[] {0,0,0,0,0};
    private int statePos = 0;
    private int stepCounter = 0;
    private State currentState = State.REST;
    private ArrayList<Float> dataPoints = new ArrayList<Float>();

    public static StepCounterManager getInstance() {
        if (manager == null) {
            manager = new StepCounterManager();
        }
        return manager;
    }

    private StepCounterManager() {
        setUpSensors();
    }

    private void setUpSensors() {
        // Set up the sensor here
        Sensor sensor = MainActivity.getSensorManager().getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        SensorEventListener listener = new StepCounterEventListener();
        MainActivity.getSensorManager().registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        dataPoints.add(0.0f);
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

    private void setCurrentState(State state) {
        currentState = state;
        statePos = dataPoints.size() - 1;
    }


    public void registerListener(StepCounterListener listener) {
        listenerList.add(listener);
    }
    private void incrementStepCounter() {
        stepCounter++;
        Log.e("STEP", "INCREMENTED");
        ListIterator<StepCounterListener> iterator = listenerList.listIterator();
        while (iterator.hasNext()) {
            iterator.next().onStepCounterIncremented();
        }
    }
    private void updateState() {
        switch (currentState) {
            case REST:
                Log.e("STATE", "REST");
                if (1.75 < dataPoints.get(dataPoints.size() - 1) && dataPoints.get(dataPoints.size() - 1) < 7 && isDataTrending(true, dataPoints.size() - 7, dataPoints.size() - 1)) {
                    setCurrentState(State.RISE);
                }
                break;
            case RISE:
                Log.e("STATE", "RISE");
                if (dataPoints.get(dataPoints.size() - 1) <= dataPoints.get(dataPoints.size() - 2)) {
                    setCurrentState(State.FALL);
                }
//                else if (dataPoints.size() - 1 - statePos > 20) {
//                setCurrentState(State.REST);
//                }
                break;
            case FALL:
                Log.e("STATE", "FALL");
                if (isDataTrending(true, dataPoints.size() - 4, dataPoints.size() - 1)) {
                    setCurrentState(State.SMALL_RISE);
                }
                break;
            case SMALL_RISE:
                Log.e("STATE", "SMALL_RISE");
                if (dataPoints.get(dataPoints.size() - 1) <= dataPoints.get(dataPoints.size() - 2)) {
                    if (dataPoints.get(dataPoints.size() - 2) > 3) {
                        // Not a small rise; Start over
                        setCurrentState(State.REST);
                    } else {
                        setCurrentState(State.SMALL_FALL);
                    }
                }
                break;
            case SMALL_FALL:
                Log.e("STATE", "SMALL_FALL");
                if (dataPoints.get(dataPoints.size() - 1) < 1) {
                    incrementStepCounter();
                    setCurrentState(State.REST);
                }
                break;
        }
    }
    // Return RC low-pass filter output samples, given input samples,
    // time interval dt, and time constant RC
    private float[] lowPassFilter(float[] in, float alpha) {
        float[] out = new float[in.length];
        out[0] = in[0];
        for (int i = 1; i < out.length; i++) {
            out[i] = alpha * in[i] + (1 - alpha) * out[i - 1];
        }
        return out;
    }

    private float lowPassFilter(float current, float past, float alpha) {
        return alpha * current + (1 - alpha) * past;
    }

    public interface StepCounterListener {
        void onStepCounterIncremented();
    }

    public int getStepsCounted() {
        return stepCounter;
    }
    private class StepCounterEventListener implements SensorEventListener {

        private StepCounterEventListener() {
            // Do nothing
        }

        @Override
        public void onSensorChanged(SensorEvent se) {
            // Update the interface only if the sensor type is the one we want
            if (se.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
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
