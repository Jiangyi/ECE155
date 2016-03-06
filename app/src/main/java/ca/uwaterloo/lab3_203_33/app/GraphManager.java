package ca.uwaterloo.lab3_203_33.app;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

/**
 * Created by Jiangyi on 2016-01-31.
 * A general fragment class that encompasses a graph view, and will serve as the basis for many of the fragments in this lab
 */
public class GraphManager {

    // Declare constants for better code readability
    public static final int X = 0;
    public static final int Y = 1;
    public static final int Z = 2;
    public static final int PYTHA = 3;
    private static final int MAX_X_VALUE = 100;

    // Member variables that will be used by classes which inherit this one, along with the sensor listener
    // Graph that we will be displaying
    private GraphView graph;
    // Variable used to update the graph properly
    private int prevXValue = 0;
    // Array for the 3 series on the line graph for each of the axes
//    protected LineGraphSeries<DataPoint> graphSeries[] = new LineGraphSeries[4];
    private LineGraphSeries<DataPoint> graphSerie = new LineGraphSeries<DataPoint>(new DataPoint[]{new DataPoint(prevXValue, 0)});

    public GraphManager(GraphView view) {
        graph = view;
        setUpGraph();
    }

    private void setUpGraph() {
        // Set up the graph
        // NOTE: The LineGraphView class included on LEARN is outdated; Use one from:
        // http://www.android-graphview.org/
        // For loop to create the 4 series and to add them to the graph
//        for (int i = 0; i < 4; i++) {
//            graphSeries[i] = new LineGraphSeries<DataPoint>(new DataPoint[] {new DataPoint(prevXValue, 0)});
//            graph.addSeries(graphSeries[i]);
//        }
//        graphSeries[X].setColor(Color.RED);
//        graphSeries[Y].setColor(Color.BLUE);
//        graphSeries[Z].setColor(Color.GREEN);
//        graphSeries[PYTHA].setColor(Color.GRAY);
        graph.addSeries(graphSerie);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(MAX_X_VALUE);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getLegendRenderer().setVisible(true);


        StepCounterManager.getInstance().registerListener(new StepCounterManager.StepCounterListener() {
            @Override
            public void onStepChanged(int stepCounter) {
                // Do nothing
            }

            @Override
            public void onDataPointAdded(float dataPoint) {
                appendPoint(dataPoint);
            }
        });
    }

//    public void appendPoint(float... values) {
//        for (int i = 0; i < graphSeries.length; i++) {
//            prevXValue++;
//            // Append the new sensor reading to the graph
//            graphSeries[i].appendData(new DataPoint(prevXValue, values[i]), true, MAX_X_VALUE);
//        }
//    }

    public void appendPoint(float value) {
        // Append the new sensor reading to the graph
        graphSerie.appendData(new DataPoint(++prevXValue, value), true, MAX_X_VALUE);
    }

    public void resetGraph() {
        prevXValue = 0;
        graphSerie.resetData(new DataPoint[]{new DataPoint(prevXValue, 0)});
    }
}
