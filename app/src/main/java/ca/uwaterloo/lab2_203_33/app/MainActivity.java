package ca.uwaterloo.lab2_203_33.app;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;

public class MainActivity extends AppCompatActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks {

    // Declare a static sensor manager that the fragments may get from the getter
    private static SensorManager sensorManager;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        StepCounterManager.getInstance();
        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

    }

    @Override
    public void onNavigationDrawerItemSelected(int position) {
        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = null;
        // Select the proper fragment based on user selection, and update the title accordingly
        switch (position) {
            case 0:
                mTitle = getString(R.string.title_linear_acceleration_collector);
                fragment = DataCollectorFragment.newInstance();
                break;
            case 1:
                mTitle = getString(R.string.title_light);
                fragment = LightSensorFragment.newInstance();
                break;
            case 2:
                mTitle = getString(R.string.title_magnetic);
                fragment = MagneticSensorFragment.newInstance(this);
                break;
            case 3:
                mTitle = getString(R.string.title_rotation);
                fragment = RotationVectorFragment.newInstance(this);
                break;
            case 4:
                mTitle = getString(R.string.title_accelerometer);
                fragment = AccelerometerFragment.newInstance(this);
                break;
            case 5:
                mTitle = getString(R.string.title_linear_acceleration);
                fragment = LinearAccelerationFragment.newInstance(this);
                break;
        }
        fragmentManager.beginTransaction().replace(R.id.container, fragment).commit();

    }

    public void restoreActionBar() {
        // Set up/update the action bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#009688")));
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            // getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    // Getter for the sensor manager
    public static SensorManager getSensorManager() {
        return sensorManager;
    }
}
