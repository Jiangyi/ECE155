package ca.uwaterloo.lab3_203_33.app;

import android.content.Context;
import android.hardware.Sensor;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.jjoe64.graphview.GraphView;

/**
 * A placeholder fragment containing a simple view.
 */
public class StepDisplacementFragment extends Fragment {

    // Boolean for pause function
    public static boolean isPaused = true;

    private GraphManager graphManager;
    private StepCounterManager stepCounterManager;
    private OrientationManager orientationManager;
    private float eastDisplacement, northDisplacement;
    private float angle;
    private Button pauseButton;
    // TextViews that will be updated with step counter info
    private TextView stepCounterLabel, eastDisplacementLabel, northDisplacementLabel;

    /**
     * Returns a new instance of this fragment.
     */
    public static StepDisplacementFragment newInstance(Context context) {
        StepDisplacementFragment fragment = new StepDisplacementFragment();
        // Use a bundle to attach extra arguments to the fragment,
        // including what type of sensor we want, and what the unit of measurement is.
        // This is so that everything in GraphViewElement will be set up properly.
        Bundle args = new Bundle();
        args.putInt("sensorType", Sensor.TYPE_LINEAR_ACCELERATION);
        fragment.setArguments(args);
        return fragment;
    }

    public StepDisplacementFragment() {
        super();
    }

    private void resetState() {
        stepCounterManager.resetStepCounter();
        graphManager.resetGraph();
        eastDisplacement = 0;
        northDisplacement = 0;
        stepCounterLabel.setText(String.format(getString(R.string.step_counter), stepCounterManager.getStepCounter()));
        eastDisplacementLabel.setText(String.format(getString(R.string.displacement_east), eastDisplacement));
        northDisplacementLabel.setText(String.format(getString(R.string.displacement_north), northDisplacement));
    }

    @Override
    public void onPause() {
        super.onPause();
        isPaused = true;
        pauseButton.setText(isPaused ? getString(R.string.button_start) : getString(R.string.button_pause));
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_graphview, container, false);
        graphManager = new GraphManager((GraphView) rootView.findViewById(R.id.graph));
        // Set up the step counter TextView
        stepCounterLabel = (TextView) rootView.findViewById(R.id.step_counter_label);
        eastDisplacementLabel = (TextView) rootView.findViewById(R.id.displacement_east_label);
        northDisplacementLabel = (TextView) rootView.findViewById(R.id.displacement_north_label);

        // Set up the reset button
        Button resetButton = (Button) rootView.findViewById(R.id.reset_button);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetState();
            }
        });

        // Set up the pause button
        pauseButton = (Button) rootView.findViewById(R.id.pause_button);
        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPaused = !isPaused;
                pauseButton.setText(isPaused ? getString(R.string.button_start) : getString(R.string.button_pause));
            }
        });

        stepCounterManager = StepCounterManager.getInstance();
        stepCounterManager.registerListener(new StepCounterManager.StepCounterListener() {
            @Override
            public void onStepChanged(int stepCounter) {
                stepCounterLabel.setText(String.format(getString(R.string.step_counter), stepCounter));
                eastDisplacement += Math.sin(angle);
                northDisplacement += Math.cos(angle);
                eastDisplacementLabel.setText(String.format(getString(R.string.displacement_east), eastDisplacement));
                northDisplacementLabel.setText(String.format(getString(R.string.displacement_north), northDisplacement));
            }

            @Override
            public void onDataPointAdded(float dataPoint) {
                // Do nothing
            }
        });

        orientationManager = OrientationManager.getInstance();
        orientationManager.registerListener(new OrientationManager.OrientationListener() {
            @Override
            public void onOrientationChanged(float azimuth) {
                long degrees = (Math.round(Math.toDegrees(azimuth)) + 360) % 360;
                angle = (float) Math.toRadians(degrees);
            }
        });
        resetState();
        return rootView;
    }
}