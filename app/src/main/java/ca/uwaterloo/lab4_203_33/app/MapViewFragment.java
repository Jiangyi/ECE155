package ca.uwaterloo.lab4_203_33.app;

import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.*;
import android.widget.LinearLayout;
import ca.uwaterloo.lab4_203_33.app.mapper.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by jycyq on 2016-03-06.
 */
public class MapViewFragment extends Fragment {

    // Member variable for the actual map view that the fragment will be containing
    MapView mapView;

    public enum Direction {
        NORTH, SOUTH, EAST, WEST
    };
    Direction calcDirection;
    private static final float USER_POINT_OFFSET = 0.1f;
    private static final float CHECK_WALL_OFFSET = 0.2f;
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
        boolean ret = mapView.onContextItemSelected(item);
        final PointF startPoint = mapView.getStartPoint();
        final PointF endPoint = mapView.getEndPoint();
        if (startPoint.x != 0 && startPoint.y != 0
                && endPoint.x != 0 && endPoint.y != 0) {
            List<InterceptPoint> intersectList = mapView.calculateIntersections(startPoint, endPoint);
            if (!intersectList.isEmpty()) {
                calculateUserPath(startPoint, endPoint, intersectList);
            } else {
                mapView.setUserPoint(startPoint);
                mapView.setUserPath(new ArrayList<PointF>(){{
                    add(startPoint);
                    add(endPoint);
                }});
            }
        }
        return ret;
    }

    private void calculateUserPath(PointF start, PointF end, List<InterceptPoint> intersectList) {
        ArrayList<PointF> userPath = new ArrayList<PointF>();
        userPath.add(new PointF(start.x, start.y));

        double x, y;
        x = start.x < end.x ? intersectList.get(0).getPoint().x - USER_POINT_OFFSET
                            : intersectList.get(0).getPoint().x + USER_POINT_OFFSET;
        y = intersectList.get(0).getPoint().y;
        userPath.add(new PointF((float) x, (float) y));
        calcDirection = start.x < end.x ? Direction.SOUTH
                                        : Direction.NORTH;
        while (!mapView.calculateIntersections(userPath.get(userPath.size() - 1), end).isEmpty()) {
            PointF userPoint = userPath.get(userPath.size() - 1);
            LineSegment wall = getLeftSideWall(userPoint, calcDirection);
            PointF endPoint = getWantedEndPoint(wall);

            userPath.add(getNextUserPoint(userPoint, endPoint));
        }
        userPath.add(end);
        mapView.setUserPath(userPath);
        mapView.setUserPoint(start);
    }

    private LineSegment getLeftSideWall(PointF currentPoint, Direction direction) {

        PointF currentModified, compare;
        switch (direction) {
            case NORTH:
                currentModified = new PointF(currentPoint.x, currentPoint.y - CHECK_WALL_OFFSET);
                compare = new PointF(currentModified.x - CHECK_WALL_OFFSET, currentModified.y);
                break;
            case SOUTH:
                currentModified = new PointF(currentPoint.x, currentPoint.y + CHECK_WALL_OFFSET);
                compare = new PointF(currentModified.x + CHECK_WALL_OFFSET, currentModified.y);
                break;
            case EAST:
                currentModified = new PointF(currentPoint.x + CHECK_WALL_OFFSET, currentPoint.y);
                compare = new PointF(currentModified.x, currentModified.y - CHECK_WALL_OFFSET);
                break;
            case WEST:
                currentModified = new PointF(currentPoint.x - CHECK_WALL_OFFSET, currentPoint.y);
                compare = new PointF(currentModified.x, currentModified.y + CHECK_WALL_OFFSET);
                break;
            default:
                // Will never get here
                Log.e("getLeftSideWall", "Something went wrong, this should not be seen!!!");
                currentModified = new PointF();
                compare = new PointF();
                break;
        }
        List<InterceptPoint> list = mapView.calculateIntersections(currentModified, compare);
        if (!list.isEmpty()) {
            return list.get(0).getLine();
        } else {
            return null;
        }
    }

    private PointF getWantedEndPoint(LineSegment wall) {
        float x = 0, y = 0;
        switch (calcDirection) {
            case NORTH:
                if (wall.start.y < wall.end.y) {
                    return wall.start;
                } else {
                    return wall.end;
                }
            case SOUTH:
                if (wall.start.y > wall.end.y) {
                    return wall.start;
                } else {
                    return wall.end;
                }
            case EAST:
                if (wall.start.x > wall.end.x) {
                    return wall.start;
                } else {
                    return wall.end;
                }
            case WEST:
                if (wall.start.x < wall.end.x) {
                    return wall.start;
                } else {
                    return wall.end;
                }
        }
        return null;
    }

    private PointF getNextUserPoint(PointF currentPoint, PointF endPoint) {
        PointF out;
        List<InterceptPoint> list;
        switch (calcDirection) {
            case NORTH:
                out = new PointF(currentPoint.x, endPoint.y - USER_POINT_OFFSET);
                list = mapView.calculateIntersections(currentPoint, out);
                if (!list.isEmpty()) {
                    out = list.get(0).getPoint();
                    out.y += USER_POINT_OFFSET;
                    calcDirection = Direction.EAST;
                } else {
                    calcDirection = Direction.WEST;
                }
                return out;
            case SOUTH:
                out = new PointF(currentPoint.x, endPoint.y + USER_POINT_OFFSET);
                list = mapView.calculateIntersections(currentPoint, out);
                if (!list.isEmpty()) {
                    out = list.get(0).getPoint();
                    out.y -= USER_POINT_OFFSET;
                    calcDirection = Direction.WEST;
                } else {
                    calcDirection = Direction.EAST;
                }
                return out;
            case EAST:
                out = new PointF(endPoint.x + USER_POINT_OFFSET, currentPoint.y);
                list = mapView.calculateIntersections(currentPoint, out);
                if (!list.isEmpty()) {
                    out = list.get(0).getPoint();
                    out.x -= USER_POINT_OFFSET;
                    calcDirection = Direction.SOUTH;
                } else {
                    calcDirection = Direction.NORTH;
                }
                return out;
            case WEST:
                out = new PointF(endPoint.x - USER_POINT_OFFSET, currentPoint.y);
                list = mapView.calculateIntersections(currentPoint, out);
                if (!list.isEmpty()) {
                    out = list.get(0).getPoint();
                    out.x += USER_POINT_OFFSET;
                    calcDirection = Direction.NORTH;
                } else {
                    calcDirection = Direction.SOUTH;
                }
                return out;
        }
        // Shouldn't reach here
        return null;
    }
}
