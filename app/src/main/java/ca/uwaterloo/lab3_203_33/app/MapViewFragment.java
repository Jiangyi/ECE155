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
        mapView = new MapView(rootView.getContext(), 400, 400, 400, 400);
        MapLoader mapLoader = new MapLoader();
        PedometerMap map = mapLoader.loadMap(new File(Environment.getExternalStorageDirectory().getAbsolutePath()), "DCTestArea.svg");
        mapView.setMap(map);
        LinearLayout layout = (LinearLayout) rootView.findViewById(R.id.mapview_layout);
        layout.addView(mapView);

        return rootView;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        mapView.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        return super.onContextItemSelected(item) || mapView.onContextItemSelected(item);
    }

}
