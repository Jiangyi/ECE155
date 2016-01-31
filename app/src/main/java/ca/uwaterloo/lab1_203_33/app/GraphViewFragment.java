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

    // Member variables that will be used by classes which inherit this one, along with the sensor listener
    protected Sensor sensor;
    protected GraphView graph;
    protected int sensorType;
    protected TextView[] xyzTextLabels = new TextView[3];
    protected String unit;
    protected int prevXValue = 0;

    public GraphViewFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_graphview, container, false);
        this.sensorType = getArguments().getInt("sensorType"); // Get sensor type
        this.unit = getArguments().getString("unit"); // Get the unit the sensor reading is in
        sensor = MainActivity.getSensorManager().getDefaultSensor(sensorType); // Get the specified sensor

        // Set up the graph
        // NOTE: The LineGraphView class included on LEARN is kind of buggy; Use one from:
        // http://www.android-graphview.org/
        graph = (GraphView) rootView.findViewById(R.id.graph);
        LineGraphSeries<DataPoint> xSeries = new LineGraphSeries<DataPoint>(new DataPoint[] {new DataPoint(prevXValue, 0)});
        LineGraphSeries<DataPoint> ySeries = new LineGraphSeries<DataPoint>(new DataPoint[] {new DataPoint(prevXValue, 0)});
        LineGraphSeries<DataPoint> zSeries = new LineGraphSeries<DataPoint>(new DataPoint[] {new DataPoint(prevXValue, 0)});
        xSeries.setColor(Color.RED);
        ySeries.setColor(Color.BLUE);
        zSeries.setColor(Color.GREEN);
        graph.addSeries(xSeries);
        graph.addSeries(ySeries);
        graph.addSeries(zSeries);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(117);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getLegendRenderer().setVisible(true);

        // Get the text labels to display info for the 3 different axis
        xyzTextLabels[0] = (TextView) rootView.findViewById(R.id.x_label);
        xyzTextLabels[1] = (TextView) rootView.findViewById(R.id.y_label);
        xyzTextLabels[2] = (TextView) rootView.findViewById(R.id.z_label);

        // Set up the sensor event listener
        SensorEventListener listener = new xyzSensorEventListener(xSeries, ySeries, zSeries);
        MainActivity.getSensorManager().registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);

        return rootView;
    }

    private class xyzSensorEventListener implements SensorEventListener {
        // Array to keep track of the max values on each of the axes
        private float[] maxValues = new float[]{0, 0, 0};
        // Name labels for each of the axes
        private String axisLabels[] = new String[]{"X", "Y", "Z"};
        // Array to contain the 3 line graph series
        private LineGraphSeries<DataPoint> graphSeries[];

        // Event listener constructor, passes in the line graph series as a parameter
        private xyzSensorEventListener(LineGraphSeries<DataPoint>... mSeries) {
            graphSeries = mSeries;
        }

        @Override
        public void onSensorChanged(SensorEvent se) {
            // Update the interface only if the fragment is currently attached to the activity
            // and the sensor type is the one we want
            if (isAdded() && se.sensor.getType() == sensorType) {
                // For loop to update the 3 axes
                for (int i = 0; i < 3; i++) {
                    // Set the max value to be the current value if the current value is larger
                    if (se.values[i] > maxValues[i]) {
                        maxValues[i] = se.values[i];
                    }
                    // Append the new sensor reading to the graph
                    graphSeries[i].appendData(new DataPoint(++prevXValue, se.values[i]), true, 40);
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
