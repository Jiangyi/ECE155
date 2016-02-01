package ca.uwaterloo.lab1_203_33.app;

import android.graphics.Color;
import android.hardware.SensorEvent;
import android.support.v4.app.Fragment;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import org.w3c.dom.Text;

import java.util.Arrays;

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
    protected TextView[] xyzTextLabels = new TextView[3];
    // Array for the 3 series on the line graph for each of the axes
    protected LineGraphSeries<DataPoint> graphSeries[] = new LineGraphSeries[3];
    // Array to keep track of the max values on each of the axes
    protected float[] maxValues = new float[]{0, 0, 0};

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
        for (int i = 0; i < 3; i++) {
            graphSeries[i] = new LineGraphSeries<DataPoint>(new DataPoint[] {new DataPoint(prevXValue, 0)});
            graph.addSeries(graphSeries[i]);
        }
        graphSeries[X].setColor(Color.RED);
        graphSeries[Y].setColor(Color.BLUE);
        graphSeries[Z].setColor(Color.GREEN);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(MAX_X_VALUE);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getLegendRenderer().setVisible(true);

        // Get the text labels to display info for the 3 different axis
        xyzTextLabels[X] = (TextView) rootView.findViewById(R.id.x_label);
        xyzTextLabels[Y] = (TextView) rootView.findViewById(R.id.y_label);
        xyzTextLabels[Z] = (TextView) rootView.findViewById(R.id.z_label);

        // Set up the reset button
        Button resetButton = (Button) rootView.findViewById(R.id.reset_button);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < 3; i++) {
                    // Reset the graph on the data
                    graphSeries[i].resetData(new DataPoint[] {new DataPoint(prevXValue, 0)});
                    // Reset the max values
                    maxValues[i] = 0;
                }
            }
        });

        // Set up the sensor event listener
        SensorEventListener listener = new xyzSensorEventListener();
        MainActivity.getSensorManager().registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        return rootView;
    }

    private class xyzSensorEventListener implements SensorEventListener {

        // Name labels for each of the axes
        private String axisLabels[] = new String[]{"X", "Y", "Z"};

        // Event listener constructor, passes in the line graph series as a parameter
        private xyzSensorEventListener(LineGraphSeries<DataPoint>... mSeries) {
        }

        @Override
        public void onSensorChanged(SensorEvent se) {
            // Update the interface only if the fragment is currently attached to the activity
            // and the sensor type is the one we want
            if (isAdded() && se.sensor.getType() == sensorType) {
                // For loop to update the 3 axes
                for (int i = 0; i < 3; i++) {
                    // Set the max value to be the current value if the current value is larger
                    if (Math.abs(se.values[i]) > maxValues[i]) {
                        maxValues[i] = Math.abs(se.values[i]);
                    }
                    // Append the new sensor reading to the graph
                    graphSeries[i].appendData(new DataPoint(++prevXValue, se.values[i]), true, MAX_X_VALUE);
                    // Update the labels accordingly to reflect the updated information
                    xyzTextLabels[i].setText(String.format(getString(R.string.xyz_data_label), axisLabels[i], se.values[i], maxValues[i], unit));
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Not implemented, do nothing
        }
    }
}
