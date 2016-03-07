package ca.uwaterloo.lab3_203_33.app;

import android.content.Context;
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

    // Member variables for the three managers required
    private GraphManager graphManager;
    private StepCounterManager stepCounterManager;
    private OrientationManager orientationManager;

    // Member variables to store the necessary data
    private float eastDisplacement, northDisplacement;
    private float angle;

    private Button pauseButton;
    // TextViews that will be updated with step counter info
    private TextView stepCounterLabel, eastDisplacementLabel, northDisplacementLabel, angleLabel;
    // Used to make this a singleton class
    private static StepDisplacementFragment stepDisplacementFragment = null;

    /**
     * Returns a new instance of this fragment should the fragment not be initialized previously
     * Else, return the previous instantiated fragment.
     */
    public static StepDisplacementFragment newInstance(Context context) {
        if (stepDisplacementFragment == null) {
            stepDisplacementFragment = new StepDisplacementFragment();
        }
        return stepDisplacementFragment;
    }

    public StepDisplacementFragment() {
        super();
    }

    // Helper method to reset everything back to a clean slate
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
        // Set the app to pause
        super.onPause();
        isPaused = true;
        pauseButton.setText(isPaused ? getString(R.string.button_start) : getString(R.string.button_pause));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_step_displacement, container, false);

        // Instantiate a new GraphManager and ask it to handle the graph within this fragment
        graphManager = new GraphManager((GraphView) rootView.findViewById(R.id.graph));

        // Set up the step counter TextView
        stepCounterLabel = (TextView) rootView.findViewById(R.id.step_counter_label);
        eastDisplacementLabel = (TextView) rootView.findViewById(R.id.displacement_east_label);
        northDisplacementLabel = (TextView) rootView.findViewById(R.id.displacement_north_label);
        angleLabel = (TextView) rootView.findViewById(R.id.angle);

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

        // Call the step counter manager and register a listener on it
        stepCounterManager = StepCounterManager.getInstance();
        stepCounterManager.registerListener(new StepCounterManager.StepCounterListener() {
            @Override
            public void onStepChanged(int stepCounter) {
                // Calculate the component of the steps from the angle
                eastDisplacement += Math.sin(angle);
                northDisplacement += Math.cos(angle);
                // Update the step counter and displacement labels when the steps registered have changed
                stepCounterLabel.setText(String.format(getString(R.string.step_counter), stepCounter));
                eastDisplacementLabel.setText(String.format(getString(R.string.displacement_east), eastDisplacement));
                northDisplacementLabel.setText(String.format(getString(R.string.displacement_north), northDisplacement));
            }

            @Override
            public void onDataPointAdded(float dataPoint) {
                // Do nothing
            }
        });

        // Call the orientation manager and register a listener on it
        orientationManager = OrientationManager.getInstance();
        orientationManager.registerListener(new OrientationManager.OrientationListener() {
            @Override
            public void onOrientationChanged(float azimuth) {
                // When the orientation has changed, convert and normalize the direction to
                // degrees between 0 and 360
                long degrees = (Math.round(Math.toDegrees(azimuth)) + 360) % 360;
                // Then store the angle in radians
                angle = (float) Math.toRadians(degrees);
                angleLabel.setText(String.format(getString(R.string.angle), angle));

            }
        });

        // Call reset state to ensure everything is a clean slate before starting operations
        resetState();
        return rootView;
    }
}