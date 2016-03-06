package ca.uwaterloo.lab3_203_33.app;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.view.*;
import android.widget.LinearLayout;
import ca.uwaterloo.lab3_203_33.app.mapper.MapLoader;
import ca.uwaterloo.lab3_203_33.app.mapper.MapView;
import ca.uwaterloo.lab3_203_33.app.mapper.PedometerMap;

import java.io.File;

/**
 * Created by jycyq on 2016-03-06.
 */
public class MapViewFragment extends Fragment {

    // Member variable for the actual map view that the fragment will be containing
    MapView mapView;
    /**
     * Returns a new instance of this fragment.
     */
    public static MapViewFragment newInstance(Context context) {
        MapViewFragment fragment = new MapViewFragment();
        return fragment;
    }

    public MapViewFragment() {
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_mapview, container, false);
        // Instantiate the map view (Fits a 1280x720 panel at this moment)
        mapView = new MapView(rootView.getContext(), 700, 1000, 25, 25);
        // Get map object from the MapLoader
        PedometerMap map = MapLoader.loadMap(new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "E2-3344.svg"));
        // Give the map to the MapView to display
        mapView.setMap(map);
        // Add the MapView to the fragment layout
        LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.mapview_layout);
        layout.addView(mapView);
        // Register the MapView context menu
        registerForContextMenu(mapView);

        return rootView;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        // Inflate the MapView context menu and add options
        getActivity().getMenuInflater().inflate(R.menu.mapview_menu, menu);
        mapView.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return mapView.onContextItemSelected(item);
    }

}
