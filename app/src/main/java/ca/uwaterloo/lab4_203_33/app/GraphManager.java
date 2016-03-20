package ca.uwaterloo.lab4_203_33.app;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

/**
 * Created by Jiangyi on 2016-01-31.
 * A general fragment class that encompasses a graph view, and will serve as the basis for many of the fragments in this lab
 */
public class GraphManager {

    // Declare constant for better code readability
    private static final int MAX_X_VALUE = 100;

    // Graph that we will be displaying
    private GraphView graph;
    // Variable used to update the graph properly
    private int prevXValue = 0;
    // The graph serie that we will be displaying
    private LineGraphSeries<DataPoint> graphSerie = new LineGraphSeries<DataPoint>(new DataPoint[]{new DataPoint(prevXValue, 0)});

    public GraphManager(GraphView view) {
        graph = view;
        setUpGraph();
    }

    private void setUpGraph() {
        // Set up the graph
        // NOTE: The LineGraphView class included on LEARN is outdated; Use one from:
        // http://www.android-graphview.org/
        graph.addSeries(graphSerie);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(MAX_X_VALUE);
        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getLegendRenderer().setVisible(true);
        graphSerie.setTitle("Lin. Acc. w/LPF");
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

        // Register a listener on the step counter
        StepCounterManager.getInstance().registerListener(new StepCounterManager.StepCounterListener() {
            @Override
            public void onStepChanged(int stepCounter) {
                // Do nothing
            }

            @Override
            public void onDataPointAdded(float dataPoint) {
                // Add a point every time the step counter processes a new filtered point
                appendPoint(dataPoint);
            }
        });
    }

    public void appendPoint(float value) {
        // Append the new sensor reading to the graph
        graphSerie.appendData(new DataPoint(++prevXValue, value), true, MAX_X_VALUE);
    }

    public void resetGraph() {
        // Reset the graph to 0-state
        prevXValue = 0;
        graphSerie.resetData(new DataPoint[]{new DataPoint(prevXValue, 0)});
    }
}
