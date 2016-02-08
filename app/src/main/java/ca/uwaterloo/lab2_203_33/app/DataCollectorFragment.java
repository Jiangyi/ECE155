package ca.uwaterloo.lab2_203_33.app;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.StringBuilderPrinter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.jjoe64.graphview.series.DataPoint;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by jycyq on 2016-02-07.
 */
public class DataCollectorFragment extends Fragment {
    // Member variables that will be used by the sensor listener
    private boolean isPaused = false;
    private static Sensor sensor;
    private ArrayList<Float>[] dataArray = new ArrayList[4];

    protected static final int X = 0;
    protected static final int Y = 1;
    protected static final int Z = 2;
    protected static final int PYTHA = 3;
    /**
     * Returns a new instance of this fragment.
     */
    public static DataCollectorFragment newInstance() {
        DataCollectorFragment fragment = new DataCollectorFragment();
        // Set up the sensor here
        sensor = MainActivity.getSensorManager().getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        return fragment;
    }

    public DataCollectorFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_data_collection, container, false);

        for (int i = 0; i < dataArray.length; i++) {
            dataArray[i] = new ArrayList<Float>();
        }
        // Set up the listener for the sensor event
        SensorEventListener listener = new DataCollectorEventListener();
        MainActivity.getSensorManager().registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST);
        // Set up the reset graph button
        Button resetButton = (Button) rootView.findViewById(R.id.reset_button);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            for (int i = 0; i < dataArray.length; i++) {
                dataArray[i].clear();
            }
            }
        });

        // Set up the pause button
        Button pauseButton = (Button) rootView.findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPaused = !isPaused;
            }
        });


        Button saveButton = (Button) rootView.findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeToFile();
            }
        });
        return rootView;
    }

    private void writeToFile() {
        String filename = "data";
        String filenameWithFilter = "data_filtered";
        int counter = 0;
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilderPrinter printer = new StringBuilderPrinter(stringBuilder);
        for (int i = 0; i < dataArray[PYTHA].size(); i++) {
            printer.println(i + "," + dataArray[X].get(i) + "," + dataArray[Y].get(i) + "," + dataArray[Z].get(i) + "," + dataArray[PYTHA].get(i));
        }

        FileOutputStream outputStream;
        File outFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), filename + ".csv");
        while (outFile.exists()) {
            outFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), filename + ++counter + ".csv");
        }
        try {
            outputStream = new FileOutputStream(outFile);
            outputStream.write(stringBuilder.toString().getBytes());
            Toast.makeText(getActivity(), "file saved to: " +
                    outFile.toString(), Toast.LENGTH_LONG).show();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class DataCollectorEventListener implements SensorEventListener {

        private DataCollectorEventListener() {
            // Do nothing
        }

        @Override
        public void onSensorChanged(SensorEvent se) {
            // Update the interface only if the fragment is currently attached to the activity
            // and the sensor type is the one we want
            if (isAdded() && se.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION && !isPaused) {
                double pytha = 0;
                for (int i = 0; i < 3; i++) {
                    pytha += se.values[i] * se.values[i];
                    dataArray[i].add(se.values[i]);
                }
                pytha = Math.sqrt(pytha);
                dataArray[PYTHA].add((float) pytha);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not implemented, do nothing
        }
    }
}
