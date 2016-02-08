package ca.uwaterloo.lab2_203_33.app;

import android.content.Context;
import android.graphics.Color;
import android.hardware.SensorEvent;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.StringBuilderPrinter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Jiangyi on 2016-01-31.
 * A general fragment class that encompasses a graph view, and will serve as the basis for many of the fragments in this lab
 */
public abstract class GraphViewFragment extends Fragment {

    // Declare constants for better code readability
    protected static final int X = 0;
    protected static final int Y = 1;
    protected static final int Z = 2;
    protected static final int MAX_X_VALUE = 200;

    // Member variables that will be used by classes which inherit this one, along with the sensor listener
    // The sensor object associated with what we're getting data from
    protected Sensor sensor;
    // Graph that we will be displaying
    protected GraphView graph;
    // Type of sensor we're reading
    protected int sensorType;
    // The unit the sensor data is in
    protected String unit;
    // Variable used to update the graph properly
    protected int prevXValue = 0;
    // Array for the 3 labels for each axes
    protected TextView[] xyzTextLabels = new TextView[4];
    // Array for the 3 series on the line graph for each of the axes
    protected LineGraphSeries<DataPoint> graphSeries[] = new LineGraphSeries[4];

    // Array to keep track of the max values on each of the axes
    protected float[] maxValues = new float[]{0, 0, 0, 0};
    protected float[] minValues = new float[]{0, 0, 0, 0};
    // Name labels for each of the axes
    protected String axisLabels[] = new String[]{"X", "Y", "Z", "Pytha"};

    protected boolean isPaused = false;

    public GraphViewFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_graphview, container, false);
        this.sensorType = getArguments().getInt("sensorType"); // Get sensor type
        this.unit = getArguments().getString("unit"); // Get the unit the sensor reading is in
        sensor = MainActivity.getSensorManager().getDefaultSensor(sensorType); // Get the specified sensor

        // Set up the graph
        // NOTE: The LineGraphView class included on LEARN is outdated; Use one from:
        // http://www.android-graphview.org/
        graph = (GraphView) rootView.findViewById(R.id.graph);

        // For loop to create the 3 axes and to add them to the graph
        for (int i = 0; i < 4; i++) {
            graphSeries[i] = new LineGraphSeries<DataPoint>(new DataPoint[] {new DataPoint(prevXValue, 0)});
            graphSeries[i].setTitle(axisLabels[i]);
            graph.addSeries(graphSeries[i]);
        }
        graphSeries[X].setColor(Color.RED);
        graphSeries[Y].setColor(Color.BLUE);
        graphSeries[Z].setColor(Color.GREEN);
        graphSeries[3].setColor(Color.BLACK);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(MAX_X_VALUE);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

        // Get the text labels to display info for the 3 different axis
        xyzTextLabels[X] = (TextView) rootView.findViewById(R.id.x_label);
        xyzTextLabels[Y] = (TextView) rootView.findViewById(R.id.y_label);
        xyzTextLabels[Z] = (TextView) rootView.findViewById(R.id.z_label);
        xyzTextLabels[3] = (TextView) rootView.findViewById(R.id.pytha_label);

        // Set up the reset button
        Button resetButton = (Button) rootView.findViewById(R.id.reset_button);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < 4; i++) {
                    // Reset the graph on the data
                    graphSeries[i].resetData(new DataPoint[] {new DataPoint(prevXValue, 0)});
                    // Reset the max values
                    maxValues[i] = 0;
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
                writeTofile();
            }
        });

        // Set up the sensor event listener
        SensorEventListener listener = new xyzSensorEventListener();
        MainActivity.getSensorManager().registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST);

        return rootView;
    }

    private void writeTofile() {
        String filename = "data";
        int counter = 0;
        StringBuilder stringBuilder = new StringBuilder();
        StringBuilderPrinter printer = new StringBuilderPrinter(stringBuilder);
        Iterator[] itrs = new Iterator[4];
        for (int i = 0; i < itrs.length; i++) {
            itrs[i] = graphSeries[i].getValues(0, MAX_X_VALUE);
        }
        DataPoint[] tmp = new DataPoint[4];
        while (itrs[3].hasNext()) {
            for (int i = 0; i < itrs.length; i++) {
                tmp[i] = (DataPoint) itrs[i].next();
            }
            printer.println(tmp[0].getX() + "," + tmp[0].getY() + "," + tmp[1].getY() + "," + tmp[2].getY() + "," + tmp[3].getY());
        }

        FileOutputStream outputStream;
        File outFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), filename + ".csv");
        while (outFile.exists()) {
            outFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), filename + ++counter +".csv");
        }
        try {
            outputStream = new FileOutputStream(outFile);
            outputStream.write(stringBuilder.toString().getBytes());
            Toast.makeText(getActivity(),"file saved to: " +
                    outFile.toString(), Toast.LENGTH_LONG).show();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class xyzSensorEventListener implements SensorEventListener {

        // Event listener constructor, passes in the line graph series as a parameter
        private xyzSensorEventListener(LineGraphSeries<DataPoint>... mSeries) {
        }

        @Override
        public void onSensorChanged(SensorEvent se) {
            double product = 0;
            // Update the interface only if the fragment is currently attached to the activity
            // and the sensor type is the one we want
            if (isAdded() && se.sensor.getType() == sensorType && !isPaused) {
                prevXValue++;
                product = 0;
                // For loop to update the 3 axes
                for (int i = 0; i < 3; i++) {
                    product += se.values[i] * se.values[i];
                    // Set the max value to be the current value if the current value is larger
                    if (se.values[i] > maxValues[i]) {
                        maxValues[i] = se.values[i];
                    }
                    if (se.values[i] < minValues[i]) {
                        minValues[i] = se.values[i];
                    }
                    // Append the new sensor reading to the graph
                    graphSeries[i].appendData(new DataPoint(prevXValue, se.values[i]), true, MAX_X_VALUE);
                    // Update the labels accordingly to reflect the updated information
                    xyzTextLabels[i].setText(String.format(getString(R.string.xyz_data_label), axisLabels[i], se.values[i], maxValues[i], minValues[i], unit));
                }
                product = Math.sqrt(product);
                if (product > maxValues[3]) {
                    maxValues[3] = (float) product;
                }

                if (product < minValues[3]) {
                    minValues[3] = (float) product;
                }

                graphSeries[3].appendData(new DataPoint(prevXValue, product), true, MAX_X_VALUE);
                xyzTextLabels[3].setText(String.format(getString(R.string.xyz_data_label), axisLabels[3], product, maxValues[3], minValues[3], unit));
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not implemented, do nothing
        }
    }
}
